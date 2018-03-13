package eu.h2020.symbiote.ssp.lwsp.model;
/*
 * Format EXAMPLE from https://colab.intracom-telecom.com/display/SYM/Security+services+for+the+smart+space+in+H2020+symbIoTe
{
"SDEVHello": {
"mti": "0x10",
"SDEVmac":"aa-bb-cc-dd-ee-ff",
"cp": "0x00a8, 0xccab, 0x008c,0xc004, 0xc005",
"kdf": "PBKDF2",
"nonce": "7050182458",
"x509":
"MIIDXTCCAkWgAwIBAgIJAJC1HiIAZAiIMA0GCSqGSIb3DfBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVxaWRnaXRzIFB0eSBMdGQwHhcNMTExMjMxMDg1OTQ0WhcNMTAJjyzfN746vaInA1KxYEeI1Rx5KXY8zIdj6a7hhphpj2E04C3Fayua4DRHyZOLmlvQ6tIChY0ClXXuefbmVSDeUHwc8YuB7xxt8BVc69rLeHV15A0qyx77CLSj3tCx2IUXVqRs5mlSbvA=="  
    }
}
 */

import com.fasterxml.jackson.annotation.JsonProperty;

public class SDEVHello extends LwspMessage{

	@JsonProperty("SDEVmac") private String sdevmac;
	@JsonProperty("cp") 		private String cp;
	@JsonProperty("kdf") 	private String kdf;
	@JsonProperty("nonce") 	private String nonce;
	@JsonProperty("x509")	private String x509;

	public SDEVHello() {
		this.setMti(LwspConstants.SDEV_Hello);	
	}	
	public SDEVHello(String sdevmac, String cp, String kdf, String nonce, String x509) {
		this.sdevmac= sdevmac;
		this.cp=cp;
		this.kdf= kdf;
		this.nonce=nonce;
		this.x509=x509;
	}
	String getSDEVmac() {
		return this.sdevmac;
	}
	String getCp() {
		return this.cp;
	}
	String getKdf() {
		return this.kdf;
	}
	String getNonce() {
		return this.nonce;
	}
	String getX509() {
		return this.x509;
	}




}
