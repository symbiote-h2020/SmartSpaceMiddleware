/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.Id;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
public class ParameterInfo {
    
    @JsonIgnore
    private RegistrationInfoOData registrationInfoOData;
    
    @JsonProperty("type")
    private String type;
    
    @JsonProperty("name")
    private String name;
    
    @JsonProperty(value = "mandatory")
    private boolean mandatory;

    @Id
    private int id;

    ParameterInfo() { // jpa only
    }

    
    public ParameterInfo(String type, String name) {
        this.type = type;
        this.name = name;
    }
    
    /**
     *
     * @param type
     * @param name
     * @param mandatory
     */
    public ParameterInfo(String type, String name, boolean mandatory) {
        this.type = type;
        this.name = name;
        this.mandatory = mandatory;
    }

    public RegistrationInfoOData getRegistrationInfoOData() {
        return registrationInfoOData;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }
    
    

    @Override
    public boolean equals(Object obj) {
        if(!obj.getClass().equals(ParameterInfo.class))
            return false;
        ParameterInfo pi = (ParameterInfo)obj;
        return this.name.equals(pi.getName()) && this.type.equals(pi.getType());
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() + this.type.hashCode();
    }
    
    
}
