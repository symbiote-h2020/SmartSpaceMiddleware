package eu.h2020.symbiote.ssp.lwsp;

import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;


public class Lwsp {
	@Autowired
	SessionRepository sessionRepository;
	
	
	private String data;
	//private SessionRepository sessionRepository;
	

	public Lwsp(String data, SessionRepository sessionRepository) {
		// TODO Auto-generated constructor stub
		this.data=data;		
		this.sessionRepository=sessionRepository;
		//check here the message received and provide a new session registration. 
		//save session in mongoDB, need to add more fields for LWSP
		Date currTime=new Date(new Date().getTime());
		String cookie = RandomStringUtils.randomAlphanumeric(17).toUpperCase();
		SessionInfo sessionInfo = new SessionInfo(cookie,currTime);
		this.sessionRepository.save(sessionInfo);
		
	}

	
	public String encode() {
		return "this is an encoded message";
	}
	
	public String decode() {
		return this.data;
	}
	
	public String getMti() {
		//TODO: get correct Mti from message payload, if available, if Mti is not available return null.
		return LwspConstants.GW_INK_AuthN;
	}
	 

}
