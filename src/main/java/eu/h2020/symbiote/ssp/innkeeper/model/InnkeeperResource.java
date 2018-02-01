package eu.h2020.symbiote.ssp.innkeeper.model;

	

import java.util.HashSet;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

public class InnkeeperResource {
	private static Log log = LogFactory.getLog(InnkeeperRestController.class);

	private Map<String,String> payload;
	private Integer expirationTime;
	
	
	SessionRepository sessionRepository;
	ResourcesRepository resourcesRepository;
	
	public InnkeeperResource(Map<String,String> payload,	
					SessionRepository sessionRepository,
					ResourcesRepository resourcesRepository) {
		this.payload = payload;
		this.sessionRepository = sessionRepository;
		this.resourcesRepository = resourcesRepository;
		
	}
	
	public Map<String, String> getPayload(){
		return payload;
	}

	public ResponseEntity<Object> requestHandler() {
		ResponseEntity<Object> responseEntity= null;
		
		if (new HashSet<String>( this.payload.keySet()).equals( 
				new HashSet<String>(InnkeeperRestControllerConstants.JOIN_RESOURCE_PAYLOAD_VALS))){
			log.info("JOIN RESOURCE");
			log.info(payload);
			// DO SOMETHING FOR JOIN RESOURCE
			/* TODO:
			 * 1. check if SDEV is registered
			 * 2. check if the current resource is registered
			 * 3. update mongoDB
			 * 4. give some feedback to SDEV 
			 */


			//forge response
			HttpHeaders headers =new HttpHeaders();
			headers.add("1", "this is a SDEV");
			String mybody="<html>OK, this is a SDEV, here some BODY</html>\n";
			responseEntity=new ResponseEntity<Object>(mybody,headers, HttpStatus.OK);
						
		}
		
		if (new HashSet<String>( this.payload.keySet()).equals( 
				new HashSet<String>(InnkeeperRestControllerConstants.PLATFORM_REGISTRY_PAYLOAD_VALS))){
			log.info("registry PLATFORM");
			log.info(payload);
			// DO SOMETHING FOR PLATFORM REGISTRY
			/* TODO:
			 * 1. check if PLATFORM is registered in the CORE
			 * 2. check if PLATFORM is registered in LAAM 
			 * 3. give some feedback to SDEV 
			 */
			
			HttpHeaders headers =new HttpHeaders();
			headers.add("1", "this is a PLATFORM");
			String mybody="<html>OK, PLATFORM REGISTRY, here some BODY</html>\n";
			responseEntity=new ResponseEntity<Object>(mybody,headers, HttpStatus.OK);
		}
		
		if (new HashSet<String>( this.payload.keySet()).equals( 
				new HashSet<String>(InnkeeperRestControllerConstants.SDEV_REGISTRY_PAYLOAD_VALS))){
			log.info("registry SDEV");
			log.info(payload);
			// DO SOMETHING FOR SDEV REGISTRY
			
			/* TODO:
			 * 1. Uncypher data
			 * 2. check if SDEV is registered L3/L4
			 * 2. update mongoDB 
			 * 3. give some feedback to SDEV 
			 */
			
			
					
			
//			if (sessionRepository.findById(payload.get("session")) == null) {
//				log.info(payload.get("session") + " does not exists, need run LWSP key exchange... maybe..." );
//			}else {
//				log.info("I got a session, let's check if session is expired");
//			}
			String expiration_time="100";
			SessionInfo res1 = new SessionInfo("id",payload.get("session"),expiration_time);
//			ResourceInfo res = new ResourceInfo("id",payload.get("session"));
			
			Object o = sessionRepository.save(res1);
			if (o == null) {
				log.info("NOTHING DONE");
			}else {
				log.info("Collection updated");
			}
			
			HttpHeaders headers =new HttpHeaders();
			headers.add("1", "this is a SDEV");
			String mybody="<html>OK, SDEV REGISTRY, here some BODY</html>\n";
			responseEntity=new ResponseEntity<Object>(mybody,headers, HttpStatus.OK);
		}
		
		if (responseEntity == null) {
		//forge response
			HttpHeaders headers =new HttpHeaders();
			headers.add("1", "NOT FOUND, UNKNOWN PAYLOAD");
			String mybody="<html>NOT FOUND, UNKOWN PAYLOAD</html>\n";
			responseEntity= new ResponseEntity<Object>(mybody,headers, HttpStatus.NOT_FOUND);
		}
		return responseEntity;
		
		
	}

}