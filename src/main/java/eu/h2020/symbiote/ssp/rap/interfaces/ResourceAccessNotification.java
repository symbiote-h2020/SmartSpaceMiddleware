/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.FailedAccessMessageInfo;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessMessageInfo;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulPushesMessageInfo;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class ResourceAccessNotification {
    private String notificationUrl;
    
    private IComponentSecurityHandler securityHandler;
    
    private static final Logger log = LoggerFactory.getLogger(ResourceAccessNotification.class);
    
    @JsonProperty("successfulAttempts")
    private List<SuccessfulAccessMessageInfo> successfulAttempts;
    
    @JsonProperty("successfulPushes")
    private List<SuccessfulPushesMessageInfo> successfulPushes;
    
    @JsonProperty("failedAttempts")
    private List<FailedAccessMessageInfo> failedAttempts;
    
    public ResourceAccessNotification(IComponentSecurityHandler securityHandler, String notificationUrl) {
        this.securityHandler = securityHandler;
        this.notificationUrl = notificationUrl;
    }
            
    public void SetSuccessfulAttempts (String symbioTeId, List<Date> timestamp, String accessType){
        SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioTeId, timestamp, accessType);
        this.successfulAttempts = new ArrayList<>();
        this.successfulAttempts.add(succAccMess);
    }
    
    public void SetSuccessfulAttemptsList (List<String> symbioTeIdList, List<Date> timestamp, String accessType){
        this.successfulAttempts = new ArrayList<>();
        for(String symbioteId: symbioTeIdList){
            SuccessfulAccessMessageInfo succAccMess = new SuccessfulAccessMessageInfo(symbioteId, timestamp, accessType);
            this.successfulAttempts.add(succAccMess);
        }
    }
    
    public void SetSuccessfulPushes (String symbioTeId, List<Date> timestamp){
        SuccessfulPushesMessageInfo succPushMess = new SuccessfulPushesMessageInfo(symbioTeId, timestamp);
        this.successfulPushes = new ArrayList<>();
        this.successfulPushes.add(succPushMess);
    }
    
    public void SetFailedAttempts (String symbioTeId, List<Date> timestamp, 
            String code, String message, String appId, String issuer, 
            String validationStatus, String requestParams) {
        FailedAccessMessageInfo failMess= new FailedAccessMessageInfo(symbioTeId, timestamp, 
                code, message, appId, issuer, validationStatus, requestParams);
        this.failedAttempts = new ArrayList<>();
        this.failedAttempts.add(failMess);
    }
    
    
    public void SendSuccessfulAttemptsMessage(String message){
        sendMessage(message);
    }
    
    public void SendFailAccessMessage(String message){
        sendMessage(message);
    }
    
    public void SendSuccessfulPushMessage(String message){
        sendMessage(message);
    }
    
    private void sendMessage(String message){
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        
        HttpHeaders httpHeaders = getHeader();
        HttpEntity<String> httpEntity = new HttpEntity<String>(message,httpHeaders);
        
        Object response = restTemplate.postForObject(notificationUrl, httpEntity, Object.class);
        log.info("Response resource access notification message: "+ (String)response);
    }
    
    public HttpHeaders getHeader(){
        Map<String, String> securityRequestHeaders = null;
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);

        if(securityHandler != null){
            try {
                SecurityRequest securityRequest = securityHandler.generateSecurityRequestUsingLocalCredentials();
                securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

                for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
                    httpHeaders.add(entry.getKey(), entry.getValue());
                }
                log.info("request headers: " + httpHeaders);

            } catch (SecurityHandlerException | JsonProcessingException e) {
                log.error("Fail to take header",e);
            }
        }
        return httpHeaders;
    }
}
