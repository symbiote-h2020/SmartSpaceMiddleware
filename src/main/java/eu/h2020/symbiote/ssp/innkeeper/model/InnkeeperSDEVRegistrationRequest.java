package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;


@Service
public class InnkeeperSDEVRegistrationRequest {

	@Autowired
	SessionRepository sessionRepository;
	public InnkeeperSDEVRegistrationResponse registry(SspSDEVInfo sspSDEVInfo) {
		InnkeeperSDEVRegistrationResponse regResponse = null;

		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		String symIdFromCore = this.checkCoreSymbioteIdRegistration(sspSDEVInfo.getSymIdSDEV());
		switch (symIdFromCore) {
		case InnkeeperRestControllerConstants.OFFLINE_SYMID:
			regResponse= new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymIdSDEV(),this.createInternalId(),InnkeeperRestControllerConstants.SDEV_REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);
			break;
		case InnkeeperRestControllerConstants.REJECTED_SYMID:	

			regResponse= new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymIdSDEV(),this.createInternalId(),InnkeeperRestControllerConstants.SDEV_REGISTRATION_REJECTED,0);
			break;
		default:
			if (symIdFromCore.equals(sspSDEVInfo.getSymIdSDEV())) {			
				String internalIdSDEV = sessionRepository.findBySymIdSDEV(symIdFromCore).getInternalIdSDEV();
				regResponse= new InnkeeperSDEVRegistrationResponse(
						symIdFromCore,internalIdSDEV,InnkeeperRestControllerConstants.SDEV_REGISTRATION_ALREADY_REGISTERED,0);
			}else {
				regResponse= new InnkeeperSDEVRegistrationResponse(
						symIdFromCore,this.createInternalId(),InnkeeperRestControllerConstants.SDEV_REGISTRATION_OK,DbConstants.EXPIRATION_TIME);
			}
			
			break;
		}
		return regResponse;



	}
	private String createInternalId() {
		// TODO Auto-generated method stub

		List<SessionInfo> ss= sessionRepository.findAll();
		List<String> sessionIdList = new ArrayList<String>();
		for (SessionInfo s : ss)
			sessionIdList.add(s.getInternalIdSDEV() );
		int i=0;
		String myInternalId = Integer.toString(i);

		while (true) {
			myInternalId = Integer.toString(i);
			if (!sessionIdList.contains(myInternalId)) {
				break;
			}
			i++;
		}
		return myInternalId;

	}

	private boolean isSSPOnline() {
		boolean isOnline=true;
		return isOnline;
	}
	private String checkCoreSymbioteIdRegistration(String symIdSDEV) {

		if (!this.isSSPOnline())
			return InnkeeperRestControllerConstants.OFFLINE_SYMID;

		try {
			boolean smyIdSessionRepositoryExists=false;
			//Mock function

			//if I register an SDEV for first time and symIdSDEV is null
			String symIdSDEV_ret="";
			if (symIdSDEV.equals("")) {
				//Check MAC address of SDEV TODO:
				int symIdMock = new Random().nextInt((1000 - 1) + 1) + 1;
				symIdSDEV_ret="sym"+symIdMock;
				smyIdSessionRepositoryExists=true;
			}else {
				//search in the sessionRepository if  symId Exists							
				smyIdSessionRepositoryExists = sessionRepository.findBySymIdSDEV(symIdSDEV) != null;
				symIdSDEV_ret=symIdSDEV;
			}
			//DEBUG: search if symId Exists in the Core ALWAYS TRUE FOR THE MOMENT
			boolean smyIdCoreExists=true;

			if (smyIdSessionRepositoryExists && smyIdCoreExists) {
				return symIdSDEV_ret;
			}else {
				return InnkeeperRestControllerConstants.REJECTED_SYMID;
			}
		}catch(Exception e) {			
			System.err.println(e);
			return InnkeeperRestControllerConstants.REJECTED_SYMID;
		}
	}
}
