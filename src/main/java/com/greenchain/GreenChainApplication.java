package com.greenchain;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.neo4j.repository.config.EnableNeo4jRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 绿链锂电采购平台启动类
 * <p>
 * 启用 Neo4j 图数据库 Repository 扫描、MyBatis Mapper 扫描、
 * Spring 定时任务调度（用于超时订单自动关单、每日缓存预热等后台任务）。
 */
@SpringBootApplication
@EnableNeo4jRepositories(basePackages = "com.greenchain.repository")
@MapperScan("com.greenchain.mapper")
@EnableScheduling
public class GreenChainApplication {

    public static void main(String[] args) {
        SpringApplication.run(GreenChainApplication.class, args);
    }
}