/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;

import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Repository
public interface ResourcesRepository extends MongoRepository<ResourceInfo, String> {
    
    /**
     * This method will find a Resource instance in the database by 
     * its resourceId.
     * 
     * @param resourceId    the id of the resource
     * @return              the Resource instance
     */
    public Optional<ResourceInfo> findById(String resourceId);
    
    /**
     * This method will find (a) Resource instance(s) in the database by 
     * its(their) internalId.
     * 
     * @param internalId            the id of the resource in the platform
     * @return                      the Resource instance(s)
     */
    public List<ResourceInfo> findByInternalId(String internalId);
    
    @Override
    public List<ResourceInfo> findAll();
}
