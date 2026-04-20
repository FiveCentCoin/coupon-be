package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.*;
import com.example.springboot.service.*;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CouponStrategyServiceImpl extends ServiceImpl<CouponStrategyMapper, CouponStrategy> implements ICouponStrategyService {

    @Resource
    private CouponStrategyMapper couponStrategyMapper;

    @Autowired
    private UserProfileMapper userProfileMapper;

    @Resource
    private UserMapper userMapper;

    @Resource
    private CouponMapper couponMapper;

    @Resource
    private UserCouponMapper userCouponMapper;

    @Resource
    private CouponIssueRecordMapper couponIssueRecordMapper;

    @Resource
    private IUserProfileService userProfileService;

    @Resource
    private IUserCouponService userCouponService;

    @Resource
    private ICouponService couponService;

    @Resource
    private OllamaChatModel ollamaChatModel;

    @Override
    public List<CouponStrategy> getActiveStrategies() {
        LocalDateTime now = LocalDateTime.now();
        return couponStrategyMapper.selectList(
                new LambdaQueryWrapper<CouponStrategy>()
                        .eq(CouponStrategy::getStatus, 1)
                        .and(w -> w.isNull(CouponStrategy::getStartTime)
                                .or().le(CouponStrategy::getStartTime, now))
                        .and(w -> w.isNull(CouponStrategy::getEndTime)
                                .or().ge(CouponStrategy::getEndTime, now))
                        .orderByDesc(CouponStrategy::getPriority)
        );
    }

    @Override
    public List<CouponStrategy> matchStrategiesForUser(Integer userId) {
        UserProfile profile = userProfileService.getOrCreateProfile(userId);
        List<CouponStrategy> allActive = getActiveStrategies();

        return allActive.stream()
                .filter(strategy -> isUserMatchStrategy(profile, userId, strategy))
                .collect(Collectors.toList());
    }

    private boolean isUserMatchStrategy(UserProfile profile, Integer userId, CouponStrategy strategy) {
        if (strategy.getTargetUserIds() != null && !strategy.getTargetUserIds().isEmpty()) {
            List<Integer> targetIds = JSONUtil.toList(strategy.getTargetUserIds(), Integer.class);
            if (!targetIds.contains(userId)) {
                return false;
            }
        }

        if (strategy.getTargetConsumeLevel() != null && !strategy.getTargetConsumeLevel().isEmpty()) {
            List<String> levels = Arrays.asList(strategy.getTargetConsumeLevel().split(","));
            if (!levels.contains(profile.getConsumeLevel())) {
                return false;
            }
        }

        if (strategy.getTargetActivityLevel() != null && !strategy.getTargetActivityLevel().isEmpty()) {
            List<String> levels = Arrays.asList(strategy.getTargetActivityLevel().split(","));
            if (!levels.contains(profile.getActivityLevel())) {
                return false;
            }
        }

        if (strategy.getTargetLoyaltyLevel() != null && !strategy.getTargetLoyaltyLevel().isEmpty()) {
            List<String> levels = Arrays.asList(strategy.getTargetLoyaltyLevel().split(","));
            if (!levels.contains(profile.getLoyaltyLevel())) {
                return false;
            }
        }

        return true;
    }

    @Override
    public Map<String, Object> executeStrategy(Integer strategyId) {
        CouponStrategy strategy = couponStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new ServiceException("策略不存在");
        }
        if (strategy.getStatus() != 1) {
            throw new ServiceException("策略未启用");
        }

        List<User> allUsers = userMapper.selectList(null);
        int successCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();

        for (User user : allUsers) {
            try {
                Map<String, Object> result = executeStrategyForUser(strategyId, user.getId());
                if ((Boolean) result.getOrDefault("success", false)) {
                    successCount++;
                } else {
                    failCount++;
                    failReasons.add(user.getName() + ": " + result.get("message"));
                }
            } catch (Exception e) {
                failCount++;
                failReasons.add(user.getName() + ": " + e.getMessage());
            }
        }

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("success", true);
        resultMap.put("strategyName", strategy.getName());
        resultMap.put("totalUsers", allUsers.size());
        resultMap.put("successCount", successCount);
        resultMap.put("failCount", failCount);
        resultMap.put("message", "策略执行完成，成功发放" + successCount + "张优惠券");
        return resultMap;
    }

    @Override
    public Map<String, Object> executeStrategyForUser(Integer strategyId, Integer userId) {
        CouponStrategy strategy = couponStrategyMapper.selectById(strategyId);
        if (strategy == null) {
            throw new ServiceException("策略不存在");
        }

        UserProfile profile = userProfileService.getOrCreateProfile(userId);

        if (!isUserMatchStrategy(profile, userId, strategy)) {
            return Map.of("success", false, "message", "用户不满足策略条件");
        }

        if (strategy.getTotalIssueLimit() != null && strategy.getTotalIssueLimit() > 0 &&
                strategy.getIssuedCount() >= strategy.getTotalIssueLimit()) {
            return Map.of("success", false, "message", "策略已达到发放上限");
        }

        Long existingIssue = couponIssueRecordMapper.selectCount(
                new LambdaQueryWrapper<CouponIssueRecord>()
                        .eq(CouponIssueRecord::getStrategyId, strategyId)
                        .eq(CouponIssueRecord::getUserId, userId)
                        .eq(CouponIssueRecord::getStatus, 1)
        );
        if (existingIssue >= strategy.getIssueCountPerUser()) {
            return Map.of("success", false, "message", "该用户已领取过此策略的优惠券");
        }

        Coupon baseCoupon = couponMapper.selectById(strategy.getCouponTemplateId());
        if (baseCoupon == null) {
            throw new ServiceException("关联的优惠券模板不存在");
        }

        BigDecimal actualDiscountAmount = calculateDynamicAmount(strategy, profile);

        Coupon dynamicCoupon = createDynamicCoupon(baseCoupon, actualDiscountAmount, strategy.getName());

        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setUserId(Long.valueOf(userId));
        userCoupon.setCouponId(dynamicCoupon.getId());
        userCoupon.setStatus(0);
        userCoupon.setCreateTime(LocalDateTime.now());
        userCouponService.save(userCoupon);

        CouponIssueRecord record = new CouponIssueRecord();
        record.setStrategyId(strategyId);
        record.setStrategyName(strategy.getName());
        record.setUserId(userId);
        record.setUserCouponId(Math.toIntExact(userCoupon.getId()));
        record.setCouponId(Math.toIntExact(baseCoupon.getId()));
        record.setOriginalAmount(baseCoupon.getDiscountAmount());
        record.setActualAmount(actualDiscountAmount);
        record.setAdjustReason(buildAdjustReason(strategy, profile));
        record.setUserConsumeLevel(profile.getConsumeLevel());
        record.setUserLoyaltyLevel(profile.getLoyaltyLevel());
        record.setUserActivityLevel(profile.getActivityLevel());
        record.setIssueSource("STRATEGY");
        record.setStatus(1);
        record.setCreateTime(LocalDateTime.now());
        couponIssueRecordMapper.insert(record);

        strategy.setIssuedCount(strategy.getIssuedCount() + 1);
        couponStrategyMapper.updateById(strategy);

        userProfileService.recordBehavior(userId, "COUPON_GET", "COUPON",
                Math.toIntExact(baseCoupon.getId()), baseCoupon.getName(), actualDiscountAmount);

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("couponName", dynamicCoupon.getName());
        result.put("discountAmount", actualDiscountAmount);
        result.put("originalAmount", baseCoupon.getDiscountAmount());
        result.put("adjustReason", record.getAdjustReason());
        result.put("userLevel", profile.getLoyaltyLevel());
        result.put("message", "成功为用户" +
                (profile.getConsumeLevel() != null ? getLevelLabel(profile.getConsumeLevel()) : "") +
                "发放个性化优惠券");

        return result;
    }

    private BigDecimal calculateDynamicAmount(CouponStrategy strategy, UserProfile profile) {
        if (strategy.getEnableDynamicAmount() == null || strategy.getEnableDynamicAmount() != 1) {
            return strategy.getBaseDiscountAmount();
        }

        BigDecimal base = strategy.getBaseDiscountAmount() != null ? strategy.getBaseDiscountAmount() : BigDecimal.ZERO;
        BigDecimal max = strategy.getMaxDiscountAmount();

        try {
            AIEvaluationResult aiResult = evaluateWithAI(strategy, profile, base, max);
            lastAIAdjustReason = aiResult.reason;
            return aiResult.amount;
        } catch (Exception e) {
            System.err.println("AI动态面额评估失败，使用规则降级: " + e.getMessage());
            return calculateWithRules(strategy, profile);
        }
    }

    private String lastAIAdjustReason = "";

    static class AIEvaluationResult {
        BigDecimal amount;
        String reason;
        AIEvaluationResult(BigDecimal amount, String reason) {
            this.amount = amount;
            this.reason = reason;
        }
    }

    private AIEvaluationResult evaluateWithAI(CouponStrategy strategy, UserProfile profile, BigDecimal base, BigDecimal max) {
        StringBuilder profileInfo = new StringBuilder();
        profileInfo.append("【用户画像数据】\n");
        profileInfo.append("- 消费能力等级: ").append(profile.getConsumeLevel() != null ? getLevelLabel(profile.getConsumeLevel()) : "未知").append("(").append(profile.getConsumeLevel()).append(")\n");
        profileInfo.append("- 忠诚度等级: ").append(profile.getLoyaltyLevel() != null ? profile.getLoyaltyLevel() : "BRONZE").append("\n");
        profileInfo.append("- 价格敏感度: ").append(profile.getPriceSensitivity() != null ? profile.getPriceSensitivity() : "MEDIUM").append("\n");
        profileInfo.append("- 活跃度: ").append(profile.getActivityLevel() != null ? getActivityLabel(profile.getActivityLevel()) : "NORMAL").append("\n");
        profileInfo.append("- 累计消费金额: ¥").append(profile.getTotalSpend() != null ? profile.getTotalSpend() : 0).append("\n");
        profileInfo.append("- 订单数量: ").append(profile.getOrderCount() != null ? profile.getOrderCount() : 0).append("单\n");
        profileInfo.append("- 平均订单金额: ¥").append(profile.getAvgOrderAmount() != null ? profile.getAvgOrderAmount() : 0).append("\n");
        profileInfo.append("- 近30天登录天数: ").append(profile.getLoginDays30() != null ? profile.getLoginDays30() : 0).append("天\n");
        profileInfo.append("- 近30天下单天数: ").append(profile.getOrderDays30() != null ? profile.getOrderDays30() : 0).append("天\n");
        profileInfo.append("- 连续活跃天数: ").append(profile.getContinuousDays() != null ? profile.getContinuousDays() : 0).append("天\n");
        profileInfo.append("- 优惠券使用率: ").append(profile.getCouponUseRate() != null ? profile.getCouponUseRate() : 0).append("%\n");
        if (profile.getAiTags() != null && !profile.getAiTags().isEmpty()) {
            profileInfo.append("- AI标签: ").append(profile.getAiTags()).append("\n");
        }
        if (profile.getAiProfileSummary() != null && !profile.getAiProfileSummary().isEmpty()) {
            profileInfo.append("- 用户画像摘要: ").append(profile.getAiProfileSummary()).append("\n");
        }

        String prompt = String.format(
                "你是一个电商智能定价引擎专家。请根据用户画像数据，为该用户计算优惠券的最优面额倍率。\n\n" +
                "%s\n" +
                "【优惠券策略配置】\n" +
                "- 基础面额: ¥%.2f\n" +
                "- 最大面额上限: %s\n" +
                "- 策略名称: %s\n\n" +
                "【评估维度】(请综合考虑)\n" +
                "1. 用户价值(40%%): 高消费+高忠诚→高倍率(1.3-1.8)，低价值→低倍率(0.6-1.0)\n" +
                "2. 价格敏感度(25%%): 高敏感→高倍率刺激转化，低敏感→适中即可\n" +
                "3. 活跃与留存(20%%): 流失风险用户→高倍率挽留，稳定用户→正常倍率\n" +
                "4. 历史行为(15%%): 高使用率→可适当降低，低使用率→提高吸引力度\n\n" +
                "【输出要求】(严格JSON格式):\n" +
                "{\"factor\":倍率数字,\"reason\":\"简短中文理由(20字内)\"}\n\n" +
                "【倍率范围】: 0.5 - 2.0 之间的小数\n" +
                "【示例输出]: {\"factor\":1.35,\"reason\":\"高价值客户，适当提升优惠\"}",
                profileInfo.toString(),
                base,
                max != null && max.compareTo(BigDecimal.ZERO) > 0 ? "¥" + max : "无上限",
                strategy.getName()
        );

        ChatResponse response = ollamaChatModel.call(new Prompt(new UserMessage(prompt)));
        String content = response.getResult().getOutput().getContent().trim();

        content = content.replaceAll("```json", "").replaceAll("```", "").trim();

        double factor = parseAIFactor(content);
        String reason = parseAIReason(content);

        BigDecimal calculated = base.multiply(BigDecimal.valueOf(factor));

        if (max != null && max.compareTo(BigDecimal.ZERO) > 0 && calculated.compareTo(max) > 0) {
            calculated = max;
            reason += "(已达上限)";
        }

        calculated = calculated.setScale(2, RoundingMode.HALF_UP);

        return new AIEvaluationResult(calculated,
                "AI评估: " + reason + " (倍率x" + String.format("%.2f", factor) + ")");
    }

    private double parseAIFactor(String jsonContent) {
        try {
            int factorStart = jsonContent.indexOf("\"factor\"");
            if (factorStart == -1) return 1.0;
            int colonPos = jsonContent.indexOf(":", factorStart);
            int commaEnd = jsonContent.indexOf(",", colonPos);
            int braceEnd = jsonContent.indexOf("}", colonPos);
            int endPos = commaEnd > 0 ? Math.min(commaEnd, braceEnd) : braceEnd;
            String factorStr = jsonContent.substring(colonPos + 1, endPos).trim();
            return Double.parseDouble(factorStr);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private String parseAIReason(String jsonContent) {
        try {
            int reasonStart = jsonContent.indexOf("\"reason\"");
            if (reasonStart == -1) return "基于用户画像智能调整";
            int colonPos = jsonContent.indexOf(":", reasonStart);
            int startQuote = jsonContent.indexOf("\"", colonPos + 1);
            int endQuote = jsonContent.indexOf("\"", startQuote + 1);
            return jsonContent.substring(startQuote + 1, endQuote);
        } catch (Exception e) {
            return "基于用户画像智能调整";
        }
    }

    private String getActivityLabel(String activityLevel) {
        if (activityLevel == null) return "未知";
        switch (activityLevel) {
            case "ACTIVE": return "活跃";
            case "NORMAL": return "正常";
            case "SILENT": return "低活跃";
            case "LOST": return "流失";
            case "NEW": return "新用户";
            default: return activityLevel;
        }
    }

    private BigDecimal calculateWithRules(CouponStrategy strategy, UserProfile profile) {
        BigDecimal base = strategy.getBaseDiscountAmount() != null ? strategy.getBaseDiscountAmount() : BigDecimal.ZERO;
        BigDecimal factor = BigDecimal.ONE;

        String consumeLevel = profile.getConsumeLevel();
        if ("HIGH".equals(consumeLevel)) {
            factor = strategy.getAmountFactorHigh() != null ? strategy.getAmountFactorHigh() : BigDecimal.ONE;
        } else if ("MEDIUM".equals(consumeLevel)) {
            factor = strategy.getAmountFactorMedium() != null ? strategy.getAmountFactorMedium() : BigDecimal.ONE;
        } else {
            factor = strategy.getAmountFactorLow() != null ? strategy.getAmountFactorLow() : BigDecimal.ONE;
        }

        BigDecimal calculated = base.multiply(factor);
        BigDecimal max = strategy.getMaxDiscountAmount();

        if (max != null && max.compareTo(BigDecimal.ZERO) > 0 && calculated.compareTo(max) > 0) {
            calculated = max;
        }

        lastAIAdjustReason = buildRuleBasedReason(strategy, profile, factor);
        return calculated.setScale(2, RoundingMode.HALF_UP);
    }

    private Coupon createDynamicCoupon(Coupon baseCoupon, BigDecimal dynamicAmount, String strategyName) {
        Coupon newCoupon = new Coupon();
        newCoupon.setName("[AI专属]" + baseCoupon.getName());
        newCoupon.setType(baseCoupon.getType());
        newCoupon.setMinAmount(baseCoupon.getMinAmount());
        newCoupon.setDiscountAmount(dynamicAmount);
        newCoupon.setDiscountRate(baseCoupon.getDiscountRate());
        newCoupon.setTotalCount(1);
        newCoupon.setUsedCount(0);
        newCoupon.setStartTime(LocalDateTime.now());
        newCoupon.setEndTime(LocalDateTime.now().plusDays(30));
        newCoupon.setStatus(1);
        newCoupon.setCreateTime(LocalDateTime.now());
        newCoupon.setUpdateTime(LocalDateTime.now());
        couponService.save(newCoupon);
        return newCoupon;
    }

    private String buildAdjustReason(CouponStrategy strategy, UserProfile profile) {
        if (lastAIAdjustReason != null && !lastAIAdjustReason.isEmpty()) {
            String reason = lastAIAdjustReason;
            lastAIAdjustReason = "";
            return reason;
        }
        return buildRuleBasedReason(strategy, profile, BigDecimal.ONE);
    }

    private String buildRuleBasedReason(CouponStrategy strategy, UserProfile profile, BigDecimal factor) {
        StringBuilder reason = new StringBuilder();
        reason.append("规则调整: ");
        reason.append("消费等级=").append(getLevelLabel(profile.getConsumeLevel()));
        reason.append(", 忠诚度=").append(profile.getLoyaltyLevel());

        String factorKey = "";
        String consumeLevel = profile.getConsumeLevel();
        if ("HIGH".equals(consumeLevel)) {
            factorKey = "高价值客户倍率x" + strategy.getAmountFactorHigh();
        } else if ("MEDIUM".equals(consumeLevel)) {
            factorKey = "普通客户倍率x" + strategy.getAmountFactorMedium();
        } else {
            factorKey = "新客/低消倍率x" + strategy.getAmountFactorLow();
        }
        reason.append(", ").append(factorKey);
        return reason.toString();
    }

    private String getLevelLabel(String level) {
        if (level == null) return "未知";
        switch (level) {
            case "HIGH": return "高";
            case "MEDIUM": return "中";
            case "LOW": return "低";
            default: return level;
        }
    }
}
