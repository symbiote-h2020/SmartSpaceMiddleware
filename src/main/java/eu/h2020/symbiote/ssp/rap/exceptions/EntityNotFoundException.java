/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class EntityNotFoundException extends RuntimeException {
    
    private String symbioteId;
    public EntityNotFoundException(String entityId) {
        super ("Could not find object with id " + entityId);
        this.symbioteId = entityId;
    }
    
    public String getSymbioteId(){
        return symbioteId;
    }
}