package eu.h2020.symbiote.ssp.innkeeper.model;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.h2020.symbiote.model.cim.*;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.ssp.CoreRegister.CoreRegistry;
import eu.h2020.symbiote.ssp.CoreRegister.SspIdUtils;
import eu.h2020.symbiote.ssp.constants.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import eu.h2020.symbiote.ssp.model.InnkeeperResourceRegistrationResponse;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.*;

@Component
public class InnkeeperResourceRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationRequest.class);

	
	@Value("${ssp.id}")
	String sspName;
	
	@Value("${symbIoTe.core.interface.url}")
	String coreIntefaceUrl;
	
	@Value("${innk.core.enabled:true}")
	private Boolean isCoreOnline;
	
	
	
	
	@Value("${latitude}") 
	double latitude;
	@Value("${longitude}") 
	double longitude;
	@Value("${altitude}") 
	double altitude;
	@Value("${ssp.location_name}")
	String locationName;
	

	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	SessionsRepository sessionsRepository;

	@Autowired
	private RegistrationInfoODataRepository infoODataRepo;
	
	@Autowired
	AuthorizationService authorizationService;
	
	@Autowired
	CoreRegistry coreRegistry;
	
	public void setIsCoreOnline(Boolean v) {
		this.isCoreOnline=v;
	}

	public Boolean isCoreOnline() {
		return this.isCoreOnline;
	}
	public ResponseEntity<Object> SspJoinResource(String msg, String type) throws JsonParseException, JsonMappingException, IOException, InvalidArgumentsException{


		ResponseEntity<Object> responseEntity = null;

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		SspResource sspResource =  new ObjectMapper().readValue(msg, SspResource.class);
		Device dd = (Device)sspResource.getResource();

		
		
		// Assign Location on Resource
		if (dd.getLocatedAt()==null) {
			Location loc = new WGS84Location(longitude,latitude,altitude,locationName,null);
			dd.setLocatedAt(loc);
		}
		SessionInfo s=sessionsRepository.findBySymId(sspResource.getSymIdParent());
		// found Symbiote Id in Session Repository
		if (s != null) {			
			
			Date sessionExpiration = null;
			if (type.equals(InnkeeperRestControllerConstants.SDEV)){
				sessionExpiration = sessionsRepository.findBySymId(sspResource.getSymIdParent()).getSessionExpiration();	
			}
			
			InnkeeperResourceRegistrationResponse respSspResource = this.joinResource(sspResource,sessionExpiration,type);
			switch (respSspResource.getResult()) {
			case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
				httpStatus = HttpStatus.BAD_REQUEST;
				break;
			default:
				httpStatus = HttpStatus.OK;
			}

			responseEntity = new  ResponseEntity<Object>(respSspResource,responseHeaders,httpStatus);
			return responseEntity;

		} else {
			log.warn("SymId not found, check with SSP ID...");
		}

		s= sessionsRepository.findBySspId(sspResource.getSspIdParent());

		if (s != null) {
			// If previous registration provides a symId, complete the sspResource payload filling also the symbioteId
			sspResource.setSymIdParent(s.getSymId());
			Date sessionExpiration = s.getSessionExpiration();					
			InnkeeperResourceRegistrationResponse respSspResource = this.joinResource(sspResource,sessionExpiration,type);

			switch (respSspResource.getResult()) {

			case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
				httpStatus = HttpStatus.BAD_REQUEST;
				break;
			default:
				httpStatus = HttpStatus.OK;
			}
			return new ResponseEntity<Object>(respSspResource,responseHeaders,httpStatus);
		} else {
			log.error("sspId not found, registration Failed");
		}

		//DEFAULT: ERROR
		httpStatus=HttpStatus.BAD_REQUEST;
		return new ResponseEntity<Object>("ERROR:sspId not found\nregistration Failed\nsent message: "+msg,responseHeaders,httpStatus);


	}

	public InnkeeperResourceRegistrationResponse joinResource(SspResource msg, Date currTime,String type) throws InvalidArgumentsException, IOException {
		InnkeeperResourceRegistrationResponse res= null;		

		//check The core and assign symId to the Resource (R5 optional)
		coreRegistry.setOnline(this.isCoreOnline);
		coreRegistry.setRepository(resourcesRepository);

		String symIdResource = coreRegistry.getSymbioteIdFromCore(msg,type);

		String results=InnkeeperRestControllerConstants.REGISTRATION_REJECTED;

		res = new InnkeeperResourceRegistrationResponse(
				msg.getResource().getId(), 	//symIdResource
				msg.getSspIdResource(),					//sspIdResource
				msg.getSymIdParent(),							//symId (SDEV)
				msg.getSspIdParent(),							//sspId (SDEV)
				results,								//Result
				0									//registration expiration
				);

		//assign internal a new Id to the resource (R4)		
		if (symIdResource == null) { //REJECTED
			log.error("REJECTED: symIdResource=null");
			results=InnkeeperRestControllerConstants.REGISTRATION_REJECTED;

			return res;
		}

		if (sessionsRepository.findBySspId(msg.getSspIdParent())==null) {
			log.error("REJECTED: symIdResource=null SspId="+msg.getSspIdParent()+" not found");
			results=InnkeeperRestControllerConstants.REGISTRATION_REJECTED; 
			return res;
		}

		//OFFLINE
		if (symIdResource == "") { 

			log.debug("REGISTRATION OFFLINE symIdResource="+symIdResource);
			Resource r=msg.getResource();
			r.setId(symIdResource);
			String newSspIdResource = new SspIdUtils(resourcesRepository).createSspId();
			msg.setSspIdResource(newSspIdResource);
			msg.setResource(r);
			msg.setSymIdParent(sessionsRepository.findBySspId(msg.getSspIdParent()).getSymId());
			this.saveResource(msg,sessionsRepository.findBySspId(msg.getSspIdParent()).getSessionExpiration());

			results=InnkeeperRestControllerConstants.REGISTRATION_OFFLINE;

			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 
					msg.getSspIdResource(),
					msg.getSymIdParent(),
					msg.getSspIdParent(),
					results,
					DbConstants.EXPIRATION_TIME);
		}  

		// ONLINE

		// request symId and sspId not match in SessionRepository
		SessionInfo s = sessionsRepository.findBySymId(msg.getSymIdParent());
		
		
		log.info("symIdResource:"+symIdResource);
		log.info("msg.getSymIdParent()"+msg.getSymIdParent());
		if( s==null ) {
			if (msg.getSymIdParent()==null)
				log.error("REJECTED: Join ONLINE: SymId is null");
			else {
				log.error("REJECTED: Join ONLINE: SymId="+msg.getSymIdParent()+" Not found");
			}
			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 								//symIdResource
					msg.getSspIdResource(),												//sspIdResource
					msg.getSymIdParent(),														//symId (SDEV)
					msg.getSspIdParent(),														//sspId (SDEV)
					InnkeeperRestControllerConstants.REGISTRATION_REJECTED,				//Result
					0																	//registration expiration
					);

		}

		if ( !(s.getSspId().equals(msg.getSspIdParent())) ) {
			log.error("REJECTED: symIdResource=null SspId"+msg.getSymIdParent()+" not matched in Session Repository");

			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 								//symIdResource
					msg.getSspIdResource(),												//sspIdResource
					msg.getSymIdParent(),														//symId (SDEV)
					msg.getSspIdParent(),														//sspId (SDEV)
					InnkeeperRestControllerConstants.REGISTRATION_REJECTED,				//Result
					0																	//registration expiration
					);
		}
		
		Optional<ResourceInfo> rinfo = resourcesRepository.findBySymIdResource(symIdResource);
		
		if (symIdResource != "" && msg.getResource().getId().equals("") 
				//|| (symIdResource != "" && rinfo==null)  						//Not locally exists
				) { //NEW REGISTRATION

			log.debug("NEW REGISTRATION symIdResource="+symIdResource);
			Resource r=msg.getResource();
			r.setId(symIdResource);			
			msg.setSspIdResource(new SspIdUtils(resourcesRepository).createSspId());
			msg.setResource(r);
						
			this.saveResource(msg,sessionsRepository.findBySspId(msg.getSspIdParent()).getSessionExpiration());
			
			results=InnkeeperRestControllerConstants.REGISTRATION_OK;						
			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 
					msg.getSspIdResource(),
					msg.getSymIdParent(),
					msg.getSspIdParent(),
					results,
					0);

		}	
		
		if (symIdResource != "" && rinfo.isPresent()) { //Already exists
			log.error("ALREADY REGISTERED symIdResource="+symIdResource);
			results=InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED;
			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 
					msg.getSspIdResource(),
					msg.getSymIdParent(),
					msg.getSspIdParent(),
					results,
					0);

		}
		
		if (symIdResource != "" && !rinfo.isPresent()) { //Not locally exists
			log.debug("NEW REGISTRATION symIdResource="+symIdResource);
			Resource r=msg.getResource();
			r.setId(symIdResource);			
			msg.setSspIdResource(new SspIdUtils(resourcesRepository).createSspId());
			msg.setResource(r);
						
			this.saveResource(msg,sessionsRepository.findBySspId(msg.getSspIdParent()).getSessionExpiration());
			
			results=InnkeeperRestControllerConstants.REGISTRATION_OK;						
			return new InnkeeperResourceRegistrationResponse(
					msg.getResource().getId(), 
					msg.getSspIdResource(),
					msg.getSymIdParent(),
					msg.getSspIdParent(),
					results,
					0);
			
			
		}
		log.error("RESOURCE REGISTARTION DEFAULT REJECTED");
		return res;

	}

	private void saveResource(SspResource msg,Date currTime) throws InvalidArgumentsException {
		Resource resource = msg.getResource();
		String pluginId = sessionsRepository.findBySspId(msg.getSspIdParent()).getPluginId();
		List<String> props = null;
		if(resource instanceof StationarySensor) {
			props = ((StationarySensor)resource).getObservesProperty();
		} else if(resource instanceof MobileSensor) {
			props = ((MobileSensor)resource).getObservesProperty();
		}
		/*
		try {
			addPolicy(msg.getSspIdResource(), msg.getInternalIdResource(), msg.getAccessPolicy(),currTime);			
			
		}catch (NullPointerException e) {
			log.warn("AccessPolicy is null\n");
		}*/

		addResource(
				msg.getSspIdResource(),				//sspId resource
				msg.getResource().getId(), //symbioteId Resource
				msg.getInternalIdResource(),		//internal Id resource
				msg.getSymIdParent(),						//symbiote Id of SDEV
				msg.getSspIdParent(),						//sspId of SDEV
				props, pluginId,currTime, msg.getAccessPolicy(),msg.getResource());

		//ADD OData
		addInfoForOData(msg);
	}

	private RegistrationInfoOData addInfoForOData(SspResource sspResource){
		RegistrationInfoOData result = null;
		List<Parameter> parameters;
		Resource r = sspResource.getResource();
		
		Date session_expiration=null; 
		Optional<ResourceInfo> rInfo = resourcesRepository.findById(sspResource.getSspIdResource());
		if (rInfo != null) {
			session_expiration=rInfo.get().getSessionExpiration();
		}
		
		if (r.getClass().equals(Actuator.class)) {
			log.debug("Save Device for Resource");
			log.debug("sspResource.getSspIdResource() = " + sspResource.getSspIdResource());
			log.debug("r.getId() = " + r.getId());
			Actuator actuator = (Actuator) r;
			for (Capability capability : actuator.getCapabilities()) {
				parameters = capability.getParameters();
				//String className = "GenericCapability";
				//String superClass = Capability.class.getSimpleName();
				String className = Capability.class.getSimpleName();
				String superClass = null;
				
				result = saveRegistrationInfoODataInDb(sspResource.getSspIdResource(),r.getId(), className, superClass, parameters,session_expiration);
			}
		} else if (r.getClass().equals(Device.class)) {
			log.debug("Save Device for Resource");
			log.debug("sspResource.getSspIdResource() = " + sspResource.getSspIdResource());
			log.debug("r.getId() = " + r.getId());

			Device device = (Device) r;
			for ( Service service : device.getServices()) {
				parameters = service.getParameters();
				//String className = "GenericService";
				//String superClass = Service.class.getSimpleName();
				String className = Service.class.getSimpleName();
				String superClass = null;
				result = saveRegistrationInfoODataInDb(sspResource.getSspIdResource(), r.getId(), className, superClass, parameters,session_expiration);
			}
		} else if (r.getClass().equals(Service.class)) {

			Service service = (Service) r;
			parameters = service.getParameters();
			String className = Service.class.getSimpleName();
			String superClass = null;
			result = saveRegistrationInfoODataInDb(sspResource.getSspIdResource(), r.getId(), className, superClass, parameters,session_expiration);
		}	
		return result;


	}

	private RegistrationInfoOData saveRegistrationInfoODataInDb(String sspIdResource, String id, String className, String superClass, List<Parameter> parameters, Date session_expiration) {
		Set<ParameterInfo> parameterInfoList = new HashSet<>();
		for (Parameter p : parameters) {
			String type = "string";
			Datatype datatype = p.getDatatype();
			if (datatype.getClass().equals(ComplexDatatype.class)) {
				type = ((ComplexDatatype) datatype).getBasedOnClass();
			} else if (datatype.getClass().equals(PrimitiveDatatype.class)) {
				type = ((PrimitiveDatatype) datatype).getBaseDatatype();
			}

			ParameterInfo parameterInfo = new ParameterInfo(type, p.getName(), p.isMandatory());
			parameterInfoList.add(parameterInfo);
		}
		RegistrationInfoOData infoOData = new RegistrationInfoOData(sspIdResource, id, className, superClass, parameterInfoList,session_expiration);
		RegistrationInfoOData infoODataNew = infoODataRepo.insertNewSSP(infoOData);
		return infoODataNew;
	}


	private void addResource(
			String sspIdResource,
			String symIdResource,
			String internalIdResource,
			String symId, // of SDEV/Plat
			String sspId,
			List<String> obsProperties, 
			String pluginId, 
			Date currTime,
			IAccessPolicySpecifier policySpecifier,
			Resource resource) {

		ResourceInfo resourceInfo = new ResourceInfo(
				sspIdResource, symIdResource, internalIdResource,
				symId, sspId, currTime, policySpecifier);
		if(obsProperties != null)
			resourceInfo.setObservedProperties(obsProperties);
		if(pluginId != null && pluginId.length()>0)
			resourceInfo.setPluginUrl(pluginId);
		resourceInfo.setResource(resource);
		resourcesRepository.save(resourceInfo);

	}



}
