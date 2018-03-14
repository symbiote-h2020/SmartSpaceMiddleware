package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.Random;

import org.json.JSONObject;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;


@Service
public class InnkeeperSDEVRegistrationRequest {
	public InnkeeperSDEVRegistrationResponse registry(SspSDEVInfo sspSDEVInfo) {
		InnkeeperSDEVRegistrationResponse regResponse = null;
		
		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		String symIdFromCore = this.checkCoreSymbioteIdRegistration(sspSDEVInfo.getSymIdSDEV());
		String internalIdSDEV = this.createInternalId();
		if (symIdFromCore !=null) {
			//Search SymId in session DB
			// SymbioteID exists, and the core has validated it
			if (symIdFromCore.equals(sspSDEVInfo.getSymIdSDEV())) {				
				return new InnkeeperSDEVRegistrationResponse(
						symIdFromCore,internalIdSDEV,InnkeeperRestControllerConstants.SDEV_REGISTRATION_ALREADY_REGISTERED,0);
			}else {	
				return new InnkeeperSDEVRegistrationResponse(
						symIdFromCore,internalIdSDEV,InnkeeperRestControllerConstants.SDEV_REGISTRATION_OK,DbConstants.EXPIRATION_TIME);
			}				
		}else {
			
			return new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,internalIdSDEV,InnkeeperRestControllerConstants.SDEV_REGISTRATION_CLOUD_REJECTED,0);			
		}
		
	}
	private String createInternalId() {
		// TODO Auto-generated method stub
		return null;
	}
	private String checkCoreSymbioteIdRegistration(String symIdSDEV) {
		boolean isOffline=true;
		if (isOffline)
			return "OFFLINE";
		try {
			//Mock function
			if (symIdSDEV.equals("")) {
				//Check MAC address of SDEV
				int symIdMock = new Random().nextInt((1000 - 1) + 1) + 1;
			    return "sym"+symIdMock;
			}
			//TODO: search in the core if symID
			boolean symIDisValid=true;
			if (symIDisValid) {
				return symIdSDEV;
			}
		}catch(Exception e) {
			return null;
		}
		return null;
	}
}
