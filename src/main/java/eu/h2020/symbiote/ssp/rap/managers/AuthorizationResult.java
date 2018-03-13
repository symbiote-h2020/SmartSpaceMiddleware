/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.managers;

/**
 *
 * @author Matteo Pardi <m.pardi@nextworks.it>
 */
public class AuthorizationResult {

    private String message;
    private boolean validated;

    public AuthorizationResult(String message, boolean validated) {
        this.message = message;
        this.validated = validated;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public boolean isValidated() { return validated; }
    public void setValidated(boolean validated) { this.validated = validated; }
}
