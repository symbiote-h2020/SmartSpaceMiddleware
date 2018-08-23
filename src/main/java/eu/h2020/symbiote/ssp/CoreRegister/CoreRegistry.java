package eu.h2020.symbiote.ssp.CoreRegister;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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
import eu.h2020.symbiote.core.cci.SspResourceReqistryResponse;
import eu.h2020.symbiote.model.cim.Location;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.WGS84Location;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
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

	@Value("${symbIoTe.cloud.interface.url}")
	String cloudInterfaceUrl;

	@Value("${symbIoTe.core.interface.url}")
	String coreInterfaceUrl;


	@Value("${ssp.url}")
	String sspUrl;



	@Autowired
	AuthorizationService authorizationService;
	
	@Autowired
	SessionsRepository sessionsRepository;


	private static Log log = LogFactory.getLog(InnkeeperRestController.class);

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
		ret.setPluginURL(setSSPUrlStr(null));
		return ret;
	}
	
	private String setSSPUrlStr(String pluginURL) {
		return "https://"+sspUrl;
		/*
		String ret="";
		String[] sep = pluginURL.split("\\/");
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
		ret=sepStr;
		return ret;		
		*/
	}
	
	
	private SspRegInfo setSSPsymbioteID(SspRegInfo sspRegInfo) {
		SspRegInfo ret = sspRegInfo;
		sspRegInfo.setPluginId(sspName);
		return ret;
	}
	private ResponseEntity<SdevRegistryResponse> registerSDEV(SspRegInfo sspRegInfo) throws JsonProcessingException {
		SspRegInfo sspRegInfoCore = new SspRegInfo();
		sspRegInfoCore.setDerivedKey1(sspRegInfo.getDerivedKey1());
		sspRegInfoCore.setHashField(sspRegInfo.getHashField());
		sspRegInfoCore.setPluginId(sspRegInfo.getPluginId());
		sspRegInfoCore.setPluginURL(sspRegInfo.getPluginURL());
		sspRegInfoCore.setRoaming(sspRegInfo.getRoaming());
		sspRegInfoCore.setSspId(sspRegInfo.getSspId());
		
		if (sspRegInfo.getSymId().equals(""))
			sspRegInfoCore.setSymId(null);
		else
			sspRegInfoCore.setSymId(sspRegInfo.getSymId());
		
		String endpoint=cloudInterfaceUrl+"/ssps/"+sspName+"/sdevs";

		

		sspRegInfoCore=setSSPUrl(sspRegInfoCore);
		sspRegInfoCore=setSSPsymbioteID(sspRegInfoCore);


		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);
		log.info(httpHeaders);
		
		String jsonInString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sspRegInfoCore);


		log.info("JSON PAYLOAD:"+jsonInString);
				
		

		SdevRegistryRequest sdevRegistryRequest = new SdevRegistryRequest(sspRegInfoCore);
		HttpEntity<SdevRegistryRequest> httpEntity = new HttpEntity<>(sdevRegistryRequest, httpHeaders) ;
		RestTemplate restTemplate = new RestTemplate();

		log.info("SDEV Interworking Service URL exposed to Core:"+sdevRegistryRequest.getBody().getPluginURL());
		log.info("sspRegInfoCore.getSspId()="+sspRegInfoCore.getSspId());
		//Create a smart device

		log.info("URL CORE REGISTER ENDPOINT:"+endpoint);




		try {

			if (sspRegInfoCore.getSymId()==null) {
				// FIRST REGISTRATION perform POST
				log.info("FIRST REGISTRATION: POST");
				return restTemplate.exchange(endpoint, HttpMethod.POST,
						httpEntity, SdevRegistryResponse.class);
			}else {
				log.info("SDEV UPDATE REGISTRATION: PUT");
				log.info("updating symId = "+sspRegInfoCore.getSymId()+"...");
				return restTemplate.exchange(endpoint, HttpMethod.PUT,
						httpEntity, SdevRegistryResponse.class);
			}

		}catch (Exception e) {
			log.error("[SDEV registration] SymId="+sspRegInfoCore.getSymId()+" FAILED:"+ e);
		}		
		return null;

	}

	@Value("${latitude}") 
	double latitude;
	@Value("${longitude}") 
	double longitude;
	@Value("${altitude}") 
	double altitude;
	
	private Location getSSPLocation() {		
		Location res = new WGS84Location(longitude,latitude,altitude,"",null);
		/*
		List<String> ll = new ArrayList<String>();
		ll.add("This is the Resource Description");
		res.setDescription(ll);
		res.setName("SENSOR NAME");
		*/		
		return res;
	}
	
	private ResponseEntity<SspResourceReqistryResponse> registerResource(SspResource sspResource, String type) throws JsonProcessingException {
		String sdevId=null;
		//if (type==InnkeeperRestControllerConstants.SDEV)
		sdevId = sspResource.getSymIdParent();
		//if (type==InnkeeperRestControllerConstants.PLATFORM)
		//	sdevId = "";
		String currInterworkingServiceURL = sspResource.getResource().getInterworkingServiceURL();
		String symIdParent = sspResource.getSymIdParent();

		
		ResponseEntity<SspResourceReqistryResponse> response = null;
		if (sdevId !=null) if (!sdevId.equals("") || type==InnkeeperRestControllerConstants.PLATFORM){
			String endpoint=cloudInterfaceUrl+"/ssps/"+sspName+"/sdevs/"+sdevId+"/resources";

			HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
			httpHeaders.setContentType(MediaType.APPLICATION_JSON);
			
			log.info(httpHeaders);

			// Create the httpEntity which you are going to send. The Object should be replaced by the message you are

			Map<String,Resource> resMap = new HashMap<String,Resource>();
			
			// assign Resource Interworking Service URL
			SessionInfo s = sessionsRepository.findBySspId(sspResource.getSspIdParent());
			sspResource.getResource().setInterworkingServiceURL(setSSPUrlStr(s.getPluginURL()));
			
			sspResource.setSymIdParent(sspName);
			
			//String jsonInString = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(localSspRes.getResource());
			//
			log.info(">>>>> RESOURCE PAYLOAD:\n"+new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(sspResource.getResource()));
			//String jsonInStringSSPRes = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(localSspRes);
			//log.info("RESOURCE PAYLOAD SSP:\n"+jsonInStringSSPRes);
			
			
			
			resMap.put("1",sspResource.getResource());
			SspResourceRegistryRequest sdevResourceRequest = new SspResourceRegistryRequest(resMap);
			HttpEntity<SspResourceRegistryRequest> httpEntity = new HttpEntity<>(sdevResourceRequest, httpHeaders) ;
			//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;

			RestTemplate restTemplate = new RestTemplate();

			log.info("[Resource registration] Http request to "+endpoint);
			
			try {
				if (sspResource.getResource().getId()==null) {
					response = restTemplate.exchange(endpoint, HttpMethod.POST,
							httpEntity, SspResourceReqistryResponse.class);
					log.info("RESPONSE HEADER:"+response.getHeaders());
					log.info("RESPONSE BODY:"+response.getBody());					
				} else 
					if (sspResource.getResource().getId().equals("") ) {
						response = restTemplate.exchange(endpoint, HttpMethod.POST,
								httpEntity, SspResourceReqistryResponse.class);
						log.info("RESPONSE HEADER:"+response.getHeaders());
						log.info("RESPONSE BODY:"+response.getBody());

					}else {
						log.info("UPDATE REGISTRATION: PUT");
						response = restTemplate.exchange(endpoint, HttpMethod.PUT,
								httpEntity, SspResourceReqistryResponse.class);
						log.info("RESPONSE HEADER:"+response.getHeaders());
						log.info("RESPONSE BODY:"+response.getBody());

					}

			}catch (Exception e) {
				log.error("[Resource registration] FAILED:"+ e);

			}
		}else{
			log.info("SymId is null, return null");
		}
		
		sspResource.getResource().setInterworkingServiceURL(currInterworkingServiceURL);
		sspResource.setSymIdParent(symIdParent);

		return response;




	}

	public ResponseEntity<SdevRegistryResponse> unregisterSDEV(String symId) {

		SspRegInfo sspRegInfo = new SspRegInfo();
		sspRegInfo.setSymId(symId);
		sspRegInfo.setPluginId(sspName);
		sspRegInfo.setPluginURL("https://"+sspUrl);
		
		String endpoint=cloudInterfaceUrl+"/ssps/"+sspName+"/sdevs";
		

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		SdevRegistryRequest sdevRegistryRequest = new SdevRegistryRequest(sspRegInfo);
		HttpEntity<SdevRegistryRequest> httpEntity = new HttpEntity<>(sdevRegistryRequest, httpHeaders) ;

		RestTemplate restTemplate = new RestTemplate();

		//Create a smart device

		log.info("[SDEV delete]: endpoint:"+endpoint);

		try {
			return restTemplate.exchange(endpoint, HttpMethod.DELETE,
					httpEntity, SdevRegistryResponse.class);
			
		}catch (Exception e) {
			log.error("[SDEV delete] SymId="+sspRegInfo.getSymId()+" FAILED:"+ e);
		}		
		return null;

	}

	public ResponseEntity<SdevRegistryResponse> unregisterResource(ResourceInfo resourceInfo) throws JsonProcessingException {


		String sdevId = resourceInfo.getSymIdParent();
		
		String resInfoStr= new ObjectMapper().writeValueAsString(resourceInfo.getResource());
		log.info("PAYLOAD:"+resInfoStr);
		String endpoint=cloudInterfaceUrl+"/ssps/"+sspName+"/sdevs/"+sdevId+"/resources";

		HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
		httpHeaders.setContentType(MediaType.APPLICATION_JSON);

		// Create the httpEntity which you are going to send. The Object should be replaced by the message you are

		Map<String,Resource> resMap = new HashMap<String,Resource>();
		resMap.put("1", resourceInfo.getResource());
		SspResourceRegistryRequest sdevResourceRequest = new SspResourceRegistryRequest(resMap);
		HttpEntity<SspResourceRegistryRequest> httpEntity = new HttpEntity<>(sdevResourceRequest, httpHeaders) ;
		//HttpEntity<Object> httpEntity = new HttpEntity<>("{}", httpHeaders) ;
		
		RestTemplate restTemplate = new RestTemplate();
		String currInterworkingServiceURL = resourceInfo.getResource().getInterworkingServiceURL();
		SessionInfo s = sessionsRepository.findBySspId(resourceInfo.getSspIdParent());
		resourceInfo.getResource().setInterworkingServiceURL(setSSPUrlStr(s.getPluginURL()));

		log.info("[Resource DELETE] Http request to"+endpoint);

		try {
			ResponseEntity<SdevRegistryResponse> response = restTemplate.exchange(endpoint, HttpMethod.DELETE,
					httpEntity, SdevRegistryResponse.class);
			log.info(response.getHeaders());
			return response;

		}catch (Exception e) {
			log.error("[Resource DELETE] FAILED:"+ e);

		}
		resourceInfo.getResource().setInterworkingServiceURL(currInterworkingServiceURL);

		return null;


	}





	public String getSymbioteIdFromCore(String symId, Object msg, String type) throws IOException {
		ResponseEntity<?>  response = null;
		//log.info(new ObjectMapper().writeValueAsString(msg).toString());	

		if (!this.isOnline)
			return "";



		if (msg instanceof SspRegInfo) {
			//ResponseEntity<SdevRegistryResponse> response=null;
			log.info("SDEV Core Registration");

			SspRegInfo sspRegInfo = (SspRegInfo)(msg);
			response = registerSDEV(sspRegInfo);

			//Response is null if the registration function got an exception, in this case I assume that the SSP is offline.
			if (response == null) {
				log.error("Something goes wrong in Core, Response is null");
				return "";
			}

			if (	response.getStatusCode()==HttpStatus.BAD_GATEWAY ||
					response.getStatusCode()==HttpStatus.SERVICE_UNAVAILABLE ||
					response.getStatusCode()==HttpStatus.GATEWAY_TIMEOUT
					) {
				log.error("Something goes wrong in Core, SDEV registration Failed");
				log.error(response.getHeaders());
				log.error(response.getStatusCode());
				log.error(response.getBody());
				return "";
			}
			log.debug("response.getHeaders()="+response.getHeaders());
			log.debug("response.getStatusCode()="+response.getStatusCode());
			
			ObjectMapper mapper = new ObjectMapper();
			String jsonInString = mapper.writeValueAsString(response.getBody());
			log.debug("response.getBody()="+jsonInString);
			SdevRegistryResponse respBody = (SdevRegistryResponse) response.getBody();
			SspRegInfo sspRegInfoRes = respBody.getBody();

			log.debug("sspRegInfoRes.getSymId():"+sspRegInfoRes.getSymId());

			return sspRegInfoRes.getSymId();

		}

		//Resource Registration
		if (msg instanceof SspResource) {
			//TBD
			//ResponseEntity<SspResourceReqistryResponse> response=null;
			log.info("Resource Core Registration");
			
			SspResource sspResource = (SspResource)(msg);
			//log.info("sspResource.getSspIdResource()="+sspResource.getSspIdResource());
			
			log.info("["+type+"]RESOURCE:"+new ObjectMapper().writeValueAsString(sspResource));
			
			response = registerResource(sspResource,type);

			//Response is null if the registration function got an exception, in this case I assume that the SSP is offline.
			if (response == null) {
				log.info("RESPONSE IS NULL");
				return "";
			}

			if (	response.getStatusCode()==HttpStatus.BAD_GATEWAY ||
					response.getStatusCode()==HttpStatus.SERVICE_UNAVAILABLE ||
					response.getStatusCode()==HttpStatus.GATEWAY_TIMEOUT
					) {
				log.error("Something goes wrong in Core, Resource registration Failed");
				log.error(response.getHeaders());
				log.error(response.getStatusCode());
				log.error(response.getBody());
				return "";
			}

			SspResourceReqistryResponse respBody = (SspResourceReqistryResponse) response.getBody();
			String symIdRes=null;
			for (Map.Entry<String, Resource> entry : respBody.getBody().entrySet()) {
				System.out.println("Item : " + entry.getKey() + " Count : " + entry.getValue());
				Resource res = entry.getValue();
				symIdRes=res.getId();
				log.info("Got symId for resurce="+symIdRes);
				break;
			}
			if (symIdRes!=null)
				return symIdRes;
			else
				return "";
			


		}



		return "";

		/*
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
		}*/
	}





}
