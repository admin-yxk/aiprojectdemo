package com.example.demo.controller;

import com.example.demo.dto.AiStreamRequest;
import com.example.demo.service.AiStreamService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
public class AiStreamController {

    private final AiStreamService aiStreamService;

    public AiStreamController(AiStreamService aiStreamService) {
        this.aiStreamService = aiStreamService;
    }

    /**
     * 单独提供流式输出的 POST 接口。
     * 和 /agent 分开，避免影响现有非流式接口逻辑。
     */
    @PostMapping(value = "/agent/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> aiAgentStream(@RequestBody AiStreamRequest aiRequest) {
        return aiStreamService.aiAgentStream(aiRequest);
    }
}
