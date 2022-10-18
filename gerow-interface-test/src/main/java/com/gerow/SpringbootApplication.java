package com.gerow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class SpringbootApplication implements WebMvcConfigurer {
    public static void main(String[] args) {
        SpringApplication.run(SpringbootApplication.class);
    }

//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        InterceptorRegistration ir = registry.addInterceptor(new LoginHandlerInterceptor());
//        ir.addPathPatterns("/**");
//        ir.excludePathPatterns("/login");
//    }
}