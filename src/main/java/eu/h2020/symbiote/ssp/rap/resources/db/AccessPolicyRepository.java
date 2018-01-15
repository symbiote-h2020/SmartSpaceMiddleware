/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources.db;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@Repository
public interface AccessPolicyRepository extends MongoRepository<AccessPolicy, String> {
    /**
     * This method will find an AccessPolicy in the database 
     * by its resource id.
     * 
     * @param resourceId    the id of the resource
     * @return              the AccessPolicy instances
     */
    public Optional<AccessPolicy> findById(String resourceId);
    
    /**
     * This method will find an AccessPolicy in the database 
     * by its internal id.
     * 
     * @param internalId    the internal id of the resource
     * @return              the AccessPolicy instances
     */
    public Optional<AccessPolicy> findByInternalId(String internalId);
}
