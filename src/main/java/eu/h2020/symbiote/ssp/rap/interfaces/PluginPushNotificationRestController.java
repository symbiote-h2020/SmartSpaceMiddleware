/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.ssp.rap.pushNotificationService.WebSocketController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
*/
@RestController("/rap/plugin")
public class PluginPushNotificationRestController {
    private static final Logger log = LoggerFactory.getLogger(PluginPushNotificationRestController.class);

    @Autowired
    WebSocketController webSocketController;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Autowired
    private IComponentSecurityHandler securityHandler;
    
    @RequestMapping(value="/notification", method=RequestMethod.POST)
    public ResponseEntity<?> getPushNotificationFromSubscribedResource(@RequestBody String payload, @RequestParam(required = false) String resourceId) {
        try {
            log.debug("Plugin Notification message received.\n" + payload);
            try {
                ObjectMapper mapper = new ObjectMapper();
                Observation obs = mapper.readValue(payload, Observation.class);
                if(resourceId == null || resourceId.length() < 1) 
                    resourceId = obs.getResourceId();
            } catch (Exception e) {
                log.warn("Payload is not an Observation");
            }
            sendSuccessfulPushMessage(resourceId);
            
            webSocketController.SendMessage(resourceId, payload);
        } catch (Exception e) {
            log.info("Error during plugin registration process\n" + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    public void sendSuccessfulPushMessage(String symbioteId){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        List<Date> dateList = new ArrayList();
        dateList.add(new Date());
        ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(securityHandler,notificationUrl);
        
        try{
            notificationMessage.SetSuccessfulPushes(symbioteId, dateList);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        notificationMessage.SendSuccessfulPushes(jsonNotificationMessage);
    }
}
