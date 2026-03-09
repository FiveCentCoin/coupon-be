package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Message;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/customerService")
public class CustomerServiceController {

    @Resource
    private OllamaChatModel ollamaChatModel;
    @Resource
    private RestTemplate restTemplate;

    @PostMapping("/message")
    public Result receiveMessage(@RequestBody Message message) {
//        获取sessionId
        String sessionId = message.getSessionId();
//        获取用户信息
        String message1 = message.getMessage();
        String  messages = "请使用中文简体回答,并控制字数在30字以内：" + message.getMessage();
        Prompt prompt = new Prompt(new UserMessage(messages));
        ChatResponse chatResponse = ollamaChatModel.call(prompt);
        String content = chatResponse.getResult().getOutput().getContent();
         content = content.split("</think>")[1];
        System.out.println("content = " + content);
        return Result.success(content);
    }


    @PostMapping("/message2")
    public Result receiveMessage2 (@RequestBody Message message) {
//       请求体数据
        Map<String, Object> requestBody = new HashMap<>();
        String message1 = message.getMessage();
        requestBody.put("message",  message1);
        requestBody.put("mode", "chat");
        requestBody.put("userId", 1);
//  请求头数据
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(MediaType.APPLICATION_JSON);
        requestHeaders.set("Authorization","PHHR5RS-A92MCC8-MGZXVH3-PKT0KTP");
        requestHeaders.set("accept", "application/json");
        HttpEntity<Map<String, Object>> r = new HttpEntity<Map<String, Object>>(requestBody, requestHeaders);

        String url = "http://localhost:3001/api/v1/workspace/aiserver/thread/9fb069f2-1251-41a3-8e2e-da6bfeb801b5/chat";
        String content = restTemplate.postForObject(url, r, String.class);

        
        JSONObject jsonObject = JSONUtil.parseObj(content);
        String aiAnswer = jsonObject.getStr("textResponse");
        content = aiAnswer.split("</think>")[1];
        return  Result.success(content);
    }

}