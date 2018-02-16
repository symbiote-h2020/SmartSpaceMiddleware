package eu.h2020.symbiote.ssp.lwsp.model;
/*
 * Format EXAMPLE from https://colab.intracom-telecom.com/display/SYM/Security+services+for+the+smart+space+in+H2020+symbIoTe
{
"GWInnkeeperHello": {
"mti": "0x20",
"cc": "0x00a8",
"iv": "110131581702",
"nonce": "5742335597",
"x509":
"DFIDXTBBAkWgAwFKxgIJAJC1HiIAZAiIMA0GCSqGSIb3DfBAYTAkFVMRMwEQYDVQQIDApTb21lLVN0YXRlMSEwHwYDVxaWRnaXRzIFB0eSBMdGQwHhcNMTExMjMxMDg1OTQ0WhcNMTAJjyzfN746vaInA1KxYEeI1Rx5KXY8zIdj6a7hhphpj2E04C3Fayua4DRHyZOLmlvQ6tIChY0Clfwue9hr97FGTR3s7fhUHwc8YuB7xxt8BVc69rLeHV15A0qyx77CLSj3tCx2IUXVqRs5mlsgVhA=="
    }
}
 */

import com.fasterxml.jackson.annotation.JsonProperty;

public class GWINKHello extends LwspMessage{

	@JsonProperty("cc") 		private String cc;
	@JsonProperty("iv") 		private String iv;
	@JsonProperty("nonce") 	private String nonce;
	@JsonProperty("x509")	private String x509;

	public GWINKHello() {
		this.setMti(LwspConstants.GW_INK_Hello);
	}
	
	public GWINKHello(String cc,String iv,String nonce,String x509) {
		this.cc=cc;
		this.iv=iv;
		this.nonce=nonce;
		this.x509=x509;
	}
	
	public String getCc() {
		return this.cc; 
	}
	public String getIv() {
		return this.iv; 
	}
	public String getNonce() {
		return this.nonce; 
	}
	public String getX509() {
		return this.x509; 
	}

}
