/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote;

import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Configuration
public class SecurityHandlerConfig {
    private final String RAP_KEY = "rap";
    
    @Value("${symbIoTe.component.keystore.path}")
    private String keystorePath;
    
    @Value("${symbIoTe.component.keystore.password}")
    private String keystorePasswd;
    
    @Value("${platform.id}") 
    private String platformId;
    
    @Value("${symbIoTe.core.interface.url}")
    private String coreAAMUrl;
    
    @Value("${symbIoTe.localaam.url}")
    private String localAAMUrl;
    
    @Value("${symbIoTe.component.username}")
    private String platformOwner;
    
    @Value("${symbIoTe.component.password}")
    private String platformPasswd;

    @Value("${symbIoTe.validation.localaam}")
    private Boolean alwaysUseLocalAAMForValidation;
    
    @Value("${securityEnabled}")
    private Boolean securityEnabler;
        
/*  
    @Value("${rabbit.host}")
    String rabbitMQHostIP;

    @Value("${rabbit.username}")
    String rabbitMQUsername;  

    @Value("${rabbit.password}")
    String rabbitMQPassword;
*/
    

    @Bean
    public IComponentSecurityHandler securityHandler() throws SecurityHandlerException {
        
        String componentId = RAP_KEY + "@" + platformId;
        // generating the CSH
        IComponentSecurityHandler rapCSH = null;
        if(securityEnabler){
            rapCSH = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                    coreAAMUrl,
                    keystorePath,
                    keystorePasswd,
                    componentId,
                    localAAMUrl,
                    alwaysUseLocalAAMForValidation,
                    platformOwner,
                    platformPasswd
            );
            // workaround to speed up following calls
            rapCSH.generateServiceResponse();
        }
        return rapCSH;
    }    
}