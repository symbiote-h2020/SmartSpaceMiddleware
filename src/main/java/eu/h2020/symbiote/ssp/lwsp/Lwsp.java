package eu.h2020.symbiote.ssp.lwsp;

import eu.h2020.symbiote.ssp.lwsp.model.LwspConstants;


public class Lwsp {
	
	//TODO: implement here LWSP core algorithm, 
	// 1. manipulate data.  2. encode 3. decode 4. offer data to be persisted in DB. (session)
	private String data;
	private int saltLength = 8;
	private String alpha="ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz-_+#";
	
	
	//private SessionRepository sessionRepository;
	

	public Lwsp(String data) {
		// TODO Auto-generated constructor stub
		this.data=data;				
	}

	public String getRawData() {
		return data;
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
	
    protected String getSaltS() 
    {
 	char[] symb,buf;
 	
 	if (saltLength < 1) throw new IllegalArgumentException();
     if (alpha.length() < 2) throw new IllegalArgumentException();
     symb = alpha.toCharArray();
     buf = new char[saltLength];
     for (int i=0;i<saltLength; i++) 
      {
     	buf[i]=symb[(int)(Math.random() * alpha.length())];
      }
     return new String(buf);
    }
	 

}
