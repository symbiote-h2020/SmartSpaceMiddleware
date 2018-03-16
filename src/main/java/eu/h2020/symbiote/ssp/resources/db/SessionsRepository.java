/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 *
 * @author Fabrizio Giuliano <fabrizio.giuliano@cnit.it>
 */
@Repository
public interface SessionsRepository extends MongoRepository<SessionInfo, String> {
    
    
    //public Optional<SessionInfo> findById(String cookie);
	public SessionInfo findBySessionId(String sessionId);
	public SessionInfo findBySymId(String symId);
	//public SessionInfo findBySymbioteId(String symbioteId);
    
}
