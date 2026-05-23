package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class AiStreamRequest {

    /**
     * 预留给后续业务扩展使用，流式接口不会把它当作用户问题。
     */
    private String prompt;

    private String model = "deepseek-chat";

    /**
     * DeepSeek 流式开关，流式服务发送请求前会设置为 true。
     */
    private Boolean stream = true;

    /**
     * 调用方传入的消息列表，只会把最后一条非空 user 消息当作本次问题。
     */
    private List<Message> messages;
}
