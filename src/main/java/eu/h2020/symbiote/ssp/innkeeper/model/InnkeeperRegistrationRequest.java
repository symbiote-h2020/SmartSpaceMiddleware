package eu.h2020.symbiote.ssp.innkeeper.model;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.CoreRegister.CoreRegistry;
import eu.h2020.symbiote.ssp.CoreRegister.SspIdUtils;
import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.innkeeper.services.AuthorizationService;
import eu.h2020.symbiote.ssp.lwsp.Lwsp;
//import eu.h2020.symbiote.ssp.resources.SspRegInfo;
import eu.h2020.symbiote.cloud.model.ssp.SspRegInfo;
import eu.h2020.symbiote.ssp.resources.SspResource;
import eu.h2020.symbiote.ssp.resources.db.DbConstants;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;


@Component
public class InnkeeperRegistrationRequest {

	private static Log log = LogFactory.getLog(InnkeeperRegistrationRequest.class);

	@Value("${ssp.id}")
	String sspName;
	@Value("${symbIoTe.core.interface.url}")
	String coreIntefaceUrl;
	@Value("${innk.core.enabled:true}")
	Boolean isCoreOnline;
	@Value("${innk.lwsp.enabled:true}")
	Boolean isLwspEnabled;
	@Autowired
	SessionsRepository sessionsRepository;

	@Autowired
	ResourcesRepository resourcesRepository;

	@Autowired
	AuthorizationService authorizationService;

	@Autowired
	RegistrationInfoODataRepository registrationInfoODataRepository;
	@Autowired
	CoreRegistry coreRegistry; 

	public void setIsCoreOnline(Boolean v) {
		this.isCoreOnline=v;
	}

	public Boolean isCoreOnline() {
		return this.isCoreOnline;
	}

	public Boolean checkRegistrationInjection(SspRegInfo sspRegInfo) {
		List<SessionInfo> plgIdLst = sessionsRepository.findByPluginId(sspRegInfo.getPluginId());
		List<SessionInfo> plgURLLst = sessionsRepository.findByPluginURL(sspRegInfo.getPluginURL());
		List<SessionInfo> dk1Lst = sessionsRepository.findByDk1(sspRegInfo.getDerivedKey1());		
		return plgIdLst.size()!=0 && plgURLLst.size()!=0 && dk1Lst.size()!=0; 
	}

	public ResponseEntity<Object> SspRegister(String sessionId, String msg, String type) throws IOException {
		
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;

		SspRegInfo sspRegInfo =  new ObjectMapper().readValue(msg, SspRegInfo.class);

		InnkeeperRegistrationResponse regResp = registry(sspRegInfo, type);		
		log.info("REGISTRATION INFO, type:"+type+", SSP registartion status:"+regResp.getResult());
		switch (regResp.getResult()) {		
		case InnkeeperRestControllerConstants.REGISTRATION_OFFLINE: //OFFLINE
		case InnkeeperRestControllerConstants.REGISTRATION_OK:		
			
			SessionInfo s = new SessionInfo();
			
			if (type.equals(InnkeeperRestControllerConstants.SDEV)){
				if (!isLwspEnabled) {
					sessionId = new Lwsp().generateSessionId();				
					s.setsessionId(sessionId);
					s.setSessionExpiration(new Timestamp(System.currentTimeMillis()));
				} else {
					s=sessionsRepository.findBySessionId(sessionId);
				}
			}
			
			if (type.equals(InnkeeperRestControllerConstants.PLATFORM)){
				sessionId = new Lwsp().generateSessionId();				
				s.setsessionId(sessionId);
				s.setSessionExpiration(null);
			}
			
			httpStatus=HttpStatus.OK;			
			s.setSspId(regResp.getSspId());			
			s.setSymId(regResp.getSymId());								
			s.setPluginId(sspRegInfo.getPluginId());			
			s.setPluginURL(sspRegInfo.getPluginURL());
			if (s.getRoaming()==null) {
				s.setRoaming(false);
			}
						
			sessionsRepository.save(s);				
			break;


		case InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED:
			httpStatus=HttpStatus.OK;
			break;

		case InnkeeperRestControllerConstants.REGISTRATION_REJECTED:
		default:
			httpStatus=HttpStatus.BAD_REQUEST;
		}
		String response = new ObjectMapper().writeValueAsString(regResp);
		return new ResponseEntity<Object>(response,responseHeaders,httpStatus);
	}

