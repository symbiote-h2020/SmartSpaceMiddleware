package eu.h2020.symbiote.ssp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class SspApplication {

    @Value("${registrationExpiration}")
    private String registrationExpiration;

    public static void main(String[] args) {
		SpringApplication.run(SspApplication.class, args);
	}

    @Bean(name="registrationExpiration")
    public Integer registrationExpiration() {
        Integer regExp = Integer.parseInt(registrationExpiration);

        if (regExp < 0)
            return 0;
        else
            return regExp;
    }
}
