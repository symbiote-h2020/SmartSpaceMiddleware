package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.List;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonProperty;

import eu.h2020.symbiote.cloud.model.internal.CloudResource;
import eu.h2020.symbiote.model.cim.MobileSensor;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.model.cim.StationarySensor;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.rap.odata.OwlApiHelper;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;

public class InnkSDEVRegistration {

	private static Log log = LogFactory.getLog(InnkSDEVRegistration.class);
    @Autowired
    ResourcesRepository resourcesRepository;

    @Autowired
    AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    OwlApiHelper owlApiHelp;
	
	@JsonProperty("symId") 					private String symId;
	@JsonProperty("dk1") 					private String dk1;
	@JsonProperty("hashField") 				private String hashField;
	@JsonProperty("semanticDescription") 	private List<CloudResource> semanticDescription;

	@JsonProperty("connectedTo") 			private String connectedTo;
	@JsonProperty("available") 				private boolean available;
	@JsonProperty("agentType") 				private String agentType;

	public InnkSDEVRegistration() {
		this.symId=null;
		this.dk1=null;
		this.hashField=null;
		this.semanticDescription=null;

		this.connectedTo=null;
		this.available=false;
		this.agentType = LwspConstants.SDEV;
	}
	public String getSymId() {
		return this.symId;
	}
	public String getdk1() {
		return this.dk1;
	}

	public String getHashField() {
		return this.hashField;
	}
	public List<CloudResource> getSemanticDescription() {
		return this.semanticDescription;
	}

	public String getConnectedTo() {
		return this.connectedTo;
	}

	public void setConnectedTo(String connectedTo) {
		this.connectedTo=connectedTo;
	}

	public boolean getAvailable() {
		return this.available;
	}

	public String getAgentType() {
		return this.agentType;
	}
	public void registry() {
		try {

			List<CloudResource> msgs = this.getSemanticDescription();
			for(CloudResource msg: msgs){
				String internalId = msg.getInternalId();
				Resource resource = msg.getResource();
				String pluginId = msg.getPluginId();
				String symbioteId = resource.getId();
				List<String> props = null;
				if(resource instanceof StationarySensor) {
					props = ((StationarySensor)resource).getObservesProperty();
				} else if(resource instanceof MobileSensor) {
					props = ((MobileSensor)resource).getObservesProperty();
				}
				log.debug("Updating resource with symbioteId: " + symbioteId + ", internalId: " + internalId);

				addPolicy(symbioteId, internalId, msg.getSingleTokenAccessPolicy());
				addResource(symbioteId, internalId, props, pluginId);
			}
			addCloudResourceInfoForOData(msgs);
		} catch (Exception e) {
			log.error("Error during registration process\n" + e.getMessage());
		}
	}
	
	
	private void addCloudResourceInfoForOData(List<CloudResource> cloudResourceList) {
        try{
            owlApiHelp.addCloudResourceList(cloudResourceList);
        }
        catch(Exception e){
            log.error("Error add info registration for OData\n"+e.getMessage());
        }
    }
	private void addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, platformResourceId);
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginId(pluginId);

        resourcesRepository.save(resourceInfo);

        log.debug("Resource " + resourceId + " registered");
    }

    private void deleteResource(String internalId) {
        try {
            List<ResourceInfo> resourceList = resourcesRepository.findByInternalId(internalId);
            if(resourceList != null && !resourceList.isEmpty()) {
                resourcesRepository.delete(resourceList.get(0).getSymbioteId());
                log.info("Resource " + internalId + " unregistered");
            } else {
                log.error("Resource " + internalId + " not found");
            }
        } catch (Exception e) {
            log.error("Resource with id " + internalId + " not found - Exception: " + e.getMessage());
        }
    }

    private void addPolicy(String resourceId, String internalId, SingleTokenAccessPolicySpecifier accPolicy) throws InvalidArgumentsException {
        try {
            IAccessPolicy policy = SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(accPolicy);
            AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
            accessPolicyRepository.save(ap);

            log.info("Policy successfully added for resource " + resourceId);
        } catch (InvalidArgumentsException e) {
            log.error("Invalid Policy definition for resource with id " + resourceId);
            throw e;
        }
    }

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


}
