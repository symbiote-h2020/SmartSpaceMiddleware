/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import eu.h2020.symbiote.ssp.communication.rabbit.*;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceRegistrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceRegistrationHandler.class);

    @Autowired
    ResourcesRepository resourcesRepository;

    public void receiveRegistrationMessage(Object obj) throws Exception {
        String message;
        if (obj instanceof byte[]) {
            message = new String((byte[]) obj, "UTF-8");
        } else {
            message = (String) obj;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SSPRecourceCreatedOrUpdated newResource = mapper.readValue(message, SSPRecourceCreatedOrUpdated.class);
        if(newResource == null)
            throw new Exception("receiveRegistrationMessage error format");
        addResource(newResource.getId(), null, null, null, newResource.getUrl());
    }
    
    public void receiveUpdateMessage(Object obj) throws Exception {
        String message;
        if (obj instanceof byte[]) {
            message = new String((byte[]) obj, "UTF-8");
        } else {
            message = (String) obj;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SSPRecourceCreatedOrUpdated newResource = mapper.readValue(message, SSPRecourceCreatedOrUpdated.class);
        if(newResource == null)
            throw new Exception("receiveRegistrationMessage error format");
        addResource(newResource.getId(), null, null, null, newResource.getUrl());
    }
    
    public void receiveUnregistrationMessage(Object obj) throws Exception{
        String message;
        if (obj instanceof byte[]) {
            message = new String((byte[]) obj, "UTF-8");
        } else {
            message = (String) obj;
        }
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        SSPResourceDeleted resourceDelete = mapper.readValue(message, SSPResourceDeleted.class);
        if(resourceDelete == null)
            throw new Exception("receiveRegistrationMessage error format");
        deleteResource(resourceDelete.getId());
    }
    
    private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String platformId, String url) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId, platformId, url);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        
        resourcesRepository.save(resourceInfo);
    }
    
    private void deleteResource(String resourceId) {
        try {
            List<ResourceInfo> resourceList = resourcesRepository.findByInternalId(resourceId);        
            if(resourceList != null && !resourceList.isEmpty())
                resourcesRepository.delete(resourceList.get(0).getSymbioteId());
        } catch (Exception e) {
            log.info("Resource with id " + resourceId + " not found: "+e);
        }
    }
}
