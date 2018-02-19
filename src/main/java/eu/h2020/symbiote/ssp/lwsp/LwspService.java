package eu.h2020.symbiote.ssp.lwsp;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

@Service
public class LwspService {
	//TODO: implement here Lwsp Services, i.e. save Session on DB.
	@Autowired
	SessionRepository sessionRepository;

	private String data;
	//private SessionRepository sessionRepository;


	public void saveSession() {
		//TODO: decide what/how to save	 
		Date currTime=new Date(new Date().getTime());
		String cookie = RandomStringUtils.randomAlphanumeric(17).toUpperCase();
		SessionInfo sessionInfo = new SessionInfo(cookie,currTime);
		sessionRepository.save(sessionInfo);
	}



}
