package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationResponse;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
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
			//LWSP TODO: implement here
			return null;
		}else{	
			// NO encryption
			return innkeeperResourceRegistrationRequest.SspJoinResource(payload);
		}


	}

	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

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
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspRegistry(decoded_message);
				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(res.getBody()));
				return new ResponseEntity<Object>(encodedResponse,res.getHeaders(),res.getStatusCode());
			default:
				return new ResponseEntity<Object>("",responseHeaders,HttpStatus.INTERNAL_SERVER_ERROR);
			}

		}else{
			return innkeeperSDEVRegistrationRequest.SspRegistry(payload);
		}

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
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspDelete(decoded_message);
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
			return innkeeperSDEVRegistrationRequest.SspDelete(payload);

		}
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
				ResponseEntity<Object> res = innkeeperSDEVRegistrationRequest.SspKeepAlive(decoded_message);
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
			return innkeeperSDEVRegistrationRequest.SspKeepAlive(payload);

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
