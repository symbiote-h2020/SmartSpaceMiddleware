package eu.h2020.symbiote.ssp.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;


public class SspResource {
    @org.springframework.data.annotation.Id
    @JsonProperty("internalId")
    private String internalId;
    @JsonProperty("pluginId")
    private String pluginId;
    @JsonProperty("pluginUrl")
    private String pluginUrl;
    @JsonProperty("dk1")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String derivedKey1;
    @JsonProperty("hashField")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hashField;
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

    public String getPluginId() {
        return this.pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginUrl() { return pluginUrl; }

    public void setPluginUrl(String pluginUrl) { this.pluginUrl = pluginUrl; }

    public String getDerivedKey1() { return derivedKey1; }

    public void setDerivedKey1(String derivedKey1) { this.derivedKey1 = derivedKey1; }

    public String getHashField() { return hashField; }

    public void setHashField(String hashField) { this.hashField = hashField; }

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
