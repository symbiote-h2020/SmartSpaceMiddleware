/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.cim.Observation;
import eu.h2020.symbiote.ssp.rap.pushNotificationService.WebSocketController;
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
@RestController("/rap/v1/plugin")
public class PluginPushNotificationRestController {
    private static final Logger log = LoggerFactory.getLogger(PluginPushNotificationRestController.class);

    @Autowired
    WebSocketController webSocketController;
    
    @Autowired
    RapCommunicationHandler communicationHandler;

    @Value("${rap.json.property.type}")
    private String jsonPropertyClassName;
    
    @RequestMapping(value="/notification", method=RequestMethod.POST)
    public ResponseEntity<?> getPushNotificationFromSubscribedResource(@RequestBody Object payload, @RequestParam(required = false) String resourceId) {
        try {
            log.debug("Plugin Notification message received.\n" + payload);

            String responseString = (payload instanceof byte[]) ? new String((byte[]) payload, "UTF-8") : payload.toString();
            // checking if plugin response is a valid json
            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonObj = mapper.readTree(responseString);
                if (!jsonObj.has(jsonPropertyClassName)) {
                    log.error("Field " + jsonPropertyClassName + " is mandatory in plugin response");
                    //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                }
            } catch (Exception ex) {
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                Observation obs = mapper.readValue(responseString, Observation.class);
                if (resourceId == null || resourceId.length() < 1)
                    resourceId = obs.getResourceId();
            } catch (Exception e) {
                log.warn("Payload is not an Observation");
            }
            communicationHandler.sendSuccessfulPushMessage(resourceId);
            
            webSocketController.SendMessage(resourceId, responseString);
        } catch (Exception e) {
            log.info("Error during plugin registration process\n" + e.getMessage());
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
