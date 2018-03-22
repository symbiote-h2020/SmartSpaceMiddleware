/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp;

import eu.h2020.symbiote.ssp.rap.managers.AuthorizationManager;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Profile("test")
@Configuration
public class SspApplicationTestConfiguration {
    
    @Bean
    @Primary
    public AuthorizationManager authorizationManager() {
        return Mockito.mock(AuthorizationManager.class);
    }
}
