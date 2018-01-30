/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="policies")
public class AccessPolicy {
    @Id
    private final String id;    
    private final String internalId;
    private final IAccessPolicy policy;
    
    public AccessPolicy() {
        id = "";
        internalId = "";
        policy = null;
    }
    
    public AccessPolicy(String resourceId, String internalId, IAccessPolicy policy) {
        this.id = resourceId;
        this.internalId = internalId;
        this.policy = policy;
    }
    
    public String getResourceId() {
        return id;
    }
    
    public String getInternalId() {
        return internalId;
    }
    
    public IAccessPolicy getPolicy() {
        return policy;
    }        
}
