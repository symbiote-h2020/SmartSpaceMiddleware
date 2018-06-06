package eu.h2020.symbiote.ssp.CoreRegister;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.core.cci.SdevRegistryRequest;
import eu.h2020.symbiote.core.cci.SdevRegistryResponse;
import eu.h2020.symbiote.core.cci.SspResourceRegistryRequest;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;

@Service
public class CoreRegistry {


	@Value("${innk.lwsp.enabled:true}") 
	Boolean isLwspEnabled;

	@Value("${ssp.id}")
	String sspName;

	@Value("${ssp.location_name}")
	String locationName;

	@Value("${symbIoTe.aam.integration}")
	Boolean securityEnabled;

	@Value("${symbIoTe.core.interface.url}")
	String coreIntefaceUrl;
	
	@Autowired
	AuthorizationService authorizationService;
	
	
	private static Log log = LogFactory.getLog(InnkeeperRestController.class);
	
	//String sspName;
	//String coreIntefaceUrl;
	//AuthorizationService authorizationService;
	private Object repository;
	private Boolean isOnline;

	public CoreRegistry() {

	}
	
	public CoreRegistry(Object repo, Boolean isOnline) {
		this.repository=repo;
		this.isOnline=isOnline;
	}
	
	

	public Boolean getCoreConnectivity() {
		return this.isOnline;
	}
	
	public void setRepository(Object repo) {
		this.repository=repo;
	}
	public void setOnline(boolean isOnline) {
		this.isOnline=isOnline;
	}

	private ResponseEntity<SdevRegistryResponse> registerSDEV(SspRegInfo sspRegInfo) {

		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/sdevs";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		log.info(httpHeaders);

		// Create the httpEntity which you are going to send. The Object should be replaced by the message you are


		SdevRegistryRequest sdevRegistryRequest = new SdevRegistryRequest(sspRegInfo);
		HttpEntity<SdevRegistryRequest> httpEntity = new HttpEntity<>(sdevRegistryRequest, httpHeaders) ;
		//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		//Create a smart device

		//String endpoint="https://symbiote-open.man.poznan.pl/cloudCoreInterface/platforms/"+sspName+"/resources";
		//String endpoint="https://symbiote-open.man.poznan.pl/coreInterface/get_available_aams";

		log.info(endpoint);

		// The Object should be replaced by the class representing the response that you expect
		// sending to the core
		try {
			ResponseEntity<SdevRegistryResponse> response = restTemplate.exchange(endpoint, HttpMethod.GET,
					httpEntity, SdevRegistryResponse.class);
			log.info(response.getHeaders());
			return response;

		}catch (Exception e) {
			log.error("Got error here");
			log.error(e);
		}
		//
		return null;

	}

	private ResponseEntity<SdevRegistryResponse> registerResource(SspResource sspResource) {

		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/sdevs";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		log.info(httpHeaders);

		// Create the httpEntity which you are going to send. The Object should be replaced by the message you are

		Map<String,Resource> resMap = new HashMap<String,Resource>();
		resMap.put(sspResource.getInternalIdResource(), sspResource.getResource());
		SspResourceRegistryRequest sdevResourceRequest = new SspResourceRegistryRequest(resMap);
		HttpEntity<SspResourceRegistryRequest> httpEntity = new HttpEntity<>(sdevResourceRequest, httpHeaders) ;
		//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		//Create a smart device

		//String endpoint="https://symbiote-open.man.poznan.pl/cloudCoreInterface/platforms/"+sspName+"/resources";
		//String endpoint="https://symbiote-open.man.poznan.pl/coreInterface/get_available_aams";

		log.info(endpoint);

		// The Object should be replaced by the class representing the response that you expect
		// sending to the core
		try {
			ResponseEntity<SdevRegistryResponse> response = restTemplate.exchange(endpoint, HttpMethod.GET,
					httpEntity, SdevRegistryResponse.class);
			log.info(response.getHeaders());
			return response;

		}catch (Exception e) {
			log.error("Got error here");
			log.error(e);
		}
		//
		return null;

	}

	public String checkCoreSymbioteIdRegistration(String symId, Object msg) throws IOException {
		ResponseEntity<SdevRegistryResponse> response=null;
		
		log.info(new ObjectMapper().writeValueAsString(msg).toString());		
		if (msg instanceof SspRegInfo) {
			SspRegInfo sspRegInfo = (SspRegInfo)(msg);
			response = registerSDEV(sspRegInfo);
		}
		if (msg instanceof SspResource) {
			SspResource sspResource = (SspResource)(msg);
			response = registerResource(sspResource);
			
		}
		//Response is null if the registration function got an exception, in this case I assume that the SSP is offline.
		if (response == null) {
			return "";
		}
		
		if (	response.getStatusCode()==HttpStatus.BAD_GATEWAY ||
				response.getStatusCode()==HttpStatus.SERVICE_UNAVAILABLE ||
				response.getStatusCode()==HttpStatus.GATEWAY_TIMEOUT
				) {
			return "";
		}
		
	
		SspRegInfo sspRegInfo = new ObjectMapper().readValue(response.getBody().toString(), SspRegInfo.class);
		
		
		
		if (!this.isOnline)
			return "";

		try {

			boolean smyIdSessionsRepositoryExists=false;
			//Mock function

			//if I register an SDEV for first time and symId is null
			String symId_ret="";
			if (symId.equals("")) {
				int symIdMock = new Random().nextInt((1000 - 1) + 1) + 1;
				symId_ret="sym"+symIdMock;
				smyIdSessionsRepositoryExists=true;
			}else {
				//search in the sessionsRepository if  symId Exists											
				if (repository instanceof SessionsRepository) {
					smyIdSessionsRepositoryExists = ((SessionsRepository) (repository)).findBySymId(symId) != null;
				}

				if (repository instanceof ResourcesRepository) {
					smyIdSessionsRepositoryExists = ((ResourcesRepository) (repository)).findBySymIdParent(symId) != null;
				}

				symId_ret=symId;
			}
			//DEBUG: search if symId Exists in the Core ALWAYS TRUE FOR THE MOMENT
			boolean smyIdCoreExists=true;

			if (smyIdSessionsRepositoryExists && smyIdCoreExists) {
				return symId_ret;
			}else {
				return null;
			}
		}catch(Exception e) {			
			System.err.println(e);
			return null;
		}
	}
	
	
	


}
