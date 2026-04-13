package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Coupon;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.Message;
import com.example.springboot.entity.Orders;
import com.example.springboot.entity.UserCoupon;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.service.ICouponService;
import com.example.springboot.service.IGoodsService;
import com.example.springboot.service.IOrdersService;
import com.example.springboot.service.IUserCouponService;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
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

    @Autowired
    private ICouponService couponService;

    @Autowired
    private IGoodsService goodsService;

    @Autowired
    private IUserCouponService userCouponService;

    @Autowired
    private IOrdersService ordersService;

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
        String[] parts = content.split("</think>");
        if (parts.length > 1) {
            content = parts[1];
        }
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
        requestHeaders.set("Authorization","Bearer BWP9CZX-XT4MMFX-MAZNRBS-089EGYW");
        requestHeaders.set("accept", "application/json");
        HttpEntity<Map<String, Object>> r = new HttpEntity<Map<String, Object>>(requestBody, requestHeaders);

        String url = "http://localhost:3001/api/v1/workspace/ollamade/thread/d32477e6-d829-4590-a59b-8ca2c2ea9fd9/chat";
        String content = restTemplate.postForObject(url, r, String.class);

        
        JSONObject jsonObject = JSONUtil.parseObj(content);
        String aiAnswer = jsonObject.getStr("textResponse");
        String[] parts = aiAnswer.split("</think>");
        if (parts.length > 1) {
            content = parts[1];
        } else {
            content = aiAnswer;
        }
        return  Result.success(content);
    }

    @PostMapping("/recommendGoods")
    public Result recommendGoods(@RequestBody Map<String, Object> params) {
        BigDecimal currentAmount = new BigDecimal(params.get("currentAmount").toString());
        Long userCouponId = Long.valueOf(params.get("userCouponId").toString());
        List<Integer> orderIds = (List<Integer>) params.get("orderIds");

        UserCoupon userCoupon = userCouponService.getById(userCouponId);
        if (userCoupon == null) {
            throw new ServiceException("用户优惠券不存在");
        }

        Coupon coupon = couponService.getById(userCoupon.getCouponId());
        if (coupon == null) {
            throw new ServiceException("优惠券不存在");
        }

        if (orderIds != null && !orderIds.isEmpty()) {
            BigDecimal actualTotal = BigDecimal.ZERO;
            for (Integer orderId : orderIds) {
                Orders order = ordersService.getById(orderId);
                if (order == null) {
                    throw new ServiceException("订单ID " + orderId + " 不存在");
                }
                if (!"待付款".equals(order.getState())) {
                    throw new ServiceException("订单ID " + orderId + " 不是待付款状态");
                }
                actualTotal = actualTotal.add(new BigDecimal(order.getPrice().toString()));
            }
            if (actualTotal.compareTo(currentAmount) != 0) {
                throw new ServiceException("订单总金额与传入金额不一致");
            }
        }

        if (coupon.getType() == null || (coupon.getType() != 1 && coupon.getType() != 3)) {
            throw new ServiceException("该优惠券类型不支持满减推荐");
        }

        BigDecimal minAmount = coupon.getMinAmount();
        if (minAmount == null) {
            return Result.success(Map.of(
                    "needRecommend", false,
                    "message", "该优惠券无门槛限制"
            ));
        }

        if (currentAmount.compareTo(minAmount) >= 0) {
            return Result.success(Map.of(
                    "needRecommend", false,
                    "message", "已达到优惠券使用门槛"
            ));
        }

        BigDecimal gap = minAmount.subtract(currentAmount);

        List<Goods> goodsList = goodsService.selectAvailableGoods(20);

        StringBuilder goodsInfo = new StringBuilder();
        for (int i = 0; i < goodsList.size(); i++) {
            Goods goods = goodsList.get(i);
            goodsInfo.append(i + 1).append(". ").append(goods.getName())
                    .append(" - 价格：¥").append(goods.getPrice()).append("\n");
        }

        String aiPrompt = String.format(
                "你是一个电商智能推荐助手。请根据以下信息为用户推荐商品，使其总金额达到优惠券使用门槛。\n\n" +
                "【当前订单金额】：¥%.2f\n" +
                "【优惠券名称】：%s\n" +
                "【优惠券门槛】：¥%.2f（满减券/折扣券）\n" +
                "【还差金额】：¥%.2f\n\n" +
                "【可选商品列表】：\n%s\n" +
                "【推荐要求】：\n" +
                "1. 从上述商品列表中选择1-3个商品进行推荐\n" +
                "2. 推荐的商品价格之和应该刚好超过或等于还差金额 ¥%.2f，不要超出太多\n" +
                "3. 优先选择价格接近差额的商品组合\n" +
                "4. 请用中文回答，格式如下：\n" +
                "   推荐商品：商品A(¥XX)、商品B(¥XX)\n" +
                "   推荐后总金额：¥XX.XX（达到满减门槛）\n" +
                "   推荐理由：简要说明为什么推荐这些商品",
                currentAmount,
                coupon.getName(),
                minAmount,
                gap,
                goodsInfo.toString(),
                gap
        );

        Message message = new Message();
        message.setMessage(aiPrompt);
        message.setSessionId("recommend_" + System.currentTimeMillis());

        Prompt prompt = new Prompt(new UserMessage(aiPrompt));
        ChatResponse chatResponse = ollamaChatModel.call(prompt);
        String aiContent = chatResponse.getResult().getOutput().getContent();
        String[] parts = aiContent.split(" ");
        if (parts.length > 1) {
            aiContent = parts[1];
        }

        Map<String, Object> result = new HashMap<>();
        result.put("needRecommend", true);
        result.put("currentAmount", currentAmount);
        result.put("minAmount", minAmount);
        result.put("gap", gap);
        result.put("couponName", coupon.getName());
        result.put("aiRecommendation", aiContent);
        result.put("message", "AI已为您智能推荐凑单商品");

        return Result.success(result);
    }
}