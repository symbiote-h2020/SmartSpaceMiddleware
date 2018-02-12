package eu.h2020.symbiote.ssp.lwsp;

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

	public String rx() {
		// TODO: 
		//String res="{\"GWInnkeeperHello\":\"pippo\"}";
		//TEST
		String res="{\"GWINKAuthn\":\"pippo\"}";

		//save session in mongoDB, need to add more fields for LWSP
		SessionInfo sessionInfo = new SessionInfo("id","SESSIONVALUE","EXPIRATION");
		Object o = sessionRepository.save(sessionInfo);
		return res;
	}

	public String decode() {
		// TODO Auto-generated method stub
		return this.data;
	}

}
