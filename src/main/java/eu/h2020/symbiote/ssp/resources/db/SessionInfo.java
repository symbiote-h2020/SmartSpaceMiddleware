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
    private String id;
    @JsonProperty("cookie")
    private String cookie;
    @Field
    @Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
    private Date session_expiration;
    
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

    public SessionInfo() {
        this.id = "";
        this.cookie=null;
    }
    
    @JsonCreator
    public SessionInfo( @JsonProperty("cookie") String cookie,
                        @JsonProperty("session_expiration") Date session_expiration) {
        this.cookie = cookie;
        this.session_expiration = session_expiration;
        
    }
    
    @JsonProperty("sessionId")
    public String getSessionId() {
        return id;
    }
    
    @JsonProperty("cookie")
    public void getCookie(String cookie) {
        this.cookie = cookie;
    }
    @JsonProperty("session_expiration")
    public void setSessionExpiration(Date session_expiration) {
        this.session_expiration = session_expiration;
    }
    
    
   
}
