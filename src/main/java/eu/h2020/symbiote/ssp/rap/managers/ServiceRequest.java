/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.rap.managers;

import org.springframework.http.HttpHeaders;


/**
 *
 * @author mpard
 */
public class ServiceRequest {
    
    private HttpHeaders serviceRequestHeaders;
    private boolean createdSuccessfully;

    public ServiceRequest() {
    }

    public ServiceRequest(HttpHeaders serviceHeaders, boolean createdSuccessfully) {
        setServiceRequestHeaders(serviceHeaders);
        setCreatedSuccessfully(createdSuccessfully);
    }

    public HttpHeaders getServiceRequestHeaders() { return serviceRequestHeaders; }
    public void setServiceRequestHeaders(HttpHeaders serviceRequestHeaders) { this.serviceRequestHeaders = serviceRequestHeaders; }

    public boolean isCreatedSuccessfully() { return createdSuccessfully; }
    public void setCreatedSuccessfully(boolean createdSuccessfully) { this.createdSuccessfully = createdSuccessfully; }
}
