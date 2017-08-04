/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class SspRap {
    
    @Bean
    public ResourceRegistrationHandler resourceRegistrationHandler() {
        return new ResourceRegistrationHandler();
    }
}
