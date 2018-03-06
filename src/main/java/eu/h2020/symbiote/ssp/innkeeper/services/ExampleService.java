package eu.h2020.symbiote.ssp.innkeeper.services;

import eu.h2020.symbiote.ssp.innkeeper.helpers.AuthorizationServiceHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vasileios Glykantzis (ICOM)
 * @since 2/22/2018.
 */
@Service
public class ExampleService {
    private static Log log = LogFactory.getLog(ExampleService.class);

    private AuthorizationService authorizationService;

    /**
     * Constructor
     *
     * @param authorizationService the AuthorizationService
     */
    @Autowired
    public ExampleService(AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * Handles the requests to the /innkeeper/example of the ExampleController
     *
     * @param httpHeaders the HTTP headers of the client's request
     * @return the response to the client
     */
    public ResponseEntity handleExampleRequest(HttpHeaders httpHeaders) {
        log.trace("handleExampleRequest");

        ResponseEntity securityChecks = AuthorizationServiceHelper.checkSecurityRequestAndCreateServiceResponse(
                authorizationService, httpHeaders);

        // We check the securityChecks code. If it is not OK, we just returned as is to the client. The body of the
        // securityChecks contains a helpful notice of what went wrong. If the status is ok, then the body contains
        // the serviceResponse
        if (securityChecks.getStatusCode() != HttpStatus.OK)
            return securityChecks;

        // Creating just a mock object as a response
        Map<String, Object> response = new HashMap<>();

        // We add the service response, which is included in securityChecks, to the response
        return AuthorizationServiceHelper.addSecurityService(response, new HttpHeaders(),
                HttpStatus.OK, (String) securityChecks.getBody());
    }


}
