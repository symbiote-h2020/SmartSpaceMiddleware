/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

//*
//* @author Alessandro Carminati <a.carminati@unidata.it>
//*	

@Document(collection="sessions")
public class SessionInfo {

	//@JsonProperty("sessionId")
	@Id
	@JsonProperty("sessionId")
	private String sessionId;
	@Field
	@Indexed(name="session_expiration", expireAfterSeconds=DbConstants.EXPIRATION_TIME)
	private Date session_expiration;

	@JsonProperty("data")
	private String data;

	@JsonProperty("OutBuffer")
	private String OutBuffer;

	@JsonProperty("cipher")
	private String cipher;

	@JsonProperty("macaddress")
	private String macaddress;

	@JsonProperty("snonce")
	private String snonce;

	@JsonProperty("snonce2")
	private String snonce2;

	@JsonProperty("gnonce")
	private String gnonce;

	@JsonProperty("gnonce2")
	private String gnonce2;

	@JsonProperty("kdf")
	private String kdf;

	@JsonProperty("iv")
	private String iv;

	@JsonProperty("dk")
	private String dk;

	@JsonProperty("dk1")
	private String dk1;

	@JsonProperty("dk2")
	private String dk2;

	@JsonProperty("sn")
	private String sn;

	@JsonProperty("sign")
	private String sign;

	@JsonProperty("authn")
	private String authn;

	@JsonProperty("psk")
	private byte[] psk;
	
	@JsonProperty("symIdSDEV")
	private String symIdSDEV;

	@JsonProperty("internalIdSDEV")
	private String internalIdSDEV;

	/* HOWTO read expiration time directly via mongoDB client
      db.sessions.aggregate(     
      	{ $project: {         
      		session_expiration: 1,         
      		ttlMillis: {             
      			$subtract: [ new Date(), "$session_expiration"]         
      		}     
      	  }
     	} )
	 * */


	@JsonCreator
	public SessionInfo( @JsonProperty("sessionId") String sessionId,
			@JsonProperty("iv") String iv,
			@JsonProperty("psk") byte[] psk,
			@JsonProperty("dk") String dk,
			@JsonProperty("dk1") String dk1,
			@JsonProperty("dk2") String dk2,
			@JsonProperty("sn") String sn,
			@JsonProperty("sign") String sign,
			@JsonProperty("authn") String authn,
			@JsonProperty("data") String data,
			@JsonProperty("OutBuffer") String OutBuffer,
			@JsonProperty("cipher") String cipher,
			@JsonProperty("macaddress") String macaddress,
			@JsonProperty("snonce") String snonce,
			@JsonProperty("snonce2") String snonce2,
			@JsonProperty("gnonce") String gnonce,
			@JsonProperty("gnonce2") String gnonce2,
			@JsonProperty("kdf") String kdf,
			@JsonProperty("session_expiration") Date session_expiration,
			@JsonProperty("symIdSDEV") String symIdSDEV,
			@JsonProperty("internalIdSDEV") String internalIdSDEV

			) {
		System.out.println("sessionId="+sessionId);
		this.sessionId = sessionId;
		this.session_expiration = session_expiration;
		this.iv = iv;
		this.psk = psk;
		this.dk = dk;
		this.dk1 = dk1;
		this.dk2 = dk2;
		this.sn = sn;
		this.sign = sign;
		this.authn = authn;
		this.data = data;
		this.OutBuffer = OutBuffer;
		this.cipher = cipher;
		this.macaddress = macaddress;
		this.snonce = snonce;
		this.snonce2 = snonce2;
		this.gnonce = gnonce;
		this.gnonce2 = gnonce2;
		this.kdf = kdf;        
		this.symIdSDEV=symIdSDEV;
		this.internalIdSDEV=internalIdSDEV;
	}

	@JsonProperty("session_expiration")
	public void setSessionExpiration(Date session_expiration) {
		this.session_expiration = session_expiration;
	}
	public Date getSessionExpiration() {
		return this.session_expiration;
	}



