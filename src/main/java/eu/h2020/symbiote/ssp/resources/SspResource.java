package eu.h2020.symbiote.ssp.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;


public class SspResource {
    @org.springframework.data.annotation.Id
    
    @JsonProperty("internalId")
    private String internalId; //resource Internal Id
    
    @JsonProperty("symIdSDEV") 
    private String symIdSDEV;
    
    @JsonProperty("symIdResource")
    private String symIdResource;
    
    @JsonProperty("accessPolicy")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IAccessPolicySpecifier accessPolicy;
    @JsonProperty("filteringPolicy")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private IAccessPolicySpecifier filteringPolicy;
    @JsonProperty("resource")
    Resource resource;


    public SspResource() {
    }

    public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getSymIdDEV() {
        return this.symIdSDEV;
    }

    public void setSymIdDEV(String symIdSDEV) {
        this.symIdSDEV = symIdSDEV;
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

    public Resource getResource() {
        return this.resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }
}
