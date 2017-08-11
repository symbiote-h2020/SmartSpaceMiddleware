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
import eu.h2020.symbiote.ssp.rap.exceptions.EntityNotFoundException;
import eu.h2020.symbiote.ssp.rap.exceptions.GenericException;
import eu.h2020.symbiote.ssp.rap.resources.ResourceInfo;
import eu.h2020.symbiote.ssp.rap.resources.ResourcesRepository;
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
    @RequestMapping(value = "**", method=RequestMethod.GET)
    public List<Observation> readResourceRequest(@PathVariable String resourceId/*, @RequestHeader("X-Auth-Token") String token*/, HttpServletRequest request) {
        List<Observation> obsList = null;
        try {
            log.info("Received read resource request for ID = " + resourceId);       
        //    checkToken(token);
        
            ResourceInfo info = getResourceInfo(resourceId);
            if(info.getPlatformId() != null) {
                // platform device
                String url = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
                obsList = forwardReadRequestToUrl(url);
            } else {
                // SDEV without platform
                
                // qui formattare un json e inviarlo all'SDEV via REST (?)
                // con tutte le info necessarie (ResourceAccessGetMessage?)               
            }
            
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
    
    @CrossOrigin(origins = "*")
    @RequestMapping(value = "**", method=RequestMethod.PUT)
    public ResponseEntity<?> writeResourceRequest(@PathVariable String resourceId, @RequestBody String values
            /*, @RequestHeader("X-Auth-Token") String token*/, HttpServletRequest request) {
        try {
            log.info("Received write resource request for ID = " + resourceId + " with values " + values);
            
            //checkToken(token);

            ResourceInfo info = getResourceInfo(resourceId);
            
            if(info.getPlatformId() != null) {
                // platform device
                String url = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
                forwardWriteRequestToUrl(url, values);
            } else {
                // SDEV without platform
                
                // qui formattare un json e inviarlo all'SDEV via REST (?)
                // con tutte le info necessarie (ResourceAccessSetMessage?)
            }
            
        /*} catch(TokenValidationException e) {
            log.error(e.toString());*/
        } catch (Exception ex) {
            String err = "Unable to write resource with id: " + resourceId;
            log.error(err + "\n" + ex.getMessage());
            throw new GenericException(ex.getMessage());
        }
        
        return new ResponseEntity<>(HttpStatus.OK);
    }
    
    private ResourceInfo getResourceInfo(String resourceId) {
        Optional<ResourceInfo> resInfo = resourcesRepo.findById(resourceId);
        if(!resInfo.isPresent())
            throw new EntityNotFoundException("Resource " + resourceId + " not found");
        
        return resInfo.get();
    }
    
    private List<Observation> forwardReadRequestToUrl(String url) {
        RestTemplate restTemplate = new RestTemplate();
        List<Observation> response = restTemplate.postForObject(url, "", List.class);
        
        return response;
    }

    private ResponseEntity<?> forwardWriteRequestToUrl(String url, String requestJson) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity(requestJson,headers);
        ResponseEntity response = restTemplate.postForObject(url, entity, ResponseEntity.class);
        
        return response;
    }    
}
