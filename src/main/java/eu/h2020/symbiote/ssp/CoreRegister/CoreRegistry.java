package eu.h2020.symbiote.ssp.CoreRegister;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
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
	
	
	@Value("${ssp.url}")
	String sspUrl;
	
	
	
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

	private SspRegInfo setSSPUrl(SspRegInfo sspRegInfo) {
		SspRegInfo ret = sspRegInfo;
		
		String[] sep = sspRegInfo.getPluginURL().split("\\/");
		List<String> sepList = Arrays.asList(sep);
		sep[2]=sspUrl;
		String sepStr="";
		int l=0;
		for (String s : sepList) {
			String separator="/";
			if (l==sepList.size()-1) separator="";
			sepStr+=s+separator;
			l++;
		}
		ret.setPluginURL(sepStr);
		return ret;
	}
	
	private ResponseEntity<SdevRegistryResponse> registerSDEV(SspRegInfo sspRegInfo) {
		SspRegInfo sspRegInfoCore = new SspRegInfo();
		sspRegInfoCore.setDerivedKey1(sspRegInfo.getDerivedKey1());
		sspRegInfoCore.setHashField(sspRegInfo.getHashField());
		sspRegInfoCore.setPluginId(sspRegInfo.getPluginId());
		sspRegInfoCore.setPluginURL(sspRegInfo.getPluginURL());
		sspRegInfoCore.setRoaming(sspRegInfo.getRoaming());
		sspRegInfoCore.setSspId(sspRegInfo.getSspId());
		sspRegInfoCore.setSymId(sspRegInfo.getSymId());
		
 		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/sdevs";
		
		sspRegInfoCore=setSSPUrl(sspRegInfoCore);

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		log.info(httpHeaders);

		SdevRegistryRequest sdevRegistryRequest = new SdevRegistryRequest(sspRegInfoCore);
		HttpEntity<SdevRegistryRequest> httpEntity = new HttpEntity<>(sdevRegistryRequest, httpHeaders) ;
		RestTemplate restTemplate = new RestTemplate();
		log.info("SDEV Interworking Service URL exposed to Core:"+sdevRegistryRequest.getBody().getPluginURL());
		//Create a smart device

		log.info("URL CORE REGISTER ENDPOINT:"+endpoint);
		
		try {
			
			if (sspRegInfoCore.getSymId().equals("") || sspRegInfoCore.getSymId()==null) {
				// FIRST REGISTRATION perform POST
				log.info("FIRST REGISTRATION: POST");
				return restTemplate.exchange(endpoint, HttpMethod.POST,
						httpEntity, SdevRegistryResponse.class);
			}else {
				log.info("UPDATE REGISTRATION: PUT");
				return restTemplate.exchange(endpoint, HttpMethod.PUT,
						httpEntity, SdevRegistryResponse.class);
			}

		}catch (Exception e) {
			log.error("[SDEV registration] SymId="+sspRegInfoCore.getSymId()+" FAILED:"+ e);
		}		
		return null;

	}

	private ResponseEntity<SdevRegistryResponse> registerResource(ResourceInfo resourceInfo) {

		String sdevId = resourceInfo.getSspIdParent();
		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/"+sdevId+"/resources";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		log.info(httpHeaders);

		// Create the httpEntity which you are going to send. The Object should be replaced by the message you are

		Map<String,Resource> resMap = new HashMap<String,Resource>();
		resMap.put(resourceInfo.getInternalIdResource(), resourceInfo.getResource());
		SspResourceRegistryRequest sdevResourceRequest = new SspResourceRegistryRequest(resMap);
		HttpEntity<SspResourceRegistryRequest> httpEntity = new HttpEntity<>(sdevResourceRequest, httpHeaders) ;
		//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		log.info("[Resource registration] Http request to"+endpoint);

		try {
			ResponseEntity<SdevRegistryResponse> response = restTemplate.exchange(endpoint, HttpMethod.POST,
					httpEntity, SdevRegistryResponse.class);
			log.info(response.getHeaders());
			return response;

		}catch (Exception e) {
			log.error("[Resource registration] FAILED:"+ e);
			
		}
		return null;

	}
	
	public ResponseEntity<SdevRegistryResponse> unregisterSDEV(String symId) {

		SspRegInfo sspRegInfo = new SspRegInfo();
		sspRegInfo.setSymId(symId);
		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/sdevs";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		SdevRegistryRequest sdevRegistryRequest = new SdevRegistryRequest(sspRegInfo);
		HttpEntity<SdevRegistryRequest> httpEntity = new HttpEntity<>(sdevRegistryRequest, httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		//Create a smart device

		log.info(endpoint);
		
		try {			
				return restTemplate.exchange(endpoint, HttpMethod.DELETE,
						httpEntity, SdevRegistryResponse.class);
		}catch (Exception e) {
			log.error("[SDEV delete] SymId="+sspRegInfo.getSymId()+" FAILED:"+ e);
		}		
		return null;

	}

	public ResponseEntity<SdevRegistryResponse> unregisterResource(ResourceInfo resourceInfo) {


		String sdevId = resourceInfo.getSspIdParent();
		String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/"+sdevId+"/resources";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Create the httpEntity which you are going to send. The Object should be replaced by the message you are

		Map<String,Resource> resMap = new HashMap<String,Resource>();
		resMap.put(resourceInfo.getInternalIdResource(), resourceInfo.getResource());
		SspResourceRegistryRequest sdevResourceRequest = new SspResourceRegistryRequest(resMap);
		HttpEntity<SspResourceRegistryRequest> httpEntity = new HttpEntity<>(sdevResourceRequest, httpHeaders) ;
		//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		log.info("[Resource DELETE] Http request to"+endpoint);

		try {
			ResponseEntity<SdevRegistryResponse> response = restTemplate.exchange(endpoint, HttpMethod.DELETE,
					httpEntity, SdevRegistryResponse.class);
			log.info(response.getHeaders());
			return response;

		}catch (Exception e) {
			log.error("[Resource DELETE] FAILED:"+ e);
			
		}
		return null;


	}

		
	
	

	public String getSymbioteIdFromCore(String symId, Object msg) throws IOException {
		ResponseEntity<SdevRegistryResponse> response=null;
		log.info("GET SYMBIOTE ID FROM CORE");
		log.info(new ObjectMapper().writeValueAsString(msg).toString());		
		if (msg instanceof SspRegInfo) {
			log.info("SDEV Core Registration");
			SspRegInfo sspRegInfo = (SspRegInfo)(msg);
			response = registerSDEV(sspRegInfo);
		}
		if (msg instanceof ResourceInfo) {
			log.info("Resource Core Registration");
			ResourceInfo resourceInfo = (ResourceInfo)(msg);
			response = registerResource(resourceInfo);
			
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
