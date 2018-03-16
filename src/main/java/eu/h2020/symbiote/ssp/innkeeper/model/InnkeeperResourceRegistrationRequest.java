package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.Date;
import java.util.List;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;
import eu.h2020.symbiote.ssp.utils.InternalIdUtils;

@Service
public class InnkeeperResourceRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperResourceRegistrationRequest.class);

	@Autowired
	ResourcesRepository resourcesRepository;
	
	@Autowired
	SessionsRepository sessionsRepository;

	@Autowired
	AccessPolicyRepository accessPolicyRepository;

	@Autowired
	OwlApiHelper owlApiHelp;

	public InnkeeperResourceRegistrationResponse registry(SspResource msg, Date currTime) throws InvalidArgumentsException {
		InnkeeperResourceRegistrationResponse res= null;
		
		
		//check The core and assign symId to the Resource (R5 optional)
		
		String symIdResource = new CheckCoreUtility(resourcesRepository).checkCoreSymbioteIdRegistration(msg.getSymIdResource());
		
		//assign internal a new Id to the resource (R4)
		String internalIdResource=new InternalIdUtils(resourcesRepository).createInternalId();
		
		//get internalId
		log.info("msg.getSymId()="+msg.getSymId());
		SessionInfo s = sessionsRepository.findBySymId(msg.getSymId());
		String internalId = null;
		if (s!=null) {
			internalId = s.getInternalId(); //of SDEV/PLAT
		}else {
			log.warn("Symbiote ID for SDEV/PLat "+msg.getSymId()+" not exists");
			res = new InnkeeperResourceRegistrationResponse(
					symIdResource, 
					internalIdResource,
					msg.getSymId(),
					internalId,
					InnkeeperRestControllerConstants.SDEV_REGISTRATION_REJECTED,
					0);
			return res;
		}
		//Save Resource in MongoDB
		
		Resource resource = msg.getSemanticDescription();
		String pluginId = msg.getPluginId();
		String symbioteId = resource.getId(); 
		List<String> props = null;
		if(resource instanceof StationarySensor) {
			props = ((StationarySensor)resource).getObservesProperty();
		} else if(resource instanceof MobileSensor) {
			props = ((MobileSensor)resource).getObservesProperty();
		}
		try {
			addPolicy(symbioteId, internalId, msg.getAccessPolicy());
		}catch (NullPointerException e) {
			log.warn("AccessPolicy is null\n");
		}
		
		addResource(symIdResource, internalIdResource,msg.getSymId(),internalId, props, pluginId,currTime);

		//addCloudResourceInfoForOData(msgs);
		res = new InnkeeperResourceRegistrationResponse(
				symIdResource, 
				internalIdResource,
				msg.getSymId(),
				internalId,
				InnkeeperRestControllerConstants.SDEV_REGISTRATION_OK,
				DbConstants.EXPIRATION_TIME);
		return res;		
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
			String internalIdResource,
			String symId, // of SDEV/Plat
			String internalId,
			List<String> obsProperties, 
			String pluginId, 
			Date currTime) {
		ResourceInfo resourceInfo = new ResourceInfo(symIdResource, internalIdResource,symId,internalId, currTime);
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
