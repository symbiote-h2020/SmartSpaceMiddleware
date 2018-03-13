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
public final class ServiceResponseResult {
    private String serviceResponse;
    private boolean createdSuccessfully;

    public ServiceResponseResult() {
    }

    public ServiceResponseResult(String serviceResponse, boolean createdSuccessfully) {
        setServiceResponse(serviceResponse);
        setCreatedSuccessfully(createdSuccessfully);
    }

    public String getServiceResponse() { return serviceResponse; }
    public void setServiceResponse(String serviceResponse) { this.serviceResponse = serviceResponse; }

    public boolean isCreatedSuccessfully() { return createdSuccessfully; }
    public void setCreatedSuccessfully(boolean createdSuccessfully) { this.createdSuccessfully = createdSuccessfully; }
}
