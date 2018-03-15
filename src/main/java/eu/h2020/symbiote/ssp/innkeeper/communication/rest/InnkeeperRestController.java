package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.innkeeper.model.InkRegistrationInfo;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResourceRegistrationResponse;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationRequest;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperSDEVRegistrationResponse;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

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
	Lwsp lwsp;

	@Autowired
	SessionRepository sessionRepository;


	//REGISTARTION OF A RESOURCE
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {

		ResponseEntity<Object> responseEntity = null;

		return responseEntity;
	}


	//REGISTRATION OF SDEV
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody String payload) throws InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException, Exception {

		ResponseEntity<Object> responseEntity = null;
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		boolean isLwspEnabled = false;
		if (isLwspEnabled) {
			lwsp.setData(payload);
			lwsp.setAllowedCipher("0x008c");
			String outputMessage = lwsp.processMessage();
			log.info(outputMessage);
			log.info("MTI:"+lwsp.get_mti());


			switch (lwsp.get_mti()) {
			case LwspConstants.SDEV_Hello:
			case LwspConstants.SDEV_AuthN:

				responseHeaders.setContentType(MediaType.APPLICATION_JSON);
				return new ResponseEntity<Object>(outputMessage,responseHeaders,HttpStatus.OK);
			case LwspConstants.SDEV_REGISTRY:
				String decoded_message = lwsp.get_response();

				SspSDEVInfo sspSDEVInfo =  new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);
				
				InnkeeperSDEVRegistrationResponse respSDEV = innkeeperSDEVRegistrationRequest.registry(sspSDEVInfo);

				switch (respSDEV.getResult()) {
				case InnkeeperRestControllerConstants.SDEV_REGISTRATION_OFFLINE: //OFFLINE
					httpStatus=HttpStatus.OK;
					lwsp.updateSessionRepository(lwsp.getSessionId(), respSDEV.getSymIdSDEV(), respSDEV.getInternalIdSDEV());
					break;
				case InnkeeperRestControllerConstants.SDEV_REGISTRATION_REJECTED:
					httpStatus=HttpStatus.BAD_REQUEST;
				case InnkeeperRestControllerConstants.SDEV_REGISTRATION_ALREADY_REGISTERED:
					httpStatus=HttpStatus.OK;
					sessionRepository.delete(sessionRepository.findBySessionId(lwsp.getSessionId()));
					break;
				default:
					lwsp.updateSessionRepository(lwsp.getSessionId(), respSDEV.getSymIdSDEV(), respSDEV.getInternalIdSDEV());
					break;
				}

				String encodedResponse = lwsp.send_data(new ObjectMapper().writeValueAsString(respSDEV));
				return new ResponseEntity<Object>(encodedResponse,responseHeaders,httpStatus);
			}

		}else{
			Date currTime = new Timestamp(System.currentTimeMillis());
			lwsp.generateSessionId();
			SessionInfo sessionInfo = new SessionInfo(lwsp.getSessionId(),null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,null,currTime,null,"0");
			sessionRepository.save(sessionInfo);			

			String decoded_message = payload;

			SspSDEVInfo sspSDEVInfo =  new ObjectMapper().readValue(decoded_message, SspSDEVInfo.class);

			InnkeeperSDEVRegistrationResponse respSDEV = innkeeperSDEVRegistrationRequest.registry(sspSDEVInfo);

			//DEBUG: MOCK
			switch (respSDEV.getResult()) {
			case InnkeeperRestControllerConstants.SDEV_REGISTRATION_OFFLINE: //OFFLINE
				httpStatus=HttpStatus.OK;
				lwsp.updateSessionRepository(lwsp.getSessionId(), respSDEV.getSymIdSDEV(), respSDEV.getInternalIdSDEV());
				break;
			case InnkeeperRestControllerConstants.SDEV_REGISTRATION_REJECTED:
				httpStatus=HttpStatus.BAD_REQUEST;
			case InnkeeperRestControllerConstants.SDEV_REGISTRATION_ALREADY_REGISTERED:
				httpStatus=HttpStatus.OK;
				sessionRepository.delete(sessionRepository.findBySessionId(lwsp.getSessionId()));
				break;
			default:
				lwsp.updateSessionRepository(lwsp.getSessionId(), respSDEV.getSymIdSDEV(), respSDEV.getInternalIdSDEV());
				break;
			}
			
			String encodedResponse = new ObjectMapper().writeValueAsString(respSDEV);
			return new ResponseEntity<Object>(encodedResponse,responseHeaders,httpStatus);
		}




		return responseEntity;

	}





	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_UNREGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregistry(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {
		ResponseEntity<Object> responseEntity = null;

		//Lwsp lwsp = new Lwsp(payload);
		//String sessionId = lwspService.unregistry(lwsp);

		/*
		if (sessionId!=null) {

			ObjectMapper m = new ObjectMapper();

			JsonNode node = m.readTree(lwsp.getRawData());
			InkRegistrationInfo innkInfo = new ObjectMapper().readValue(node.get("payload").toString(), InkRegistrationInfo.class);
			log.info("UNREGISTRY DELETE SymId:"+innkInfo.getSymId());

			List<ResourceInfo> resInfos = resourcesRepository.findByIdLike(innkInfo.getSymId());
			if (resInfos !=null) {
				if (resInfos.size() > 0){
					for (ResourceInfo r: resInfos) {
						resourcesRepository.delete(r);
					}
				}

			}
		}
		 */
		return responseEntity;

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


	/*
	 * ResponseEntity<JoinResponse> join(@RequestBody JoinRequest joinRequest)
	 * throws Exception { System.out.println("test"); boolean alreadyRegistered =
	 * false; JoinResponse joinResponse;
	 * 
	 * log.info("New join request was received for resource id = " +
	 * joinRequest.getId());
	 * 
	 * if(joinRequest.getDeviceDescriptor() == null ||
	 * joinRequest.getDeviceDescriptor().getUrl() == null ||
	 * joinRequest.getObservesProperty() == null ||
	 * joinRequest.getObservesProperty().isEmpty()) throw new
	 * Exception("Url of deviceDescriptor in body cannot be empty");
	 * 
	 * if (joinRequest.getId() == null || joinRequest.getId().isEmpty()) { ObjectId
	 * objectId = new ObjectId(); joinRequest.setId(objectId.toString()); } else {
	 * // Check if the resource is already registered InnkeeperResource resource =
	 * resourceRepository.findOne(joinRequest.getId());
	 * 
	 * if (resource != null) alreadyRegistered = true; }
	 * 
	 * // Create UnregistrationTimerTask ScheduledUnregisterTimerTask
	 * unregisterTimerTask = createUnregisterTimerTask(joinRequest.getId());
	 * 
	 * // Create OfflineTimerTask ScheduledResourceOfflineTimerTask offlineTimerTask
	 * = createOfflineTimerTask(joinRequest.getId());
	 * 
	 * InnkeeperResource newResource = resourceRepository.save(new
	 * InnkeeperResource(joinRequest, unregisterTimerTask, offlineTimerTask));
	 * log.info("newResource.getId() = " + newResource.getId());
	 * 
	 * // Inform RAP about the new resource SSPRecourceCreatedOrUpdated
	 * sspRecourceCreatedOrUpdated = new
	 * SSPRecourceCreatedOrUpdated(newResource.getId(),
	 * newResource.getDeviceDescriptor().getUrl());
	 * rabbitTemplate.convertAndSend(rapExchange, rapSSPResourceCreatedRoutingKey,
	 * sspRecourceCreatedOrUpdated);
	 * 
	 * if (alreadyRegistered) joinResponse = new
	 * JoinResponse(JoinResponseResult.ALREADY_REGISTERED, newResource.getId(), "",
	 * registrationExpiration); else joinResponse = new
	 * JoinResponse(JoinResponseResult.OK, newResource.getId(), "",
	 * registrationExpiration);
	 * 
	 * return ResponseEntity.ok(joinResponse); }
	 */
	/*
	@PostMapping(InnkeeperRestControllerConstants.INNKEEPER_LIST_RESOURCES_REQUEST_PATH)
	ResponseEntity<ListResourcesResponse> listResources(@RequestBody ListResourcesRequest request) {

		log.info("New list_resource request was received from symbIoTe device with id = " + request.getId());

		ListResourcesResponse listResourcesResponse = new ListResourcesResponse();
		List<InnkeeperListResourceInfo> innkeeperListResourceInfoList = new ArrayList<>();
		List<InnkeeperResource> innkeeperResourceList = resourceRepository.findAll();

		for (InnkeeperResource resource : innkeeperResourceList) {
			InnkeeperListResourceInfo innkeeperListResourceInfo = new InnkeeperListResourceInfo(resource.getId(),
					resource.getDeviceDescriptor().getName(), resource.getDeviceDescriptor().getDescription(),
					resource.getDeviceDescriptor().getUrl(), resource.getStatus(), resource.getObservesProperty());
			innkeeperListResourceInfoList.add(innkeeperListResourceInfo);
		}

		listResourcesResponse.setInnkeeperListResourceInfoList(innkeeperListResourceInfoList);
		return ResponseEntity.ok(listResourcesResponse);
	}

	@PostMapping(InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH)
	ResponseEntity<KeepAliveResponse> keepAlive(@RequestBody KeepAliveRequest req) {

		log.info("New keep_alive request was received from symbIoTe device with id = " + req.getId());

		InnkeeperResource innkeeperResource = resourceRepository.findOne(req.getId());

		if (innkeeperResource == null) {
			KeepAliveResponse response = new KeepAliveResponse(
					"The request with id = " + req.getId() + " was not registered.");
			return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
		} else {
			ScheduledResourceOfflineTimerTask offlineTimerTask = createOfflineTimerTask(innkeeperResource.getId());
			innkeeperResource.setOfflineEventTime(offlineTimerTask.scheduledExecutionTime());
			innkeeperResource.setStatus(InnkeeperResourceStatus.ONLINE);
			resourceRepository.save(innkeeperResource);

			KeepAliveResponse response = new KeepAliveResponse(
					"The keep_alive request from resource with id = " + req.getId() + " was received successfully!");
			return ResponseEntity.ok(response);
		}
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<JoinResponse> httpMessageNotReadableExceptionHandler(HttpServletRequest req) {
		ObjectMapper mapper = new ObjectMapper();
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);

		try {
			String requestInString = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
			if (req.getMethod().equals(HttpMethod.POST.toString())
					&& req.getPathInfo().equals(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH
							+ InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH)) {
				JoinRequest joinRequest = mapper.readValue(requestInString, JoinRequest.class);
			}
		} catch (JsonMappingException e) {
			e.printStackTrace();
			StringWriter sw = new StringWriter();
			e.printStackTrace(new PrintWriter(sw));

			if (sw.toString().contains(InvalidMacAddressException.class.getName())) {
				JoinResponse joinResponse = new JoinResponse(JoinResponseResult.INVALID_MAC_ADDRESS_FORMAT, null, null,
						null);
				return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
			} else {
				JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
				return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
			}
		} catch (IOException e) {
			e.printStackTrace();
			JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
			return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
		}

		JoinResponse joinResponse = new JoinResponse(JoinResponseResult.REJECT, null, null, null);
		return new ResponseEntity<>(joinResponse, responseHeaders, HttpStatus.BAD_REQUEST);
	}

	private void cancelUnregisterTimerTask(String resourceId) {
		ScheduledUnregisterTimerTask timerTask = unregisteringTimerTaskMap.get(resourceId);

		if (timerTask != null)
			timerTask.cancel();
	}

	private void cancelOfflineTimerTask(String resourceId) {
		ScheduledResourceOfflineTimerTask timerTask = offlineTimerTaskMap.get(resourceId);

		if (timerTask != null)
			timerTask.cancel();
	}

	private ScheduledUnregisterTimerTask createUnregisterTimerTask(String resourceId) {
		cancelUnregisterTimerTask(resourceId);

		ScheduledUnregisterTimerTask timerTask = new ScheduledUnregisterTimerTask(resourceRepository, rabbitTemplate,
				resourceId, rapExchange, rapSSPResourceDeletedRoutingKey, unregisteringTimerTaskMap,
				offlineTimerTaskMap);
		timer.schedule(timerTask, registrationExpiration);
		unregisteringTimerTaskMap.put(resourceId, timerTask);

		return timerTask;
	}

	private ScheduledResourceOfflineTimerTask createOfflineTimerTask(String resourceId) {
		cancelOfflineTimerTask(resourceId);

		ScheduledResourceOfflineTimerTask timerTask = new ScheduledResourceOfflineTimerTask(resourceRepository,
				resourceId, offlineTimerTaskMap);
		timer.schedule(timerTask, makeResourceOffine);
		offlineTimerTaskMap.put(resourceId, timerTask);

		return timerTask;
	}
	 */
}
