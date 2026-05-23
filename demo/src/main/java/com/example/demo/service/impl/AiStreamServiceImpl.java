package com.example.demo.service.impl;

import com.example.demo.dto.AiStreamRequest;
import com.example.demo.dto.Message;
import com.example.demo.exception.BusinessException;
import com.example.demo.service.AiStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class AiStreamServiceImpl implements AiStreamService {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${deepseek.api-key}")
    private String apiKey;

    private final List<Message> messages = Collections.synchronizedList(new ArrayList<>());

    public AiStreamServiceImpl(WebClient webClient, ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public Flux<String> aiAgentStream(AiStreamRequest aiRequest) {
        log.debug("【AiStreamServiceImpl#aiAgentStream】流式AI接口入参：{}", objectMapper.writeValueAsString(aiRequest));

        // 只把本次请求中最后一条 user 消息当作当前问题。
        // prompt 字段预留给后续业务扩展，不参与聊天内容处理。
        String userContent = getCurrentUserContent(aiRequest);
        if (isBlank(userContent)) {
            return Flux.error(new BusinessException("Question content must not be empty"));
        }

        // 发送给 DeepSeek 的消息由服务端历史上下文和本次问题组成。
        Message userMessage = buildMessage("user", userContent);
        List<Message> requestMessages = buildRequestMessages(userMessage);

        // 重新组装请求体，不直接转发调用方传入的 messages。
        // 这样可以避免一次 HTTP 请求里的多条 user 消息被当作多轮对话处理。
        AiStreamRequest request = new AiStreamRequest();
        request.setModel(isBlank(aiRequest.getModel()) ? "deepseek-chat" : aiRequest.getModel());
        request.setStream(true);
        request.setMessages(requestMessages);

        // 收集流式返回的片段，等流结束后再把完整 AI 回复写入上下文。
        StringBuilder assistantContent = new StringBuilder();

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class)
                                .flatMap(errorBody -> {
                                    log.error("【AiStreamServiceImpl#aiAgentStream】调用DeepSeek流式接口失败：{}", errorBody);
                                    return Mono.error(new BusinessException("Call AI stream API failed"));
                                })
                )
                .bodyToFlux(String.class)
                // DeepSeek 流式响应包含 SSE data 行，这里只向调用方输出 delta.content。
                .flatMapIterable(this::extractDeltaContents)
                .doOnNext(assistantContent::append)
                .doOnComplete(() -> {
                    log.debug("【AiStreamServiceImpl#aiAgentStream】结果：" + assistantContent.toString());
                    saveContext(userMessage, assistantContent.toString());
                });
    }

    /**
     * 发送请求前先复制当前历史上下文。
     * 本次 user 消息只追加到本次请求中，等 AI 完整回复后再正式保存到上下文。
     */
    private List<Message> buildRequestMessages(Message userMessage) {
        synchronized (messages) {
            List<Message> requestMessages = new ArrayList<>(messages);
            requestMessages.add(userMessage);
            return requestMessages;
        }
    }

    /**
     * 只在流式响应成功结束后保存本轮上下文。
     * 如果中途失败，未完成的 AI 回复不会进入后续上下文。
     */
    private void saveContext(Message userMessage, String assistantContent) {
        if (isBlank(assistantContent)) {
            return;
        }
        synchronized (messages) {
            messages.add(userMessage);
            messages.add(buildMessage("assistant", assistantContent));
        }
    }

    /**
     * 从请求消息中提取本次用户问题。
     * 如果调用方一次传入多条 user 消息，只取最后一条非空内容。
     */
    private String getCurrentUserContent(AiStreamRequest aiRequest) {
        if (aiRequest == null || aiRequest.getMessages() == null || aiRequest.getMessages().isEmpty()) {
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

    /**
     * 将原始流式片段转换成文本内容列表。
     * 空事件和最终的 [DONE] 标记会被忽略。
     */
    private List<String> extractDeltaContents(String streamChunk) {
        if (isBlank(streamChunk)) {
            return List.of();
        }

        List<String> contents = new ArrayList<>();
        for (String line : streamChunk.split("\\R")) {
            String data = line.strip();
            if (data.startsWith("data:")) {
                data = data.substring("data:".length()).strip();
            }
            if (data.isEmpty() || "[DONE]".equals(data)) {
                continue;
            }
            String content = extractDeltaContent(data);
            if (!isBlank(content)) {
                contents.add(content);
            }
        }
        //log.debug("【AiStreamServiceImpl#extractDeltaContents】模型返回结果：" + );
        return contents;
    }

    /**
     * DeepSeek 聊天流式响应的增量文本位于 choices[0].delta.content。
     */
    private String extractDeltaContent(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);
            JsonNode contentNode = root.at("/choices/0/delta/content");
            if (contentNode == null) {
                return null;
            }
            return contentNode.asText();
        } catch (Exception e) {
            log.debug("【AiStreamServiceImpl#extractDeltaContent】忽略无法解析的流式响应片段：{}", data, e);
            return null;
        }
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
