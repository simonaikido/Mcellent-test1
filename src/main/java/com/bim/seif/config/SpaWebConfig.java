package com.bim.seif.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;

@Configuration
public class SpaWebConfig {

    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/{path:^(?!.*\\.).*$}")
                .setViewName("forward:/int/index.html");
        registry.addViewController("/**/{path:^(?!.*\\.).*$}")
                .setViewName("forward:/int/index.html");
    }
}
