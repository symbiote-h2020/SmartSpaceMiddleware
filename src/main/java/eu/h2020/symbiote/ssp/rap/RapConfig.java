/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@Configuration
public class RapConfig {

    public static String JSON_PROPERTY_CLASS_NAME;

    @Value("${rap.json.property.type}")
    public void setPropertyName(String property) {
        JSON_PROPERTY_CLASS_NAME = property;
    }

    /*
    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }*/

    @Bean
    public RestTemplate restTemplate()
    {
        RestTemplateBuilder builder = new RestTemplateBuilder();
        return builder
                .setConnectTimeout(10)
                .setReadTimeout(10)
                .build();
    }
}
