package eu.h2020.symbiote.ssp.lwsp;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;

import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

public class Lwsp {
	private String data;
	private SessionRepository sessionRepository;
	

	public Lwsp(String data, SessionRepository sessionRepository) {
		// TODO Auto-generated constructor stub
		this.data=data;
		this.sessionRepository=sessionRepository;
	}

	public String response() {
		// TODO: Handle Lwsp messages and generate response
		//String res="{\"GWInnkeeperHello\":\"pippo\"}";
		//TEST
		String res="{\"GWINKAuthn\":\"pippo\"}";

		//save session in mongoDB, need to add more fields for LWSP
		Date currTime=new Date(new Date().getTime());
		String cookie = RandomStringUtils.randomAlphanumeric(17).toUpperCase();
		SessionInfo sessionInfo = new SessionInfo(cookie,currTime);
		sessionRepository.save(sessionInfo);
		return res;
	}

	public String decode() {
		return this.data;
	}

}
