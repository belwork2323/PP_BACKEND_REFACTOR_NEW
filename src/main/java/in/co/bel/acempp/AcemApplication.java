package in.co.bel.acempp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AcemApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcemApplication.class, args);

	}

}
