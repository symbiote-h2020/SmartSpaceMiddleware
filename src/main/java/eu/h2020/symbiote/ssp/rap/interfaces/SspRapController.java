/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.interfaces;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.exceptions.GenericException;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.messages.RequestMessage;
import java.util.List;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;

/*
*
* @author Matteo Pardi <m.pardi@nextworks.it>
 */
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PUT, RequestMethod.GET})
@RestController
@RequestMapping("rap")
public class SspRapController {

    private static final Logger log = LoggerFactory.getLogger(SspRapController.class);
    private static final String PathPlatformGet = "/rap/Sensor/";
    private static final String PathPlatformPost = "/rap/Service/";
    private static final String PathSdevGet = "/RequestResourceAgent";

    @Autowired
    ResourcesRepository resourcesRepo;

    /**
     * Process.
     *
     * @param resourceId
     * @param request
     *
     * @return the response entity
     *
     */
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "**", method = RequestMethod.POST)
    public ResponseEntity<?> readResourceREST(@RequestBody String body /*, @RequestHeader("X-Auth-Token") String token*/,
             HttpServletRequest request) {
        String resourceId = null;
        try {
            ObjectMapper mapper = new ObjectMapper();
            RequestMessage requestMessage = mapper.readValue(body, RequestMessage.class);
            resourceId = requestMessage.getResourceId();
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);

            //checkToken(token);
            readResourcePrivate(resourceId, body, request);

            /*} catch(TokenValidationException e) {
            log.error(e.toString());*/
        } catch (Exception ex) {
            String err;
            if(resourceId == null || resourceId.isEmpty())
                err = "Cannot find resourceId in body request";
            else
                err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    public void writeResourcePrivate(String resourceId, String body, HttpServletRequest request) {
        ResourceInfo info = getResourceInfo(resourceId);

        if (info.getPlatformId() != null) {
            // platform device
            String url = info.getHost();
            url += PathPlatformPost + resourceId;
            forwardWriteRequestToUrl(url, body);
        } else {
            // SDEV without platform

            // qui formattare un json e inviarlo all'SDEV via REST (?)
            // con tutte le info necessarie (ResourceAccessSetMessage?)
        }
    }

    public List<Observation> readResourcePrivate(String resourceId, String body, HttpServletRequest request) {
        List<Observation> obsList;
        String url;
        String method ;
        String bodyReq;
        ResourceInfo info = getResourceInfo(resourceId);

        if (info.getPlatformId() != null) {
            // platform device
            url = info.getHost();
            url += PathPlatformGet + resourceId;
            method = "GET";
            bodyReq = null;
        } else {
            // SDEV without platform
            url = info.getHost();
            url += PathSdevGet;
            method = "POST";
            bodyReq = "{\"id\":\""+resourceId+"\"}";
        }
        obsList = forwardReadRequestToUrl(url,method,bodyReq);
        return obsList;
    }

    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if (!resInfo.isPresent()) {
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        }

        return resInfo.get();
    }

    private List<Observation> forwardReadRequestToUrl(String url, String method, String body) {
        RestTemplate restTemplate = new RestTemplate();
        List<Observation> response = null;
        if(method.equals("GET"))
            response = restTemplate.getForObject(url, List.class);
        else if (method.equals("POST"))
            response = restTemplate.postForObject(url, body, List.class);

        return response;
    }

    private ResponseEntity<?> forwardWriteRequestToUrl(String url, String requestJson) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity(requestJson, headers);
        ResponseEntity response = restTemplate.postForObject(url, entity, ResponseEntity.class);

        return response;
    }
}
