/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.databind.JsonNode;
import eu.h2020.symbiote.ssp.rap.RapConfig;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.rap.exceptions.*;
import eu.h2020.symbiote.ssp.rap.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;

import java.util.*;
import javax.servlet.http.HttpServletRequest;

import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;


/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 *
 * REST controller to receive resource access requests
 * 
 */
@Conditional(NBInterfaceRESTCondition.class)
@RestController
@RequestMapping("rap")
public class NorthboundRestController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    private final int TOP_LIMIT = 100;
    public final String SECURITY_RESPONSE_HEADER = "x-auth-response";
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ResourcesRepository resourcesRepo;

    @Autowired
    private SessionsRepository sessionsRepo;

    @Autowired
    private RapCommunicationHandler communicationHandler;

    /**
     * Used to retrieve the current value of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param request 
     * @return  the current value read from the resource
     * @throws java.lang.Exception
     */
    @RequestMapping(value="/Sensor/{resourceId}", method=RequestMethod.GET)
    public ResponseEntity<Object> readResource(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        HttpHeaders responseHeaders = new HttpHeaders();
        Object response = null;
        try {
        
            log.info("Received read resource request for ID = " + resourceId);
            // checking access policies
            if(!communicationHandler.checkAccessPolicies(request, resourceId))
                    throw new Exception("Auhtorization refused");
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            
            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(infoList);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            SessionInfo sessionInfo = sessionsRepo.findBySspId(info.getSspIdResource());
            String pluginUrl = sessionInfo.getPluginURL();
            if(pluginUrl == null) {
                log.error("No plugin url found");
                throw new Exception("No plugin url associated with resource");
            }            
            log.info("Sending POST request to " + pluginUrl);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            ResponseEntity obj = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, byte[].class);
            if (obj == null) {
                log.error("No response from plugin");
                throw new Exception("No response from plugin");
            }
            if (obj.getStatusCode() != HttpStatus.ACCEPTED && obj.getStatusCode() != HttpStatus.OK) {
                log.error("Error response from plugin: " + obj.getStatusCodeValue() + " " + obj.getStatusCode().toString());
                log.error("Body:\n" + obj.getBody());
                throw new Exception("Error response from plugin");
            }
            String resp = (obj.getBody() instanceof byte[]) ? new String((byte[]) obj.getBody(), "UTF-8") : obj.getBody().toString();
            log.info("response:\n" + resp);
            // checking if plugin response is a valid json
            try {
                JsonNode jsonObj = mapper.readTree(resp.toString());
                if(!jsonObj.has(RapConfig.JSON_PROPERTY_CLASS_NAME)) {
                    log.error("Field " + RapConfig.JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                    //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                }
                response = jsonObj;
            } catch (Exception ex){
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }
            communicationHandler.sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseHeaders = communicationHandler.generateServiceResponse();

        return new ResponseEntity<>(response , responseHeaders, HttpStatus.OK);
    }
    
    /**
     * Used to retrieve the history values of a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param request 
     * @return  the current value read from the resource
     */
    @RequestMapping(value="/Sensor/{resourceId}/history", method=RequestMethod.GET)
    public ResponseEntity<Object> readResourceHistory(@PathVariable String resourceId, HttpServletRequest request) throws Exception {
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        HttpHeaders responseHeaders = new HttpHeaders();
        Object response = null;
        try {        
            log.info("Received read history resource request for ID = " + resourceId);

            // checking access policies
            if(!communicationHandler.checkAccessPolicies(request, resourceId))
                throw new Exception("Auhtorization refused");
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(infoList, TOP_LIMIT, null);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            SessionInfo sessionInfo = sessionsRepo.findBySspId(info.getSspIdResource());
            String pluginUrl = sessionInfo.getPluginURL();
            if(pluginUrl == null) {
                log.error("No plugin url found");
                throw new Exception("No plugin url associated with resource");
            }
            log.info("Sending POST request to " + pluginUrl);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            ResponseEntity obj = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, byte[].class);
            if (obj == null) {
                log.error("No response from plugin");
                throw new Exception("No response from plugin");            }
            if (obj.getStatusCode() != HttpStatus.ACCEPTED && obj.getStatusCode() != HttpStatus.OK) {
                log.error("Error response from plugin: " + obj.getStatusCodeValue() + " " + obj.getStatusCode().toString());
                log.error("Body:\n" + obj.getBody());
                throw new Exception("Error response from plugin");
            }
            String resp = (obj.getBody() instanceof byte[]) ? new String((byte[]) obj.getBody(), "UTF-8") : obj.getBody().toString();

            log.info("response:\n" + resp);
            // checking if plugin response is a valid json
            try {
                JsonNode jsonObj = mapper.readTree(resp.toString());
                if(!jsonObj.has(RapConfig.JSON_PROPERTY_CLASS_NAME)) {
                    log.error("Field " + RapConfig.JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                    //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                }
                response = jsonObj;
            } catch (Exception ex){
                log.error("Response from plugin is not a valid json", ex);
                throw new Exception("Response from plugin is not a valid json");
            }
            communicationHandler.sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseHeaders = communicationHandler.generateServiceResponse();

        return new ResponseEntity<>(response , responseHeaders, HttpStatus.OK);
    }
    
    /**
     * Used to write a value in a registered resource
     * 
     * 
     * @param resourceId    the id of the resource to query 
     * @param body 
     * @param request 
     * @return              the http response code
     * @throws java.lang.Exception
     */
    @RequestMapping(value={"/Actuator/{resourceId}", "/Service/{resourceId}"}, method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String body, HttpServletRequest request) throws Exception {
        HttpHeaders responseHeaders = new HttpHeaders();
        Object response = null;
        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        try {        
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);
            
            // checking access policies
            if(!communicationHandler.checkAccessPolicies(request, resourceId))
                throw new Exception("Auhtorization refused");
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonBody =  mapper.readTree(body);
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, jsonBody);
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);

            SessionInfo sessionInfo = sessionsRepo.findBySspId(info.getSspIdResource());
            String pluginUrl = sessionInfo.getPluginURL();
            if(pluginUrl == null) {
                log.error("No plugin url found");
                throw new Exception("No plugin url associated with resource");
            }
            log.info("Sending POST request to " + pluginUrl);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            ResponseEntity obj = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, byte[].class);
            if(obj != null) {
                if (obj.getStatusCode() != HttpStatus.ACCEPTED && obj.getStatusCode() != HttpStatus.OK) {
                    log.error("Error response from plugin: " + obj.getStatusCodeValue() + " " + obj.getStatusCode().toString());
                    log.error("Body:\n" + obj.getBody());
                    throw new Exception("Error response from plugin");
                }
                if(obj.getBody() != null) {
                    String resp = (obj.getBody() instanceof byte[]) ? new String((byte[]) obj.getBody(), "UTF-8") : obj.getBody().toString();

                    log.info("response:\n" + resp);
                    // checking if plugin response is a valid json
                    try {
                        JsonNode jsonObj = mapper.readTree(resp.toString());
                        if (!jsonObj.has(RapConfig.JSON_PROPERTY_CLASS_NAME)) {
                            log.error("Field " + RapConfig.JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                            //    throw new Exception("Field " + JSON_PROPERTY_CLASS_NAME + " is mandatory in plugin response");
                        }
                        response = jsonObj;
                    } catch (Exception ex) {
                        log.error("Response from plugin is not a valid json", ex);
                        throw new Exception("Response from plugin is not a valid json");
                    }
                }
            }
            communicationHandler.sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            communicationHandler.sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseHeaders = communicationHandler.generateServiceResponse();

        return new ResponseEntity<>(response , responseHeaders, HttpStatus.OK);
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        // first search by symbioteId (global)
        Optional<ResourceInfo> resInfo = resourcesRepo.findBySymIdResource(resourceId);
        // if not present, search by sspId (local)
        if(resInfo == null || !resInfo.isPresent()) {
            Optional<ResourceInfo> tmp = resourcesRepo.findById(resourceId);
            if(tmp != null && tmp.isPresent()) {
                // check if symbioteId is empty, otherwise using sspId is not valid (it could be a mismatch)
                String symId = tmp.get().getSymIdResource();
                if(symId == null || symId.length()<1) {
                    resInfo = tmp;
                } else {
                    log.error("Resource with local id " + resourceId + " has a valid symbioteId");
                    throw new EntityNotFoundException(resourceId);
                }
            } else {
                throw new EntityNotFoundException(resourceId);
            }
        }
        return resInfo.get();
    }
    

}

