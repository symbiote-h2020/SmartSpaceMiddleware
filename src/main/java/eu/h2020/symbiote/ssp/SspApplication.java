package eu.h2020.symbiote.ssp;

import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledResourceOfflineTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledUnregisterTimerTask;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

@SpringBootApplication
public class SspApplication {

    @Value("${registrationExpiration}")
    private String registrationExpiration;

    @Value("${makeResourceOffline}")
    private String makeResourceOffline;

    public static void main(String[] args) {
		SpringApplication.run(SspApplication.class, args);
	}

    @Bean(name="registrationExpiration")
    public Integer registrationExpiration() {
        Integer registrationsExp = Integer.parseInt(registrationExpiration);

        if (registrationsExp < 0)
            return 0;
        else
            return registrationsExp;
    }

    @Bean(name="makeResourceOffline")
    public Integer makeResourceOffline() {
        return Integer.parseInt(makeResourceOffline);
    }

    @Bean
    public Timer timer() {
        return new Timer();
    }

    @Bean(name="unregisteringTimerTaskMap")
    public Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap() {
        return new HashMap<>();
    }

    @Bean(name="offlineTimerTaskMap")
    public Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap() {
        return new HashMap<>();
    }
}
