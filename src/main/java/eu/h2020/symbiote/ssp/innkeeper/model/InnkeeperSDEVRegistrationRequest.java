package eu.h2020.symbiote.ssp.innkeeper.model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
import eu.h2020.symbiote.ssp.resources.SspSDEVInfo;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
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
	
	@Autowired
	ResourcesRepository resourcesRepository;

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

	public ResponseEntity<Object> SspRegistry(String msg) throws IOException {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;		

		Date currTime = new Timestamp(System.currentTimeMillis());
		String sessionId = new Lwsp().generateSessionId();
		SessionInfo s = new SessionInfo();

		SspSDEVInfo sspSDEVInfo =  new ObjectMapper().readValue(msg, SspSDEVInfo.class);

		InnkeeperSDEVRegistrationResponse respSDEV = registry(sspSDEVInfo);

		//DEBUG: MOCK
		switch (respSDEV.getResult()) {
		case InnkeeperRestControllerConstants.REGISTRATION_OFFLINE: //OFFLINE
		case InnkeeperRestControllerConstants.REGISTRATION_OK:					
			httpStatus=HttpStatus.OK;
			s.setsessionId(sessionId);
			s.setdk1(sspSDEVInfo.getDerivedKey1());
			s.setSspId(respSDEV.getSspId());
			s.setSymId(respSDEV.getSymId());						
			s.setPluginId(sspSDEVInfo.getPluginId());
			s.setPluginURL(sspSDEVInfo.getPluginURL());
			s.setSessionExpiration(currTime);				
			sessionsRepository.save(s);				
			break;


		case InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED:
			httpStatus=HttpStatus.OK;
			break;

		case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
		default:
			httpStatus=HttpStatus.BAD_REQUEST;
		}
		String response = new ObjectMapper().writeValueAsString(respSDEV);
		return new ResponseEntity<Object>(response,responseHeaders,httpStatus);
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
			
			// registration injection workaround: check sspSDEVInfo.getSspIdParent() value, multiple registration for the same SDEV
			if (checkRegistrationInjection(sspSDEVInfo)) {
				// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
				log.error("REGISTRATION REJECTED AS SUSPECT DUPLICATED SDEVOInfo="+new ObjectMapper().writeValueAsString(sspSDEVInfo));
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
		log.error("SDEV REGISTARTION DEFAULT REJECTED");
		return new InnkeeperSDEVRegistrationResponse(
				sspSDEVInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	

	}
	
	
	
	public ResponseEntity<Object> SspKeepAlive(String msg) throws IOException {

		//KEEP ALIVE ACTIONS:
		// 1. update Session Expiration
		// 2. check if SSP is online and update symbiote id for SDEV and its Resources (Currently Mock)

		Date currTime = new Timestamp(System.currentTimeMillis());

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(msg, SspSDEVInfo.class);

		SessionInfo s = null;

		if (sspSdevInfo.getSymId()==null || sspSdevInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspSdevInfo.getSspId());

		}else {
			// UPDATE USING SYMID
			s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
		}

		InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();

		if (s==null) {			
			log.error("ERROR1 - no session found");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);
		}
		if (	!s.getSspId().equals(sspSdevInfo.getSspId()) && 
				!s.getSymId().equals(sspSdevInfo.getSymId())){
			log.error("ERROR2 - no match Ids");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);

		}else {
			log.info("SSpId or SymId match");
		}

		if (	( !isCoreOnline && (s.getSspId()!="" && !s.getSspId().equals(sspSdevInfo.getSspId())) )
				) {
			log.error("ERROR3 - SSP online and SymId not match or SSP offline and SspId not match");
			//DEFAULT: ERROR
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response),
					responseHeaders,httpStatus);
		}else {
			log.info("SspId "+s.getSspId()+" Match");
		}

		String symIdSDEV=null;
		if (sspSdevInfo.getSymId()==null)
			symIdSDEV = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(""); //generate new symId from Core
		else
			symIdSDEV = new CheckCoreUtility(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(sspSdevInfo.getSymId());
		//if I found my symId/SspId of SDEV in the MongoDb session, update expiration time it

		//UPDATE Expiration time of session
		if (symIdSDEV!=null) {
			if (s.getSymId()==null) {
				s.setSymId(symIdSDEV); // update SymId
			}else if (s.getSymId().equals("")) {
				s.setSymId(symIdSDEV); // update SymId
			}
		}

		log.info("s.getSymIdParent()="+s.getSymId());
		s.setSessionExpiration(currTime);
		sessionsRepository.save(s);


		//UPDATE Expiration time of Resources

		//TODO: check also for Policies and ODATA?
		List<ResourceInfo> resList= resourcesRepository.findBySspIdParent(s.getSspId());
		List<Map<String, String>> updatedSymIdList = new ArrayList<Map<String,String>>();
		for (ResourceInfo r : resList) {

			String symIdRes = new CheckCoreUtility(resourcesRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(r.getSymIdResource());
			HashMap<String,String> symIdEntry = new HashMap<String,String>();

			if (symIdRes!=null && r.getSymIdResource().equals("")) 
				r.setSymIdResource(symIdRes); // update SymId of Resource

			symIdEntry.put("symIdResource", r.getSymIdResource());
			symIdEntry.put("sspIdResource", r.getSspIdResource());
			updatedSymIdList.add(symIdEntry);
			r.setSymIdParent(s.getSymId());
			r.setSessionExpiration(currTime);

			resourcesRepository.save(r);
		}
		response.setSymId(s.getSymId());
		response.setSspId(s.getSspId());

		if (isCoreOnline){
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);
			httpStatus=HttpStatus.OK;
		} else {
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OFFLINE);
			httpStatus=HttpStatus.OK;
		}

		response.setUpdatedSymId(updatedSymIdList);


		return new ResponseEntity<Object>(
				new ObjectMapper().writeValueAsString(response), 
				responseHeaders,httpStatus);

	}
	public ResponseEntity<Object> SspDelete(String msg) throws IOException {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspSDEVInfo sspSdevInfo = new ObjectMapper().readValue(msg, SspSDEVInfo.class);
		//Delete Session

		SessionInfo s = null;

		if (sspSdevInfo.getSymId()==null || sspSdevInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspSdevInfo.getSspId());

		}else {
			// REMOVE USING SYMID
			s = sessionsRepository.findBySymId(sspSdevInfo.getSymId());
		}

		InnkeeperSDEVRegistrationResponse response =new InnkeeperSDEVRegistrationResponse();

		//if I found my symId/SspId of SDEV in the MongoDb session, delete it
		if (s!=null) {

			//Delete session
			sessionsRepository.delete(s);

			//Delete Resources
			//TODO: check also for Policies and ODATA?
			List<ResourceInfo> resList= resourcesRepository.findBySspIdParent(s.getSspId());

			for (ResourceInfo r : resList) {
				resourcesRepository.delete(r);
			}

			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
			httpStatus=HttpStatus.OK;

			return new ResponseEntity<Object>(
					new ObjectMapper().writeValueAsString(response), 
					responseHeaders,httpStatus);
		}

		//DEFAULT: ERROR
		response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
		httpStatus=HttpStatus.BAD_REQUEST;
		return new ResponseEntity<Object>(
				new ObjectMapper().writeValueAsString(response),
				responseHeaders,httpStatus);

	}			

}
