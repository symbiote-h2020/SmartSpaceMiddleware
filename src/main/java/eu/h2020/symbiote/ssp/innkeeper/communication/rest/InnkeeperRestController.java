package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

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
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
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
import java.util.Date;

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

	@Autowired
	SessionsRepository sessionsRepository;
	public InnkeeperRestController() {

	}

	//TODO: REGISTARTION OF A RESOURCE
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException, InvalidArgumentsException {

		ResponseEntity<Object> responseEntity = null;

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		
		if (isLwspEnabled) {
			//LWSP
		}else{	
			// NO encryption
			String decoded_message = payload;
			SspResource sspResource =  new ObjectMapper().readValue(decoded_message, SspResource.class);

			SessionInfo s=sessionsRepository.findBySymId(sspResource.getSymId());
			// found Symbiote Id in Session Repository
			if (s != null) {			
				Date sessionExpiration = sessionsRepository.findBySymId(sspResource.getSymId()).getSessionExpiration();					
				InnkeeperResourceRegistrationResponse respSspResource = innkeeperResourceRegistrationRequest.registry(sspResource,sessionExpiration);
				switch (respSspResource.getResult()) {
				case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
					httpStatus = HttpStatus.BAD_REQUEST;
				default:
					httpStatus = HttpStatus.OK;
				}

				responseEntity = new  ResponseEntity<Object>(respSspResource,responseHeaders,httpStatus);
				return responseEntity;
				
			}else {
				log.info("SymId not found, check with internal ID...");
			}

			s= sessionsRepository.findByInternalId(sspResource.getInternalId());

			if (s != null) {			
				Date sessionExpiration = sessionsRepository.findBySymId(sspResource.getSymId()).getSessionExpiration();					
				InnkeeperResourceRegistrationResponse respSspResource = innkeeperResourceRegistrationRequest.registry(sspResource,sessionExpiration);
			}else {
				log.info("InternalId not found, registration Failed");
			}


		}

		return responseEntity;
	}


	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		ResponseEntity<Object> responseEntity = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;		
		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			String outputMessage = lwsp.processMessage();
			log.info(outputMessage);
			log.info("MTI:"+lwsp.get_mti());


			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_Hello:
			case LwspConstants.SDEV_AuthN:
				return new ResponseEntity<Object>(outputMessage,responseHeaders,HttpStatus.OK);

			case LwspConstants.SDEV_REGISTRY:
				String decoded_message = lwsp.get_response();
				SspSDEVInfo sspSDEVInfo = new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);
				InnkeeperSDEVRegistrationResponse respSDEV = 
						innkeeperSDEVRegistrationRequest.registry(sspSDEVInfo);
				SessionInfo s = null;
				switch (respSDEV.getResult()) {
				case InnkeeperRestControllerConstants.REGISTRATION_OFFLINE: //OFFLINE
					httpStatus=HttpStatus.OK;
					s = sessionsRepository.findBySessionId(lwsp.getSessionId());
					s.setInternalId(respSDEV.getInternalId());
					s.setSymId(respSDEV.getSymId());
					s.setPluginId(sspSDEVInfo.getPluginId());
					s.setPluginURL(sspSDEVInfo.getPluginUrl());
					sessionsRepository.save(s);
					break;
				case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
					httpStatus=HttpStatus.BAD_REQUEST;
				case InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED:
					httpStatus=HttpStatus.OK;
					break;
				default:			
					// OK ONLINE
					s = sessionsRepository.findBySessionId(lwsp.getSessionId());
					s.setInternalId(respSDEV.getInternalId());
					s.setSymId(respSDEV.getSymId());
					s.setPluginId(sspSDEVInfo.getPluginId());
					s.setPluginURL(sspSDEVInfo.getPluginUrl());
					sessionsRepository.save(s);					
					break;
				}

				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(respSDEV));
				return new ResponseEntity<Object>(encodedResponse,responseHeaders,httpStatus);
			}

		}else{

			Date currTime = new Timestamp(System.currentTimeMillis());
			String sessionId = new Lwsp().generateSessionId();
			SessionInfo s = new SessionInfo();

			String decoded_message = payload;

			SspSDEVInfo sspSDEVInfo =  new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

			InnkeeperSDEVRegistrationResponse respSDEV = innkeeperSDEVRegistrationRequest.registry(sspSDEVInfo);

			//DEBUG: MOCK
			switch (respSDEV.getResult()) {
			case InnkeeperRestControllerConstants.REGISTRATION_OFFLINE: //OFFLINE
			case InnkeeperRestControllerConstants.REGISTRATION_OK:					
				httpStatus=HttpStatus.OK;
				s.setsessionId(sessionId);
				s.setdk1(sspSDEVInfo.getDerivedKey1());
				s.setInternalId(respSDEV.getInternalId());
				s.setSymId(respSDEV.getSymId());						
				s.setPluginId(sspSDEVInfo.getPluginId());
				s.setPluginURL(sspSDEVInfo.getPluginUrl());
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

			String encodedResponse = new ObjectMapper().writeValueAsString(respSDEV);
			return new ResponseEntity<Object>(encodedResponse,responseHeaders,httpStatus);
		}

		return responseEntity;

	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_UNREGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregistry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		
		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			String outputMessage = lwsp.processMessage();
			String encodedResponse=null;
			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_REGISTRY:
				String decoded_message = lwsp.get_response();
				SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

				//Delete Session
				SessionInfo s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
				InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();
				if (s!=null) { //if I found my SymIdDEv in the MongoDb session:
					sessionsRepository.delete(s);					
					response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
					httpStatus=HttpStatus.OK;
				}else {					
					response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
					httpStatus=HttpStatus.BAD_REQUEST;

				}
				return new ResponseEntity<Object>(
						lwsp.send_data(new ObjectMapper().writeValueAsString(response)), //encode the message with LWSP
						responseHeaders,httpStatus);

				//TODO: Delete Resources
			default:
				InnkeeperSDEVRegistrationResponse errorResponse =new InnkeeperSDEVRegistrationResponse();
				errorResponse.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);				
				encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(errorResponse));
				return new ResponseEntity<Object>(encodedResponse,responseHeaders,HttpStatus.BAD_REQUEST);

			}

		} else { 
			String decoded_message = payload;
			SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

			//Delete Session
			SessionInfo s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
			InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();
			if (s!=null) { //if I found my SymIdDEv in the MongoDb session:
				sessionsRepository.delete(s);					
				response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
				httpStatus=HttpStatus.OK;
			} else {					
				response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
				httpStatus=HttpStatus.BAD_REQUEST;

			}
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response), //encode the message with LWSP
					responseHeaders,httpStatus);

			//TODO: Delete Resources

		}
	}


	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> keep_alive(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {

		ResponseEntity<Object> responseEntity = null;
		/*
		Lwsp lwsp = new Lwsp(payload);
		Date currTime = lwspService.keepAliveSession(lwsp);
		if (currTime!=null) {

			ObjectMapper m = new ObjectMapper();

			JsonNode node = m.readTree(lwsp.getRawData());
			InkRegistrationInfo innkInfo = new ObjectMapper().readValue(node.get("payload").toString(), InkRegistrationInfo.class);
			log.info("KEEP ALIVE SymId:"+innkInfo.getSymId());

			List<ResourceInfo> resInfos = resourcesRepository.findByIdLike(innkInfo.getSymId());
			if (resInfos !=null) {
				if (resInfos.size() > 0){
					for (ResourceInfo r: resInfos) {
						r.setSessionExpiration(currTime);
						resourcesRepository.save(r);
					}
				}

			}

		}
		 */
		return responseEntity;
	}


}
