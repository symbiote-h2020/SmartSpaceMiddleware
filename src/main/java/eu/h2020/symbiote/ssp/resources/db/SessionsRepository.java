/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Fabrizio Giuliano <fabrizio.giuliano@cnit.it>
 */
@Repository
public interface SessionsRepository extends MongoRepository<SessionInfo, String> {
    
    
    //public Optional<SessionInfo> findById(String cookie);
	public SessionInfo findBySessionId(String sessionId);
	public SessionInfo findBySspId(String sspId);
	public SessionInfo findBySymId(String symId);
	public List<SessionInfo> findByPluginId(String pluginId);
	public List<SessionInfo> findByPluginURL(String pluginURL);
	public List<SessionInfo> findByDk1(String dk1);
	public List<SessionInfo> findByMacaddress(String macaddress);
	//public SessionInfo findBySymbioteId(String symbioteId);
    
}
