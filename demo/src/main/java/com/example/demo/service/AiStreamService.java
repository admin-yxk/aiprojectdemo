package com.example.demo.service;

import com.example.demo.dto.AiStreamRequest;
import reactor.core.publisher.Flux;

public interface AiStreamService {

    Flux<String> aiAgentStream(AiStreamRequest aiRequest);
}
