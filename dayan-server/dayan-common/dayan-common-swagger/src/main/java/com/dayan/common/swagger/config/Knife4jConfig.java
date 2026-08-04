package com.dayan.common.swagger.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j（OpenAPI3）配置。
 *
 * <p>各启动模块自动引入本配置生成接口文档，访问 {@code /doc.html}。
 * 文档标题/描述可通过 application.yml 的 {@code springdoc.info.*} 覆盖。
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI dayanOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("大雁养老服务权益平台 API")
                        .description("大雁养老 - 服务权益全生命周期管理平台接口文档")
                        .version("1.0.0")
                        .contact(new Contact().name("dayan").email("dev@dayanpeng.com"))
                        .license(new License().name("Proprietary")));
    }
}
