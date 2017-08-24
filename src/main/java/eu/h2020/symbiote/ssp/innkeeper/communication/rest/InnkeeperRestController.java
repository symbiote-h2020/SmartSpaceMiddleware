package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.communication.rest.JoinRequest;
import eu.h2020.symbiote.ssp.communication.rest.JoinResponse;
import eu.h2020.symbiote.ssp.communication.rest.JoinResponseResult;
import eu.h2020.symbiote.ssp.exception.InvalidMacAddressException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

/**
 * Created by vasgl on 8/24/2017.
 */

@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {

    private static Log log = LogFactory.getLog(InnkeeperRestController.class);

    private Integer registrationExpiration;

    private ResourceRepository resourceRepository;

    @Autowired
    public InnkeeperRestController(ResourceRepository resourceRepository,
                                   @Qualifier("registrationExpiration") Integer registrationExpiration) {

        Assert.notNull(resourceRepository,"Resource repository can not be null!");
        this.resourceRepository = resourceRepository;

        Assert.notNull(registrationExpiration,"registrationExpiration can not be null!");
        this.registrationExpiration = registrationExpiration;
    }

    @PostMapping(InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH)
    ResponseEntity<JoinResponse> join(@RequestBody JoinRequest joinRequest) {

        log.info("New join request was received for resource id = " + joinRequest.getId());

        if (joinRequest.getId() != null && joinRequest.getId().isEmpty())
            joinRequest.setId(null);

        InnkeeperResource newResource = resourceRepository.save(new InnkeeperResource(joinRequest));
        log.info("newResource.getId() = " + newResource.getId());

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.OK, newResource.getId(),
                "", registrationExpiration);
        return ResponseEntity.ok(joinResponse);
    }


    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<JoinResponse> httpMessageNotReadableExceptionHandler(HttpServletRequest req) {
        ObjectMapper mapper = new ObjectMapper();
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setContentType(MediaType.APPLICATION_JSON);

        try {
            String requestInString = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            if (req.getMethod().equals(HttpMethod.POST.toString()) &&
                    req.getPathInfo().equals(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH +
                            InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH)) {
                JoinRequest joinRequest = mapper.readValue(requestInString, JoinRequest.class);
            }
        } catch (JsonMappingException e) {
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));

            if (sw.toString().contains(InvalidMacAddressException.class.getName())) {
                JoinResponse joinResponse = new JoinResponse(JoinResponseResult.INVALID_MAC_ADDRESS_FORMAT, null, null, null);
                return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
            } else {
                JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
                return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
            }
        } catch (IOException e) {
            e.printStackTrace();
            JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
            return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
        }

        JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
        return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
    }

}
