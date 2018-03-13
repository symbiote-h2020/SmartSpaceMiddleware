/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */

@Repository
public interface PluginRepository extends MongoRepository<PluginInfo, String> {
    /**
     * This method will find a Plugin in the database 
     * by its platformId.
     * 
     * @param pluginId    the id of the platform
     * @return              the Resource instances
     */
    public Optional<PluginInfo> findById(String pluginId);
}
