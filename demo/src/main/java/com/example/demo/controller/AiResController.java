package com.example.demo.controller;

import com.example.demo.dto.AiRequest;
import com.example.demo.service.AiResService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@RestController
public class AiResController {
    @Autowired
    private AiResService aiResService;

    @Autowired
    private ObjectMapper objectMapper;

    @PostMapping("/agent")
    public String aiAgentReturn(@RequestBody AiRequest aiRequest){
        log.debug("【AiResController#aiAgentReturn】AI接口入参：{}", objectMapper.writeValueAsString(aiRequest));
        return aiResService.aiAgentReturn(aiRequest);
    }
}
