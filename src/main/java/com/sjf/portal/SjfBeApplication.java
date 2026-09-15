package com.sjf.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SjfBeApplication {

	public static void main(String[] args) {
		SpringApplication.run(SjfBeApplication.class, args);
	}

}
