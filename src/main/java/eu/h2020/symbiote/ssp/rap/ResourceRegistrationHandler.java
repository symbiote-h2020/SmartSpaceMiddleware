/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap;

import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.core.model.resources.MobileSensor;
import eu.h2020.symbiote.core.model.resources.Resource;
import eu.h2020.symbiote.core.model.resources.StationarySensor;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class ResourceRegistrationHandler {
    
    private static final Logger log = LoggerFactory.getLogger(ResourceRegistrationHandler.class);

    @Autowired
    ResourcesRepository resourcesRepository;
    /**
     * Register a list of resources
     * @param resourceList 
     */
    public void registerResources(List<CloudResource> resourceList) {
        try {
            log.info("Registering Resources: \n" + resourceList);            
            for(CloudResource clRes: resourceList){
                String internalId = clRes.getInternalId();
                Resource resource = clRes.getResource();
                String resourceClass = resource.getClass().getName();
                String symbioteId = resource.getId();                
                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }
                String platformId = /*resource.getPlatformId()*/"";

                log.info("Registering "+ resourceClass +" with symbioteId: " + symbioteId + ", internalId: " + internalId);
                addResource(symbioteId, internalId, props, platformId);
            }
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }
    
    public void registerResource(CloudResource resource) {
        try {
            log.info("Registering Resource: \n" + resource);            
            
            String internalId = resource.getInternalId();
            Resource res = resource.getResource();
            String resourceClass = res.getClass().getName();
            String symbioteId = res.getId();                
            List<String> props = null;
            if(res instanceof StationarySensor) {
                props = ((StationarySensor)res).getObservesProperty();
            } else if(res instanceof MobileSensor) {
                props = ((MobileSensor)res).getObservesProperty();
            }
            String platformId = /*resource.getPlatformId()*/"";

            log.info("Registering "+ resourceClass +" with symbioteId: " + symbioteId + ", internalId: " + internalId);
            addResource(symbioteId, internalId, props, platformId);
            
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }
    
    public void unregisterResources(List<String> resourceIdList) {
        try {            
            log.info("Unregistering resources: \n" + resourceIdList);
            for(String id: resourceIdList){
                // TODO: to check if ID at this level is correct
                log.debug("Unregistering resource with symbioteId " + id);
                deleteResource(id);
            }
        } catch (Exception e) {
            log.info("Error during unregistration process\n" + e.getMessage());
        }
    }
    
    public void unregisterResource(String resourceId) {
        try {            
            log.info("Unregistering resource: \n" + resourceId);            
            // TODO: to check if ID at this level is correct
            deleteResource(resourceId);            
        } catch (Exception e) {
            log.info("Error during unregistration process\n" + e.getMessage());
        }
    }
    
    public void updateResources(List<CloudResource> resourceList) {
        try {
            log.info("Updating resources: \n" + resourceList);
        
            for(CloudResource clRes: resourceList){
                String internalId = clRes.getInternalId();
                Resource resource = clRes.getResource();
                String symbioteId = resource.getId();
                List<String> props = null;
                if(resource instanceof StationarySensor) {
                    props = ((StationarySensor)resource).getObservesProperty();
                } else if(resource instanceof MobileSensor) {
                    props = ((MobileSensor)resource).getObservesProperty();
                }
                String platformId = /*resource.getPlatformId()*/"";
                
                log.info("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
                addResource(symbioteId, internalId, props, platformId);
            }
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }
    
    public void updateResource(CloudResource resource) {
        try {
            log.info("Updating resource: \n" + resource);
                    
            String internalId = resource.getInternalId();
            Resource res = resource.getResource();
            String symbioteId = res.getId();
            List<String> props = null;
            if(res instanceof StationarySensor) {
                props = ((StationarySensor)res).getObservesProperty();
            } else if(res instanceof MobileSensor) {
                props = ((MobileSensor)res).getObservesProperty();
            }
            String platformId = /*resource.getPlatformId()*/"";

            log.info("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
            addResource(symbioteId, internalId, props, platformId);
            
        } catch (Exception e) {
            log.info("Error during registration process\n" + e.getMessage());
        }
    }
    
    private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String platformId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId, platformId);
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
            log.info("Resource with id " + resourceId + " not found");
        }
    }
}
