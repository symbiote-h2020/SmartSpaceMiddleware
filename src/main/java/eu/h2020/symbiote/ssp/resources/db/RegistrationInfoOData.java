/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import eu.h2020.symbiote.ssp.rap.odata.CustomField;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.semanticweb.owlapi.model.IRI;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Document(collection="registrationInfoOData")
public class RegistrationInfoOData {

	@JsonProperty("parameters")
	private Set<ParameterInfo> parameters = new HashSet<>();

	@Id
	private String id;

	@JsonProperty("symbioteId")
	private String symbioteId;

	@JsonProperty("sspId")
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private String sspId;

	@JsonProperty("className")
	private String className;

	@JsonProperty("superClass")
	private String superClass;

	@Field
	@Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Date session_expiration;

	public RegistrationInfoOData() {
		this.symbioteId = null;
		this.className = null;
		this.superClass = null;
		this.parameters = null;
		this.session_expiration=null;
	}

	public RegistrationInfoOData(String symbioteId, String className, String superClass, Set parameters) {
		this.symbioteId = symbioteId;
		this.className = className;
		this.superClass = superClass;
		this.parameters = parameters;
	}

	public RegistrationInfoOData(String sspId, String symbioteId, String className, String superClass, Set parameters) {
		this.id=sspId;
		this.sspId = sspId;
		this.symbioteId = symbioteId;
		this.className = className;
		this.superClass = superClass;
		this.parameters = parameters;
	}

	public RegistrationInfoOData(String sspId, String symbioteId, String className, String superClass, Set parameters, Date session_expiration) {
		this.id=sspId;
		this.sspId = sspId;
		this.symbioteId = symbioteId;
		this.className = className;
		this.superClass = superClass;
		this.parameters = parameters;
		this.session_expiration=session_expiration;
	}


	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getSymbioteId() {
		return symbioteId;
	}

	public void setSymbioteId(String symbioteId) {
		this.symbioteId = symbioteId;
	}

	public String getSspId() {
		return sspId;
	}

	public void setSspId(String sspId) {
		this.sspId = sspId;
	}



	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getSuperClass() {
		return superClass;
	}

	public void setSuperClass(String superClass) {
		this.superClass = superClass;
	}

	public Set getParameters() {
		return parameters;
	}

	public void setParameters(Set parameters) {
		this.parameters = parameters;
	}
	
	public Date getSessionExpiration() {
		return this.session_expiration;
	}

	public void setSessionExpiration(Date session_expiration) {
		this.session_expiration = session_expiration;
	}


	/*public List<CustomField> getParametersComplete(){
        if(this.parameters == null || this.parameters.isEmpty())
            return null;
        List<CustomField> customFieldList = new ArrayList<CustomField>();
        String[] parametersList = this.parameters.split(",");
        for (String param : parametersList) {
            String[] nameType = param.split(":");
            if(nameType != null && nameType.length == 2){
                String name = nameType[0];
                String type = nameType[1];
                CustomField cf = new CustomField(type, name);
                customFieldList.add(cf);
            }
        }
        return customFieldList;
    }*/

	public List<CustomField> getParametersComplete(){
		if(this.parameters == null || this.parameters.isEmpty())
			return null;
		List<CustomField> customFieldList = new ArrayList<CustomField>();
		for(ParameterInfo pi: this.parameters){
			String name = pi.getName();
			String type = getShortClassName(pi.getType());
			CustomField cf = new CustomField(type, name);
			customFieldList.add(cf);
		}
		return customFieldList;
	}

	public Boolean containsParameter(String parameter){
		return true;
	}


	private String getShortClassName(String type) {
		String simpleName = type.replace("[]", "");
		if (!CustomField.typeIsPrimitive(simpleName)) {
			IRI iri = IRI.create(simpleName);
			simpleName = iri.getShortForm();
			if (simpleName.contains("#")) {
				String[] array = simpleName.split("#");
				simpleName = array[array.length - 1];
			}
		}
		return simpleName;
	}
}
