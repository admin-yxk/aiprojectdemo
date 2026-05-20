package com.example.demo.controller;

import com.example.demo.dto.AiRequest;
import com.example.demo.service.AiResService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AiResController {
    @Autowired
    private AiResService aiResService;

    @PostMapping("/agent")
    public String aiAgentReturn(@RequestBody AiRequest aiRequest){
        return "您输入的问题是：" +  aiResService.aiAgentReturn(aiRequest);
    }
}
