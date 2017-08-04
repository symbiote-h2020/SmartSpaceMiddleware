/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.resources;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@Document(collection="plugins")
public class PlatformInfo {
    @Id
    private final String id;
    private final String url;
       
    public PlatformInfo(String platformId, String url) {
        this.id = platformId;
        this.url = url;
    }
    
    public String getPlatformId() {
        return id;
    }
    
    public String getUrl() {
        return url;
    }
}
