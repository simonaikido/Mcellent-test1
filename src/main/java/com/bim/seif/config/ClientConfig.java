package com.bim.seif.config;


import feign.Logger;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClientConfig {

    Logger.Level loggerLevel() {
        return Logger.Level.FULL;
    }
}
