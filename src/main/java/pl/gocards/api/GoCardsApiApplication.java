package pl.gocards.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
public class GoCardsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoCardsApiApplication.class, args);
	}

}
