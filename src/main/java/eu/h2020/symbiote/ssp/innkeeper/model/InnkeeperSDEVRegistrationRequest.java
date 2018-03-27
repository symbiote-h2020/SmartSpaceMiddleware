package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import eu.h2020.symbiote.ssp.utils.SspIdUtils;


@Service
public class InnkeeperSDEVRegistrationRequest {

	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;
	@Autowired
	SessionsRepository sessionsRepository;

	public Boolean checkRegistrationInjection(SspSDEVInfo sspSDEVInfo) {
		List<SessionInfo> plgIdLst = sessionsRepository.findByPluginId(sspSDEVInfo.getPluginId());
		List<SessionInfo> plgURLLst = sessionsRepository.findByPluginURL(sspSDEVInfo.getPluginURL());
		List<SessionInfo> dk1Lst = sessionsRepository.findByDk1(sspSDEVInfo.getDerivedKey1());
		return plgIdLst.size()!=0 && plgURLLst.size()!=0 && dk1Lst.size()!=0; 
	}

	public InnkeeperSDEVRegistrationResponse registry(SspSDEVInfo sspSDEVInfo) {
		InnkeeperSDEVRegistrationResponse regResponse = null;

		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		String symIdFromCore = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(sspSDEVInfo.getSymId());

		if (symIdFromCore==null) { // a null SymId from core == REJECT THE REQUEST
			regResponse= new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymId(),sspSDEVInfo.getSspId(),InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	

		}else if (symIdFromCore.equals("")) { //EMPTY smyId from core == OFFLINE
			
			//  check if sspId exists
			System.out.println("symIdFromCore="+symIdFromCore);
			SessionInfo sInfo= sessionsRepository.findBySspId(sspSDEVInfo.getSspId());
			
			if (sInfo!=null) {
				String sspId = sInfo.getSspId();
				regResponse= new InnkeeperSDEVRegistrationResponse(
						symIdFromCore,sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
			} else {
				// registration injection workaround: check sspSDEVInfo.getSspId() value, multiple registration for the same SDEV
				if (checkRegistrationInjection(sspSDEVInfo)) {
					// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
					regResponse= new InnkeeperSDEVRegistrationResponse(
							sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
				}else { 
					//No duplicate registration, go ahead
					regResponse= new InnkeeperSDEVRegistrationResponse(
							sspSDEVInfo.getSymId(),new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);
				}
			}

		} else if (symIdFromCore.equals(sspSDEVInfo.getSymId())) { 	
			String sspId = sessionsRepository.findBySymId(symIdFromCore).getSspId();
			regResponse= new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
		} else {
			if ( sspSDEVInfo.getSymId().equals("")) {

				if (checkRegistrationInjection(sspSDEVInfo)) {
					// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
					regResponse= new InnkeeperSDEVRegistrationResponse(
							sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
				}else { 
					//No duplicate registration, go ahead
					regResponse= new InnkeeperSDEVRegistrationResponse(
							sspSDEVInfo.getSymId(),new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);
				}

			}else {
				regResponse= new InnkeeperSDEVRegistrationResponse(
						sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	

			}
		}

		return regResponse;



	}

}
