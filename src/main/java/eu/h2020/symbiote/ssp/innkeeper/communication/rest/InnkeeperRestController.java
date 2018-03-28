package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationResponse;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationResponse;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
	InnkeeperSDEVRegistrationRequest innkeeperSDEVRegistrationRequest;

	@Autowired
	InnkeeperResourceRegistrationRequest innkeeperResourceRegistrationRequest;

	@Autowired
	Lwsp lwsp;

	@Value("${innk.lwsp.enabled:true}") 
	Boolean isLwspEnabled;

	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;

	@Autowired
	SessionsRepository sessionsRepository;
	public InnkeeperRestController() {

	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException, InvalidArgumentsException {

		if (isLwspEnabled) {
			//LWSP TBD:
			return null;
		}else{	
			// NO encryption
			return SspJoinResource(payload);
		}


	}

	private ResponseEntity<Object> SspJoinResource(String msg) throws JsonParseException, JsonMappingException, IOException, InvalidArgumentsException{


		ResponseEntity<Object> responseEntity = null;

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		SspResource sspResource =  new ObjectMapper().readValue(msg, SspResource.class);

		SessionInfo s=sessionsRepository.findBySymId(sspResource.getSymId());
		// found Symbiote Id in Session Repository
		if (s != null) {			
			Date sessionExpiration = sessionsRepository.findBySymId(sspResource.getSymId()).getSessionExpiration();					
			InnkeeperResourceRegistrationResponse respSspResource = innkeeperResourceRegistrationRequest.registry(sspResource,sessionExpiration);
			switch (respSspResource.getResult()) {
			case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
				httpStatus = HttpStatus.BAD_REQUEST;
				break;
			default:
				httpStatus = HttpStatus.OK;
			}

			responseEntity = new  ResponseEntity<Object>(respSspResource,responseHeaders,httpStatus);
			return responseEntity;

		}else {
			log.info("SymId not found, check with SSP ID...");
		}

		s= sessionsRepository.findBySspId(sspResource.getSspId());

		if (s != null) {			
			Date sessionExpiration = s.getSessionExpiration();					
			InnkeeperResourceRegistrationResponse respSspResource = innkeeperResourceRegistrationRequest.registry(sspResource,sessionExpiration);

			switch (respSspResource.getResult()) {

			case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
				httpStatus = HttpStatus.BAD_REQUEST;
				break;
			default:
				httpStatus = HttpStatus.OK;
			}			
			return new ResponseEntity<Object>(respSspResource,responseHeaders,httpStatus);
		}else { 
			log.info("sspId not found, registration Failed");
		}

		//DEFAULT: ERROR
		httpStatus=HttpStatus.BAD_REQUEST;
		return new ResponseEntity<Object>("ERROR:sspId not found\nregistration Failed\nsent message: "+msg,responseHeaders,httpStatus);


	}


	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		log.info("Resource is Core ONLINE = "+innkeeperResourceRegistrationRequest.isCoreOnline());
		log.info("SDEV is Core ONLINE = "+innkeeperSDEVRegistrationRequest.isCoreOnline());


		if (isLwspEnabled) {

			//TODO: DEBUG

			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			String outputMessage = lwsp.processMessage();
			log.info(outputMessage);
			log.info("MTI:"+lwsp.get_mti());
			HttpHeaders responseHeaders = new HttpHeaders();
			responseHeaders.setContentType(MediaType.APPLICATION_JSON);

			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_Hello:
			case LwspConstants.SDEV_AuthN:
				return new ResponseEntity<Object>(outputMessage,responseHeaders,HttpStatus.OK);

			case LwspConstants.SDEV_REGISTRY:
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = SspRegistry(decoded_message);
				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{
			return SspRegistry(payload);
		}

	}


	private ResponseEntity<Object> SspRegistry(String decoded_message) throws IOException {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;		

		Date currTime = new Timestamp(System.currentTimeMillis());
		String sessionId = new Lwsp().generateSessionId();
		SessionInfo s = new SessionInfo();

		SspSDEVInfo sspSDEVInfo =  new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

		InnkeeperSDEVRegistrationResponse respSDEV = innkeeperSDEVRegistrationRequest.registry(sspSDEVInfo);

		//DEBUG: MOCK
		switch (respSDEV.getResult()) {
		case InnkeeperRestControllerConstants.REGISTRATION_OFFLINE: //OFFLINE
		case InnkeeperRestControllerConstants.REGISTRATION_OK:					
			httpStatus=HttpStatus.OK;
			s.setsessionId(sessionId);
			s.setdk1(sspSDEVInfo.getDerivedKey1());
			s.setSspId(respSDEV.getSspId());
			s.setSymId(respSDEV.getSymId());						
			s.setPluginId(sspSDEVInfo.getPluginId());
			s.setPluginURL(sspSDEVInfo.getPluginURL());
			s.setSessionExpiration(currTime);				
			sessionsRepository.save(s);				
			break;


		case InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED:
			httpStatus=HttpStatus.OK;
			break;

		case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
		default:
			httpStatus=HttpStatus.BAD_REQUEST;
		}
		String response = new ObjectMapper().writeValueAsString(respSDEV);
		return new ResponseEntity<Object>(response,responseHeaders,httpStatus);
	}
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_UNREGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregistry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			lwsp.processMessage();
			String encodedResponse=null;
			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_REGISTRY:

				//TODO: DEBUG
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = SspDelete(decoded_message);
				encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());

			default:
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.setContentType(MediaType.APPLICATION_JSON);
				InnkeeperSDEVRegistrationResponse errorResponse =new InnkeeperSDEVRegistrationResponse();
				errorResponse.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);				
				encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(errorResponse));
				return new ResponseEntity<Object>(encodedResponse,responseHeaders,HttpStatus.BAD_REQUEST);

			}
			// LWSP DISABLED
		} else { 
			return SspDelete(payload);

		}
	}

	private ResponseEntity<Object> SspDelete(String decoded_message) throws IOException {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);
		//Delete Session

		SessionInfo s = null;

		if (sspSdevInfo.getSymId()==null || sspSdevInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspSdevInfo.getSspId());

		}else {
			// REMOVE USING SYMID
			s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
		}

		InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();

		//if I found my symId/SspId of SDEV in the MongoDb session, delete it
		if (s!=null) {

			//Delete session
			sessionsRepository.delete(s);

			//Delete Resources
			//TODO: check also for Policies and ODATA?
			List<ResourceInfo> resList= resourcesRepository.findBySspId(s.getSspId());

			for (ResourceInfo r : resList) {
				resourcesRepository.delete(r);
			}

			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
			httpStatus=HttpStatus.OK;

			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response), 
					responseHeaders,httpStatus);
		}

		//DEFAULT: ERROR
		response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
		httpStatus=HttpStatus.BAD_REQUEST;
		return new ResponseEntity<Object>(
				new ObjectMapper().writeValueAsString(response),
				responseHeaders,httpStatus);

	}			

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> keep_alive(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			lwsp.processMessage();
			String encodedResponse=null;
			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_REGISTRY:

				//TODO: DEBUG
				String decoded_message = lwsp.get_response();
				ResponseEntity<Object> res = SspKeepAlive(decoded_message);
				encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());

			default:
				HttpHeaders responseHeaders = new HttpHeaders();
				responseHeaders.setContentType(MediaType.APPLICATION_JSON);
				InnkeeperSDEVRegistrationResponse errorResponse =new InnkeeperSDEVRegistrationResponse();
				errorResponse.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);				
				encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(errorResponse));
				return new ResponseEntity<Object>(encodedResponse,responseHeaders,HttpStatus.BAD_REQUEST);

			}
			// LWSP DISABLED
		} else { 
			return SspKeepAlive(payload);

		}
	}

	private ResponseEntity<Object> SspKeepAlive(String decoded_message) throws IOException {

		//KEEP ALIVE ACTIONS:
		// 1. update Session Expiration
		// 2. check if SSP is online and update symbiote id for SDEV and its Resources

		Date currTime = new Timestamp(System.currentTimeMillis());

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

		SessionInfo s = null;

		if (sspSdevInfo.getSymId()==null || sspSdevInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspSdevInfo.getSspId());

		}else {
			// UPDATE USING SYMID
			s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
		}



		InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();

		if (s==null) {			
			log.info("ERROR1 - no session found");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);
		}
		if (	!s.getSspId().equals(sspSdevInfo.getSspId()) && 
				!s.getSymId().equals(sspSdevInfo.getSymId())){
			log.info("ERROR2 - no match Ids");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);

		}else {
			log.info("SSpId and SymId match");
		}

		if (	( !isCoreOnline && (s.getSspId()!="" && !s.getSspId().equals(sspSdevInfo.getSspId())) )
				) {
			log.info("ERROR3 - SSP online and SymId not match or SSP offline and SspId not match");
			//DEFAULT: ERROR
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);
		}else {
			log.info("SspId "+s.getSspId()+" Match");
		}

		String symIdSDEV=null;
		if (sspSdevInfo.getSymId()==null)
			symIdSDEV = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(""); //generate new symId from Core
		else
			symIdSDEV = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(sspSdevInfo.getSymId());
		//if I found my symId/SspId of SDEV in the MongoDb session, update expiration time it

		//UPDATE Expiration time of session

		if (symIdSDEV!=null && (s.getSymId().equals("") || s.getSymId()==null) ) {
			// ONLINE and Generate new SymId or MATCH requested SymId AND
			// current saved SymId is null or empty
			s.setSymId(symIdSDEV); // update SymId
		}
		log.info("s.getSymId()="+s.getSymId());
		s.setSessionExpiration(currTime);
		sessionsRepository.save(s);


		//UPDATE Expiration time of Resources

		//TODO: check also for Policies and ODATA?
		List<ResourceInfo> resList= resourcesRepository.findBySspId(s.getSspId());
		List<Map<String, String>> updatedSymIdList = new ArrayList<Map<String,String>>();
		for (ResourceInfo r : resList) {

			String symIdRes = new CheckCoreUtility(resourcesRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(r.getSymIdResource());
			log.info("symIdRes="+symIdRes);
			HashMap<String,String> symIdEntry = new HashMap<String,String>();
			
			if (symIdRes!=null && r.getSymIdResource().equals("")) 
				r.setSymIdResource(symIdRes); // update SymId of Resource
			
			symIdEntry.put("symIdResource", r.getSymIdResource());
			symIdEntry.put("sspIdResource", r.getSspIdResource());
			updatedSymIdList.add(symIdEntry);
			
			r.setSessionExpiration(currTime);

			resourcesRepository.save(r);
		}
		response.setSymId(s.getSymId());
		response.setSspId(s.getSspId());

		if (isCoreOnline){
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);
			httpStatus=HttpStatus.OK;
		} else {
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OFFLINE);
			httpStatus=HttpStatus.OK;
		}

		response.setUpdatedSymId(updatedSymIdList);


		return new ResponseEntity<Object>(
				new ObjectMapper().writeValueAsString(response), 
				responseHeaders,httpStatus);

	}			



	private ResponseEntity<Object>setCoreOnline(Boolean v){
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		innkeeperSDEVRegistrationRequest.setIsCoreOnline(v);
		innkeeperResourceRegistrationRequest.setIsCoreOnline(v);
		this.isCoreOnline=v;
		log.info("Resource is Core ONLINE = "+innkeeperResourceRegistrationRequest.isCoreOnline());
		log.info("SDEV is Core ONLINE = "+innkeeperSDEVRegistrationRequest.isCoreOnline());
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
