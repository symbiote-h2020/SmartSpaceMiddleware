package eu.h2020.symbiote.ssp.lwsp;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.model.InkRegistrationInfo;
import eu.h2020.symbiote.ssp.lwsp.model.LwspMessage;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

@Service
public class LwspService {
	private static Log log = LogFactory.getLog(LwspService.class);

	//TODO: implement here Lwsp Services, i.e. save Session on DB.
	@Autowired
	SessionRepository sessionRepository;

	private String data;


	public String saveSession(Lwsp lwsp) throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper m = new ObjectMapper();

		JsonNode node = m.readTree(lwsp.getRawData());
		log.info(node.toString());
		String sessionId = node.get("sessionId").asText();
		SessionInfo s = null;
		if ( (sessionId.equals("")) || (sessionId == "null")) { 
			sessionId=lwsp.getSaltS();
			Date currTime=new Date(new Date().getTime());
			s = new SessionInfo(sessionId,currTime);
			log.info("NEW SESSION");
			log.info("SessionId():" +node.get("sessionId"));
			log.info("SessionExpiration:" +s.getSessionExpiration());
			sessionRepository.save(s);
			return sessionId;
		} else {
			// Check if session is activer or not
			log.info("CHECK SESSION");
			s = sessionRepository.findById(sessionId);
			if (s !=null) {
				log.info("GOT A SESSION, refresh it");
				log.info("SessionId():" +node.get("sessionId"));
				log.info("SessionExpiration:" +s.getSessionExpiration());
				Date currTime=new Date(new Date().getTime());
				s.setSessionExpiration(currTime);
				sessionRepository.save(s);
				return s.getSessionId();
			}else {
				log.info("SESSION ID "+sessionId+" NOT FOUND, ignore this message");
				return null;
			}
			
			
			
		}
		
		
		



		//TODO: decide what/how to save	using lwsp informations 

	}



}
