package com.sxw;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MessagePushApp {
	public static void main(String[] args) throws Exception {
		SpringApplication springApplication = new SpringApplication(MessagePushApp.class);
		springApplication.run(args);
	}
}
