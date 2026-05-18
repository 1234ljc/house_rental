package com.example.demo.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@Slf4j
public class MvcConfig implements WebMvcConfigurer {

    @Value("${upload.save-path:uploads}")
    private String savePath;

    @Value("${upload.mapping:/uploads/}")
    private String mapping;

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 原有的upload映射
        String location = System.getProperty("os.name").toLowerCase().contains("win")
                ? "file:///" + savePath.replace("\\", "/")
                : "file:" + savePath;

        log.info("注册资源映射: {} -> {}", mapping, location);

        registry.addResourceHandler(mapping + "**")
                .addResourceLocations(location + "/");
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**");
    }
}
