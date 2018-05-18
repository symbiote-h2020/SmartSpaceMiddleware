package eu.h2020.symbiote.ssp.lwsp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.io.*;
import java.sql.Timestamp;
import java.math.BigInteger;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.json.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestControllerConstants;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;

@Service
public class Lwsp {

	private static Log log = LogFactory.getLog(Lwsp.class);

	/*
  _____      _            _         _____                           _   _
 |  __ \    (_)          | |       |  __ \                         | | (_)
 | |__) | __ ___   ____ _| |_ ___  | |__) | __ ___  _ __   ___ _ __| |_ _  ___  ___
 |  ___/ '__| \ \ / / _` | __/ _ \ |  ___/ '__/ _ \| '_ \ / _ \ '__| __| |/ _ \/ __|
 | |   | |  | |\ V / (_| | ||  __/ | |   | | | (_) | |_) |  __/ |  | |_| |  __/\__ \
 |_|   |_|  |_| \_/ \__,_|\__\___| |_|   |_|  \___/| .__/ \___|_|   \__|_|\___||___/
                                                   | |
                                                   |_|
	 */
	@Autowired
	SessionsRepository sessionsRepository;
	@Autowired
	ResourcesRepository resourcesRepository;
	
	@Autowired
	RegistrationInfoODataRepository registrationInfoODataRepository;

	private String data;
	private String allowedCipher;
	private String OutBuffer;
	private String cipher;
	private String macaddress;
	private String snonce,snonce2;
	private String gnonce,gnonce2;
	private String kdf;
	private String sessionId;
	private String iv;
	private byte[] psk;
	private String dk,dk1,dk2;
	private String sn;
	private String sign;
	private String authn;
	private final static char[] hexArray ="0123456789abcdef".toCharArray();
	private final String alpha=           "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz";
	private final String hexa=            "0123456789abcdef";
	private final String kdfSupported=    "PBKDF2";
	private final String error_ff=        "{\"mti\": \"0xff\",\"description\": \"macaddress invalid\"}";
	private final String error_fe=        "{\"mti\": \"0xfe\",\"description\": \"Derivation key function not supported\"}";
	private final String error_fd=        "{\"mti\": \"0xfd\",\"description\": \"nonce syntax invalid\"}";
	private final String error_fc=        "{\"mti\": \"0xfc\",\"description\": \"No matches found\"}";
	private final String error_fb=        "{\"mti\": \"0xfb\",\"description\": \"sessionId invalid\"}";
	private final String error_fa=        "{\"mti\": \"0xfa\",\"description\": \"sessionId not found.\"}";
	private final String error_f9=        "{\"mti\": \"0xf9\",\"description\": \"iv syntax error\"}";
	private final String error_f8=        "{\"mti\": \"0xf8\",\"description\": \"cipher syntax invalid\"}";
	private final String error_f7=        "{\"mti\": \"0xf7\",\"description\": \"kdf invalid\"}";
	private final String error_f6=        "{\"mti\": \"0xf6\",\"description\": \"stored dk invalid\"}";
	private final String error_f5=        "{\"mti\": \"0xf5\",\"description\": \"serialnumber invalid\"}";
	private final String error_f4=        "{\"mti\": \"0xf4\",\"description\": \"Base64 data wrong.\"}";
	private final String error_f3=        "{\"mti\": \"0xf3\",\"description\": \"Wrong sign syntax\"}";
	private final String error_f2=        "{\"mti\": \"0xf2\",\"description\": \"Auth verification Failed.\"}";
	private final String error_f1=        "{\"mti\": \"0xf1\",\"description\": \"\"}";
	private final String error_f0=        "{\"mti\": \"0xf0\",\"description\": \"\"}";
	private final String sessionIdREGEX=  "^[A-Z0-9a-z]{8}$";	
	private final String macaddressREGEX= "^([a-fA-F0-9]{2}[:-]){5}[a-fA-F0-9]{2}$";	
	private final String nonceREGEX=      "^[0-9a-fA-F]{3,8}$";	
	private final String kdfREGEX=        "^[A-Z]*KDF[0-9]*$";
	private final String ivREGEX=         "^[A-Z0-9a-z]{16}$";
	private final String cipherREGEX=     "^0x[0-9a-f]+$"; 
	private final String snREGEX=         "^[0-9a-f]{1,8}$";
	private final String base64REGEX=     "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
	private final String signREGEX=       "^[0-9a-f]{16,64}$";
	private final String DecJson=         "{\"mti\": \"0x55\",\"data\": \"%s\"}";
	private final String EncJson=         "{\"mti\": \"0x60\",\"data\": \"%s\",\"sessionId\": \"%s\"}";

