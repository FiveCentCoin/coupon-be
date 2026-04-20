package com.example.springboot.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.common.Result;
import com.example.springboot.entity.*;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.CouponIssueRecordMapper;
import com.example.springboot.service.*;
import com.example.springboot.utils.TokenUtils;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/aiCoupon")
public class AiCouponController {

    @Autowired
    private IUserProfileService userProfileService;

    @Autowired
    private ICouponStrategyService couponStrategyService;

    @Autowired
    private IAiRecommendLogService aiRecommendLogService;

    @Autowired
    private IUserBehaviorLogService userBehaviorLogService;

    @Autowired
    private IUserCouponService userCouponService;

    @Autowired
    private ICouponService couponService;

    @Autowired
    private OllamaChatModel ollamaChatModel;

    @GetMapping("/profile")
    public Result getMyProfile() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        UserProfile profile = userProfileService.getOrCreateProfile(currentUser.getId());
        profile.setUsername(currentUser.getName());
        profile.setAvatar(currentUser.getAvatar());
        return Result.success(profile);
    }

    @PostMapping("/profile/refresh")
    public Result refreshMyProfile() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        userProfileService.refreshUserProfile(currentUser.getId());
        return Result.success("画像刷新成功");
    }

    @PostMapping("/smartRecommend")
    public Result smartRecommend(@RequestBody Map<String, Object> params) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        BigDecimal orderAmount = new BigDecimal(params.getOrDefault("orderAmount", "0").toString());
        String scene = (String) params.getOrDefault("scene", "ORDER_PAY");

        List<UserCoupon> availableCoupons = userCouponService.list(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, currentUser.getId())
                        .eq(UserCoupon::getStatus, 0)
        );

        if (availableCoupons.isEmpty()) {
            return Result.success(Map.of(
                    "hasRecommendation", false,
                    "message", "当前没有可用优惠券"
            ));
        }

        List<Map<String, Object>> couponDetails = new ArrayList<>();
        for (UserCoupon uc : availableCoupons) {
            Coupon coupon = couponService.getById(uc.getCouponId());
            if (coupon != null && coupon.getStatus() == 1) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("userCouponId", uc.getId());
                detail.put("couponId", coupon.getId());
                detail.put("name", coupon.getName());
                detail.put("type", coupon.getType());
                detail.put("minAmount", coupon.getMinAmount());
                detail.put("discountAmount", coupon.getDiscountAmount());
                detail.put("discountRate", coupon.getDiscountRate());
                detail.put("endTime", coupon.getEndTime());
                couponDetails.add(detail);
            }
        }

        if (couponDetails.isEmpty()) {
            return Result.success(Map.of(
                    "hasRecommendation", false,
                    "message", "没有符合条件的可用优惠券"
            ));
        }

        UserProfile profile = userProfileService.getOrCreateProfile(currentUser.getId());

        List<Map<String, Object>> rankedCoupons = rankCouponsByAI(couponDetails, orderAmount, profile);

        Map<String, Object> bestChoice = rankedCoupons.get(0);
        BigDecimal saveAmount = calculateSaveAmount(bestChoice, orderAmount);

        String recommendReason = buildRecommendReason(bestChoice, orderAmount, saveAmount, profile);

        aiRecommendLogService.logRecommendation(
                currentUser.getId(),
                scene,
                params.toString(),
                couponDetails.toString(),
                (Integer) bestChoice.get("userCouponId"),
                recommendReason,
                new BigDecimal(bestChoice.getOrDefault("score", 0).toString())
        );

        Map<String, Object> result = new HashMap<>();
        result.put("hasRecommendation", true);
        result.put("bestCoupon", bestChoice);
        result.put("saveAmount", saveAmount);
        result.put("recommendReason", recommendReason);
        result.put("allRankedCoupons", rankedCoupons);
        result.put("userProfile", Map.of(
                "consumeLevel", profile.getConsumeLevel(),
                "loyaltyLevel", profile.getLoyaltyLevel(),
                "priceSensitivity", profile.getPriceSensitivity()
        ));
        result.put("message", "AI已为您智能推荐最优优惠券组合");

        return Result.success(result);
    }

    private List<Map<String, Object>> rankCouponsByAI(List<Map<String, Object>> coupons, BigDecimal orderAmount, UserProfile profile) {
        StringBuilder couponsInfo = new StringBuilder();
        for (int i = 0; i < coupons.size(); i++) {
            Map<String, Object> c = coupons.get(i);
            couponsInfo.append(i + 1).append(". ").append(c.get("name"))
                    .append(" | 类型:").append(getTypeName((Integer) c.get("type")))
                    .append(" | 门槛:¥").append(c.getOrDefault("minAmount", "无"))
                    .append(" | 优惠:¥").append(c.getOrDefault("discountAmount", c.getOrDefault("discountRate", "-")))
                    .append("\n");
        }

        String prompt = String.format(
                "你是一个电商AI优惠券推荐引擎。请根据以下信息对优惠券进行排序打分。\n\n" +
                "【用户画像】\n" +
                "- 消费能力: %s\n" +
                "- 忠诚度: %s\n" +
                "- 价格敏感度: %s\n\n" +
                "【订单金额】: ¥%.2f\n\n" +
                "【可选优惠券(%d张)】:\n%s\n" +
                "【评分规则】(满分100分):\n" +
                "1. 可用性(40分): 优惠券是否满足使用门槛，满足得满分，差越多分越低\n" +
                "2. 省钱效果(30分): 实际节省金额越大分数越高\n" +
                "3. 匹配度(20分): 优惠券类型与用户画像匹配度(高消费→满减券/价格敏感→大额券)\n" +
                "4. 紧迫性(10分): 即将过期的券优先推荐\n\n" +
                "【输出格式(JSON数组)]:\n" +
                "[{\"index\":序号,\"score\":分数,\"reason\":\"简短理由\"},...]",
                getLevelLabel(profile.getConsumeLevel()),
                profile.getLoyaltyLevel() != null ? profile.getLoyaltyLevel() : "BRONZE",
                profile.getPriceSensitivity() != null ? profile.getPriceSensitivity() : "MEDIUM",
                orderAmount,
                coupons.size(),
                couponsInfo.toString()
        );

        try {
            ChatResponse response = ollamaChatModel.call(new Prompt(new UserMessage(prompt)));
            String content = response.getResult().getOutput().getContent();

            String[] parts = content.split(" ");
            if (parts.length > 1) {
                content = parts[1];
            }

            content = content.replaceAll("```json", "").replaceAll("```", "").trim();

            List<Map<String, Object>> scores = parseAIScores(content);

            if (!scores.isEmpty()) {
                for (Map<String, Object> score : scores) {
                    int idx = ((Number) score.get("index")).intValue() - 1;
                    if (idx >= 0 && idx < coupons.size()) {
                        coupons.get(idx).putAll(score);
                    }
                }

                coupons.sort((a, b) -> {
                    double scoreA = a.containsKey("score") ? ((Number) a.get("score")).doubleValue() : 0;
                    double scoreB = b.containsKey("score") ? ((Number) b.get("score")).doubleValue() : 0;
                    return Double.compare(scoreB, scoreA);
                });
            } else {
                // 如果AI评分解析失败，使用默认评分逻辑
                for (int i = 0; i < coupons.size(); i++) {
                    Map<String, Object> coupon = coupons.get(i);
                    // 基于优惠金额和门槛计算默认分数
                    BigDecimal minAmount = coupon.get("minAmount") != null ?
                            new BigDecimal(coupon.get("minAmount").toString()) : BigDecimal.ZERO;
                    BigDecimal discount = BigDecimal.ZERO;
                    
                    if (coupon.get("discountAmount") != null) {
                        discount = new BigDecimal(coupon.get("discountAmount").toString());
                    } else if (coupon.get("discountRate") != null) {
                        discount = orderAmount.multiply(BigDecimal.ONE.subtract(
                                new BigDecimal(coupon.get("discountRate").toString())));
                    }
                    
                    // 计算分数：优惠金额占订单金额的比例
                    double score = 0;
                    if (orderAmount.compareTo(BigDecimal.ZERO) > 0) {
                        score = discount.divide(orderAmount, 2, java.math.RoundingMode.HALF_UP).doubleValue() * 100;
                    }
                    
                    // 门槛越低，分数越高
                    if (minAmount.compareTo(BigDecimal.ZERO) > 0 && orderAmount.compareTo(minAmount) >= 0) {
                        score += 10; // 满足门槛额外加分
                    }
                    
                    coupon.put("score", Math.min(score, 100));
                    coupon.put("reason", "默认排序(基于优惠力度)");
                }
                
                coupons.sort((a, b) -> {
                    double scoreA = a.containsKey("score") ? ((Number) a.get("score")).doubleValue() : 0;
                    double scoreB = b.containsKey("score") ? ((Number) b.get("score")).doubleValue() : 0;
                    return Double.compare(scoreB, scoreA);
                });
            }

        } catch (Exception e) {
            for (int i = 0; i < coupons.size(); i++) {
                coupons.get(i).put("score", 100 - i * 10);
                coupons.get(i).put("reason", "默认排序(基于面额)");
            }
        }

        return coupons;
    }

    private BigDecimal calculateSaveAmount(Map<String, Object> coupon, BigDecimal orderAmount) {
        Integer type = (Integer) coupon.get("type");
        BigDecimal minAmount = coupon.get("minAmount") != null ?
                new BigDecimal(coupon.get("minAmount").toString()) : BigDecimal.ZERO;

        if (type == 3) {
            BigDecimal rate = coupon.get("discountRate") != null ?
                    new BigDecimal(coupon.get("discountRate").toString()) : new BigDecimal("0.8");
            return orderAmount.multiply(BigDecimal.ONE.subtract(rate)).setScale(2, java.math.RoundingMode.HALF_UP);
        } else {
            BigDecimal discount = coupon.get("discountAmount") != null ?
                    new BigDecimal(coupon.get("discountAmount").toString()) : BigDecimal.ZERO;
            if (orderAmount.compareTo(minAmount) >= 0) {
                return discount;
            }
            return BigDecimal.ZERO;
        }
    }

    private String buildRecommendReason(Map<String, Object> coupon, BigDecimal orderAmount,
                                        BigDecimal saveAmount, UserProfile profile) {
        StringBuilder reason = new StringBuilder();
        Integer type = (Integer) coupon.get("type");

        reason.append("AI分析：这张").append(getTypeName(type));
        if (orderAmount.compareTo(new BigDecimal(coupon.getOrDefault("minAmount", "0").toString())) >= 0) {
            reason.append("可直接使用，预计为您节省¥").append(saveAmount);
        } else {
            reason.append("还差¥")
                    .append(new BigDecimal(coupon.getOrDefault("minAmount", "0").toString()).subtract(orderAmount))
                    .append("即可使用");
        }

        if ("HIGH".equals(profile.getConsumeLevel()) && type == 1) {
            reason.append("。匹配您的高消费习惯");
        } else if ("LOW".equals(profile.getConsumeLevel()) && type == 2) {
            reason.append("。无门槛设计适合您的消费特点");
        }

        return reason.toString();
    }

    @GetMapping("/expireWarning")
    public Result expireWarning() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threeDaysLater = now.plusDays(3);
        LocalDateTime sevenDaysLater = now.plusDays(7);

        List<UserCoupon> allCoupons = userCouponService.list(
                new LambdaQueryWrapper<UserCoupon>()
                        .eq(UserCoupon::getUserId, currentUser.getId())
                        .eq(UserCoupon::getStatus, 0)
        );

        List<Map<String, Object>> urgentList = new ArrayList<>();
        List<Map<String, Object>> warningList = new ArrayList<>();

        for (UserCoupon uc : allCoupons) {
            Coupon coupon = couponService.getById(uc.getCouponId());
            if (coupon == null || coupon.getStatus() != 1 || coupon.getEndTime() == null) continue;

            long daysUntilExpire = ChronoUnit.DAYS.between(now, coupon.getEndTime());
            if (daysUntilExpire < 0) continue;

            Map<String, Object> item = new HashMap<>();
            item.put("userCouponId", uc.getId());
            item.put("couponName", coupon.getName());
            item.put("couponType", coupon.getType());
            item.put("discountAmount", coupon.getDiscountAmount());
            item.put("discountRate", coupon.getDiscountRate());
            item.put("minAmount", coupon.getMinAmount());
            item.put("endTime", coupon.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            item.put("daysLeft", daysUntilExpire);

            if (daysUntilExpire <= 3) {
                item.put("level", "urgent");
                item.put("suggestMessage", generateExpireSuggest(item, daysUntilExpire));
                urgentList.add(item);
            } else if (daysUntilExpire <= 7) {
                item.put("level", "warning");
                item.put("suggestMessage", generateExpireSuggest(item, daysUntilExpire));
                warningList.add(item);
            }
        }

        urgentList.sort(Comparator.comparingLong(m -> (long) m.get("daysLeft")));
        warningList.sort(Comparator.comparingLong(m -> (long) m.get("daysLeft")));

        UserProfile profile = userProfileService.getOrCreateProfile(currentUser.getId());

        Map<String, Object> result = new HashMap<>();
        result.put("urgentCoupons", urgentList);
        result.put("warningCoupons", warningList);
        result.put("totalUrgent", urgentList.size());
        result.put("totalWarning", warningList.size());
        result.put("hasUrgent", !urgentList.isEmpty());

        if (!urgentList.isEmpty()) {
            result.put("aiAdvice", generateAIExpireAdvice(urgentList, profile));
        }

        return Result.success(result);
    }

    private String generateExpireSuggest(Map<String, Object> coupon, long daysLeft) {
        if (daysLeft <= 1) {
            return "⚠️ 明天过期！建议立即使用";
        } else if (daysLeft <= 3) {
            return "🔥 仅剩" + daysLeft + "天，即将失效";
        } else {
            return "⏰ 还有" + daysLeft + "天过期";
        }
    }

    private String generateAIExpireAdvice(List<Map<String, Object>> urgentCoupons, UserProfile profile) {
        StringBuilder advice = new StringBuilder();
        advice.append("AI提醒：您有").append(urgentCoupons.size()).append("张优惠券即将过期！");

        BigDecimal totalValue = BigDecimal.ZERO;
        for (Map<String, Object> coupon : urgentCoupons) {
            if (coupon.get("discountAmount") != null) {
                totalValue = totalValue.add(new BigDecimal(coupon.get("discountAmount").toString()));
            }
        }

        advice.append("总价值约¥").append(totalValue.setScale(2, java.math.RoundingMode.HALF_UP));

        if ("PRICE_SENSITIVE".equals(profile.getPriceSensitivity()) || "HIGH".equals(profile.getConsumeLevel())) {
            advice.append("。根据您的消费习惯，建议优先使用满减券凑单");
        } else {
            advice.append("。建议尽快选购心仪商品使用");
        }

        return advice.toString();
    }

    @GetMapping("/strategy/list")
    public Result strategyList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<CouponStrategy> page = couponStrategyService.page(
                new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<CouponStrategy>().orderByDesc(CouponStrategy::getPriority)
        );
        return Result.success(page);
    }

    @PostMapping("/strategy/add")
    public Result addStrategy(@RequestBody CouponStrategy strategy) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        strategy.setStatus(1);
        strategy.setIssuedCount(0);
        strategy.setCreateTime(LocalDateTime.now());
        couponStrategyService.save(strategy);
        return Result.success("策略创建成功");
    }

    @PutMapping("/strategy/update")
    public Result updateStrategy(@RequestBody CouponStrategy strategy) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        strategy.setUpdateTime(LocalDateTime.now());
        couponStrategyService.updateById(strategy);
        return Result.success("策略更新成功");
    }

    @DeleteMapping("/strategy/delete/{id}")
    public Result deleteStrategy(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        couponStrategyService.removeById(id);
        return Result.success("策略删除成功");
    }

    @PostMapping("/strategy/execute/{id}")
    public Result executeStrategy(@PathVariable Integer id) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        Map<String, Object> result = couponStrategyService.executeStrategy(id);
        return Result.success(result);
    }

    @PostMapping("/strategy/executeForMe")
    public Result executeStrategyForMe(@RequestParam Integer strategyId) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        Map<String, Object> result = couponStrategyService.executeStrategyForUser(strategyId, currentUser.getId());
        return Result.success(result);
    }

    @GetMapping("/strategy/matchForMe")
    public Result matchStrategiesForMe() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        List<CouponStrategy> matched = couponStrategyService.matchStrategiesForUser(currentUser.getId());
        return Result.success(matched);
    }

    @GetMapping("/issueRecords")
    public Result issueRecords(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        Page<CouponIssueRecord> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<CouponIssueRecord> wrapper = new LambdaQueryWrapper<CouponIssueRecord>()
                .orderByDesc(CouponIssueRecord::getId);
        return Result.success(couponIssueRecordMapper.selectPage(page, wrapper));
    }

    @Autowired
    private CouponIssueRecordMapper couponIssueRecordMapper;

    @GetMapping("/recommendHistory")
    public Result recommendHistory(@RequestParam(defaultValue = "10") Integer limit) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null) {
            throw new ServiceException("请先登录");
        }
        List<AiRecommendLog> history = aiRecommendLogService.getUserRecommendHistory(currentUser.getId(), limit);
        return Result.success(history);
    }

    @GetMapping("/dashboard/stats")
    public Result dashboardStats() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }

        long totalUsers = userProfileService.count();
        long totalStrategies = couponStrategyService.count();
        long activeStrategies = couponStrategyService.count(
                new LambdaQueryWrapper<CouponStrategy>().eq(CouponStrategy::getStatus, 1)
        );
        long totalIssueRecords = couponIssueRecordMapper.selectCount(null);

        Map<String, Object> highUsers = countByField("consume_level", "HIGH");
        Map<String, Object> activeUsers = countByField("activity_level", "ACTIVE");
        Map<String, Object> diamondUsers = countByField("loyalty_level", "DIAMOND");

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", totalUsers);
        stats.put("totalStrategies", totalStrategies);
        stats.put("activeStrategies", activeStrategies);
        stats.put("totalIssueRecords", totalIssueRecords);
        stats.put("highValueUsers", highUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("diamondUsers", diamondUsers);
        stats.put("profileCoverage", totalUsers > 0 ? String.format("%.1f%%",
                totalUsers * 100.0 / Math.max(totalUsers, 1)) : "0%");

        return Result.success(stats);
    }

    @GetMapping("/profiles/list")
    public Result profilesList(@RequestParam(defaultValue = "1") Integer pageNum,
                               @RequestParam(defaultValue = "10") Integer pageSize) {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        Page<UserProfile> page = userProfileService.listWithUserDetails(new Page<>(pageNum, pageSize));
        return Result.success(page);
    }

    @PostMapping("/profiles/refreshAll")
    public Result refreshAllProfiles() {
        User currentUser = TokenUtils.getCurrentUser();
        if (currentUser == null || !"ADMIN".equals(currentUser.getRole())) {
            throw new ServiceException("无权限操作");
        }
        userProfileService.refreshAllProfiles();
        return Result.success("全部用户画像刷新成功");
    }

    private Map<String, Object> countByField(String field, String value) {
        return Map.of("count", 0, "label", value);
    }

    private String getTypeName(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 1: return "满减券";
            case 2: return "无门槛券";
            case 3: return "折扣券";
            default: return "未知";
        }
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

    private List<Map<String, Object>> parseAIScores(String jsonContent) {
        List<Map<String, Object>> scores = new ArrayList<>();
        if (jsonContent == null || jsonContent.trim().isEmpty()) {
            return scores;
        }

        try {
            // 清理JSON字符串，移除可能的Markdown代码块标记
            String cleanedContent = jsonContent.trim()
                    .replaceAll("^```json", "")
                    .replaceAll("```$", "")
                    .trim();

            // 使用Jackson解析JSON
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            scores = mapper.readValue(cleanedContent, new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
        } catch (Exception e) {
            // 如果JSON解析失败，使用默认排序
            scores.clear();
        }

        return scores;
    }
}
