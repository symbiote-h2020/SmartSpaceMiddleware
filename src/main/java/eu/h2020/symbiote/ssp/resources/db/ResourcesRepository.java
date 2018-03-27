/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import java.util.Optional;
import java.util.List;
import org.springframework.stereotype.Repository;

import eu.h2020.symbiote.model.cim.Resource;

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
    public Optional<ResourceInfo> findById(String id);
    public Optional<ResourceInfo> findBySymId(String symId);
    
    /**
     * This method will find (a) Resource instance(s) in the database by 
     * its(their) internalId.
     * 
     * @param internalId            the id of the resource in the platform
     * @return                      the Resource instance(s)
     */
    public List<ResourceInfo> findBySspId(String sspId);
    public List<ResourceInfo> findByInternalIdResource(String internalIdResource);
    
    @Override
    public List<ResourceInfo> findAll();
    
    public List<ResourceInfo> findBySymIdLike(String id);
    
    public List<ResourceInfo> findByPluginId(String pluginId);
    
    
    
}
