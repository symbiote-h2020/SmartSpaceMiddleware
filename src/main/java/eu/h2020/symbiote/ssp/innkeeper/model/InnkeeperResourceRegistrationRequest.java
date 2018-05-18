package eu.h2020.symbiote.ssp.innkeeper.model;

import java.io.IOException;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.Actuator;
import eu.h2020.symbiote.model.cim.Capability;
import eu.h2020.symbiote.model.cim.ComplexDatatype;
import eu.h2020.symbiote.model.cim.Datatype;
import eu.h2020.symbiote.model.cim.Device;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Parameter;
import eu.h2020.symbiote.model.cim.PrimitiveDatatype;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.Service;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.ParameterInfo;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;
import eu.h2020.symbiote.ssp.utils.SspIdUtils;

@Component
public class InnkeeperResourceRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationRequest.class);

	@Value("${innk.core.enabled:true}")
	private Boolean isCoreOnline;

	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	SessionsRepository sessionsRepository;

	@Autowired
	private RegistrationInfoODataRepository infoODataRepo;

	public void setIsCoreOnline(Boolean v) {
		this.isCoreOnline=v;
	}

	public Boolean isCoreOnline() {
		return this.isCoreOnline;
	}
	public ResponseEntity<Object> SspJoinResource(String msg) throws JsonParseException, JsonMappingException, IOException, InvalidArgumentsException{


		ResponseEntity<Object> responseEntity = null;

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		SspResource sspResource =  new ObjectMapper().readValue(msg, SspResource.class);

		SessionInfo s=sessionsRepository.findBySymId(sspResource.getSymIdParent());
		// found Symbiote Id in Session Repository
		if (s != null) {			
			Date sessionExpiration = sessionsRepository.findBySymId(sspResource.getSymIdParent()).getSessionExpiration();
			InnkeeperResourceRegistrationResponse respSspResource = this.joinResource(sspResource,sessionExpiration);
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
			Date sessionExpiration = s.getSessionExpiration();					
			InnkeeperResourceRegistrationResponse respSspResource = this.joinResource(sspResource,sessionExpiration);

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

	public InnkeeperResourceRegistrationResponse joinResource(SspResource msg, Date currTime) throws InvalidArgumentsException, JsonProcessingException {
		InnkeeperResourceRegistrationResponse res= null;
		log.info(new ObjectMapper().writeValueAsString(msg));

		//check The core and assign symId to the Resource (R5 optional)
		String symIdResource = new CheckCoreUtility(resourcesRepository,this.isCoreOnline).checkCoreSymbioteIdRegistration(msg.getResource().getId());

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

			log.info("REGISTRATION OFFLINE symIdResource="+symIdResource);
			Resource r=msg.getResource();
			r.setId(symIdResource);
			String newSspIdResource = new SspIdUtils(resourcesRepository).createSspId();
			log.info(newSspIdResource);
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

		if (symIdResource != "" && !msg.getSymIdParent().equals(symIdResource)) { //REGISTER!

			log.info("NEW REGISTRATION symIdResource="+symIdResource);
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

		if (symIdResource != "" && msg.getSymIdParent().equals(symIdResource)) { //Already exists
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

		log.info("ADD RESOURCE:");
		addResource(
				msg.getSspIdResource(),				//sspId resource
				msg.getResource().getId(), //symbioteId Resource
				msg.getInternalIdResource(),		//internal Id resource
				msg.getSymIdParent(),						//symbiote Id of SDEV
				msg.getSspIdParent(),						//sspId of SDEV
				props, pluginId,currTime, msg.getAccessPolicy(),msg.getResource());

		//ADD OData
		log.info("ADD OData:");
		log.info("msg.getSemanticDescription().getId()="+msg.getResource().getId());
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
			log.info("Save Device for Resource");
			log.info("sspResource.getSspIdResource() = " + sspResource.getSspIdResource());
			log.info("r.getId() = " + r.getId());
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
			log.info("Save Device for Resource");
			log.info("sspResource.getSspIdResource() = " + sspResource.getSspIdResource());
			log.info("r.getId() = " + r.getId());

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
		log.info("RegistrationInfoOData, id:"+id);
		Set<ParameterInfo> parameterInfoList = new HashSet<>();
		for (Parameter p : parameters) {
			String type = "string";
			Datatype datatype = p.getDatatype();
			if (datatype.getClass().equals(ComplexDatatype.class)) {
				type = ((ComplexDatatype) datatype).getBasedOnClass();
			} else if (datatype.getClass().equals(PrimitiveDatatype.class)) {
				type = ((PrimitiveDatatype) datatype).getBaseDatatype();
			}

			log.info("Add parameter:");
			log.info("type:"+type);
			log.info("p.getName():"+p.getName());
			log.info("p.isMandatory():"+p.isMandatory());

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

		try {
			log.info("Resource " + new ObjectMapper().writeValueAsString(resourceInfo).toString() + " registered");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}

/*
	private void addPolicy(String resourceId, String internalId, IAccessPolicySpecifier accPolicy, Date currTime) throws InvalidArgumentsException {
		try {
			IAccessPolicy policy = AccessPolicyFactory.getAccessPolicy(accPolicy);
			AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy,currTime);
			log.debug("ADD POLICY ACTION");
			accessPolicyRepository.save(ap);

			log.info("Policy successfully added for resource " + resourceId);
		} catch (InvalidArgumentsException e) {
			log.error("Invalid Policy definition for resource with id " + resourceId);
		}
	}
	
	private void deletePolicy(String id) {
		try {
			Optional<AccessPolicy> accessPolicy = accessPolicyRepository.findById(id);
			if(accessPolicy == null || accessPolicy.get() == null) {
				log.error("No policy stored for resource with internalId " + id);
				return;
			}

			accessPolicyRepository.delete(accessPolicy.get().getResourceId());
			log.info("Policy removed for resource " + id);

		} catch (Exception e) {
			log.error("Resource with internalId " + id + " not found - Exception: " + e.getMessage());
		}
	}
*/	 


}
