/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 *
 * @author Fabrizio Giuliano <fabrizio.giuliano@cnit.it>
 */
@Document(collection="sessions")
public class SessionInfo {

    //@JsonProperty("sessionId")
    @Id
    @JsonProperty("sessionId")
    private String id;
    @Field
    @Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
    private Date session_expiration;
    
    @JsonProperty("symbioteId")
    private String symbioteId;
    
    /* HOWTO read expiration time directly via mongoDB client
      db.sessions.aggregate(     
      	{ $project: {         
      		session_expiration: 1,         
      		ttlMillis: {             
      			$subtract: [ new Date(), "$session_expiration"]         
      		}     
      	  }
     	} )
     * */

    
    @JsonCreator
    public SessionInfo( @JsonProperty("sessionId") String id,
    					@JsonProperty("symbioteId") String symbioteId,
                        @JsonProperty("session_expiration") Date session_expiration) {
        this.id = id;
        this.symbioteId = symbioteId;
        this.session_expiration = session_expiration;
        
    }
    
    @JsonProperty("sessionId")
    public String getSessionId() {
        return id;
    }
    
    @JsonProperty("symbioteId")
    public String getSymbioteId() {
        return symbioteId;
    }
    
    @JsonProperty("session_expiration")
    public void setSessionExpiration(Date session_expiration) {
        this.session_expiration = session_expiration;
    }
    
    public Date getSessionExpiration() {
    	return this.session_expiration;
    }
    
    
   
}
