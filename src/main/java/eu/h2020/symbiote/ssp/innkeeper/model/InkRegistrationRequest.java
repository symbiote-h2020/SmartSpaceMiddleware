package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.Date;
import java.util.List;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;
import eu.h2020.symbiote.ssp.resources.SspResource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;

@Service
public class InkRegistrationRequest {

	private static Log log = LogFactory.getLog(InkRegistrationRequest.class);

	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	AccessPolicyRepository accessPolicyRepository;

	@Autowired
	OwlApiHelper owlApiHelp;

	public InkRegistrationResponse registry(InkRegistrationInfo info, Date currTime) throws InvalidArgumentsException {
		InkRegistrationResponse res= null;

/*
		List<SspResource> msgs = info.getSemanticDescription();
		
		info.getSymId();
		for(SspResource msg: msgs){
			String internalId = msg.getInternalId(); 
			Resource resource = msg.getResource();
			String pluginId = msg.getPluginId();
			String symbioteId = resource.getId(); //each resource has an unique symbioteId
			List<String> props = null;
			if(resource instanceof StationarySensor) {
				props = ((StationarySensor)resource).getObservesProperty();
			} else if(resource instanceof MobileSensor) {
				props = ((MobileSensor)resource).getObservesProperty();
			}
			log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);
			//FIXME: if msg.getSingleTokenAccessPolicy()== null do not addPolicy
			try {
				addPolicy(symbioteId, internalId, msg.getAccessPolicy());
			}catch (NullPointerException e) {
				log.error("error during addPolicy process, AccessPolicy is null\n");
			}
			addResource(symbioteId, internalId, props, pluginId,currTime);
		}
		addSspResourceInfoForOData(msgs);
		res = new InkRegistrationResponse(info.getSymId(),LwspConstants.REGISTARTION_OK,DbConstants.EXPIRATION_TIME);

*/
		return res;
	}



	private void addSspResourceInfoForOData(List<SspResource> sspResourceList) {
		try{
			owlApiHelp.addSspResourceList(sspResourceList);
		}
		catch(Exception e){
			log.error("Error add info registration for OData\n"+e.getMessage());
		}
	}
	private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId, Date currTime) {
		ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId, currTime);
		if(obsProperties != null)
			resourceInfo.setObservedProperties(obsProperties);
		if(pluginId != null && pluginId.length()>0)
			resourceInfo.setPluginId(pluginId);
		log.info("::::::::::::::::: ADD RESOURCE :::::::::::::::::::");
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
