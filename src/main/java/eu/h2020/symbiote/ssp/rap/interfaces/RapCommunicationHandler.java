package eu.h2020.symbiote.ssp.rap.interfaces;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.managers.AuthorizationManager;
import eu.h2020.symbiote.ssp.rap.managers.AuthorizationResult;
import eu.h2020.symbiote.ssp.rap.managers.ServiceResponseResult;
import eu.h2020.symbiote.ssp.rap.messages.resourceAccessNotification.SuccessfulAccessInfoMessage;
import org.apache.olingo.server.api.ODataRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.*;


@Configuration
public class RapCommunicationHandler {

    private static final Logger log = LoggerFactory.getLogger(RapCommunicationHandler.class);

    public final String SECURITY_RESPONSE_HEADER = "x-auth-response";

    @Autowired
    private AuthorizationManager authManager;

    @Value("${symbiote.rap.cram.url}")
    private String notificationUrl;

    public boolean checkAccessPolicies(HttpServletRequest request, String resourceId) throws Exception {
        Map<String, String> secHdrs = new HashMap();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames != null && headerNames.hasMoreElements()) {
            String header = headerNames.nextElement();
            secHdrs.put(header, request.getHeader(header));
        }

        return checkAccessPolicies(secHdrs, resourceId);
    }

    public boolean checkAccessPolicies(ODataRequest request, String resourceId) throws Exception {
        Map<String,List<String>> headers = request.getAllHeaders();
        Map<String, String> secHdrs = new HashMap();
        for(String key : headers.keySet()) {
            secHdrs.put(key, request.getHeader(key));
        }

        return checkAccessPolicies(secHdrs, resourceId);
    }

    public boolean checkAccessPolicies(Map<String, String> secHdrs, List<String> resourceIdList) throws Exception {
        for(String resourceId: resourceIdList){
            checkAccessPolicies(secHdrs, resourceId);
        }
        return true;
    }

    private boolean checkAccessPolicies(Map<String, String> secHdrs, String resourceId) throws Exception {
        log.debug("secHeaders: " + secHdrs);
        SecurityRequest securityReq = new SecurityRequest(secHdrs);

        AuthorizationResult result = authManager.checkResourceUrlRequest(resourceId, securityReq);
        log.info(result.getMessage());

        return result.isValidated();
    }

    public HttpHeaders generateServiceResponse() {
        HttpHeaders resHdr = new HttpHeaders();
        ServiceResponseResult serResponse = authManager.generateServiceResponse();
        if (serResponse.isCreatedSuccessfully()) {
            resHdr.set(SECURITY_RESPONSE_HEADER, serResponse.getServiceResponse());
        }
        return resHdr;
    }

    public String sendFailAccessMessage(String path, String symbioteId, Exception e) {
        List<String> symIdList = new ArrayList();
        symIdList.add(symbioteId);
        return sendFailAccessMessage(path, symIdList, e);
    }

    public String sendFailAccessMessage(String path, List<String> symbioteIdList, Exception e) {
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
            ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(authManager, notificationUrl);
            try {
                notificationMessage.SetFailedAttemptsList(symbioteIdList, dateList,code, message, appId, issuer, validationStatus, path);
                jsonNotificationMessage = mapper.writeValueAsString(notificationMessage);
            } catch (JsonProcessingException jsonEx) {
                //log.error(jsonEx.getMessage());
            }
            notificationMessage.SendFailAccessMessage(jsonNotificationMessage);
        }catch(Exception ex){
            log.error("Error to send FailAccessMessage to CRAM");
            log.error(ex.getMessage(),ex);
        }
        return message;

    }

    public void sendSuccessfulAccessMessage(String symbioteId, String accessType) {
        List<String> symIdList = new ArrayList();
        symIdList.add(symbioteId);
        sendSuccessfulAccessMessage(symIdList, accessType);
    }

    public void sendSuccessfulAccessMessage(List<String> symbioteIdList, String accessType){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<Date> dateList = new ArrayList();
        dateList.add(new Date());
        ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(authManager,notificationUrl);

        try{
            notificationMessage.SetSuccessfulAttemptsList(symbioteIdList, dateList, accessType);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        notificationMessage.SendSuccessfulAttemptsMessage(jsonNotificationMessage);
    }

    public void sendSuccessfulPushMessage(String symbioteId){
        String jsonNotificationMessage = null;
        ObjectMapper map = new ObjectMapper();
        map.configure(SerializationFeature.INDENT_OUTPUT, true);
        map.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);

        List<Date> dateList = new ArrayList();
        dateList.add(new Date());
        ResourceAccessCramNotification notificationMessage = new ResourceAccessCramNotification(authManager,notificationUrl);

        try{
            notificationMessage.SetSuccessfulPushes(symbioteId, dateList);
            jsonNotificationMessage = map.writeValueAsString(notificationMessage);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
        notificationMessage.SendSuccessfulPushMessage(jsonNotificationMessage);
    }
}
