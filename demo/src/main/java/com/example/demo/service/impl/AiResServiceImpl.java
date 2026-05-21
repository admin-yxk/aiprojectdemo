package com.example.demo.service.impl;

import com.example.demo.dto.AiRequest;
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

    @Override
    public String aiAgentReturn(AiRequest aiRequest) {
        log.debug("AiResService_AI接口入参： " + objectMapper.writeValueAsString(aiRequest));

        String respose = webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(aiRequest)
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
                .bodyToMono(String.class)
                .block();

        log.debug("AI接口返回值： " + respose);
        return respose;
    }
}
