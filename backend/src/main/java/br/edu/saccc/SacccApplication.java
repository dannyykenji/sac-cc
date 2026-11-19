package br.edu.saccc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // jobs internos (LembreteChatService #N9)
public class SacccApplication {

	public static void main(String[] args) {
		SpringApplication.run(SacccApplication.class, args);
	}

}
