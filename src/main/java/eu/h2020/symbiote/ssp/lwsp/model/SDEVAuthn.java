package eu.h2020.symbiote.ssp.lwsp.model;
/*
 * Format EXAMPLE from https://colab.intracom-telecom.com/display/SYM/Security+services+for+the+smart+space+in+H2020+symbIoTe
{
"SDEVAuthn": {
"mti": "0x30",
"aad": "aa-bb-cc-dd-ee-ff;12792518056",
"authn":" IAkgIAkNCsKFCS4Jw6oJfwkuCS4JwrQJwpwJw6EJXgkuCcK1CTYJdAlrCcK6DQouCSMJKgkjCSYJNQkuCcKWCcOmCcKICWAJw7EJLgkuCUIJUA=="
    }
}
 */

import com.fasterxml.jackson.annotation.JsonProperty;

public class SDEVAuthn extends LwspMessage{

	@JsonProperty("aad") 	private String aad; 		//with AEAD, null if without AEAD	
	@JsonProperty("authn") 	private String authn;	
	@JsonProperty("sn") 		private String sn; 		//without AEAD, null if with AEAD
	@JsonProperty("sign") 	private String sign; 	//without AEAD, null if with AEAD
	public SDEVAuthn() {
		this.setMti(LwspConstants.SDEV_AuthN);	
	}
	public SDEVAuthn(String aad, String authn, String sn, String sign) {
		this.aad	= aad;
		this.authn=authn;
		this.sn = sn;
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
