package com.example.demo.dto;

import lombok.Getter;

import java.util.List;

@Getter
public class AiRequest {

    private String prompt;
    private String model = "deepseek-chat";
    private List<Message> messages;
}
