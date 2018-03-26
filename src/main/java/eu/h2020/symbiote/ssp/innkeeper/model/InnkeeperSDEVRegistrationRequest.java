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
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;
import eu.h2020.symbiote.ssp.utils.InternalIdUtils;


@Service
public class InnkeeperSDEVRegistrationRequest {

	@Autowired
	SessionsRepository sessionsRepository;
	public InnkeeperSDEVRegistrationResponse registry(SspSDEVInfo sspSDEVInfo) {
		InnkeeperSDEVRegistrationResponse regResponse = null;

		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		String symIdFromCore = new CheckCoreUtility(sessionsRepository).checkCoreSymbioteIdRegistration(sspSDEVInfo.getSymId());

		if (symIdFromCore==null) { // a null SymId from core == REJECT THE REQUEST
			regResponse= new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymId(),sspSDEVInfo.getInternalId(),InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	
			
		}else if (symIdFromCore.equals("")) { //EMPTY smyId from core == OFFLINE
			regResponse= new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymId(),new InternalIdUtils(sessionsRepository).createInternalId(),InnkeeperRestControllerConstants.REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);

		} else if (symIdFromCore.equals(sspSDEVInfo.getSymId())) { 	
			String internalId = sessionsRepository.findBySymId(symIdFromCore).getInternalId();
			regResponse= new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,internalId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
		} else {
			regResponse= new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,new InternalIdUtils(sessionsRepository).createInternalId(),InnkeeperRestControllerConstants.REGISTRATION_OK,DbConstants.EXPIRATION_TIME);
		}

		return regResponse;



	}
	
}
