package com.phi;

import com.phi.config.PhiProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(PhiProperties.class)
public class PhiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PhiApplication.class, args);
	}

}
