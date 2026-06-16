package com.hresources.hr.policy_assistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Policy Assistant Spring Boot application.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PolicyAssistantApplication {

	/**
	 * Starts the Spring Boot application.
	 *
	 * @param args command-line startup arguments
	 */
	public static void main(String[] args) {
		SpringApplication.run(PolicyAssistantApplication.class, args);
	}

}
