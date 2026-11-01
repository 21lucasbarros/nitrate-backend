package com.lucasbarros.nitrate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // expansão do grafo roda em segundo plano (GraphExpansionService)
public class NitrateApplication {

	public static void main(String[] args) {
		SpringApplication.run(NitrateApplication.class, args);
	}

}