	public InnkeeperRegistrationResponse registry(SspRegInfo sspRegInfo, String type) throws IOException {
		//TODO: implement checkCoreSymbioteIdRegistration with REAL Core interaction :-(
		coreRegistry.setOnline(this.isCoreOnline);
		coreRegistry.setRepository(sessionsRepository);
		
		String symIdFromCore;
		if (type==InnkeeperRestControllerConstants.PLATFORM)
			symIdFromCore = "";
		else
			symIdFromCore = coreRegistry.getSymbioteIdFromCore(sspRegInfo.getSymId(),sspRegInfo);

		//null SymId from core == REJECT THE REQUEST
		if (symIdFromCore==null) { 
			return new InnkeeperRegistrationResponse(
					sspRegInfo.getSymId(),sspRegInfo.getSspId(),InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	
		}

		//EMPTY smyId from core == OFFLINE
		if (symIdFromCore.equals("")) { 

			//  check if sspId exists
			SessionInfo sInfo= sessionsRepository.findBySspId(sspRegInfo.getSspId());
			if (sInfo!=null) {
				String sspId = sInfo.getSspId();
				return new InnkeeperRegistrationResponse(
						"",sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
			}

			// registration injection workaround: check sspRegInfo.getSspIdParent() value, multiple registration for the same SDEV/PLAT
			if (checkRegistrationInjection(sspRegInfo)) {
				// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
				log.error("REGISTRATION REJECTED AS SUSPECT DUPLICATED RegInfo="+new ObjectMapper().writeValueAsString(sspRegInfo));
				return new InnkeeperRegistrationResponse(
						sspRegInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
			}

			//No duplicate registration, go ahead
			return new InnkeeperRegistrationResponse(
					sspRegInfo.getSymId(),new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OFFLINE,DbConstants.EXPIRATION_TIME);
		}

		// if symIdFromCore == RegInfo.symId -> Already registered
		if (symIdFromCore.equals(sspRegInfo.getSymId())) { 	

			String sspId = sessionsRepository.findBySymId(symIdFromCore).getSspId();
			return new InnkeeperRegistrationResponse(
					symIdFromCore,sspId,InnkeeperRestControllerConstants.REGISTRATION_ALREADY_REGISTERED,0);
		} 

		//NEW REGISTRATION
		if ( sspRegInfo.getSymId().equals("")) {
			if (checkRegistrationInjection(sspRegInfo)) {
				// Got some duplicate fields in Session, suspect on registration,found other registration, reject.
				return new InnkeeperRegistrationResponse(
						sspRegInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);				
			} 
			//No duplicate registration, go ahead
			return	 new InnkeeperRegistrationResponse(
					symIdFromCore,new SspIdUtils(sessionsRepository).createSspId(),InnkeeperRestControllerConstants.REGISTRATION_OK,DbConstants.EXPIRATION_TIME);
		}
		log.error("REGISTARTION DEFAULT REJECTED");
		return new InnkeeperRegistrationResponse(
				sspRegInfo.getSymId(),null,InnkeeperRestControllerConstants.REGISTRATION_REJECTED,0);	

	}



	public ResponseEntity<Object> SspKeepAlive(String msg) throws IOException {

		//KEEP ALIVE ACTIONS:
		// 1. update Session Expiration
		// 2. check if SSP is online and update symbiote id for SDEV/PLAT and its Resources (Currently Mock)

		Date currTime = new Timestamp(System.currentTimeMillis());

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspRegInfo sspRegInfo = new ObjectMapper().readValue(msg, SspRegInfo.class);

		SessionInfo s = null;

		if (sspRegInfo.getSymId()==null || sspRegInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspRegInfo.getSspId());

		}else {
			// UPDATE USING SYMID
			s = sessionsRepository.findBySymId(sspRegInfo.getSymId());
		}
		
		
	
		InnkeeperRegistrationResponse response =new InnkeeperRegistrationResponse();

		if (s.getSessionExpiration()==null) {
			log.info("NO session Expiration for this Entry, skip Keep Alive");
			response.setResult("IGNORE");
			String res = new ObjectMapper().writeValueAsString(response);
			return new ResponseEntity<Object>(res,responseHeaders,httpStatus);

			
		}
		
		log.error("s="+s);
		log.error("s.getSymId()		="+s.getSymId());
		log.error("s.getSspId()		="+s.getSspId());
		log.error("s.getPluginURL()	="+s.getPluginURL());
		log.error("s.getRoaming()	="+s.getRoaming());
		/*if (s==null) {			
			log.error("ERROR1 - no session found");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;		
			String res = new ObjectMapper().writeValueAsString(response);
			return new ResponseEntity<Object>(res,responseHeaders,httpStatus);
		}*/
		if (	!s.getSspId().equals(sspRegInfo.getSspId()) && 
				!s.getSymId().equals(sspRegInfo.getSymId())){
			log.error("ERROR2 - no match Ids");
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);
			httpStatus=HttpStatus.BAD_REQUEST;
			String res = new ObjectMapper().writeValueAsString(response);
			return new ResponseEntity<Object>(res,responseHeaders,httpStatus);

		}
		/*else {
			log.info("SSpId or SymId match");
		}*/

		if (	( !isCoreOnline && (s.getSspId()!="" && !s.getSspId().equals(sspRegInfo.getSspId())) )
				) {
			log.error("ERROR3 - SSP online and SymId not match or SSP offline and SspId not match");
			//DEFAULT: ERROR
			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_ERROR);		
			httpStatus=HttpStatus.BAD_REQUEST;
			String res = new ObjectMapper().writeValueAsString(response);
			return new ResponseEntity<Object>(res,responseHeaders,httpStatus);
		}
		/*else {
			log.info("SspId "+s.getSspId()+" Match");
		}*/

		String symIdReg=null;
		
		sspRegInfo.setPluginURL(s.getPluginURL());
		sspRegInfo.setDerivedKey1(s.getdk1()); 
		sspRegInfo.setPluginId(s.getPluginId());
		sspRegInfo.setRoaming(s.getRoaming());
		sspRegInfo.setRoaming(s.getRoaming());
		sspRegInfo.setSymId(s.getSymId());
		sspRegInfo.setSspId(s.getSspId());
		//sspRegInfoCore.setHashField(sspRegInfo.getHashField()); //????
		
		
		if (sspRegInfo.getSymId()==null || sspRegInfo.getSymId().equals("")) {
			coreRegistry.setOnline(this.isCoreOnline);
			coreRegistry.setRepository(resourcesRepository);
			symIdReg = coreRegistry.getSymbioteIdFromCore("",sspRegInfo);
		}
		//	symIdReg = new CoreRegistry(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration("",sspRegInfo); //generate new symId from Core
		else {
			coreRegistry.setOnline(this.isCoreOnline);
			coreRegistry.setRepository(resourcesRepository);
			symIdReg = coreRegistry.getSymbioteIdFromCore(sspRegInfo.getSymId(),sspRegInfo);
		}
			//symIdReg = new CoreRegistry(sessionsRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(sspRegInfo.getSymId(),sspRegInfo);
		//if I found my symId/SspId of SDEV/PLAT in the MongoDb session, update expiration time it

		//UPDATE Expiration time of session
		if (symIdReg!=null) {
			if (s.getSymId()==null) {
				s.setSymId(symIdReg); // update SymId
			}else if (s.getSymId().equals("")) {
				s.setSymId(symIdReg); // update SymId
			}
		}

		//log.info("s.getSymIdParent()="+s.getSymId());
		s.setSessionExpiration(currTime);
		sessionsRepository.save(s);


		//UPDATE Expiration time of Resources

		List<ResourceInfo> resList= resourcesRepository.findBySspIdParent(s.getSspId());
		List<Map<String, String>> updatedSymIdList = new ArrayList<Map<String,String>>();
		for (ResourceInfo r : resList) {

			//String symIdRes = new CoreRegistry(resourcesRepository,	isCoreOnline).checkCoreSymbioteIdRegistration(r.getSymIdResource(),sspRegInfo);
			coreRegistry.setOnline(this.isCoreOnline);
			coreRegistry.setRepository(resourcesRepository);
			SspResource sspRes = new SspResource();
			sspRes.setSspIdParent(r.getSspIdParent());
			//sspRes.setSymIdParent(r.getSymIdParent());
			sspRes.setSymIdParent(symIdReg);
			log.error(">>>> r.getSspIdResource()="+r.getSspIdResource());
			sspRes.setSspIdResource(r.getSspIdResource());
			sspRes.setAccessPolicy(r.getAccessPolicySpecifier());
			sspRes.setResource(r.getResource());
			String symIdRes = coreRegistry.getSymbioteIdFromCore(r.getSymIdResource(),sspRes);
			HashMap<String,String> symIdEntry = new HashMap<String,String>();

			if (symIdRes!=null && r.getSymIdResource().equals("")) 
				r.setSymIdResource(symIdRes); // update SymId of Resource

			symIdEntry.put("symIdResource", r.getSymIdResource());
			symIdEntry.put("sspIdResource", r.getSspIdResource());
			updatedSymIdList.add(symIdEntry);
			r.setSymIdParent(s.getSymId());
			r.setSessionExpiration(currTime);

			resourcesRepository.save(r);


			//Keep Alive for OData
			List<RegistrationInfoOData> odataList= registrationInfoODataRepository.findBySspId(r.getSspIdResource());
			for (RegistrationInfoOData odata : odataList) {
				odata.setSessionExpiration(r.getSessionExpiration());
				odata.setSymbioteId(r.getSymIdResource());
				registrationInfoODataRepository.save(odata);
			}

/*
			//Keep Alive AccessPolicies						
			//Optional<AccessPolicy> accessPolicy= accessPolicyRepository.findById(sspIdCurr);
			Optional<AccessPolicy> ap = accessPolicyRepository.findById(r.getSspIdResource());
			AccessPolicy apUpdate = ap.get();
			apUpdate.setSessionExpiration(r.getSessionExpiration());
			accessPolicyRepository.save(apUpdate);
*/


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

		//log.info("SEND KEEP ALIVE RESPONSE: "+(String)(new ObjectMapper().writeValueAsString(response)));
		String res = new ObjectMapper().writeValueAsString(response);
		return new ResponseEntity<Object>(res,responseHeaders,httpStatus);

	}
	public ResponseEntity<Object> SspDelete(String msg) throws IOException {

		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.APPLICATION_JSON);
		HttpStatus httpStatus = HttpStatus.OK;
		SspRegInfo sspRegInfo = new ObjectMapper().readValue(msg, SspRegInfo.class);
		//Delete Session
		
		

		SessionInfo s = null;

		if (sspRegInfo.getSymId()==null || sspRegInfo.getSymId().equals("")) {
			// symId is not useful or not available, use sspId
			s = sessionsRepository.findBySspId(sspRegInfo.getSspId());

		}else {
			// REMOVE USING SYMID
			s = sessionsRepository.findBySymId(sspRegInfo.getSymId());
			//REMOVE FROM CORE
			coreRegistry.unregisterSDEV(sspRegInfo.getSymId());
		}

		InnkeeperRegistrationResponse response =new InnkeeperRegistrationResponse();

		//if I found my symId/SspId of SDEV/PLAT in the MongoDb session, delete it
		if (s!=null) {

			//Delete session
			sessionsRepository.delete(s);


			List<String> sspIdResourcesList = new ArrayList<String>();

			//Delete Resources
			List<ResourceInfo> resList= resourcesRepository.findBySspIdParent(s.getSspId());
			for (ResourceInfo r : resList) {
				resourcesRepository.delete(r);
				if (!s.getSymId().equals("")) {
					log.info("Resource is ONLINE delete from CORE");
					coreRegistry.unregisterResource(r);	
				}else {
					log.info("Resource is OFFLINE nothing to do");
				}
				sspIdResourcesList.add(r.getSspIdResource());
			}

			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
			httpStatus=HttpStatus.OK;

			//Delete OData			
			for (String sspIdCurr : sspIdResourcesList) {
				List<RegistrationInfoOData> odataList= registrationInfoODataRepository.findBySspId(sspIdCurr);
				for (RegistrationInfoOData r : odataList) {
					registrationInfoODataRepository.delete(r);
				}				
			}

			response.setResult(InnkeeperRestControllerConstants.REGISTRATION_OK);					
			httpStatus=HttpStatus.OK;
/*
			//Delete AccessPolicies			
			for (String sspIdCurr : sspIdResourcesList) {
				//Optional<AccessPolicy> accessPolicy= accessPolicyRepository.findById(sspIdCurr);				
				accessPolicyRepository.delete(sspIdCurr);

			}
*/
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
