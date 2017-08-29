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
import eu.h2020.symbiote.cloud.model.data.observation.Property;
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.exceptions.GenericException;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
import eu.h2020.symbiote.ssp.rap.resources.messages.RequestResponseMessage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
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
    private static final String PathSdevGet = "/RequestResourceAgent";
    private static final String PathSdevPost = "/ActuateResourceAgent";

    @Autowired
    ResourcesRepository resourcesRepo;

    
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "Sensor/{resourceId}", method = RequestMethod.GET)
    public List<Observation> readResourceREST(@PathVariable String resourceId/*, @RequestHeader("X-Auth-Token") String token*/,@RequestBody String body, HttpServletRequest request) {
        List<Observation> obsList = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);
            //    checkToken(token);
            obsList = readResourcePrivate(resourceId, body, request);
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
        
    public List<Observation> readResourcePrivate(String resourceId, String body, HttpServletRequest request) {
        List<Observation> obsList = null;
        RestTemplate restTemplate = new RestTemplate();
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
            
            RequestResponseMessage responseMessage = restTemplate.postForObject(url, bodyReq, RequestResponseMessage.class);
            if(responseMessage != null && responseMessage.getValue() != null){
                Date dateNow = new Date();
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                String dateNowStr = df.format(dateNow);
                
                List<ObservationValue> obsValue = new ArrayList<ObservationValue>();
                Map<String,String> values = responseMessage.getValue();
                for(String key: values.keySet()){
                    Property p = new Property(key,"");
                    ObservationValue obValue = new ObservationValue(values.get(key), p, null);
                    obsValue.add(obValue);
                }
                Observation o = new Observation(resourceId,null,dateNowStr,dateNowStr,obsValue);
                obsList = new ArrayList<>();
                obsList.add(o);
            }
        }
        return obsList;
    }
    
    
    
    @RequestMapping(value="Actuator/{resourceId}", method=RequestMethod.POST)
    public ResponseEntity<?> writeResource(@PathVariable String resourceId, @RequestBody String body,
                                           /*@RequestHeader("X-Auth-Token") String token,*/ HttpServletRequest request) {
        ResponseEntity responseEntity = null;
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
        return new ResponseEntity<>(responseEntity,HttpStatus.OK);
    }
    
    public ResponseEntity<?> writeResourcePrivate(String resourceId, String body, HttpServletRequest request) {
        ResponseEntity responseEntity;
        ResourceInfo info = getResourceInfo(resourceId);
        String url = info.getHost();
        if (info.getPlatformId() != null) {
            // platform device
            url += (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        } else {
            // SDEV without platform
            url += PathSdevPost ;
        }
        responseEntity = forwardWriteRequestToUrl(url, body);
        return responseEntity;
    }

    private ResponseEntity<?> forwardWriteRequestToUrl(String url, String requestJson) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity(requestJson, headers);
        ResponseEntity response = restTemplate.postForObject(url, entity, ResponseEntity.class);

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
