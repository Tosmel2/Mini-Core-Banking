package com.banking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MiniCoreBankingApplication {

	public static void main(String[] args) {
		SpringApplication.run(MiniCoreBankingApplication.class, args);
	}

}
