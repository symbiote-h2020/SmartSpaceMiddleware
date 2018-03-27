package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;
import eu.h2020.symbiote.ssp.utils.SspIdUtils;

@Service
public class InnkeeperResourceRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationRequest.class);

	@Value("${innk.core.enabled:true}")
	private Boolean isCoreOnline;

	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	SessionsRepository sessionsRepository;

	@Autowired
	AccessPolicyRepository accessPolicyRepository;

	@Autowired
	OwlApiHelper owlApiHelp;

	public void setIsCoreOnline(Boolean v) {
		this.isCoreOnline=v;
	}

	public Boolean isCoreOnline() {
		return this.isCoreOnline;
	}

	public InnkeeperResourceRegistrationResponse registry(SspResource msg, Date currTime) throws InvalidArgumentsException, JsonProcessingException {
		InnkeeperResourceRegistrationResponse res= null;
		log.info(new ObjectMapper().writeValueAsString(msg));

		//check The core and assign symId to the Resource (R5 optional)
		String symIdResource = new CheckCoreUtility(resourcesRepository,this.isCoreOnline).checkCoreSymbioteIdRegistration(msg.getSemanticDescription().getId());

		String results=InnkeeperRestControllerConstants.REGISTRATION_REJECTED;

		res = new InnkeeperResourceRegistrationResponse(
				msg.getSemanticDescription().getId(), 	//symIdResource
				msg.getSspIdResource(),					//sspIdResource
				msg.getSymId(),							//symId (SDEV)
				msg.getSspId(),							//sspId (SDEV)
				results,								//Result
				0									//registration expiration
				);

		//assign internal a new Id to the resource (R4)		
		if (symIdResource == null) { //REJECTED
			results=InnkeeperRestControllerConstants.REGISTRATION_REJECTED;

			return res;
		}

		//OFFLINE
		if (symIdResource == "") { 

			log.info("REGISTRATION OFFLINE symIdResource="+symIdResource);
			Resource r=msg.getSemanticDescription();
			r.setId(symIdResource);
			String newSspIdResource = new SspIdUtils(resourcesRepository).createSspId();
			log.info(newSspIdResource);
			msg.setSspIdResource(newSspIdResource);
			msg.setSemanticDesciption(r);

			this.saveResource(msg,sessionsRepository.findBySspId(msg.getSspId()).getSessionExpiration());

			results=InnkeeperRestControllerConstants.REGISTRATION_OFFLINE;

			return new InnkeeperResourceRegistrationResponse(
					msg.getSemanticDescription().getId(), 
					msg.getSspIdResource(),
					msg.getSymId(),
					new SspIdUtils(resourcesRepository).createSspId(),
					results,
					DbConstants.EXPIRATION_TIME);
		}  

		// ONLINE

		// request symId and sspId not match in SessionRepository
		SessionInfo s = sessionsRepository.findBySymId(msg.getSymId());
		if( s==null ) {
			return new InnkeeperResourceRegistrationResponse(
					msg.getSemanticDescription().getId(), 								//symIdResource
					msg.getSspIdResource(),												//sspIdResource
					msg.getSymId(),														//symId (SDEV)
					msg.getSspId(),														//sspId (SDEV)
					InnkeeperRestControllerConstants.REGISTRATION_REJECTED,				//Result
					0																	//registration expiration
					);

		}else {
			if ( !(s.getSspId().equals(msg.getSspId())) ) {
				return new InnkeeperResourceRegistrationResponse(
						msg.getSemanticDescription().getId(), 								//symIdResource
						msg.getSspIdResource(),												//sspIdResource
						msg.getSymId(),														//symId (SDEV)
						msg.getSspId(),														//sspId (SDEV)
						InnkeeperRestControllerConstants.REGISTRATION_REJECTED,				//Result
						0																	//registration expiration
						);
			}
		}

		if (symIdResource != "" && !msg.getSymId().equals(symIdResource)) { //REGISTER!

			log.info("NEW REGISTRATION symIdResource="+symIdResource);
			Resource r=msg.getSemanticDescription();
			r.setId(symIdResource);			
			msg.setSspIdResource(new SspIdUtils(resourcesRepository).createSspId());
			msg.setSemanticDesciption(r);

			this.saveResource(msg,sessionsRepository.findBySspId(msg.getSspId()).getSessionExpiration());

			results=InnkeeperRestControllerConstants.REGISTRATION_OK;						
			return new InnkeeperResourceRegistrationResponse(
					msg.getSemanticDescription().getId(), 
					msg.getSspIdResource(),
					msg.getSymId(),
					msg.getSspId(),
					results,
					0);

		}	

		if (symIdResource != "" && msg.getSymId().equals(symIdResource)) { //Already exists
			log.info("ALREADY REGISTERED symIdResource="+symIdResource);
			results=InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED;
			return new InnkeeperResourceRegistrationResponse(
					msg.getSemanticDescription().getId(), 
					msg.getSspIdResource(),
					msg.getSymId(),
					msg.getSspId(),
					results,
					0);

		}	
		log.info("RESOURCE REGISTARTION DEFAULT REJECTED");
		return res;

	}

	private void saveResource(SspResource msg,Date currTime) throws InvalidArgumentsException {
		Resource resource = msg.getSemanticDescription();
		String pluginId = sessionsRepository.findBySspId(msg.getSspId()).getPluginId(); 
		String symbioteIdResource = resource.getId();
		List<String> props = null;
		if(resource instanceof StationarySensor) {
			props = ((StationarySensor)resource).getObservesProperty();
		} else if(resource instanceof MobileSensor) {
			props = ((MobileSensor)resource).getObservesProperty();
		}

		try {
			addPolicy(symbioteIdResource, msg.getInternalIdResource(), msg.getAccessPolicy());
		}catch (NullPointerException e) {
			log.warn("AccessPolicy is null\n");
		}
		log.info("ADD RESOURCE:");
		addResource(
				msg.getSemanticDescription().getId(), //symbioteId Resource
				msg.getSspIdResource(),				//sspId resource
				msg.getInternalIdResource(),		//internal Id resource
				msg.getSymId(),						//symbiote Id of SDEV
				msg.getSspId(),						//sspId of SDEV  
				props, pluginId,currTime);	
	}

	private void addCloudResourceInfoForOData(List<CloudResource> cloudResourceList) {
		try{
			owlApiHelp.addCloudResourceList(cloudResourceList);
		}
		catch(Exception e){
			log.error("Error add info registration for OData\n"+e.getMessage());
		}
	}
	private void addResource(
			String symIdResource,
			String sspIdResource,
			String internalIdResource,
			String symId, // of SDEV/Plat
			String sspId,
			List<String> obsProperties, 
			String pluginId, 
			Date currTime) {

		ResourceInfo resourceInfo = new ResourceInfo(symIdResource, sspIdResource,internalIdResource,symId,sspId, currTime);
		if(obsProperties != null)
			resourceInfo.setObservedProperties(obsProperties);
		if(pluginId != null && pluginId.length()>0)
			resourceInfo.setPluginId(pluginId);
		resourcesRepository.save(resourceInfo);

		try {
			log.info("Resource " + new ObjectMapper().writeValueAsString(resourceInfo).toString() + " registered");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
	}


	private void addPolicy(String resourceId, String internalId, IAccessPolicySpecifier accPolicy) throws InvalidArgumentsException {
		try {
			IAccessPolicy policy = AccessPolicyFactory.getAccessPolicy(accPolicy);
			AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
			log.debug("ADD POLICY ACTION");
			accessPolicyRepository.save(ap);

			log.info("Policy successfully added for resource " + resourceId);
		} catch (InvalidArgumentsException e) {
			log.error("Invalid Policy definition for resource with id " + resourceId);
		}
	}
	/*
	private void deletePolicy(String internalId) {
		try {
			Optional<AccessPolicy> accessPolicy = accessPolicyRepository.findByInternalId(internalId);
			if(accessPolicy == null || accessPolicy.get() == null) {
				log.error("No policy stored for resource with internalId " + internalId);
				return;
			}

			accessPolicyRepository.delete(accessPolicy.get().getResourceId());
			log.info("Policy removed for resource " + internalId);

		} catch (Exception e) {
			log.error("Resource with internalId " + internalId + " not found - Exception: " + e.getMessage());
		}
	}
	 */


}