	private Timestamp sessionExpiration;

	private String symId;
	private String sspId;
	private String pluginId;
	private String pluginURL;

	/*
  _____      _            _         __  __      _   _               _
 |  __ \    (_)          | |       |  \/  |    | | | |             | |
 | |__) | __ ___   ____ _| |_ ___  | \  / | ___| |_| |__   ___   __| |___
 |  ___/ '__| \ \ / / _` | __/ _ \ | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
 | |   | |  | |\ V / (_| | ||  __/ | |  | |  __/ |_| | | | (_) | (_| \__ \
 |_|   |_|  |_| \_/ \__,_|\__\___| |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
	 */

	private static byte[] concat_LE(byte[] a,byte[] b)
	{
		byte[] app = new byte[a.length + b.length];
		System.arraycopy(b, 0, app, 0, b.length);
		System.arraycopy(a, 0, app, b.length, a.length);
		return app;
	}
	private static String zeros(int b)
	{
		String s="";
		for (int i=0; i<b;i++) s+="0";
		return s;
	}
	private static byte[] BAGet(int padlen)
	{
		byte[] padding= {0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55, 0x55};
		byte[] pad_ = new byte[padlen];
		for (byte b=0;b<padlen;b++) {pad_[b]=padding[b];}
		return pad_;
	}	

	private static byte[] half(byte[] arr)
	{
		byte[] buff=new byte[arr.length/2];
		for (int i=0; i<arr.length/2; i++) buff[i]=arr[i];
		return buff;
	}

