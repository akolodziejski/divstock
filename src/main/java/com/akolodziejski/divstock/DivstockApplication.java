package com.akolodziejski.divstock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@EnableCaching
@SpringBootApplication
public class DivstockApplication {

	public static void main(String[] args) {
		SpringApplication.run(DivstockApplication.class, args);
	}

}
