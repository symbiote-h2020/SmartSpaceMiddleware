/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.pushNotificationService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessMessage;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessSubscribeMessage;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.interfaces.conditions.NBInterfaceWebSocketCondition;
import eu.h2020.symbiote.ssp.rap.messages.access.ResourceAccessUnSubscribeMessage;
import eu.h2020.symbiote.ssp.rap.interfaces.ResourceAccessCramNotification;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.PluginInfo;
import eu.h2020.symbiote.ssp.resources.db.PluginRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import static eu.h2020.symbiote.security.commons.SecurityConstants.SECURITY_RESPONSE_HEADER;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.ssp.rap.pushNotificationService.WebSocketMessage.Action;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Conditional(NBInterfaceWebSocketCondition.class)
@Component
@CrossOrigin 
public class WebSocketController extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebSocketController.class);

    @Autowired
    ResourcesRepository resourcesRepo;
    
    @Autowired
    PluginRepository pluginRepo;
    
    @Autowired
    private AccessPolicyRepository accessPolicyRepo;
    
    @Value("${symbiote.rap.cram.url}") 
    private String notificationUrl;
    
    @Autowired
    private IComponentSecurityHandler securityHandler;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Value("${rap.debug.disableSecurity}")
    private Boolean disableSecurity;

    private final HashMap<String, WebSocketSession> idSession = new HashMap();

    @Override
    public void handleTransportError(WebSocketSession session, Throwable throwable) throws Exception {
        log.error("error occured at sender " + session, throwable);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        log.info("Session " + session.getId() + " closed with status " + status.getCode());
        idSession.remove(session.getId());

        //update DB
        List<ResourceInfo> resInfoList = resourcesRepo.findAll();
        if (resInfoList != null) {
            for (ResourceInfo resInfo : resInfoList) {
                List<String> sessionsIdOfRes = resInfo.getSessionId();
                if (sessionsIdOfRes != null) {
                    sessionsIdOfRes.remove(session.getId());
                    resInfo.setSessionId(sessionsIdOfRes);
                    resourcesRepo.save(resInfo);
                }
            }
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        log.info("Connected ... " + session.getId());
        idSession.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage jsonTextMessage) throws Exception  {
        Exception e = null;
        HttpStatus code = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = "";
        try {
            message = jsonTextMessage.getPayload();
            log.info("message received: " + message);

            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);        
            WebSocketMessageSecurityRequest webSocketMessageSecurity = mapper.readValue(message, WebSocketMessageSecurityRequest.class);
            
            Map<String,String> securityRequest = webSocketMessageSecurity.getSecRequest();
            if(securityRequest == null && !disableSecurity)
                throw new Exception("Security Request cannot be empty");
            
            WebSocketMessage webSocketMessage = webSocketMessageSecurity.getPayload();
            List<String> resourcesId = webSocketMessage.getIds();
            log.debug("Ids: " + resourcesId);
            
            checkAccessPolicies(securityRequest,resourcesId);
            
            Action act = webSocketMessage.getAction();
            switch(act) {
                case SUBSCRIBE:
                    log.debug("Subscribing resources..");
                    Subscribe(session, resourcesId, securityRequest);
                    break;
                case UNSUBSCRIBE:
                    log.debug("Unsubscribing resources..");
                    Unsubscribe(session, resourcesId, securityRequest);
                    break;
            }
        } catch (JsonParseException jsonEx){
            code = HttpStatus.BAD_REQUEST;
            e = jsonEx;
            log.error(e.getMessage(), e);
        } catch (IOException ioEx) {
            code = HttpStatus.BAD_REQUEST;
            e = ioEx;
            log.error(e.getMessage(), e);
        } catch (EntityNotFoundException entityEx){
            code = entityEx.getHttpStatus();
            e = entityEx;
            log.error(e.getMessage(), e);
        } catch (Exception ex) {
            e = ex;
            code = HttpStatus.BAD_REQUEST;
            log.error("Generic IO Exception: " + e.getMessage(), e);
        }
        
        if(e != null){
            session.sendMessage(new TextMessage(code.name()+ " " + e.getMessage()));
            sendFailMessage(message, e);
        }
    }
    
    private void Subscribe(WebSocketSession session, List<String> resourcesId, Map<String,String> securityReq) throws Exception {
        HashMap<String, List> subscribeList = new HashMap();
        for (String resId : resourcesId) {
            // adding new resource info to subscribe map, with pluginUrl as key
            ResourceInfo resInfo = getResourceInfo(resId);            
            String pluginId = resInfo.getPluginId();
            Optional<PluginInfo> plg = pluginRepo.findById(pluginId);
            if(plg == null || !plg.isPresent())
                throw new Exception("No plugin found");
            
            String pluginUrl = plg.get().getPluginURL();
            log.debug("Found plugin with id " + pluginId + " - url: " + pluginUrl);
            
            List<ResourceInfo> rl = (subscribeList.containsKey(pluginUrl)) ? subscribeList.get(pluginUrl) : new ArrayList();
            rl.add(resInfo);
            subscribeList.put(pluginUrl, rl);
            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes == null) {
                sessionsIdOfRes = new ArrayList();
            }
            sessionsIdOfRes.add(session.getId());
            resInfo.setSessionId(sessionsIdOfRes);
            resourcesRepo.save(resInfo);
            
            log.debug("Resource stored in session for plugin with id " + pluginId);
        }
        
        for(String pluginUrl : subscribeList.keySet() ) {
            List<ResourceInfo> resList = subscribeList.get(pluginUrl);
            ResourceAccessSubscribeMessage msg = new ResourceAccessSubscribeMessage(resList);
   
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
            headers.setContentType(MediaType.APPLICATION_JSON);
            for(String key : securityReq.keySet()) {
                headers.set(key, securityReq.get(key));
            }
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);
            ResponseEntity<?> responseEntity = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, Object.class);
            
            sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessInfoMessage.AccessType.SUBSCRIPTION_START.name());
            
            if(responseEntity.getStatusCode() == HttpStatus.OK || 
               responseEntity.getStatusCode() == HttpStatus.ACCEPTED)
                log.debug("Subscription for resources [" + resourcesId + "] successfully sent to " + pluginUrl);
            else
                log.warn("Error while sending subscription for resources [" + resourcesId + "] to " + pluginUrl);
            
        }        
    }
    
    private void Unsubscribe(WebSocketSession session, List<String> resourcesId, Map<String,String> securityReq) throws Exception {
        HashMap<String, List> unsubscribeList = new HashMap();
        for (String resId : resourcesId) {
            // adding new resource info to subscribe map, with pluginUrl as key
            ResourceInfo resInfo = getResourceInfo(resId);            
            String pluginId = resInfo.getPluginId();
            Optional<PluginInfo> plg = pluginRepo.findById(pluginId);
            if(plg == null || !plg.isPresent())
                throw new Exception("No plugin found");
            
            String pluginUrl = plg.get().getPluginURL();
            log.debug("Found plugin with id " + pluginId + " - url: " + pluginUrl);
            
            List<ResourceInfo> rl = (unsubscribeList.containsKey(pluginUrl)) ? unsubscribeList.get(pluginUrl) : new ArrayList();
            rl.add(resInfo);
            unsubscribeList.put(pluginId, rl);
            //update DB
            List<String> sessionsIdOfRes = resInfo.getSessionId();
            if (sessionsIdOfRes != null) {
                sessionsIdOfRes.remove(session.getId());
                resInfo.setSessionId(sessionsIdOfRes);
                resourcesRepo.save(resInfo);
                
                log.debug("Resource removed from session for plugin with id " + pluginId);
            } else {
                log.debug("Resource with id " + resInfo.getSymbioteId() + " not found ");
            }
            
        }
        
        for(String pluginUrl : unsubscribeList.keySet() ) {
            List<ResourceInfo> resList = unsubscribeList.get(pluginUrl);
            ResourceAccessUnSubscribeMessage msg = new ResourceAccessUnSubscribeMessage(resList);
   
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
            String json = mapper.writeValueAsString(msg);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
            headers.setContentType(MediaType.APPLICATION_JSON);
            for(String key : securityReq.keySet()) {
                headers.set(key, securityReq.get(key));
            }
            
            HttpEntity<String> httpEntity = new HttpEntity<>(json, headers);
            ResponseEntity<?> responseEntity = restTemplate.exchange(pluginUrl, HttpMethod.POST, httpEntity, Object.class);
            
            sendSuccessfulAccessMessage(resourcesId, SuccessfulAccessInfoMessage.AccessType.SUBSCRIPTION_END.name());
            
            if(responseEntity.getStatusCode() == HttpStatus.OK || 
               responseEntity.getStatusCode() == HttpStatus.ACCEPTED)
                log.debug("Subscription for resources [" + resourcesId + "] successfully sent to " + pluginUrl);
            else
                log.warn("Error while sending subscription for resources [" + resourcesId + "] to " + pluginUrl);
            
        }
    }

    public void SendMessage(String resourceId, String payload) {
        Map<String,String> secResponse = new HashMap();
        if(!disableSecurity){
            try{
                String serResponse = securityHandler.generateServiceResponse();
                secResponse.put(SECURITY_RESPONSE_HEADER, serResponse);
            }
            catch(SecurityHandlerException sce){
                log.error(sce.getMessage(), sce);
            }
        }
        
        WebSocketMessageSecurityResponse messageSecurityResp = new WebSocketMessageSecurityResponse(secResponse, payload);
        
        ResourceInfo resInfo = getResourceByInternalId(resourceId);
        List<String> sessionIdList = resInfo.getSessionId();
        HashSet<WebSocketSession> sessionList = new HashSet<>();
        if (sessionIdList != null && !sessionIdList.isEmpty()) {
            for (String sessionId : sessionIdList) {
                WebSocketSession session = idSession.get(sessionId);
                if(session != null)
                    sessionList.add(session);
            }

            String mess = "";
            try {
                ObjectMapper map = new ObjectMapper();
                map.configure(SerializationFeature.INDENT_OUTPUT, true);
                map.setSerializationInclusion(JsonInclude.Include.NON_NULL);
                mess = map.writeValueAsString(messageSecurityResp);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            sendAll(sessionList, mess);
        }
    }

    private static void sendAll(Set<WebSocketSession> sessionList, String msg) {
        for (WebSocketSession session : sessionList) {
            try {
                session.sendMessage(new TextMessage(msg));
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    private ResourceInfo getResourceInfo(String resId) {
        ResourceInfo resInfo = null;
        Optional<ResourceInfo> resInfoOptional = resourcesRepo.findById(resId);
        if(!resInfoOptional.isPresent())
            throw new EntityNotFoundException(resId);
        
        resInfo = resInfoOptional.get();
        return resInfo;
    }
    
    private ResourceInfo getResourceByInternalId(String internalId) {
        ResourceInfo resInfo = null;
        try {
            List<ResourceInfo> resInfoList = resourcesRepo.findByInternalId(internalId);
            if (resInfoList != null && !resInfoList.isEmpty()) {
                for(ResourceInfo ri: resInfoList){
                    resInfo = ri;
                    List<String> sessionsId = ri.getSessionId();
                    if(sessionsId != null && !sessionsId.isEmpty())
                        break;
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return resInfo;
    }
    
    
    public void sendSuccessfulAccessMessage(List<String> symbioteIdList, String accessType){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
        
        List<Date> dateList = new ArrayList();
        dateList.add(new Date());
        ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(securityHandler,notificationUrl);
        
        try{
            notificationMessage.SetSuccessfulAttemptsList(symbioteIdList, dateList, accessType);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        notificationMessage.SendSuccessfulAttempts(jsonNotificationMessage);
    }
    
    private void sendFailMessage(String path, Exception e) {
        String jsonNotificationMessage = null;
        String appId = "";String issuer = ""; String validationStatus = "";
        String symbioteId = "";
        ObjectMapper mapper = new ObjectMapper();
        
        String code = Integer.toString(HttpStatus.INTERNAL_SERVER_ERROR.value());
        String message = e.getMessage();
        if(message == null)
            message = e.toString();
        
        if(e.getClass().equals(EntityNotFoundException.class)){
            code = Integer.toString(((EntityNotFoundException) e).getHttpStatus().value());
            symbioteId = ((EntityNotFoundException) e).getSymbioteId();
        }
            
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        List<Date> dateList = new ArrayList();
        dateList.add(new Date());
        ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(securityHandler,notificationUrl);
        try {
            notificationMessage.SetFailedAttempts(symbioteId, dateList, 
            code, message, appId, issuer, validationStatus, path); 
            jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException jsonEx) {
            log.error(jsonEx.getMessage());
        }
        notificationMessage.SendFailAttempts(jsonNotificationMessage);
    }
    
    public boolean checkAccessPolicies(Map<String, String> secHdrs, List<String> resourceIdList) throws Exception {
        if(!disableSecurity){
            log.info("secHeaders: " + secHdrs);
            SecurityRequest securityReq = new SecurityRequest(secHdrs);

            for(String resourceId: resourceIdList){
                checkAuthorization(securityReq, resourceId);
            }
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
}
