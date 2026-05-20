package com.example.demo.service.impl;

import com.example.demo.dto.AiRequest;
import com.example.demo.service.AiResService;
import org.springframework.stereotype.Service;

@Service
public class AiResServiceImpl implements AiResService {
    @Override
    public String aiAgentReturn(AiRequest aiRequest) {
        return aiRequest.getPrompt();
    }
}
