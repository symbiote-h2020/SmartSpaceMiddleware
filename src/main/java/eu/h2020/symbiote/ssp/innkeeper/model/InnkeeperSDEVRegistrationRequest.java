package eu.h2020.symbiote.ssp.innkeeper.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;
import eu.h2020.symbiote.ssp.utils.CheckCoreUtility;
import eu.h2020.symbiote.ssp.utils.SspIdUtils;


@Service
public class InnkeeperSDEVRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperSDEVRegistrationRequest.class);

	
	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;
	@Autowired
	SessionsRepository sessionsRepository;

	public void setIsCoreOnline(Boolean v) {
		this.isCoreOnline=v;
	}

	public Boolean isCoreOnline() {
		return this.isCoreOnline;
	}

	public Boolean checkRegistrationInjection(SspSDEVInfo sspSDEVInfo) {
		List<SessionInfo> plgIdLst = sessionsRepository.findByPluginId(sspSDEVInfo.getPluginId());
		List<SessionInfo> plgURLLst = sessionsRepository.findByPluginURL(sspSDEVInfo.getPluginURL());
		List<SessionInfo> dk1Lst = sessionsRepository.findByDk1(sspSDEVInfo.getDerivedKey1());
		return plgIdLst.size()!=0 && plgURLLst.size()!=0 && dk1Lst.size()!=0; 
	}

	public InnkeeperSDEVRegistrationResponse registry(SspSDEVInfo sspSDEVInfo) throws JsonProcessingException {
		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		String symIdFromCore = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(sspSDEVInfo.getSymId());

		
		// a null SymId from core == REJECT THE REQUEST
		if (symIdFromCore==null) { 
			return new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymId(),sspSDEVInfo.getSspId(),InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	
		}

		//EMPTY smyId from core == OFFLINE
		if (symIdFromCore.equals("")) { 

			//  check if sspId exists
			SessionInfo sInfo= sessionsRepository.findBySspId(sspSDEVInfo.getSspId());
			if (sInfo!=null) {
				String sspId = sInfo.getSspId();
				return new InnkeeperSDEVRegistrationResponse(
						"",sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
			}
			
			// registration injection workaround: check sspSDEVInfo.getSspId() value, multiple registration for the same SDEV
			if (checkRegistrationInjection(sspSDEVInfo)) {
				// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
				log.info("REGISTRATION REJECTED AS SUSPECT DUPLICATED SDEVOInfo="+new ObjectMapper().writeValueAsString(sspSDEVInfo));
				return new InnkeeperSDEVRegistrationResponse(
						sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
			}
			
			//No duplicate registration, go ahead
			return new InnkeeperSDEVRegistrationResponse(
					sspSDEVInfo.getSymId(),new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);
		}

		// if symIdFromCore == SDEVInfo.symId -> Already registered
		if (symIdFromCore.equals(sspSDEVInfo.getSymId())) { 	

			String sspId = sessionsRepository.findBySymId(symIdFromCore).getSspId();
			return new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
		} 
		
		//NEW REGISTRATION
		if ( sspSDEVInfo.getSymId().equals("")) {
			if (checkRegistrationInjection(sspSDEVInfo)) {
				// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
				return new InnkeeperSDEVRegistrationResponse(
						sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
			} 
			//No duplicate registration, go ahead
			return	 new InnkeeperSDEVRegistrationResponse(
					symIdFromCore,new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OK,DbConstants.EXPIRATION_TIME);
		}
		System.out.println("SDEV REGISTARTION DEFAULT REJECTED");
		return new InnkeeperSDEVRegistrationResponse(
				sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	

	}

}
