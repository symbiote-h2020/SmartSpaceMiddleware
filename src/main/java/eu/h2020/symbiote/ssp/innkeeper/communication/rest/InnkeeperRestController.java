package eu.h2020.symbiote.ssp.innkeeper.communication.rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier.SingleTokenAccessPolicyType;
import eu.h2020.symbiote.security.commons.exceptions.custom.SecurityHandlerException;
import eu.h2020.symbiote.security.commons.exceptions.custom.ValidationException;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by vasgl on 8/24/2017.
 */


@RestController
@RequestMapping(InnkeeperRestControllerConstants.INNKEEPER_BASE_PATH)
public class InnkeeperRestController {
	@Value("${innkeeper.tag.connected_to}")
	private String innk_connected_to;

	@Value("${innkeeper.tag.service_url}")
	private String innk_service_url;

	@Value("${innkeeper.tag.located_at}")
	private String innk_located_at;




	private static Log log = LogFactory.getLog(InnkeeperRestController.class);
	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	SessionRepository sessionRepository;

	@Autowired
	AccessPolicyRepository accessPolicyRepository;

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_JOIN_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> join(@RequestBody Map<String, String> payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {
		log.info("NOT USED");
		return null;
	}

	@SuppressWarnings("unchecked")
	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_REGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	//public ResponseEntity<Object> registry(@RequestBody Map<String, String> payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {
	public ResponseEntity<Object> registry(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {


		//InnkeeperResource innkeeperResource = new InnkeeperResource(payload,sessionRepository,resourcesRepository);		

		Lwsp lwsp = new Lwsp(payload,sessionRepository);

		String rx_json = lwsp.rx(); // generate lwsp response message for given payload

		//save session in mongoDB
		// check MTI: if exists -> negotiation else DATA
		JsonNode lwspNode = new ObjectMapper(new JsonFactory()).readTree(rx_json);
		ResponseEntity<Object> responseEntity = null;
		if (!rx_json.isEmpty() && lwspNode.has("GWInnkeeperHello")) {
			// HANDLE HELLO RESPONSE
			log.info("negotiation");
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			responseEntity = new ResponseEntity<Object>(rx_json, headers, HttpStatus.OK);
			return responseEntity;
		}
		if (!rx_json.isEmpty() && lwspNode.has("GWINKAuthn")) {

				String json = lwsp.decode();
				//Replace INNK local tags
				json=json.replace("INNK_TAG_CONNECTED_TO", innk_connected_to);
				json=json.replace("INNK_TAG_SERVICE_URL", innk_service_url);
				json=json.replace("INNK_TAG_LOCATED_AT", innk_located_at);
				try {

					JsonNode rootNode = new ObjectMapper(new JsonFactory()).readTree(json);  

					Iterator<Map.Entry<String,JsonNode>> fieldsIterator = rootNode.fields();

					while (fieldsIterator.hasNext()) {

						Map.Entry<String,JsonNode> field = fieldsIterator.next();
						//System.out.println("Key: " + field.getKey() + "\tValue:" + field.getValue());
						if (field.getKey().equals("semanticDescription")) {
							JsonNode currNode = field.getValue();

							//log.info("semantic Description fields: connectedTo: "+currNode.get("connectedTo"));
							//log.info("semantic Description fields: hasResource: "+currNode.get("hasResource"));
							//log.info("semantic Description fields: currNode.get(\"hasResource\").size(): "+currNode.get("hasResource").size());

							int num_of_resources = currNode.get("hasResource").size();
							for (int i=0;i<num_of_resources;i++) {
								//log.info("semanticDescription.id="+currNode.get("hasResource").get(i).get("id"));
								//log.info("semanticDescription.locatedAt="+currNode.get("hasResource").get(i).get("locatedAt"));

								//id
								String resourceId=rootNode.get("id").asText();								
								//internalId
								String internalId=rootNode.get("semanticDescription").get("hasResource").get(i).get("id").asText();
								
								//query to mongoDB
								//Access Policy
								IAccessPolicy policy = null;
								try {
									SingleTokenAccessPolicySpecifier accPolicy = null;

									/* it is an object... composed by:
						        @JsonProperty("policyType") SingleTokenAccessPolicyType policyType,
					            @JsonProperty("requiredClaims") Map<String, String> requiredClaims
									 */

									JsonNode policyType = rootNode.get("accessPolicy").get("policyType");
									JsonNode jsonrequiredClaims = rootNode.get("accessPolicy").get("requiredClaims");

									Map<String, String> requiredClaims = new ObjectMapper().convertValue(jsonrequiredClaims, Map.class);

									accPolicy= new SingleTokenAccessPolicySpecifier(
											SingleTokenAccessPolicyType.values()[policyType.asInt()],
											requiredClaims);

									policy = SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(accPolicy);
								}catch (Exception e) {
									policy = null;
								}

								AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
								accessPolicyRepository.save(ap);

								//query to mongoDB
								//Resource Info
								String pluginId = rootNode.get("pluginId").asText();
								JsonNode jsonObservedProperties = rootNode.get("observedProperties");
								ObjectMapper mapperObsProperties = new ObjectMapper();
								List<String> observedProperties = mapperObsProperties.convertValue(jsonObservedProperties, List.class);

								ResourceInfo regInfo = new ResourceInfo(resourceId, internalId);
								regInfo.setPluginId(pluginId);
								regInfo.setObservedProperties(observedProperties);
								resourcesRepository.save(regInfo);

								//TODO: Registration OData

								//TODO: ParameterInfo


							}

						}

					}
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					responseEntity = new ResponseEntity<Object>(rx_json, headers, HttpStatus.OK);
					return responseEntity;
					
				} catch (Exception e) {
					//bypass
					e.printStackTrace();
					HttpHeaders headers = new HttpHeaders();
					headers.setContentType(MediaType.APPLICATION_JSON);
					responseEntity = new ResponseEntity<Object>(" { } ",headers, HttpStatus.BAD_REQUEST);
					return responseEntity;
					
				}

			}
		return responseEntity;
	}

	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_UNREGISTRY_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> unregistry(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, IOException {

		JsonNode rootNode = new ObjectMapper(new JsonFactory()).readTree(payload);				
		//TODO: Lwsp here?
		log.info(rootNode.get("id").asText());
		resourcesRepository.delete(rootNode.get("id").asText());

		return null;
	}


	@RequestMapping(value = InnkeeperRestControllerConstants.INNKEEPER_KEEP_ALIVE_REQUEST_PATH, method = RequestMethod.POST)
	public ResponseEntity<Object> keep_alive(@RequestBody String payload) throws NoSuchAlgorithmException, SecurityHandlerException, ValidationException, JsonProcessingException {


		log.info("KEEP ALIVE: TBD");

		return null;
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
