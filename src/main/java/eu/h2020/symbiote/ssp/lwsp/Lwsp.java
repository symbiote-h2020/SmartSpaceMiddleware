package eu.h2020.symbiote.ssp.lwsp;

import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;


public class Lwsp {
	
	//TODO: implement here LWSP core algorithm, 
	// 1. manipulate data.  2. encode 3. decode 4. offer data to be persisted in DB. (session)
	private String data;
	//private SessionRepository sessionRepository;
	

	public Lwsp(String data) {
		// TODO Auto-generated constructor stub
		this.data=data;				
	}

	
	public String encode() {
		return "this is an encoded message";
	}
	
	public String decode() {
		return this.data;
	}
	
	public String getMti() {
		//TODO: FIXME: get correct Mti from message payload, if available, if Mti is not available return null.
		
		return LwspConstants.GW_INK_AuthN;
	}
	 

}