	@JsonProperty("data")
	public void setdata(String data) 
	{
		this.data = data;
	}
	public String getdata() 
	{
		return this.data;
	}

	@JsonProperty("OutBuffer")
	public void setOutBuffer(String OutBuffer) 
	{
		this.OutBuffer = OutBuffer;
	}
	public String getOutBuffer() 
	{
		return this.OutBuffer;
	}

	@JsonProperty("cipher")
	public void setcipher(String cipher) 
	{
		this.cipher = cipher;
	}
	public String getcipher() 
	{
		return this.cipher;
	}

	@JsonProperty("macaddress")
	public void setmacaddress(String macaddress) 
	{
		this.macaddress = macaddress;
	}
	public String getmacaddress() 
	{
		return this.macaddress;
	}

	@JsonProperty("snonce")
	public void setsnonce(String snonce) 
	{
		this.snonce = snonce;
	}
	public String getsnonce() 
	{
		return this.snonce;
	}

	@JsonProperty("snonce2")
	public void setsnonce2(String snonce2) 
	{
		this.snonce2 = snonce2;
	}
	public String getsnonce2() 
	{
		return this.snonce2;
	}

	@JsonProperty("gnonce")
	public void setgnonce(String gnonce) 
	{
		this.gnonce = gnonce;
	}
	public String getgnonce() 
	{
		return this.gnonce;
	}

	@JsonProperty("gnonce2")
	public void setgnonce2(String gnonce2) 
	{
		this.gnonce2 = gnonce2;
	}
	public String getgnonce2() 
	{
		return this.gnonce2;
	}

	@JsonProperty("kdf")
	public void setkdf(String kdf) 
	{
		this.kdf = kdf;
	}
	public String getkdf() 
	{
		return this.kdf;
	}

	@JsonProperty("sessionId")
	public void setsessionId(String sessionId) 
	{
		this.sessionId = sessionId;
	}
	public String getsessionId() 
	{
		return this.sessionId;
	}

	@JsonProperty("iv")
	public void setiv(String iv) 
	{
		this.iv = iv;
	}
	public String getiv() 
	{
		return this.iv;
	}

	@JsonProperty("psk")
	public void setpsk(byte[] psk) 
	{
		this.psk = psk;
	}
	public byte[] getpsk() 
	{
		return this.psk;
	}

	@JsonProperty("dk")
	public void setdk(String dk) 
	{
		this.dk = dk;
	}
	public String getdk() 
	{
		return this.dk;
	}

	@JsonProperty("dk1")
	public void setdk1(String dk1) 
	{
		this.dk1 = dk1;
	}
	public String getdk1() 
	{
		return this.dk1;
	}

	@JsonProperty("dk2")
	public void setdk2(String dk2) 
	{
		this.dk2 = dk2;
	}
	public String getdk2() 
	{
		return this.dk2;
	}

	@JsonProperty("sn")
	public void setsn(String sn) 
	{
		this.sn = sn;
	}
	public String getsn() 
	{
		return this.sn;
	}

	@JsonProperty("sign")
	public void setsign(String sign) 
	{
		this.sign = sign;
	}
	public String getsign() 
	{
		return this.sign;
	}

	@JsonProperty("authn")
	public void setauthn(String authn) 
	{
		this.authn = authn;
	}
	public String getauthn() 
	{
		return this.authn;
	}



	@JsonProperty("symIdSDEV")
	public void setSymIdSDEV(String symIdSDEV) 
	{
		this.symIdSDEV = symIdSDEV;
	}
	public String getSymIdSDEV() 
	{
		return this.symIdSDEV;
	}
	
	@JsonProperty("internalIdSDEV")
	public void setInternalIdSDEV(String internalIdSDEV) 
	{
		this.internalIdSDEV = internalIdSDEV;
	}
	public String getInternalIdSDEV() 
	{
		return this.internalIdSDEV;
	}
	
}
