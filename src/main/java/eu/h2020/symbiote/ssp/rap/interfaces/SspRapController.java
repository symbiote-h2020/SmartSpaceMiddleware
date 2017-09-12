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
import eu.h2020.symbiote.cloud.model.data.observation.Observation;
import eu.h2020.symbiote.cloud.model.data.observation.ObservationValue;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.exceptions.GenericException;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
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
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

/*
*
* @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@CrossOrigin(origins = "*", methods = {RequestMethod.POST, RequestMethod.OPTIONS, RequestMethod.PUT, RequestMethod.GET})
@RestController
@RequestMapping("rap")
public class SspRapController {

    private static final Logger log = LoggerFactory.getLogger(SspRapController.class);
    private static final String PathSdevGet = "/RequestResourceAgent";
    private static final String PathSdevPost = "/ActuateResourceAgent";

    @Autowired
    ResourcesRepository resourcesRepo;

    
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "Sensor/{resourceId}", method = RequestMethod.GET)
    public List<Observation> readResourceREST(@PathVariable String resourceId/*, @RequestHeader("X-Auth-Token") String token,@RequestBody String body*/, HttpServletRequest request) {
        List<Observation> obsList = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);
            //    checkToken(token);
            obsList = readResourcePrivate(resourceId, request);
            /* } catch (TokenValidationException tokenEx) { 
            log.error(tokenEx.toString());
            throw tokenEx;*/
        } catch (Exception e) {
            String err = "Unable to read resource with id: " + resourceId;
            log.error(err + "\n" + e.getMessage());
            throw new GenericException(e.getMessage());
        }
        return obsList;
    }
        
    public List<Observation> readResourcePrivate(String resourceId, HttpServletRequest request) {
        List<Observation> obsList = null;
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        String url;
        ResourceInfo info = getResourceInfo(resourceId);
        
        if (info.getPlatformId() != null) {
            // platform device
            url = info.getHost();
            url += (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            obsList = restTemplate.getForObject(url, List.class);
        } else {
            // SDEV without platform
            url = info.getHost();
            url += PathSdevGet;
            String bodyReq = "{\"id\":\""+resourceId+"\"}";
            
            ObservationValue[] obsValue = restTemplate.postForObject(url, bodyReq, ObservationValue[].class);
            if(obsValue != null && obsValue.length > 0){
                Date dateNow = new Date();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                String dateNowStr = df.format(dateNow);
                Observation o = new Observation(resourceId,null,dateNowStr,dateNowStr,Arrays.asList(obsValue));
                obsList = new ArrayList<>();
                obsList.add(o);
            }
        }
        return obsList;
    }
    
    
    
    @RequestMapping(value="Actuator/{resourceId}", method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String body,
                                           /*@RequestHeader("X-Auth-Token") String token,*/ HttpServletRequest request) {
        String responseEntity = null;
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + body);

            //checkToken(token);
            responseEntity = writeResourcePrivate(resourceId, body, request);

            /*} catch(TokenValidationException e) {
            log.error(e.toString());*/
        } catch (Exception ex) {
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
        return new ResponseEntity<String>(responseEntity,HttpStatus.OK);
    }
    
    public String writeResourcePrivate(String resourceId, String body, HttpServletRequest request) {
        String responseEntity;
        ResourceInfo info = getResourceInfo(resourceId);
        String url = info.getHost();
        if (info.getPlatformId() != null) {
            // platform device
            url += (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        } else {
            // SDEV without platform
            url += PathSdevPost ;
            if(!body.contains(resourceId)){
                body = body.trim().substring(0, body.length() - 1) + ",\"id\":\"" + resourceId + "\"}";
            }
        }
        responseEntity = forwardWriteRequestToUrl(url, body);
        return responseEntity;
    }

    private String forwardWriteRequestToUrl(String url, String requestJson) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
        restTemplate.getMessageConverters().add(new StringHttpMessageConverter());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity(requestJson, headers);
        String response = restTemplate.postForObject(url, entity, String.class);

        return response;
    }

    
    
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if (!resInfo.isPresent()) {
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        }

        return resInfo.get();
    }
}
