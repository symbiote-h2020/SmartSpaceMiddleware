/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.ssp.rap.exceptions.*;
import eu.h2020.symbiote.ssp.rap.interfaces.conditions.NBInterfaceRESTCondition;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessGetMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessHistoryMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSetMessage;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.PluginInfo;
import eu.h2020.symbiote.ssp.resources.db.PluginRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import java.util.*;
import javax.servlet.http.HttpServletRequest;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
@RestController("/rap")
public class NorthboundRestController {

    private static final Logger log = LoggerFactory.getLogger(RestController.class);

    private final int TOP_LIMIT = 100;
    public final String SECURITY_RESPONSE_HEADER = "x-auth-response";
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Autowired
    private ResourcesRepository resourcesRepo;
    
    @Autowired
    private PluginRepository pluginRepo;
    
    @Autowired
    private IComponentSecurityHandler securityHandler; 
    
    @Autowired
    private AccessPolicyRepository accessPolicyRepo;
   
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Value("${rap.plugin.requestEndpoint}") 
    private String pluginRequestEndpoint;
    
    @Value("${rap.debug.disableSecurity}")
    private Boolean disableSecurity;
      
    
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
            checkAccessPolicies(request, resourceId);
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            
            ResourceAccessGetMessage msg = new ResourceAccessGetMessage(infoList);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                log.error("No plugin found");
                throw new Exception("No plugin associated with resource");
            }            
            Optional<PluginInfo> lst = pluginRepo.findById(pluginId);
            if(lst == null || !lst.isPresent()) {
                log.error("No plugin registered with id " + pluginId);
                throw new Exception("No plugin registered with id " + pluginId);
            }
            String pluginUrl = lst.get().getPluginURL();
            
            String url = pluginUrl + pluginRequestEndpoint;
            log.info("Sending POST request to " + url);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
            if (response == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }
            
            if(!disableSecurity){
                try {
                    String serResponse = securityHandler.generateServiceResponse();
                    responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
                } catch (SecurityHandlerException sce){
                    log.error(sce.getMessage(), sce);
                    throw sce;
                }
            
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
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
            checkAccessPolicies(request, resourceId);
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            
            ResourceAccessHistoryMessage msg = new ResourceAccessHistoryMessage(infoList, TOP_LIMIT, null);
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                log.error("No plugin found");
                throw new Exception("No plugin associated with resource");
            }            
            Optional<PluginInfo> lst = pluginRepo.findById(pluginId);
            if(lst == null || !lst.isPresent()) {
                log.error("No plugin registered with id " + pluginId);
                throw new Exception("No plugin registered with id " + pluginId);
            }
            String pluginUrl = lst.get().getPluginURL();
            
            String url = pluginUrl + pluginRequestEndpoint;
            log.info("Sending POST request to " + url);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
            if (response == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }
            
            if(!disableSecurity){
                try {
                    String serResponse = securityHandler.generateServiceResponse();
                    responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
                } catch (SecurityHandlerException sce){
                    log.error(sce.getMessage(), sce);
                    throw sce;
                }
            
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
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
            checkAccessPolicies(request, resourceId);
            
            ResourceInfo info = getResourceInfo(resourceId);
            List<ResourceInfo> infoList = new ArrayList();
            infoList.add(info);
            
            ResourceAccessSetMessage msg = new ResourceAccessSetMessage(infoList, body);            
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            String pluginId = info.getPluginId();
            if(pluginId == null) {
                log.error("No plugin found");
                throw new Exception("No plugin associated with resource");
            }            
            Optional<PluginInfo> lst = pluginRepo.findById(pluginId);
            if(lst == null || !lst.isPresent()) {
                log.error("No plugin registered with id " + pluginId);
                throw new Exception("No plugin registered with id " + pluginId);
            }
            String pluginUrl = lst.get().getPluginURL();
            
            String url = pluginUrl + pluginRequestEndpoint;
            log.info("Sending POST request to " + url);
            log.debug("Message: ");
            log.debug(json);
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json);
            response = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Object.class);
            if (response == null) {
                log.error("No response from plugin");
                throw new ODataApplicationException("No response from plugin", HttpStatusCode.GATEWAY_TIMEOUT.getStatusCode(), Locale.ROOT);
            }
            
            if(!disableSecurity){
                try {
                    String serResponse = securityHandler.generateServiceResponse();
                    responseHeaders.set(SECURITY_RESPONSE_HEADER, serResponse);
                } catch (SecurityHandlerException sce){
                    log.error(sce.getMessage(), sce);
                    throw sce;
                }
            
            }
            sendSuccessfulAccessMessage(resourceId, SuccessfulAccessInfoMessage.AccessType.NORMAL.name());
            
        } catch(EntityNotFoundException e) {
            log.error(e.getMessage(),e);           
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, e.getHttpStatus());
        } catch (Exception e) {            
            log.error(e.getMessage(), e);
            sendFailAccessMessage(path, resourceId, e);
            return new ResponseEntity<>(e.getMessage(), responseHeaders, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        
        return new ResponseEntity<>(response , responseHeaders, HttpStatus.OK);
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }
    
    public boolean checkAccessPolicies(HttpServletRequest request, String resourceId) throws Exception {
        Map<String, String> secHdrs = new HashMap();
        Enumeration<String> headerNames = request.getHeaderNames();

        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String header = headerNames.nextElement();
                secHdrs.put(header, request.getHeader(header));
            }
        }
        
        log.info("secHeaders: " + secHdrs);
        if(!disableSecurity) {
            SecurityRequest securityReq = new SecurityRequest(secHdrs);
            checkAuthorization(securityReq, resourceId);
        }
        return true;
    }
    
    private void checkAuthorization(SecurityRequest request, String resourceId) throws Exception {
        log.debug("RAP received a security request : " + request.toString());        
         // building dummy access policy
        Map<String, IAccessPolicy> accessPolicyMap = new HashMap<>();
        // to get policies here
        Optional<AccessPolicy> accPolicy = accessPolicyRepo.findById(resourceId);
        if(accPolicy == null)
            throw new Exception("No access policies for resource");
        
        accessPolicyMap.put(resourceId, accPolicy.get().getPolicy());

        Set<String> ids = null;
        try {
            ids = securityHandler.getSatisfiedPoliciesIdentifiers(accessPolicyMap, request);
        } catch (Exception e) {
            log.error("Exception thrown during checking policies:", e);
            throw new Exception(e.getMessage());
        }
        if(!ids.contains(resourceId))
            throw new Exception("Security Policy is not valid");
    }
    
    private String sendFailAccessMessage(String path, String symbioteId, Exception e) {
        String message = null;
        try{
            String jsonNotificationMessage = null;
            String appId = "";String issuer = ""; String validationStatus = "";
            ObjectMapper mapper = new ObjectMapper();
            message = e.getMessage();
            if(message == null)
                message = e.toString();

            String code;
            if(e.getClass().equals(EntityNotFoundException.class))
                code = Integer.toString(((EntityNotFoundException) e).getHttpStatus().value());
            else
                code = Integer.toString(HttpStatus.FORBIDDEN.value());

            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            List<Date> dateList = new ArrayList();
            dateList.add(new Date());
            ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(securityHandler,notificationUrl);
            try {
                notificationMessage.SetFailedAttempts(symbioteId, dateList,code, message, appId, issuer, validationStatus, path); 
                jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException jsonEx) {
                //log.error(jsonEx.getMessage());
            }
            notificationMessage.SendFailAttempts(jsonNotificationMessage);
        }catch(Exception ex){
            log.error("Error to send FailAccessMessage to CRAM");
            log.error(ex.getMessage(),ex);
        }
        return message;    
        
    }
    
    private void sendSuccessfulAccessMessage(String symbioteId, String accessType){
        try{
            String jsonNotificationMessage = null;
            if(accessType == null || accessType.isEmpty())
                accessType = SuccessfulAccessInfoMessage.AccessType.NORMAL.name();
            ObjectMapper map = new ObjectMapper();
            map.configure(SerializationFeature.INDENT_OUTPUT, true);
            map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

            List<Date> dateList = new ArrayList();
            dateList.add(new Date());
            ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(securityHandler,notificationUrl);
            try{
                notificationMessage.SetSuccessfulAttempts(symbioteId, dateList, accessType);
                jsonNotificationMessage = map.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException e) {
                //log.error(e.getMessage());
            }
            notificationMessage.SendSuccessfulAttempts(jsonNotificationMessage);
        }catch(Exception e){
            log.error("Error to send SetSuccessfulAttempts to CRAM");
            log.error(e.getMessage(),e);
        }
    }
}

