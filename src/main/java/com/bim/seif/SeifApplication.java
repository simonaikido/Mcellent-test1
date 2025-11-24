package com.bim.seif;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = "com.bim.seif")
@EnableFeignClients(basePackages =  "com.bim.seif.clients")
public class SeifApplication {

	public static void main(String[] args) {
		SpringApplication.run(SeifApplication.class, args);
	}
}