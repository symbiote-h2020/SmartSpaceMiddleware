package eu.h2020.symbiote.ssp.lwsp;

import java.io.IOException;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.model.InkRegistrationInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

@Service
public class LwspService {
	private static Log log = LogFactory.getLog(LwspService.class);

	@Autowired
	SessionRepository sessionRepository;

	public SessionInfo saveSession(Lwsp lwsp) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper m = new ObjectMapper();

		JsonNode node = m.readTree(lwsp.getRawData());
		String sessionId = node.get("sessionId").asText();
		InkRegistrationInfo innkInfo = new ObjectMapper().readValue(node.get("payload").toString(), InkRegistrationInfo.class);
		String symbioteIdFromInnk = innkInfo.getSymId();


		SessionInfo s = null;
		if ( (sessionId.equals("")) || (sessionId == "null")) {
			//search symbiotId 
			s = sessionRepository.findBySymbioteId(symbioteIdFromInnk);
			if (s == null) {
				sessionId=lwsp.getSaltS();
				Date currTime=new Date(new Date().getTime());
				// no previous symId found, new session registration
				log.info("NEW SESSION");
				log.info("SessionId():" +node.get("sessionId"));
				log.info("SessionExpiration:" +currTime);
				SessionInfo new_session = new SessionInfo(sessionId,symbioteIdFromInnk,currTime);
				sessionRepository.save(new_session);
				return new_session;
			}else {
				// I received a new registration form a not expired registered symid
				log.warn("SESSION JUST EXISTS");
				return null;
			}
		} else {
			// Check if session is active or not
			log.info("CHECK SESSION");
			s = sessionRepository.findById(sessionId);
			if (s !=null) {
				if (!s.getSymbioteId().equals(symbioteIdFromInnk)) {
					// I got a session id but the symbioteId is different: ERROR
					log.warn("Session: "+ s.getSessionId() +"innk got symbioteId=" + symbioteIdFromInnk+ " != "+s.getSymbioteId());
					return null;

				}else {
					log.warn("DUPLICATE REGISTRATION REQUEST from symbioteId:"+symbioteIdFromInnk);
					return null;
				}
			}else {
				log.info("SESSION ID "+sessionId+" NOT FOUND");
				return null;
			}
		}
	}


	public Date keepAliveSession(Lwsp lwsp) throws JsonProcessingException, IOException {

		ObjectMapper m = new ObjectMapper();
		JsonNode node = m.readTree(lwsp.getRawData());
		String sessionId = node.get("sessionId").asText();
		InkRegistrationInfo innkInfo = new ObjectMapper().readValue(node.get("payload").toString(), InkRegistrationInfo.class);
		String symbioteIdFromInnk = innkInfo.getSymId();
		SessionInfo s = sessionRepository.findById(sessionId);
		if (s==null) {
			return null;
		}else {
			s = sessionRepository.findById(sessionId);
			if (s !=null && s.getSymbioteId().equals(symbioteIdFromInnk)) {
				// I got a sessionId and its symbiote id is to received symbioteId, perform KEEP ALIVE
				Date currTime=new Date(new Date().getTime());
				s = new SessionInfo(sessionId,symbioteIdFromInnk,currTime);
				log.info("SessionId():" +node.get("sessionId"));
				log.info("SessionExpiration:" +s.getSessionExpiration());
				sessionRepository.save(s);
				return currTime;
			}else
				return null;


		}
	}


	public String unregistry(Lwsp lwsp) throws JsonProcessingException, IOException {
		ObjectMapper m = new ObjectMapper();
		JsonNode node = m.readTree(lwsp.getRawData());
		String sessionId = node.get("sessionId").asText();
		InkRegistrationInfo innkInfo = new ObjectMapper().readValue(node.get("payload").toString(), InkRegistrationInfo.class);
		String symbioteIdFromInnk = innkInfo.getSymId();
		SessionInfo s = sessionRepository.findById(sessionId);
		if (s==null) {
			return null;
		}else {
			s = sessionRepository.findById(sessionId);
			if (s !=null && s.getSymbioteId().equals(symbioteIdFromInnk)) {
				sessionRepository.delete(sessionId);
				return sessionId;
			}else
				return null;


		}		
	}



}
