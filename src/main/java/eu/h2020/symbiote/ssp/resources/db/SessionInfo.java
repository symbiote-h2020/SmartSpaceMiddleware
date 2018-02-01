/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Fabrizio Giuliano <fabrizio.giuliano@cnit.it>
 */
@Document(collection="sessions")
public class SessionInfo {
    
    @Id
    @JsonProperty("sessionId")
    private String id;
    @JsonProperty("cookie")
    private String cookie;
    @JsonProperty("session_expiration")
    private String session_expiration;

    public SessionInfo() {
        this.id = "";
        this.cookie=null;
    }
    
    @JsonCreator
    public SessionInfo(@JsonProperty("sessionId") String sessionId, 
                        @JsonProperty("cookie") String cookie,
                        @JsonProperty("cookie") String session_expiration) {
        this.id = sessionId;
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
    public void setSessionExpiration(String session_expiration) {
        this.session_expiration = session_expiration;
    }
    
    
   
}
