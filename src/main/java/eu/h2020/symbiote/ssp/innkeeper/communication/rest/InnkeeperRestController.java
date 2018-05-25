package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import eu.h2020.symbiote.core.ci.QueryResourceResult;
import eu.h2020.symbiote.core.ci.ResourceType;
import eu.h2020.symbiote.model.cim.Property;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Sensor;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperRegistrationResponse;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by vasgl on 8/24/2017.
 */


@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {

	private static Log log = LogFactory.getLog(InnkeeperRestController.class);


	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	InnkeeperRegistrationRequest innkeeperSDEVRegistrationRequest;

	@Autowired
	InnkeeperResourceRegistrationRequest innkeeperResourceRegistrationRequest;

	@Autowired
	Lwsp lwsp;

	@Value("${innk.lwsp.enabled:true}") 
	Boolean isLwspEnabled;

	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;
	
	@Value("${ssp.id}")
	String sspName;
	
	
	@Value("${ssp.location_name}")
	String locationName;
	
	@Value("${symbIoTe.aam.integration}")
	Boolean securityEnabled;
	
	@Value("${symbIoTe.core.interface.url}")
	String coreIntefaceUrl;
	
	@Autowired
	SessionsRepository sessionsRepository;
	@Autowired
	AuthorizationService authorizationService;
	public InnkeeperRestController() {

	}
	// PLATFORM REGISTRATION
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_PLATFORM_REGISTER_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> platform_register(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {
		log.info("REGISTRATION MESSAGE:"+ payload);
		return innkeeperSDEVRegistrationRequest.SspRegister(null,payload,InnkeeperRestControllerConstants.PLATFORM);			
	}

	// PLATFORM REGISTRATION
		@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_PLATFORM_JOIN_REQUEST_PATH, method = RequestMethod.POST)
		public ResponseEntity<Object> platform_resources(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {
			log.info("REGISTRATION MESSAGE:"+ payload);
			return innkeeperSDEVRegistrationRequest.SspRegister(null,payload,InnkeeperRestControllerConstants.PLATFORM);			
		}

	
	
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_SDEV_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {					
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);

			try {
				String outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("JOIN MSG from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTER:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperResourceRegistrationRequest.SspJoinResource(decoded_message);
				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));


				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{	
			// NO encryption
			return innkeeperResourceRegistrationRequest.SspJoinResource(payload);
		}


	}

	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_SDEV_REGISTER_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {

			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);
			String outputMessage="";
			try {
				outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("REGISTRY from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_Hello:				
			case LwspConstants.SDEV_AuthN:
				return new ResponseEntity<Object>(outputMessage,responseHeaders,HttpStatus.OK);

			case LwspConstants.REGISTER:

				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspRegister(lwsp.getSessionId(),decoded_message,InnkeeperRestControllerConstants.SDEV);
				String encodedResponse = lwsp.send_data(res.getBody().toString());
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{
			return innkeeperSDEVRegistrationRequest.SspRegister(null,payload,InnkeeperRestControllerConstants.SDEV);
		}

	}



	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_SDEV_UNREGISTER_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregister(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);
			try {
				String outputMessage = lwsp.processMessage();
				log.info(outputMessage);
				log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("UNREGISTER from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTER:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspDelete(decoded_message);
				//String encodedResponse = lwsp.send_data(res.getBody().toString());
				log.info("UNREGISTER SDEV");
				return new ResponseEntity<Object>("",res.getHeaders(),res.getStatusCode());

			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}
			// LWSP DISABLED
		} else { 
			return innkeeperSDEVRegistrationRequest.SspDelete(payload);

		}
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> keep_alive(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");

			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);

			try {
				String outputMessage = lwsp.processMessage();
				//log.info(outputMessage);
				//log.info("MTI:"+lwsp.get_mti());
			} catch (NullPointerException e) {
				log.error("KEEP ALIVE MSG from lwsp.processMessage() returns null");
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.BAD_REQUEST);
			}

			switch (lwsp.get_mti()) {
			case LwspConstants.REGISTER:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspKeepAlive(decoded_message);
				//log.info(res.getBody().toString());
				String encodedResponse = lwsp.send_data(res.getBody().toString());
				//String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}


			// LWSP DISABLED
		} else { 
			return innkeeperSDEVRegistrationRequest.SspKeepAlive(payload);

		}
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_PUBLIC_RESOURCES, method = RequestMethod.GET)
	public ResponseEntity<Object> public_resources() throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		List<ResourceInfo> resourcesInfo = resourcesRepository.findAll();
		List<SspResource> sspResFilt = new ArrayList<SspResource>();
		for (ResourceInfo r : resourcesInfo) {
			if (r.getAccessPolicySpecifier().getPolicyType() == AccessPolicyType.PUBLIC) {
				SspResource sspRes = new SspResource();
				//sspRes.setSspIdParent(r.getSspIdParent());
				//sspRes.setSspIdResource(r.getSspIdResource());
				//sspRes.setSymIdParent(r.getSymIdParent());
				//sspRes.setInternalIdResource(r.getInternalIdResource());
				
				/*QueryResourceResult queryRes= new QueryResourceResult();
				queryRes.setPlatformId(sspName);
				queryRes.setPlatformName(sspName);
				queryRes.setName(r.getResource().getName());
				queryRes.setId(r.getSymIdResource());
				//queryRes.setDescription(r.getResource().getDescription().toString());
				queryRes.setLocationName(locationName);
				if (r.getResource() instanceof StationarySensor) {
					StationarySensor ss = (StationarySensor) r.getResource();
					List<String> list = ss.getObservesProperty().										
					queryRes.setObservedProperties(list);
					Property pippo = null;
				}
				*/
				
				String resourceClass = r.getResource().getClass().toString();
				log.info(resourceClass);
				String [] splitResName = resourceClass.split("\\.");				
				String resType = ResourceType.getTypeForName(splitResName[splitResName.length-1]).getUri();
				List<String> resTypeList = new ArrayList<String>();
				resTypeList.add(resType);
								
				Resource rr = r.getResource();
				if (!r.getSymIdResource().equals(""))
					rr.setId(r.getSymIdResource());
				else if (!r.getSspIdResource().equals(""))
					rr.setId(r.getSspIdResource());
				rr.setInterworkingServiceURL(null);
				sspRes.setResource(rr);
				sspRes.setResourceType(resTypeList);
				sspRes.setLocationName(locationName);
				sspResFilt.add(sspRes);				
			}
		}

		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setSerializationInclusion(Include.NON_NULL);
		//	objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		String resInfoString = objectMapper.writeValueAsString(sspResFilt);

		return new ResponseEntity<Object>(resInfoString,responseHeaders,httpStatus);
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_GET_SSP_INFO, method = RequestMethod.GET)
	public ResponseEntity<Object> getSspInfo() throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {		
		HttpStatus httpStatus = HttpStatus.OK;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);

		JsonNode node = JsonNodeFactory.instance.objectNode();
		((ObjectNode) node).put("SSP_NAME", sspName);		
		ObjectMapper objectMapper = new ObjectMapper();
		objectMapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		String resInfoString = objectMapper.writeValueAsString(node);			
		return new ResponseEntity<Object>(resInfoString,responseHeaders,httpStatus);


	}

	
	@RequestMapping(value = InnkeeperRestControllerConstants.SANDBOX, method = RequestMethod.POST)
	public ResponseEntity<Object> sandbox(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {		
		if (securityEnabled) {
			
			log.info("Security Enabled");
			HttpHeaders httpHeaders = authorizationService.getHttpHeadersWithSecurityRequest();
			log.info(httpHeaders);

			
			// Create the httpEntity which you are going to send. The Object should be replaced by the message you are
			// sending to the core
			
			HttpEntity<Object> httpEntity = new HttpEntity<>("test", httpHeaders);
			RestTemplate restTemplate = new RestTemplate();
			
			String endpoint=coreIntefaceUrl+"/ssps/"+sspName+"/sdev";
			log.info(endpoint);
			/*
			// The Object should be replaced by the class representing the response that you expect
			ResponseEntity<Object> response = restTemplate.exchange(endpoint, HttpMethod.POST,
					httpEntity, Object.class);
			log.info(response.getHeaders());
			*/
			return null;

		}else {
			log.info("Security Disabled");
			return null;
		}
		
	}


	private ResponseEntity<Object>setCoreOnline(Boolean v){
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		innkeeperSDEVRegistrationRequest.setIsCoreOnline(v);
		innkeeperResourceRegistrationRequest.setIsCoreOnline(v);
		this.isCoreOnline=v;
		return new ResponseEntity<Object>("{\"result\"=\"OK\"}",responseHeaders,httpStatus);
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.SET_INNK_ONLINE, method = RequestMethod.POST)
	public ResponseEntity<Object> set_innk_online(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {

		return setCoreOnline(true);
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.SET_INNK_OFFLINE, method = RequestMethod.POST)
	public ResponseEntity<Object> set_innk_offline(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {		
		return setCoreOnline(false);

	}


}