	private static String sha1(String input) throws UnsupportedEncodingException, NoSuchAlgorithmException 
	{
		String sha1 = "null";

		MessageDigest msdDigest = MessageDigest.getInstance("SHA-1");
		msdDigest.update(input.getBytes("UTF-8"), 0, input.length());
		sha1 = DatatypeConverter.printHexBinary(msdDigest.digest());
		return sha1;
	}
	public static char[] ba2ca(byte[] data)
	{
		char[] out=new char[data.length];
		for (int i=0; i<data.length; i++) out[i]=(char) data[i];
		return out;
	}
	public static byte[] ca2ba(char[] data)
	{
		byte[] out=new byte[data.length];
		for (int i=0; i<data.length; i++) out[i]=(byte) data[i];
		return out;
	}
	public static String BArray2HexS(byte[] data) 
	{
		char[] hexChars = new char[data.length * 2];
		for ( int j = 0; j < data.length; j++ ) {
			int v = data[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static String CArray2HexS(char[] data) 
	{
		char[] hexChars = new char[data.length * 2];
		for ( int j = 0; j < data.length; j++ ) {
			int v = data[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	public static byte[] HexSS2BArray(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}
	private boolean isJSONValid(String test) 
	{
		try {
			new JSONObject(test);
		} catch (JSONException ex) 
		{
			try {
				new JSONArray(test);
			} catch (JSONException ex1) 
			{
				return false;
			}
		}
		return true;
	}
	private boolean regexvalidator(String data, String pattern) 
	{
		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(data);
		return m.find();
	}
	public String get_mti() 
	{
		try {
			JSONObject jsonData = new JSONObject(this.data);
			return jsonData.getString("mti");

		}catch (Exception e) {
			return null;	
		}		
	}
	private String cryptochoose(String Offered, String Accept)
	{
		String[] AcceptItems= Accept.split(" ");
		String[] OfferedItems= Offered.split(" ");
		int i;

		for (i=0; i<AcceptItems.length; i++) 
			if (Arrays.asList(OfferedItems).contains(AcceptItems[i]))
				return AcceptItems[i];
		return "NONE";                        
	}
	private static String pbkdf2_SHA1(byte[] chars,String DevSalt,int iterations, boolean SaltComplete) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		byte[] salt;

		if (! SaltComplete) {
			byte[] SerSalt = getSalt();
			byte[] DevSaltB = DatatypeConverter.parseHexBinary(DevSalt);
			salt = new byte[SerSalt.length + DevSaltB.length];
			System.arraycopy(DevSaltB, 0, salt, 0, DevSaltB.length);
			System.arraycopy(SerSalt, 0, salt, DevSaltB.length, SerSalt.length);
		}
		else salt=DatatypeConverter.parseHexBinary(DevSalt);
		//		log.info("[LWSP]:    psk "+toHex(chars)+"   ");
		//		log.info("[LWSP]:    salt "+toHex(salt)+"   ");

		PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA1Digest());
		gen.init(chars, salt, iterations);
		byte[] hash = ((KeyParameter) gen.generateDerivedParameters(128)).getKey();		

		//		PBEKeySpec spec = new PBEKeySpec(ba2ca(chars), salt, iterations, 128);
		//		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		//		byte[] hash = skf.generateSecret(spec).getEncoded();
		return iterations + ":" + toHex(salt) + ":" + toHex(hash);
	}
	private static byte[] getSalt() throws NoSuchAlgorithmException
	{
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[4];
		sr.nextBytes(salt);
		return salt;
	}
	private String getSaltS(int length, String symbols) 
	{
		char[] symb,buf;

		if (length < 1) throw new IllegalArgumentException();
		if (symbols.length() < 2) throw new IllegalArgumentException();
		symb = symbols.toCharArray();
		buf = new char[length];
		for (int i=0;i<length; i++) 
		{
			buf[i]=symb[(int)(Math.random() * symbols.length())];
		}
		return new String(buf);
	}
	private static String toHex(byte[] array) throws NoSuchAlgorithmException
	{
		BigInteger bi = new BigInteger(1, array);
		String hex = bi.toString(16);
		int paddingLength = (array.length * 2) - hex.length();
		if (paddingLength > 0)
		{
			return String.format("%0"  +paddingLength + "d", 0) + hex;
		}
		else{
			return hex;
		}
	}
	private static byte[] getPSK() //mock function. the actual should get the psk saved into the db
	{
		//echo -n "marcio" |  sha1sum
		//return "Embraco, operaio si ";
		return new byte[]{0x46, 0x72, 0x31, 0x73, -128, 0x52, 0x78, -110, 0x52, -127, -83, -41, 0x57, 0x2c, 0x04, -91, -35, -124, 0x16, 0x68 };
		//return new byte[]{1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 };
	}
	private static String StripHTTPHeader(String data) 
	{
		String[] rows = data.split("[\r]* ");
		int index=0;
		String res="";
		for (int i=0;i<rows.length;i++) 
		{
			if (rows[i].equals("")) index=i;
		}
		for (int i=index;i<rows.length;i++)
		{
			if (!rows[i].equals("")) res+=rows[i];
		}
		return res;
	}
	private byte[] aescbcHash(String snonce,String gnonce,String sn,String dk) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException 
	{
		/*log.info("[LWSP]:aescbcHash(): "   +
				" snonce: "         +snonce+
				" gnonce: "         +gnonce+
				" sn: "             +sn+
				" dk: "             +dk
				);*/
		log.info("[LWSP]: sn_N: "   +(sn.length()!=0?sn:("0"+sn)));
		String sha1_item=sha1(snonce+gnonce);
		log.info("[LWSP]: aescbcHash(): sha1_item="+sha1_item); 
		byte[] tmp=concat_LE(
				//				          HexSS2BArray((sn.length()%2==0?sn:("0"+sn))),
				sn.getBytes(),
				HexSS2BArray(sha1_item)
				);
		/*log.info("[LWSP]: aescbcHash(): "   +
				" iv: "             +iv+
				" tmp: "            +BArray2HexS(tmp)+
				" dk: "             +dk
				);*/
		byte[] aescoded=aes128enc(tmp,HexSS2BArray(dk),iv.getBytes());
		//log.info("[LWSP]: aescbcHash(): "   +toHex(aescoded));
		return aescoded;
	}
	private String calcSign40() throws GeneralSecurityException, IOException
	{
		byte[] tmp4=HexSS2BArray(this.sn.length()==8?this.sn:(zeros(8-this.sn.length())+this.sn));
		//log.info("[LWSP]: calcSign40() sn : "+BArray2HexS(tmp4));
		byte[] tmp=aescbcHash(this.snonce2,this.gnonce2,this.sn,this.dk1);

		byte[] tmp2=concat_LE(tmp4,tmp);
		//log.info("[LWSP]: calcSign30(): dk2 -> "+this.dk.split(":")[2]);
		byte[] tmp3=HmacSHA1(tmp2,HexSS2BArray(this.dk2.split(":")[2]));

		/*log.info("[LWSP]: calcSign40(): "+
				" payload to hmac: "+ BArray2HexS(tmp2) +
				" sequence number: "+ tmp4 +
				" data: "+ BArray2HexS(tmp)+
				" dk2: "+ this.dk2.split(":")[2]+
				" sign: "+ BArray2HexS(tmp3)+
				" signb64: "+ Base64.getEncoder().encodeToString(tmp3)+
				" RemoteSign: "+this.sign		         

				);
				*/
		return Base64.getEncoder().encodeToString(tmp3);
	}

	public static byte[] HmacSHA1(byte[] data, byte[] key) throws GeneralSecurityException, IOException 
	{
		byte[] hmacData = null;

		/*log.info("[LWSP]: <     HmacSHA1(): "+
				" >           data: "+ BArray2HexS(data) +
				" <            key: "+ BArray2HexS(key) +
				"-----------------------------------------"
				);*/
		SecretKeySpec secretKey = new SecretKeySpec(key, "HmacSHA1");
		Mac mac = Mac.getInstance("HmacSHA1");
		mac.init(secretKey);
		hmacData = mac.doFinal(data);
		return hmacData;
	}	
	private String calcSign30() throws GeneralSecurityException, IOException
	{
		byte[] tmp4=HexSS2BArray(this.sn.length()==8?this.sn:(zeros(8-this.sn.length())+this.sn));
		log.info("[LWSP]:   calcSign30() sn->: "+BArray2HexS(tmp4));
		byte[] tmp=aescbcHash(this.snonce,this.gnonce,this.sn,this.dk1);

		byte[] tmp2=concat_LE(tmp4,tmp);
		log.info("[LWSP]:   calcSign30(): dk2->  "+this.dk.split(":")[2]);
		byte[] tmp3=HmacSHA1(tmp2,HexSS2BArray(this.dk2.split(":")[2]));

		log.info("[LWSP]:       calcSign30(): "+
				"  payload to hmac: "+ BArray2HexS(tmp2) +
				"  sequence number: "+ tmp4+
				"             data: "+ BArray2HexS(tmp)+
				"              dk2: "+ this.dk2.split(":")[2]+
				"             sign: "+ BArray2HexS(tmp3)+
				"          signb64: "+ Base64.getEncoder().encodeToString(tmp3)+
				"       RemoteSign: "+this.sign		         

				);
		return Base64.getEncoder().encodeToString(tmp3);
	}
	private void incsn()
	{
		long value = new BigInteger(this.sn, 16).longValue();
		//    	this.sn=Long.toHexString(++value);
		//    	String.format("%08x", this.sn);
		this.sn=String.format("%x", ++value);
	}
	private boolean validateAuthn() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException 
	{
		String encoded = Base64.getEncoder().encodeToString(aescbcHash(this.snonce,this.gnonce,this.sn,this.dk1));
		log.info("[LWSP]:  validateAuthn(): " +encoded+
				"  snonce: "         +this.snonce+
				"  gnonce: "         +this.gnonce+
				"  sn: "             +this.sn+
				"  dk: "             +this.dk1+
				"  Lb64: "           +encoded+
				"  Rb64: "           +this.authn
				);
		if (encoded.equals(this.authn)) {log.info("[LWSP]:  validateAuthn(): result OK!  ");return true;}
		else {log.info("[LWSP]:  validateAuthn(): result K0##  ");return false;}
	}
	private static byte[] aes128enc(byte[] data, byte[] key, byte[] iv) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException
	{
		int padLen=(data.length & 0xf)==0?0:16 - (data.length & 0xf);
		byte[] pad_data=BAGet(padLen);
		//log.info("[LWSP]: aes128enc()  data len: " +data.length+" "+padLen );

		Key aesKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, aesKey,ivParameterSpec);
		byte[] tmp=concat_LE(pad_data,data);
		//log.info("[LWSP]: aes128enc() pad data: " +BArray2HexS(tmp)+"    ");
		byte[] encrypted = cipher.doFinal(tmp);
		return encrypted;
	}
	private static byte[] aes128dec(byte[] data, byte[] key, byte[] iv) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException
	{
		int padLen=16 - (data.length & 0xf);
		//log.info("[LWSP]:  aes128dec()  data len: " +data.length+" "+padLen );

		Key aesKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, aesKey,ivParameterSpec);
		//log.info("[LWSP]: aes128enc() pad data: " +BArray2HexS(data)+"    ");
		byte[] decrypted = cipher.doFinal(data);
		return decrypted;
	}

	public String processMessage() throws InvalidKeyException, NoSuchAlgorithmException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, JSONException, Exception {
		String out="";
		JSONObject jsonData, jsonData1;
		Timestamp currTime;
		SessionInfo sessionInfo,s;
		int tmp;





		switch (this.get_mti())
		{
		case "0x10":
			log.info("[LWSP]: Received 0x10: "+data);
			jsonData =new JSONObject(this.data);
			this.sessionId=getSaltS(8,alpha);                
			if ((this.cipher=cryptochoose(jsonData.getString("cp").toLowerCase(),this.allowedCipher).toLowerCase())!="NONE")
			{
				//parse input JSON
				if (! regexvalidator(this.macaddress=jsonData.getString("SDEVmac"),macaddressREGEX)) {out=this.error_ff;}
				if (! regexvalidator(this.snonce=jsonData.getString("nonce"),nonceREGEX)) {
					out=this.error_fd;
				}
				tmp=8-this.snonce.length();
				if (this.snonce.length()<8) for (int i=0; i<tmp; i++) 
					this.snonce="0"+this.snonce;
				if (! regexvalidator(this.kdf=jsonData.getString("kdf"),kdfREGEX)) {out=this.error_fe;}
				if (!this.kdf.equals(kdfSupported)) {out=this.error_fe;}
				//elaborate request
				if (out.equals(""))
				{
					this.psk=getPSK(); 
					this.dk=pbkdf2_SHA1(psk, this.snonce ,4, false);
					this.dk1=dk.split(":")[2];
					jsonData1 = new JSONObject();
					jsonData1.put("sessionId", this.sessionId);
					jsonData1.put("mti", "0x20");
					jsonData1.put("cc", this.cipher);
					jsonData1.put("iv", this.iv=getSaltS(16,alpha));
					//					jsonData1.put("iv", this.iv="1111111111111111");
					jsonData1.put("nonce", this.gnonce=dk.split(":")[1].substring(8,16));
					out=jsonData1.toString();
				}
			} else {
				out=this.error_fc;
			}
			currTime = new Timestamp(System.currentTimeMillis());
			this.OutBuffer=out;			

			log.info("[LWSP]: Sent back 0x20: "+OutBuffer);
			if (!this.get_mti().contains("0xf")) 
			{
				sessionInfo = new SessionInfo(sessionId,iv,psk,dk,dk1,dk2,sn,sign,authn,data,OutBuffer,cipher,macaddress,snonce,snonce2,gnonce,gnonce2,kdf,currTime,symId,sspId,pluginId,pluginURL);
				
				// Reset Session if a new SDEV registration with same MAC address occours. 
				List<SessionInfo> lsessions=sessionsRepository.findByMacaddress(macaddress);
				if (lsessions.size()!=0) {
					for (SessionInfo tmp_session : lsessions) {
						log.warn("FOUND EXISTS SDEV with mac address:"+macaddress+", drop previous registration (sessionID="+tmp_session.getsessionId()+") and re-registry");						
						sessionsRepository.delete(tmp_session);
						
						List<String> sspIdResourcesList = new ArrayList<String>();

						//Delete Resources
						List<ResourceInfo> resList= resourcesRepository.findBySspIdParent(tmp_session.getSspId());
						for (ResourceInfo r : resList) {
							resourcesRepository.delete(r);
							sspIdResourcesList.add(r.getSspIdResource());
						}

						//Delete OData
						for (String sspIdCurr : sspIdResourcesList) {
							List<RegistrationInfoOData> odataList= registrationInfoODataRepository.findBySspId(sspIdCurr);
							for (RegistrationInfoOData r : odataList) {
								registrationInfoODataRepository.delete(r);
							}
						}
					}
				}
				sessionsRepository.save(sessionInfo);
			}
			break;
		case "0x30":
			/*
                  __   _  _  ____  _  _ 
                 / _\ / )( \(_  _)/ )( \
                /    \) \/ (  )(  ) __ (
                \_/\_/\____/ (__) \_)(_/
			 */           
			log.info("[LWSP]: Received 0x30:"+data);
			jsonData =new JSONObject(this.data);
			if (jsonData.has("sessionId"))
			{
				log.info("[LWSP]: sessionId found into the db.");
				if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
				else {
					s = sessionsRepository.findBySessionId(sessionId);
					log.info("[LWSP]: Recover data from DB "+ new ObjectMapper().writeValueAsString(s));
					if (! regexvalidator(this.iv         = s.getiv(),ivREGEX))                 {out=this.error_f9;}
					if (! regexvalidator(this.gnonce     = s.getgnonce(),nonceREGEX))          {out=this.error_fd;}
					if (! regexvalidator(this.snonce     = s.getsnonce(),nonceREGEX))          {out=this.error_fd;}
					if (! regexvalidator(this.cipher     = s.getcipher(),cipherREGEX))         {out=this.error_f8;}
					if (! regexvalidator(this.macaddress = s.getmacaddress(),macaddressREGEX)) {out=this.error_ff;}
					if (! regexvalidator(this.kdf        = s.getkdf(),kdfREGEX))               {out=this.error_f7;}
					this.psk=s.getpsk();
					this.dk =s.getdk();
					if (! regexvalidator(this.sn         = jsonData.getString("sn"),snREGEX))                  {out=this.error_f5;}
					if (! regexvalidator(this.authn      = jsonData.getString("authn"),base64REGEX))           {out=this.error_f4;}
					if (! regexvalidator(this.sign       = jsonData.getString("sign"),base64REGEX))            {out=this.error_f3;}
					if (! regexvalidator(this.snonce2    = jsonData.getString("nonce"),nonceREGEX))            {out=this.error_f4;}
					log.info("[LWSP]:  ][ ][ ][auth out: "+out);
					if (out.equals("") && validateAuthn())
					{
						log.info("[LWSP]: validateAuthn() success! validating sign...");
						String saltT=this.snonce+this.gnonce;

						byte[] arg=concat_LE(HexSS2BArray(saltT),half(getPSK()));
						this.dk2=pbkdf2_SHA1(arg, saltT ,4, true);
						log.info("[LWSP]:  "+
								"  Salt: "+ saltT+
								"  meat: "+ toHex(arg)+
								"  dk2: "+ this.dk2						         
								);

						if (calcSign30().equals(this.sign))
						{
							log.info("[LWSP]:  *************calcsign2 test************* ");
							this.psk=getPSK(); 
							this.gnonce2=BArray2HexS(getSalt());
							incsn();
							jsonData1 = new JSONObject();
							jsonData1.put("sessionId", this.sessionId);
							jsonData1.put("mti", "0x40");
							jsonData1.put("nonce", this.gnonce2);
							jsonData1.put("sign", calcSign40());
							jsonData1.put("sn", this.sn);
							jsonData1.put("authn", Base64.getEncoder().encodeToString(aescbcHash(this.snonce2,this.gnonce2,this.sn,this.dk1)));
							out=jsonData1.toString();
							this.sessionExpiration = new Timestamp(System.currentTimeMillis());
							sessionInfo = new SessionInfo(sessionId,iv,psk,dk,dk1,dk2,sn,sign,authn,data,OutBuffer,cipher,macaddress,snonce,snonce2,gnonce,gnonce2,kdf,this.sessionExpiration,symId,sspId,pluginId,pluginURL);
							sessionsRepository.save(sessionInfo);
							out=jsonData1.toString();
							log.info("[LWSP]:  +--------------------------------+"+
									" |"+
									" | out: "+ out+
									" |"+
									" +--------------------------------+"						         
									);

						}
					}
					else {out=out.equals("")?this.error_f2:out;}
				}
			} else {out=this.error_fa;}
			this.OutBuffer=out;
			//System.out.println(Base64.getEncoder().encodeToString(aes128enc("{\"campo0\":\"topolino\",\"campo1\":\"pippo\",\"campo2\":\"pluto\",\"campo3\":\"paperino\",\"campo5\":\"qui\",\"campo6\":\"quo\",\"campo7\":\"qua\",\"campo8\":\"paperone\",\"campo9\":\"clarabella\"}".getBytes(),HexSS2BArray(dk.split(":")[2]),iv.getBytes())));
			log.info("[LWSP]:  Sent back 0x40: "+OutBuffer);
			if (this.get_mti().contains("0xf")) 
			{

				log.info("[LWSP]:  ERROR DELETE session");

				SessionInfo ss = sessionsRepository.findBySessionId(this.sessionId);
				sessionsRepository.delete(ss);
			}

			break;
		case "0x50":
			/*
                 ____   __  ____  __  
                (    \ / _\(_  _)/ _\ 
                 ) D (/    \ )( /    \
                (____/\_/\_/(__)\_/\_/ 
			 */
			//log.info("[LWSP]: Received 0x50: "+data);
			jsonData =new JSONObject(this.data);
			if (jsonData.has("sessionId"))
			{
				if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
				else {
					s = sessionsRepository.findBySessionId(sessionId);
					if (! regexvalidator(this.iv         = s.getiv(),ivREGEX))                 {out=this.error_f9;}
					if (! regexvalidator(this.gnonce     = s.getgnonce(),nonceREGEX))          {out=this.error_fd;}
					if (! regexvalidator(this.snonce     = s.getsnonce(),nonceREGEX))          {out=this.error_fd;}
					if (! regexvalidator(this.cipher     = s.getcipher(),cipherREGEX))         {out=this.error_f8;}
					if (! regexvalidator(this.macaddress = s.getmacaddress(),macaddressREGEX)) {out=this.error_ff;}
					if (! regexvalidator(this.kdf        = s.getkdf(),kdfREGEX))               {out=this.error_f7;}
					if (! regexvalidator(this.gnonce2    = s.getgnonce2(),nonceREGEX))         {out=this.error_fd;}
					if (! regexvalidator(this.snonce2    = s.getsnonce2(),nonceREGEX))         {out=this.error_fd;}
					this.psk =s.getpsk();
					this.dk  =s.getdk();
					this.dk2 =s.getdk2();
					if (out.equals(""))
					{
						String decoded= new String(aes128dec(Base64.getDecoder().decode(jsonData.getString("data").getBytes()),HexSS2BArray(dk.split(":")[2]),iv.getBytes()));
						//log.info("[LWSP]: decoded: "+decoded);
						//this.OutBuffer=String.format(DecJson, decoded);
						this.OutBuffer=decoded;
					} 

					// KEEP ALIVE					
					this.sessionExpiration = new Timestamp(System.currentTimeMillis());
					s.setSessionExpiration(this.sessionExpiration);
					sessionsRepository.save(s);
				}
			}
			//log.info("[LWSP]: Sent back 0x60: "+OutBuffer);
			break;
		case "INVALID":          
			throw new Exception("InvalidJson");
		}    

		return this.OutBuffer;


	}
	/*
  _____       _     _ _        __  __      _   _               _
 |  __ \     | |   | (_)      |  \/  |    | | | |             | |
 | |__) |   _| |__ | |_  ___  | \  / | ___| |_| |__   ___   __| |___
 |  ___/ | | | '_ \| | |/ __| | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
 | |   | |_| | |_) | | | (__  | |  | |  __/ |_| | | | (_) | (_| \__ \
 |_|    \__,_|_.__/|_|_|\___| |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
	 */


	public String get_response() {

		//save session in mongoDB, need to add more fields for LWSP
		/*		Date currTime=new Date(new Date().getTime());
		String cookie = sessionID;
		SessionInfo sessionInfo = new SessionInfo(cookie,currTime);
		sessionsRepository.save(sessionInfo);
		 */		
		return this.OutBuffer;
	}
	public Date getSessionExpiration() {
		return this.sessionExpiration;
	}
	public void setSessionExpiration() {

	}
	public String send_data(String data) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException 
	{
		JSONObject jsonData;
		SessionInfo s;
		String out="";



		jsonData =new JSONObject(this.data);
		if (jsonData.has("sessionId"))
		{
			if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
			else {
				s = sessionsRepository.findBySessionId(sessionId);
				if (! regexvalidator(this.iv         = s.getiv(),ivREGEX))                 {out=this.error_f9;}
				if (! regexvalidator(this.gnonce     = s.getgnonce(),nonceREGEX))          {out=this.error_fd;}
				if (! regexvalidator(this.snonce     = s.getsnonce(),nonceREGEX))          {out=this.error_fd;}
				if (! regexvalidator(this.cipher     = s.getcipher(),cipherREGEX))         {out=this.error_f8;}
				if (! regexvalidator(this.macaddress = s.getmacaddress(),macaddressREGEX)) {out=this.error_ff;}
				if (! regexvalidator(this.kdf        = s.getkdf(),kdfREGEX))               {out=this.error_f7;}
				if (! regexvalidator(this.gnonce2    = s.getgnonce2(),nonceREGEX))         {out=this.error_fd;}
				if (! regexvalidator(this.snonce2    = s.getsnonce2(),nonceREGEX))         {out=this.error_fd;}
				this.psk =s.getpsk();
				this.dk  =s.getdk();
				this.dk2 =s.getdk2();
				if (out.equals(""))
				{
					String decoded= Base64.getEncoder().encodeToString(aes128enc(data.getBytes(),HexSS2BArray(dk.split(":")[2]),iv.getBytes()));
					this.OutBuffer=String.format(EncJson, decoded, this.sessionId);
					return this.OutBuffer;
				} 
			}
		} 
		return null;
	}
	public void setData(String data) {
		this.data=data;
	}
	public String getData() {
		return this.data;
	}
	public void setAllowedCipher(String allowedCipher) {
		this.allowedCipher=allowedCipher;
	}


	public String getSspId() {
		return this.sspId;
	}

	public void setSspId(String sspId) {
		this.sspId = sspId;
	}

	public String getSymId() {
		return this.symId;
	}

	public void setSymId(String symId) {
		this.symId = symId;
	}

	public void updateSessionsRepository(String sessionId, String symId, String sspId) {
		SessionInfo s = sessionsRepository.findBySessionId(sessionId);
		s.setSspId(sspId);
		s.setSymId(symId);
		sessionsRepository.save(s);
	}

	public String getSessionId() {
		return sessionId;
	}
	public void setSessionId(String sessionId) {
		this.sessionId=sessionId;
	}
	public String generateSessionId() {
		this.sessionId=getSaltS(8,alpha);
		return this.sessionId;
	}
	
                

}
