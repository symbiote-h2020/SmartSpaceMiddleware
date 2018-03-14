package eu.h2020.symbiote.ssp.resources;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.model.cim.Resource;
import eu.h2020.symbiote.security.accesspolicies.common.IAccessPolicySpecifier;


public class SspSDEVInfo {
    @org.springframework.data.annotation.Id
    @JsonProperty("symIdSDEV")
    private String symIdSDEV;
    @JsonProperty("internalIdSDEV")
    private String internalIdSDEV;
    @JsonProperty("pluginId")
    private String pluginId;
    @JsonProperty("pluginURL")
    private String pluginURL;
    @JsonProperty("dk1")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String derivedKey1;
    @JsonProperty("hashField")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String hashField;


    public SspSDEVInfo() {
    }

    public String getInternalIdSDEV() {
        return this.internalIdSDEV;
    }
    
    public void setInternalIdSDEV(String internalIdSDEV) {
        this.internalIdSDEV = internalIdSDEV;
    }
    
    public String getSymIdSDEV() {
        return this.symIdSDEV;
    }
    
    public void setSymIdSDEV(String symIdSDEV) {
        this.symIdSDEV = symIdSDEV;
    }

    public String getPluginId() {
        return this.pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getPluginUrl() { return pluginURL; }

    public void setPluginUrl(String pluginUrl) { this.pluginURL = pluginURL; }

    public String getDerivedKey1() { return derivedKey1; }

    public void setDerivedKey1(String derivedKey1) { this.derivedKey1 = derivedKey1; }

    public String getHashField() { return hashField; }

    public void setHashField(String hashField) { this.hashField = hashField; }

    
}
