package com.example.springboot.common;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 * 作用：配置MyBatis-Plus的插件及相关功能，扩展MyBatis的基础能力
 *
 * @MapperScan：扫描指定包下的Mapper接口，自动生成实现类并注册到Spring容器，此处扫描"com.example.springboot.mapper"包，使该包下的所有Mapper接口无需再添加@Mapper注解
 */
@Configuration // 标识为Spring配置类，Spring启动时会自动加载该配置
@MapperScan("com.example.springboot.mapper")
public class MybatisPlusConfig {
    /**
     * 注册MyBatis-Plus拦截器，添加分页插件
     * MyBatis-Plus的拦截器用于扩展SQL执行逻辑，如分页、乐观锁、性能分析等
     *
     * 将方法返回的拦截器实例注册为Spring容器中的Bean，供MyBatis-Plus使用
     * @return MybatisPlusInterceptor 拦截器实例，包含配置好的分页插件
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        // 创建MyBatis-Plus拦截器主对象
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // 添加分页插件：实现数据库分页查询功能
        // PaginationInnerInterceptor是MyBatis-Plus提供的分页拦截器
        // 参数DbType.MYSQL指定数据库类型为MySQL，分页插件会根据数据库类型生成对应的分页SQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }
}
