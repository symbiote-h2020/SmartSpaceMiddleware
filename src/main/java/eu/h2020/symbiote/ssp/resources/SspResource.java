package eu.h2020.symbiote.ssp.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;


public class SspResource {
    @org.springframework.data.annotation.Id
    
    @JsonProperty("internalIdResource")
    private String internalIdResource; //resource Internal Id
    
    @JsonProperty("internalId")
    private String internalId; // Internal Id
    
    @JsonProperty("symId") 
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String symId;
    
    @JsonProperty("symIdResource")
    private String symIdResource;
    
    @JsonProperty("accessPolicy")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IAccessPolicySpecifier accessPolicy;
    @JsonProperty("filteringPolicy")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IAccessPolicySpecifier filteringPolicy;
    @JsonProperty("semanticDescription")
    private Resource resource;


    public SspResource() {
    }

    public String getInternalIdResource() {
        return this.internalIdResource;
    }

    public void setInternalIdResource(String internalIdResource) {
        this.internalIdResource = internalIdResource;
    }
    
    
    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getSymId() {
        return this.symId;
    }

    public void setSymIdDEV(String symId) {
        this.symId = symId;
    }

    public String getSymIdResource() {
        return this.symIdResource;
    }

    public void setSymIdResource(String symIdResource) {
        this.symIdResource = symIdResource;
    }

    public IAccessPolicySpecifier getAccessPolicy() {
        return this.accessPolicy;
    }

    public void setAccessPolicy(IAccessPolicySpecifier accessPolicySpecifier) {
        this.accessPolicy = accessPolicySpecifier;
    }

    public IAccessPolicySpecifier getFilteringPolicy() {
        return this.filteringPolicy;
    }

    public void setFilteringPolicy(IAccessPolicySpecifier filteringPolicySpecifier) {
        this.filteringPolicy = filteringPolicySpecifier;
    }

    public Resource getSemanticDescription() {
        return this.resource;
    }

    public void setSemanticDesciption(Resource resource) {
        this.resource = resource;
    }

	public String getPluginId() {
		// TODO Auto-generated method stub
		return null;
	}
}
