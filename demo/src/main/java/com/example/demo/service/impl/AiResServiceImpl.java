package com.example.demo.service.impl;

import com.example.demo.dto.AiRequest;
import com.example.demo.dto.AiRespons;
import com.example.demo.dto.Message;
import com.example.demo.exception.BusinessException;
import com.example.demo.service.AiResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class AiResServiceImpl implements AiResService {

    private final WebClient webClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    public AiResServiceImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * 服务端维护的对话上下文。
     * 当前实现保存在内存中，应用重启后会丢失，并且所有请求共享同一份上下文。
     */
    private final List<Message> messages = new ArrayList<>();

    @Override
    public synchronized String aiAgentReturn(AiRequest aiRequest) {
        log.debug("AiResService_AI接口入参： " + objectMapper.writeValueAsString(aiRequest));

        // 只把本次请求中最后一条用户消息当作当前问题，prompt 字段预留给后续业务扩展。
        String userContent = getCurrentUserContent(aiRequest);
        if (isBlank(userContent)) {
            throw new BusinessException("问题内容不能为空");
        }

        // 发送给 AI 的消息 = 历史上下文 + 本次用户问题。
        Message userMessage = buildMessage("user", userContent);
        List<Message> requestMessages = new ArrayList<>(messages);
        requestMessages.add(userMessage);

        // 重新组装请求体，避免直接把前端传入的 messages 全量当作本轮问题处理。
        AiRequest request = new AiRequest();
        request.setModel(isBlank(aiRequest.getModel()) ? "deepseek-chat" : aiRequest.getModel());
        request.setMessages(requestMessages);

        AiRespons respose = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("调用DeepSeek失败: {}", errorBody);

                                    return Mono.error(
                                            new BusinessException(
                                                    "调用AI接口失败"
                                            )
                                    );
                                })
                )
                .bodyToMono(AiRespons.class)
                .block();

        log.debug("AI接口返回值： " + respose);

        String content = respose.getChoices()
                .get(0)
                .getMessage()
                .getContent();

        // AI 成功返回后，再把本轮用户问题和 AI 回复写入上下文。
        messages.add(userMessage);
        messages.add(buildMessage("assistant", content));
        return content;
    }

    /**
     * 从请求消息中提取当前用户问题。
     * 如果前端传了多条消息，只取最后一条 role=user 的非空内容。
     */
    private String getCurrentUserContent(AiRequest aiRequest) {
        if (aiRequest == null) {
            return null;
        }
        if (aiRequest.getMessages() == null || aiRequest.getMessages().isEmpty()) {
            return null;
        }
        for (int i = aiRequest.getMessages().size() - 1; i >= 0; i--) {
            Message message = aiRequest.getMessages().get(i);
            if (message != null && "user".equals(message.getRole()) && !isBlank(message.getContent())) {
                return message.getContent();
            }
        }
        return null;
    }

    private Message buildMessage(String role, String content) {
        Message message = new Message();
        message.setRole(role);
        message.setContent(content);
        return message;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
