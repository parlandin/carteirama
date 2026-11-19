package com.parlandim.carteirama;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class CarteiramaApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(CarteiramaApplication.class);
		if (Arrays.stream(args).anyMatch(a -> a.startsWith("--create-admin"))) {
			app.setWebApplicationType(WebApplicationType.NONE);
		}
		app.run(args);
	}

}
