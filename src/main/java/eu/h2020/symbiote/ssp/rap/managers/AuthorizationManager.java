/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.managers;

import com.fasterxml.jackson.core.JsonProcessingException;
import eu.h2020.symbiote.security.ComponentSecurityHandlerFactory;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.communication.payloads.SecurityRequest;
import eu.h2020.symbiote.security.handler.IComponentSecurityHandler;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Component responsible for dealing with Symbiote Tokens and checking access right for requests.
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Component()
public class AuthorizationManager {

    private static Log log = LogFactory.getLog(AuthorizationManager.class);

    private final String componentOwnerName;
    private final String componentOwnerPassword;
    private final String localAamAddress;
    private final String sspId;
    private final String keystoreName;
    private final String keystorePass;
    private Boolean securityEnabled;
    
    @Autowired
    private ResourcesRepository resourcesRepository;


    private IComponentSecurityHandler componentSecurityHandler;

    @Autowired
    public AuthorizationManager(@Value("${symbIoTe.component.username}") String componentOwnerName,
                                @Value("${symbIoTe.component.password}") String componentOwnerPassword,
                                @Value("${symbIoTe.core.interface.url}") String coreAamAddress,
                                @Value("${symbIoTe.localaam.url}") String localAamAddress,
                                @Value("${ssp.id}") String sspId,
                                @Value("${symbIoTe.component.keystore.path}") String keystoreName,
                                @Value("${symbIoTe.component.keystore.password}") String keystorePass,
                                @Value("${rap.security.enabled:true}") Boolean securityEnabled,
                                @Value("${symbIoTe.validation.localaam}") Boolean alwaysUseLocalAAMForValidation)
            throws SecurityHandlerException, InvalidArgumentsException {

        Assert.notNull(componentOwnerName,"componentOwnerName can not be null!");
        this.componentOwnerName = componentOwnerName;

        Assert.notNull(componentOwnerPassword,"componentOwnerPassword can not be null!");
        this.componentOwnerPassword = componentOwnerPassword;
        
        Assert.notNull(localAamAddress,"localAamAddress can not be null!");
        this.localAamAddress = localAamAddress;

        Assert.notNull(sspId,"sspId can not be null!");
        this.sspId = sspId;

        Assert.notNull(keystoreName,"keystoreName can not be null!");
        this.keystoreName = keystoreName;

        Assert.notNull(keystorePass,"keystorePass can not be null!");
        this.keystorePass = keystorePass;

        Assert.notNull(securityEnabled,"securityEnabled can not be null!");
        this.securityEnabled = securityEnabled;

        if (securityEnabled)
            enableSecurity();
    }

    public AuthorizationResult checkResourceUrlRequest(String resourceId, SecurityRequest securityRequest) {
        if (securityEnabled) {
            log.debug("Received SecurityRequest of ResourceUrlsRequest to be verified: (" + securityRequest + ")");

            if (securityRequest == null) {
                return new AuthorizationResult("SecurityRequest is null", false);
            }

            Set<String> checkedPolicies;
            try {
                checkedPolicies = checkStoredResourcePolicies(securityRequest, resourceId);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                return new AuthorizationResult(e.getMessage(), false);

            }

            if (checkedPolicies.size() == 1) {
                return new AuthorizationResult("ok", true);
            } else {
                return new AuthorizationResult("The stored resource access policy was not satisfied",
                        false);
            }
        } else {
            log.debug("checkAccess: Security is disabled");
            //if security is disabled in properties
            return new AuthorizationResult("Security disabled", true);
        }
    }
    
    public ServiceRequest getServiceRequestHeaders(){
        if (securityEnabled) {
            try {
                Map<String, String> securityRequestHeaders = null;        
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.set("Accept", MediaType.APPLICATION_JSON_VALUE);
                httpHeaders.setContentType(MediaType.APPLICATION_JSON);

                SecurityRequest securityRequest = componentSecurityHandler.generateSecurityRequestUsingLocalCredentials();
                securityRequestHeaders = securityRequest.getSecurityRequestHeaderParams();

                for (Map.Entry<String, String> entry : securityRequestHeaders.entrySet()) {
                    httpHeaders.add(entry.getKey(), entry.getValue());
                }
                log.info("request headers: " + httpHeaders);
                
                return new ServiceRequest(httpHeaders, true);
            } catch (SecurityHandlerException | JsonProcessingException e) {
                log.error("Fail to take header", e);
                return new ServiceRequest(new HttpHeaders(), false);
            }
        } else {
            log.debug("generateServiceRequest: Security is disabled");
            return new ServiceRequest(new HttpHeaders(), false);
        }
    }

    public ServiceResponseResult generateServiceResponse() {
        if (securityEnabled) {
            try {
                String serviceResponse = componentSecurityHandler.generateServiceResponse();
                return new ServiceResponseResult(serviceResponse, true);
            } catch (SecurityHandlerException e) {
                log.error(e.getMessage(), e);
                return new ServiceResponseResult("", false);
            }
        } else {
            log.debug("generateServiceResponse: Security is disabled");
            return new ServiceResponseResult("", false);
        }
    }

    private void enableSecurity() throws SecurityHandlerException {
        securityEnabled = true;
        componentSecurityHandler = ComponentSecurityHandlerFactory.getComponentSecurityHandler(
                keystoreName,
                keystorePass,
                "S3M-rap@" + sspId,
                localAamAddress,
                componentOwnerName,
                componentOwnerPassword);
        
        // workaround to speed up following calls
        componentSecurityHandler.generateServiceResponse();

    }
    
    private Set<String> checkStoredResourcePolicies(SecurityRequest request, String resourceId) {
        Set<String> ids = null;
        try {       

            log.debug("Received a security request : " + request.toString());
             // building dummy access policy
            Map<String, IAccessPolicy> accessPolicyMap = new HashMap<>();
            // to get policies here
            Optional<ResourceInfo> resourceInfo = resourcesRepository.findById(resourceId);
            if(resourceInfo == null) {
                log.error("No access policies for resource");
                return ids;
            }
            accessPolicyMap.put(resourceId, resourceInfo.get().getAccessPolicy());
            String mapString = accessPolicyMap.entrySet().stream().map(entry -> entry.getKey() + " - " + entry.getValue())
                    .collect(Collectors.joining(", "));
            log.info("accessPolicyMap: " + mapString);
            log.info("request: " + request.toString());

            ids = componentSecurityHandler.getSatisfiedPoliciesIdentifiers(accessPolicyMap, request);
        } catch (Exception e) {
            log.error("Exception thrown during checking policies: " + e.getMessage(), e);
        }
        
        return ids;
    }

    /**
     * Setters and Getters
     * @return 
     */

    public IComponentSecurityHandler getComponentSecurityHandler() {
        return componentSecurityHandler;
    }

    public void setComponentSecurityHandler(IComponentSecurityHandler componentSecurityHandler) {
        this.componentSecurityHandler = componentSecurityHandler;
    }
}