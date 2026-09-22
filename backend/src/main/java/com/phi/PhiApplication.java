package com.phi;

import com.phi.config.PhiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(PhiProperties.class)
@EnableScheduling
public class PhiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhiApplication.class, args);
	}

}
