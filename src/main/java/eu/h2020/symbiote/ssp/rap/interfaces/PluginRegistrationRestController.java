/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import eu.h2020.symbiote.ssp.resources.db.PluginRepository;
import eu.h2020.symbiote.ssp.rap.messages.registration.PluginRegistrationMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.ssp.resources.db.PluginInfo;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@RestController("/rap/plugin/registration")
public class PluginRegistrationRestController {
    
    private static final Logger log = LoggerFactory.getLogger(PluginRegistrationRestController.class);

    @Autowired
    PluginRepository pluginRepository;
    
    
    @RequestMapping(value="/register", method=RequestMethod.POST)
    public ResponseEntity<?> pluginRegistration(@RequestBody String body, HttpServletRequest request) throws Exception {
        try {
            log.debug("Plugin Registration received.\n" + body);

            ObjectMapper mapper = new ObjectMapper();
            PluginRegistrationMessage msg = mapper.readValue(body, PluginRegistrationMessage.class);
            String pluginId = msg.getPluginId();
            boolean hasFilters = msg.getHasFilters();
            boolean hasNotifications = msg.getHasNotifications();
            String url = msg.getPluginUrl();
            log.info("Registering plugin with id " + pluginId);
            addPlugin(pluginId, url, hasFilters, hasNotifications);
            log.debug("Added plugin with id " + pluginId);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    @RequestMapping(value="/unregister/{pluginId}", method=RequestMethod.DELETE)
    public ResponseEntity<?> pluginUnregistration(@PathVariable String pluginId, HttpServletRequest request) throws Exception {
        try {
            log.debug("Plugin Unregistration received for ID " + pluginId);
            log.info("Unregistering plugin with id " + pluginId);
            deletePlugin(pluginId);
            log.debug("Deleted plugin with id " + pluginId);
               
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    private void addPlugin(String pluginId, String pluginUrl, boolean hasFilters, boolean hasNotifications) throws Exception {
        Optional<PluginInfo> plugin = pluginRepository.findById(pluginId);
        if(plugin.isPresent())
            throw new Exception("Plugin already present");
        PluginInfo pluginInfo = new PluginInfo(pluginId, pluginUrl, hasFilters, hasNotifications);
        pluginRepository.save(pluginInfo);
    }
    
    private void deletePlugin(String pluginId) throws Exception {
        Optional<PluginInfo> plugin = pluginRepository.findById(pluginId);
        if(plugin.isPresent())
            throw new Exception("Plugin not found");
        pluginRepository.delete(pluginId);
    }
}
