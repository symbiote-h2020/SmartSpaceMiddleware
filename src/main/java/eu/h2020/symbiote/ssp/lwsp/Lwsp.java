package eu.h2020.symbiote.ssp.lwsp;

import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.io.*;
import java.sql.Timestamp;
import java.math.BigInteger;
import java.util.concurrent.Callable;
import java.util.logging.*;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.regex.*;

import org.apache.commons.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import eu.h2020.symbiote.ssp.innkeeper.communication.rest.InnkeeperRestController;
import eu.h2020.symbiote.ssp.innkeeper.model.InkRegistrationInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionRepository;

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
	SessionRepository sessionRepository;

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
	private char[] psk;
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
	private final String cipherREGEX=     "^0x[0-9A-F]+$"; 
	private final String snREGEX=         "^[0-9a-f]{1,8}$";
	private final String base64REGEX=     "^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$";
	private final String signREGEX=       "^[0-9a-f]{16,64}$";
	private final String DecJson=         "{\"mti\": \"0x55\",\"data\": \"%s\"}";
	private final String EncJson=         "{\"mti\": \"0x60\",\"data\": \"%s\",\"sessionId\": \"%s\"}";

	/*
  _____      _            _         __  __      _   _               _
 |  __ \    (_)          | |       |  \/  |    | | | |             | |
 | |__) | __ ___   ____ _| |_ ___  | \  / | ___| |_| |__   ___   __| |___
 |  ___/ '__| \ \ / / _` | __/ _ \ | |\/| |/ _ \ __| '_ \ / _ \ / _` / __|
 | |   | |  | |\ V / (_| | ||  __/ | |  | |  __/ |_| | | | (_) | (_| \__ \
 |_|   |_|  |_| \_/ \__,_|\__\___| |_|  |_|\___|\__|_| |_|\___/ \__,_|___/
	 */
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
	private static String pbkdf2_SHA1(char[] password,String DevSalt,int iterations) throws NoSuchAlgorithmException, InvalidKeySpecException
	{
		char[] chars = password;
		byte[] SerSalt = getSalt();
		byte[] DevSaltB = DatatypeConverter.parseHexBinary(DevSalt);
		byte[] salt = new byte[SerSalt.length + DevSaltB.length];
		System.arraycopy(DevSaltB, 0, salt, 0, DevSaltB.length);
		System.arraycopy(SerSalt, 0, salt, DevSaltB.length, SerSalt.length);


		PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, 128);
		SecretKeyFactory skf = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		byte[] hash = skf.generateSecret(spec).getEncoded();
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
	private static char[] getPSK() //mock function. the actual should get the psk saved into the db
	{
		//echo -n "marcio" |  sha1sum
		//return "Embraco, operaio si ";
		return new char[]{(char) 0x46, 0x72, 0x31, 0x73, 0x80, 0x52, 0x78, 0x92, 0x52, 0x81, 0xad, 0xd7, 0x57, 0x2c, 0x04, 0xa5, 0xdd, 0x84, 0x16, 0x68 };
	}
	private static String StripHTTPHeader(String data) 
	{
		String[] rows = data.split("[\r]*\n");
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
		String sha1_item=sha1(snonce+gnonce);
		byte[] tmp =new byte[sha1_item.length()/2+sn.length()];
		byte[] tmp2= HexSS2BArray(sha1_item);
		byte[] tmp3= sn.getBytes();  	
		for (int i=0; i<tmp2.length; i++) tmp[i]=tmp2[i];
		for (int i=0; i<tmp3.length; i++) tmp[i+tmp2.length]=tmp3[i];
		byte[] aescoded=aes128enc(tmp,HexSS2BArray(dk.split(":")[2]),iv.getBytes());
		return aescoded;
	}
	private String calcSign1() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException
	{
		return BArray2HexS(aescbcHash(this.snonce,this.gnonce,this.sn,this.dk));
	}
	private String calcSign2() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException
	{
		return BArray2HexS(aescbcHash(this.snonce2,this.gnonce2,this.sn,this.dk2));
	}
	private void incsn()
	{
		long value = new BigInteger(this.sn, 16).longValue();
		//    	this.sn=Long.toHexString(++value);
		//    	String.format("%08x", this.sn);
		this.sn=String.format("%08x", ++value);
	}
	private boolean validateAuthn() throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException 
	{
		String encoded = Base64.getEncoder().encodeToString(aescbcHash(this.snonce,this.gnonce,this.sn,this.dk));
		if (encoded.equals(this.authn)) return true;
		else return false;
	}
	private static byte[] aes128enc(byte[] data, byte[] key, byte[] iv) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException
	{
		Key aesKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, aesKey,ivParameterSpec);
		byte[] encrypted = cipher.doFinal(data);
		return encrypted;
	}
	private static byte[] aes128dec(byte[] data, byte[] key, byte[] iv) throws IllegalBlockSizeException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException
	{
		Key aesKey = new SecretKeySpec(key, "AES");
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		IvParameterSpec ivParameterSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, aesKey,ivParameterSpec);
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
					this.dk=pbkdf2_SHA1(psk, this.snonce ,4);
					this.dk1=dk.split(":")[2];
					jsonData1 = new JSONObject();
					jsonData1.put("sessionId", this.sessionId);
					jsonData1.put("mti", "0x20");
					jsonData1.put("cc", this.cipher);
					jsonData1.put("iv", this.iv=getSaltS(16,alpha));
					jsonData1.put("nonce", this.gnonce=dk.split(":")[1].substring(8,16));
					out=jsonData1.toString();
				}
			} else {
				out=this.error_fc;
			}
			currTime = new Timestamp(System.currentTimeMillis());
			sessionInfo = new SessionInfo(sessionId,iv,psk,dk,dk1,dk2,sn,sign,authn,data,OutBuffer,cipher,macaddress,snonce,snonce2,gnonce,gnonce2,kdf,currTime);
			this.OutBuffer=out;			
			sessionRepository.save(sessionInfo);
			break;
		case "0x30":
			/*
                  __   _  _  ____  _  _ 
                 / _\ / )( \(_  _)/ )( \
                /    \) \/ (  )(  ) __ (
                \_/\_/\____/ (__) \_)(_/
			 */           
			jsonData =new JSONObject(this.data);
			if (jsonData.has("sessionId"))
			{
				if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
				else {
					s = sessionRepository.findBySessionId(sessionId);
					
					log.info("----------------------------\n"+
					new ObjectMapper().writeValueAsString(s)
					+"----------------------------\n");
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
					if (! regexvalidator(this.sign       = jsonData.getString("sign"),signREGEX))              {out=this.error_f3;}
					if (! regexvalidator(this.snonce2    = jsonData.getString("nonce2"),nonceREGEX))            {out=this.error_f4;}
					if (validateAuthn() && out.equals(""))
					{
						if (calcSign1().equals(this.sign))
						{
							this.psk=getPSK(); 
							this.dk2=pbkdf2_SHA1(psk, this.snonce2 ,4);
							this.gnonce2=dk.split(":")[1].substring(8,16);
							incsn();
							jsonData1 = new JSONObject();
							jsonData1.put("sessionId", this.sessionId);
							jsonData1.put("mti", "0x40");
							jsonData1.put("nonce", this.gnonce=dk2.split(":")[1].substring(8,16));
							jsonData1.put("sign", calcSign2());
							jsonData1.put("sn", this.sn);
							jsonData1.put("authn", Base64.getEncoder().encodeToString(aescbcHash(this.snonce2,this.gnonce2,this.sn,this.dk2)));
							out=jsonData1.toString();
							currTime = new Timestamp(System.currentTimeMillis());
							sessionInfo = new SessionInfo(sessionId,iv,psk,dk,dk1,dk2,sn,sign,authn,data,OutBuffer,cipher,macaddress,snonce,snonce2,gnonce,gnonce2,kdf,currTime);
							sessionRepository.save(sessionInfo);
						}
					}
					else {out=this.error_f2;}
				}
			} else {out=this.error_fa;}
			this.OutBuffer=out;
			//System.out.println(Base64.getEncoder().encodeToString(aes128enc("{\"campo0\":\"topolino\",\"campo1\":\"pippo\",\"campo2\":\"pluto\",\"campo3\":\"paperino\",\"campo5\":\"qui\",\"campo6\":\"quo\",\"campo7\":\"qua\",\"campo8\":\"paperone\",\"campo9\":\"clarabella\"}".getBytes(),HexSS2BArray(dk.split(":")[2]),iv.getBytes())));
			break;
		case "0x50":
			/*
                 ____   __  ____  __  
                (    \ / _\(_  _)/ _\ 
                 ) D (/    \ )( /    \
                (____/\_/\_/(__)\_/\_/ 
			 */
			jsonData =new JSONObject(this.data);
			if (jsonData.has("sessionId"))
			{
				if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
				else {
					s = sessionRepository.findBySessionId(sessionId);
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
						this.OutBuffer=String.format(DecJson, decoded);
					} 
				}
			} 
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
		sessionRepository.save(sessionInfo);
		 */		
		return this.OutBuffer;
	}
	public void send_data(String data, SessionRepository sessionRepository) throws IOException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, JSONException 
	{
		JSONObject jsonData;
		SessionInfo s;
		String out="";



		jsonData =new JSONObject(this.data);
		if (jsonData.has("sessionId"))
		{
			if (! regexvalidator(this.sessionId=jsonData.getString("sessionId"),sessionIdREGEX)){out=this.error_fb;}
			else {
				s = sessionRepository.findBySessionId(sessionId);
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
					String decoded= new String(aes128enc(Base64.getDecoder().decode(data.getBytes()),HexSS2BArray(dk.split(":")[2]),iv.getBytes()));
					this.OutBuffer=String.format(EncJson, decoded, this.sessionId);
				} 
			}
		} 
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
}
