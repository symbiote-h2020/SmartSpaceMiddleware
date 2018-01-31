package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.security.communication.IAAMClient;
import eu.h2020.symbiote.ssp.communication.rest.*;
import eu.h2020.symbiote.ssp.exception.InvalidMacAddressException;
import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledResourceOfflineTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.model.ScheduledUnregisterTimerTask;
import eu.h2020.symbiote.ssp.innkeeper.repository.ResourceRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.stream.Collectors;

/**
 * Created by vasgl on 8/24/2017.
 */







@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {

	private static Log log = LogFactory.getLog(InnkeeperRestController.class);
	protected IAAMClient aamClient;
	private ResourceRepository resourceRepository;
	private RabbitTemplate rabbitTemplate;
	private Integer registrationExpiration;
	private Integer makeResourceOffine;
	private Timer timer;
	private Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap;
	private Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap;

	@Value("${rabbit.exchange.rap.name}")
	private String rapExchange;

	@Value("${rabbit.routingKey.rap.sspResourceCreated}")
	private String rapSSPResourceCreatedRoutingKey;

	@Value("${rabbit.routingKey.rap.sspResourceDeleted}")
	private String rapSSPResourceDeletedRoutingKey;


	@Autowired
	public InnkeeperRestController(ResourceRepository resourceRepository, RabbitTemplate rabbitTemplate,
			@Qualifier("registrationExpiration") Integer registrationExpiration,
			@Qualifier("makeResourceOffline") Integer makeResourceOffine, Timer timer,
			@Qualifier("unregisteringTimerTaskMap") Map<String, ScheduledUnregisterTimerTask> unregisteringTimerTaskMap,
			@Qualifier("offlineTimerTaskMap") Map<String, ScheduledResourceOfflineTimerTask> offlineTimerTaskMap) {

		Assert.notNull(resourceRepository, "Resource repository can not be null!");
		this.resourceRepository = resourceRepository;
		this.resourceRepository.deleteAll();

		Assert.notNull(rabbitTemplate, "Rabbit template can not be null!");
		this.rabbitTemplate = rabbitTemplate;

		Assert.notNull(registrationExpiration, "registrationExpiration can not be null!");
		this.registrationExpiration = registrationExpiration;

		Assert.notNull(makeResourceOffine, "makeResourceOffine can not be null!");
		this.makeResourceOffine = makeResourceOffine;

		Assert.notNull(timer, "Timer can not be null!");
		this.timer = timer;

		Assert.notNull(unregisteringTimerTaskMap, "unregisteringTimerTaskMap can not be null!");
		this.unregisteringTimerTaskMap = unregisteringTimerTaskMap;

		Assert.notNull(offlineTimerTaskMap, "offlineTimerTaskMap can not be null!");
		this.offlineTimerTaskMap = offlineTimerTaskMap;
		
		//dbconnect();
	}

	
	public ResponseEntity<Object> setResponseJoinEntity() {
		HttpHeaders headers =new HttpHeaders();
		headers.add("1", "JOINuno");
		headers.add("2", "due");
		headers.add("7", "settebbabbau");
		String mybody="<html>THIS IS A BODY</html>\n";
		ResponseEntity<Object> myresponse = new ResponseEntity<>(mybody,headers, HttpStatus.OK);
		return myresponse;
	}	
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody Map<String, String> requestParams) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {
//		public ResponseEntity<Object> join(@RequestParam Map<String, String> requestParams) {
		log.info("New join request was received  requestParams = " + requestParams);
		String id = requestParams.get("id");
		String mac = requestParams.get("mac");
		String semanticDescription = requestParams.get("semanticDescription");
		
		log.info("------------------------------------");
		log.info("requestParams	   		= " + requestParams);
		log.info("id                  	= " + id);
		log.info("mac        		   	= " + mac);
		log.info("semanticDescription 	= " + semanticDescription);
		log.info("------------------------------------");
		
		
		return this.setResponseJoinEntity();

	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> registry(@RequestBody Map<String, String> payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {

		

		log.info("New registry request was received  payload = " + payload);
		log.info(payload.keySet());
		log.info(InnkeeperRestControllerConstants.PLATFORM_PAYLOAD_VALS);

		ResponseEntity<Object> myresponse = null;
		
		if (new HashSet<String>( payload.keySet()).equals( 
				new HashSet<String>(InnkeeperRestControllerConstants.PLATFORM_PAYLOAD_VALS))){
			log.info("Platform Registration do something here...");

			//Platform sends explicit payload 
			// Object response=requestParams;
			
			String id = payload.get("id");
			String name = payload.get("name");
			String description = payload.get("description");
			String url = payload.get("url");
			String informationModel = payload.get("informationModel");
	
			log.info("------------------------------------");
			log.info("id          		= " + id);
			log.info("name        		= " + name);
			log.info("description 		= " + description);
			log.info("url         		= " + url);
			log.info("informationModel	= " + informationModel);
			log.info("------------------------------------");
			
			//Handle Platform Registry here...
			//TODO: LAAM communication 
			//TODO: Core Communication
			
			//forge response
			HttpHeaders headers =new HttpHeaders();
			headers.add("1", "this is a platform");
			String mybody="<html>OK, this is a Platform, here some BODY</html>\n";
			myresponse = new ResponseEntity<>(mybody,headers, HttpStatus.OK);
			
		}else {
			if (new HashSet<String>( payload.keySet()).equals( 
					new HashSet<String>(InnkeeperRestControllerConstants.SDEV_PAYLOAD_VALS))){
				log.info("SDEV Registration do something here...");
				//Handle SDEV Registry here...
				
				//forge response
				HttpHeaders headers =new HttpHeaders();
				headers.add("1", "this is a SDEV");
				String mybody="<html>OK, this is a SDEV, here some BODY</html>\n";
				myresponse = new ResponseEntity<>(mybody,headers, HttpStatus.OK);
			}else {
				log.info("UNKNOWN PAYLOAD");
				
				//forge response
				HttpHeaders headers =new HttpHeaders();
				headers.add("1", "NOT FOUND UNKNOWN PAYLOAD");
				String mybody="<html>NOT FOUND UNKOWN PAYLOAD</html>\n";
				myresponse = new ResponseEntity<>(mybody,headers, HttpStatus.NOT_FOUND);
			}
			
		}

		// TODO: when a registry request arrives innkeeper forward those info to the
		// Core
		// 1. forge a response to symbiote agent
		
		// 1. LAAM Request

		// 2. Core Registration (forward payload)

		// 3. SSP-RAP Registration (forward payload)

		return myresponse;


		
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
}
