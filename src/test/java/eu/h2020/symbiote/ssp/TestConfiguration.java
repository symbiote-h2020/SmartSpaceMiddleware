package eu.h2020.symbiote.ssp;

import eu.h2020.symbiote.ssp.rap.managers.AuthorizationManager;

import org.mockito.Mockito;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 9/17/2017.
 */
@Profile("test")
@Configuration
public class TestConfiguration {

    @Bean
    @Primary
    public AuthorizationManager authorizationManager() {
        return Mockito.mock(AuthorizationManager.class);
    }
}