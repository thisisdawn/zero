package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsWebConfig {
    @Bean
    public CorsWebFilter corsWebFilter() {
        //cors规则配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //允许跨域的域名  *代表所有域名,不能携带cookie
        corsConfiguration.addAllowedOrigin("http://localhost:1000");
        corsConfiguration.addAllowedOrigin("http://manager.gmall.com");
        corsConfiguration.addAllowedOrigin("http://www.gmall.com");
        corsConfiguration.addAllowedOrigin("http://gmall.com");
        corsConfiguration.addAllowedMethod("*");
        corsConfiguration.addAllowedHeader("*");
        corsConfiguration.setAllowCredentials(true);//在Origin为具体路径的条件下 ,true表示允许携带cookie
        //cors配置
        UrlBasedCorsConfigurationSource configurationSource = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(configurationSource);
    }
}
