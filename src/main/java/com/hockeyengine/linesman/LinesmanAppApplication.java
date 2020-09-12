package com.hockeyengine.linesman;

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;


@SpringBootApplication
@Configuration
@EnableAutoConfiguration
@ComponentScan(basePackages = { "com.hockeyengine.*" })
public class LinesmanAppApplication {
	
	private static Logger logger = LoggerFactory.getLogger(LinesmanAppApplication.class);

	public static void main(String[] args) {		
		logger.info("Linesman is starting up");
		ApplicationContext ctx = SpringApplication.run(LinesmanAppApplication.class, args);
		logger.info("Beans provided by Spring Boot:");
		String[] beanNames = ctx.getBeanDefinitionNames();
		Arrays.sort(beanNames);
		for (String beanName : beanNames) {
			logger.info(beanName);
		}
		logger.info("Linesman is up!");
	}

}
