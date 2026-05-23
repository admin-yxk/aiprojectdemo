package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiRequest {

    private String prompt;
    private String model = "deepseek-chat";
    private List<Message> messages;
}
