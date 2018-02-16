package eu.h2020.symbiote.ssp.lwsp.model;
/*
 * Format EXAMPLE from https://colab.intracom-telecom.com/display/SYM/Security+services+for+the+smart+space+in+H2020+symbIoTe
{
"GWINKAuthn": {
"mti": "0x40",
"sn":"12792518057",
"authn":"IAkgIAkNCsKFCS4Jw6oJfwkuCS4JwrQJwpwJw6EJXgkuCcK1CTYJdAlrCcK6DQouCSMJKgkjCSYJNQkuCcKWCcOmCcKICWAJw7EJLgkuCUIJUA==",
"sign":"4bb787275abbf88ecd548ef8b018f014c09316dcb67b7765834cd537233b4b3f"
    }
}
 */

import com.fasterxml.jackson.annotation.JsonProperty;


public class GWINKAuthn extends LwspMessage {

	@JsonProperty("aad") 	private String aad=null; 		//with AEAD, null if without AEAD	
	@JsonProperty("authn") 	private String authn=null;	
	@JsonProperty("sn") 		private String sn=null; 			//without AEAD, null if with AEAD
	@JsonProperty("sign") 	private String sign=null; 		//without AEAD, null if with AEAD
	
	public GWINKAuthn() {
		this.setMti(LwspConstants.GW_INK_AuthN);	
	}
	
	public GWINKAuthn(String aad,String authn,String sn,String sign) {
		this.aad=aad;
		this.authn=authn;
		this.sn=sn;
		this.sign=sign;
	}
	
	public String getAad() {
		return this.aad;
	}
	public String getAuthn() {
		return this.authn;
	}
	public String getSn() {
		return this.sn;
	}
	public String getSign() {
		return this.sign;
	}
	public boolean isAEAD() {		
		return (this.sn == null && this.sign==null);			
	}
}
