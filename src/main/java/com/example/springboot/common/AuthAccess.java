package com.example.springboot.common;

import java.lang.annotation.*;

/**
 * 自定义权限注解：用于AuthAccess
 * 作用：标记需要进行权限验证的方法，或标识无需特定权限即可访问的接口
 * （具体权限逻辑需结合拦截器/切面实现）
 *
 * @Target 注解指定当前注解可以应用的位置，这里ElementType.METHOD表示只能标注在方法上
 * @Retention 注解指定注解的保留策略，RetentionPolicy.RUNTIME表示注解在运行时仍然有效 （即可以通过反射在运行时获取注解信息，用于权限拦截逻辑）
 * @Documented 注解表示该注解会被包含在JavaDoc文档中，方便其他开发者了解注解的用途
 *
 * 注解内部无属性定义，仅作为标记使用
 * 通常配合拦截器(Interceptor)或切面(AOP)使用，例如：
 * 1. 标记需要登录才能访问的接口，拦截器检测到该注解时验证用户登录状态
 * 2. 或作为"免权限校验"的标记，允许匿名访问（根据业务场景灵活定义）
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AuthAccess {

}
