/*******************************************************************************
* symbiote "lightweight security protocol" Library
* Version: 0.1
* Date: 12/01/2018
* Author: Unidata
* Company: Unidata
*
* Library to handle security negotiation protocol for agent
*
*
* Revision  Description
* ========  ===========
* 
* 0.1      Initial release
*******************************************************************************/
#include "lsp.h"

lsp::lsp(char* cp, char* kdf, uint8_t* psk, uint8_t psk_len) {
	_kdf = String(kdf);
	_cp = String(cp);
	_psk = (uint8_t*)malloc(psk_len);
	memcpy(_psk, psk, psk_len);
	_psk_len = psk_len;
	_SDEVNonce = 0;
	_GWNonce = 0;
	_sn = 0;
	_needInitVector = false;
	_iv = "";
	_sessionId = "";
	_lastSSPId = "";
	memset(_dk1, 0, sizeof(_dk1));
	memset(_prevDk1, 0, sizeof(_prevDk1));
	memset(_dk2, 0, sizeof(_dk2));
	memset(_SDEVmac, 0, sizeof(_SDEVmac));
}

lsp::~lsp() {
	
}

void lsp::PBKDF2function( uint8_t *pass, uint32_t pass_len, uint8_t *salt, uint32_t salt_len,uint8_t *output, uint32_t key_len, uint32_t rounds )

{

  register int ret,j;
  register uint32_t i;
  register uint8_t md1[HMAC_DIGEST_SIZE],work[HMAC_DIGEST_SIZE];
  register size_t use_len;
  register uint8_t *out_p = output;
  register uint8_t counter[4];

  for ( i = 0 ; i < sizeof ( counter ) ; i++ )

    counter[i] = 0;

  counter[3] = 1;

  while (key_len)

  {

    sha1.initHmac(pass,pass_len);
    sha1.write(salt,salt_len);
    sha1.write(counter,4);
    hmac = sha1.resultHmac();

    for ( i = 0 ; i < HMAC_DIGEST_SIZE ; i++ )

      work[i] = md1[i] = hmac[i];

    for ( i = 1 ; i < rounds ; i++ )

    {

      sha1.initHmac(pass,pass_len);
      sha1.write(md1,HMAC_DIGEST_SIZE);
      hmac = sha1.resultHmac();

      for ( j = 0 ; j < HMAC_DIGEST_SIZE ; j++ )
      {
        md1[j] = hmac[j];
        work[j] ^= md1[j];
      }
    }

    use_len = (key_len < HMAC_DIGEST_SIZE ) ? key_len : HMAC_DIGEST_SIZE;

    for ( i = 0 ; i < use_len ; i++ )
      out_p[i] = work[i];

    key_len -= use_len;
    out_p += use_len;

    for ( i = 4 ; i > 0 ; i-- )

      if ( ++counter[i-1] != 0 )

        break;
  }

}

void lsp::printBuffer(uint8_t* buff, uint8_t len, String label) {
  Serial.print(label);
  Serial.print("\t= {");
  for (uint8_t j = 0; j < len - 1; j++) {
      Serial.print(buff[j],HEX);
      Serial.print(", ");
   }
   Serial.print(buff[len - 1],HEX);
   Serial.println("}");
}


void lsp::calculateDK1(uint8_t num_iterations) {
	P("\n\nStart calculating DK1 key....");
	// search for a valid key in flash, if not found _dk1 and _prevDk1 are not populated,
	// otherwise get _dk1 and  
	if (_kdf == "PBKDF2") {
		if (_cp == TLS_PSK_WITH_AES_128_CBC_SHA) {
			printBuffer(_psk, _psk_len, "PSK");
			uint8_t salt[8];
			memset(salt, 0, sizeof(salt));
			//uint32_t tmpnonce = ENDIAN_SWAP_32(0x98ec4);
			uint32_t tmpnonce = ENDIAN_SWAP_32(_SDEVNonce);
			memcpy(salt, (uint8_t*)&tmpnonce, 4);
			tmpnonce = ENDIAN_SWAP_32(_GWNonce);
			memcpy(salt+4, (uint8_t*)&tmpnonce, 4);

			printBuffer(salt, 8, "DK1salt");

			PBKDF2function( _psk, _psk_len, salt, sizeof(salt) ,_dk1, sizeof(_dk1), num_iterations );
#ifdef DEBUG_SYM_CLASS
			printBuffer(_dk1, AES_KEY_LENGTH, "DK1");
#endif
		} else {
			//TBD
			P("DK1: CRYPTO SUITE NOT IMPLEMENTED ");
		}	
	} else {
		//TBD
		P("HKDF already not implemented!");
	}
	
}

void lsp::calculateDK2(uint8_t num_iterations) {
	P("\nStart calculating DK2 key....");
	if (_kdf == "PBKDF2") {
		if (_cp == TLS_PSK_WITH_AES_128_CBC_SHA) {
			uint8_t dk2Password[8+(_psk_len/2)];
			
			uint8_t salt[8];
			memset(salt, 0, sizeof(salt));
			memset(dk2Password, 0, sizeof(dk2Password));

			// the new password is: firstpart(PSK/2)||SDEVnonce||GW_INKnonce
			memcpy(dk2Password, (uint8_t*)_psk, (_psk_len/2));

			uint32_t tmpnonce = ENDIAN_SWAP_32(_SDEVNonce);
			memcpy(dk2Password+10, (uint8_t*)&tmpnonce, 4);
			memcpy(salt, (uint8_t*)&tmpnonce, 4);

			tmpnonce = ENDIAN_SWAP_32(_GWNonce);
			memcpy(dk2Password+14, (uint8_t*)&tmpnonce, 4);
			memcpy(salt+4, (uint8_t*)&tmpnonce, 4);

			printBuffer(dk2Password, sizeof(dk2Password), "DK2password");
			printBuffer(salt, sizeof(salt), "DK2salt");

			PBKDF2function( dk2Password, sizeof(dk2Password), salt, sizeof(salt), _dk2, sizeof(_dk2), num_iterations );
#ifdef DEBUG_SYM_CLASS
			printBuffer(_dk2, AES_KEY_LENGTH, "DK2");
#endif
		} else {
			//TBD
			P("DK2: CRYPTO SUITE NOT IMPLEMENTED ");
		}
		
	} else {
		//TBD
		P("HKDF already not implemented!");
	}
	
}


uint8_t lsp::elaborateInnkResp(String& resp) {
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.parseObject(resp);
	if (!_root.success()) {
    	P("parseObject() failed");
    	return JSON_PARSE_ERR;
	}
	P("\nGOT this response from INNK:");
	_root.prettyPrintTo(Serial);
	String mti = _root["mti"].as<String>();
	if (mti == STRING_MTI_GW_INK_HELLO) {
		// GWInnkeeperHello code, everything ok
		// get the crypto choice
		/*
		This is a GW_INKK_HELLO RESPONSE:
			{	
				"mti": "0x20",
				"cc": "0x00a8",
				"iv": "<16_characters>",
				"nonce": "<GWnonce>",
				"sessionId": <abCD123a>
			}
		*/
		P("\nGOT MTI GW INK HELLO");
		String _cc = _root["cc"].as<String>();
		if (_cc != _cp) {
			P("Crypto Choice different from Crypto Proposal, process degraded, I continue usign CP");
		}
		_iv = _root["iv"].as<String>();
		String tmpConvString = _root["nonce"].as<String>();
		PI("DEBUG: GWnonce(STRING)=");
		P(tmpConvString);
		_GWNonce = HEX2Int(tmpConvString);
		PI("DEBUG: GWnonce=");
		P(_GWNonce);
		PI("DEBUG: iv=");
		P(_iv);
		//_GWNonce = _root["nonce"].as<String>().toInt();
		if (_iv != "") {
			// we need to use the init vector for the key calculation
			P("Init vector found");
			_needInitVector == true;
		} else {
			P("Init vector not found");
			_needInitVector == false;
		}
		_sessionId = _root["sessionId"].as<String>();
		return COMMUNICATION_OK;
	} else if (mti == STRING_MTI_GW_INK_AUTHN) {
		P("\nGOT MTI GW INK AUTHN");
/*
		This is a GW INK AUTHN RESPONSE:
			{	
				"mti": "0x40",
				"sn": <HEX(SDEVsn+1)>,
				"nonce": "<GWnonce2>",
				"sessionId": <abCD123a>,
				"authn": "<b64(ENC_dk1( ( SHA-1(HEX_STRING(SDEVnonce2)||HEX_STRING(GWnonce2))  || HEX_STRING(sn) )>",
				"sign": "<b64(SHA-1-HMAC_dk2([ENC_dk1(SHA-1(SDEVnonce2||GWnonce2))]))>"
			}
*/
			String sn = _root["sn"].as<String>();
			P("SEQUENCE NUMBER GOT(String):");
			P(sn);
			PI("SEQUENCE NUMBER GOT(INT):");
			Serial.println(HEX2Int(sn), HEX);
			if (HEX2Int(sn) == (_sn+1)) {
				// everything ok
				// Increment the _sn index of 1 unit to match with the 
				// _sn value used by the innkeeeper
				_sn = _sn+1;
				String sessionId = _root["sessionId"].as<String>();
				if (sessionId != _sessionId) {
					// Wrong session id
					P("ERR: wrong session ID");
					return COMMUNICATION_ERROR;
				}
				//String gwnonce = _root["nonce"].as<String>();
				// save the new GWnonce
				//_GWNonce = gwnonce.toInt();
				String tmpConvString = _root["nonce"].as<String>();
				PI("DEBUG: GWnonce(STRING)=");
				P(tmpConvString);
				_GWNonce = HEX2Int(tmpConvString);
				PI("DEBUG: GWnonce=");
				P(_GWNonce);
				String authn = _root["authn"].as<String>();
				String sign = _root["sign"].as<String>();
				String decrypted;
				if (decryptAndVerify(authn, decrypted, sign)) {

				}
				_sn = _sn+1;
				return COMMUNICATION_OK;
			} else {
				P("ERR: sequence wrong sequence number");
				return COMMUNICATION_ERROR;
			}
	} else {
		P("ERR: wrong mti code from INNK");
		return COMMUNICATION_ERROR;
	}
}

uint32_t lsp::HEX2Int(String in) {
	uint32_t ret = 0;
	for (uint8_t i = 1; i <= in.length(); i++) {
		switch (in.charAt(i-1)){
			case '0':
			break;
			case '1':
				ret += 1<<((in.length()-i)*4);
			break;
			case '2':
				ret += 2<<((in.length()-i)*4);
			break;
			case '3':
				ret += 3<<((in.length()-i)*4);
			break;
			case '4':
				ret += 4<<((in.length()-i)*4);
			break;
			case '5':
				ret += 5<<((in.length()-i)*4);
			break;
			case '6':
				ret += 6<<((in.length()-i)*4);
			break;
			case '7':
				ret += 7<<((in.length()-i)*4);
			break;
			case '8':
				ret += 8<<((in.length()-i)*4);
			break;
			case '9':
				ret += 9<<((in.length()-i)*4);
			break;
			case 'a':
				ret += 10<<((in.length()-i)*4);
			break;
			case 'b':
				ret += 11<<((in.length()-i)*4);
			break;
			case 'c':
				ret += 12<<((in.length()-i)*4);
			break;
			case 'd':
				ret += 13<<((in.length()-i)*4);
			break;
			case 'e':
				ret += 14<<((in.length()-i)*4);
			break;
			case 'f':
				ret += 15<<((in.length()-i)*4);
			break;
			default:
				P("ERROR HEX2INT");
			break;
		}
			

	}
	return ret;
}

void lsp::begin(String SSPId) {
	randomSeed(analogRead(0));
	// setup the rest client and web server listen port/path
	_rest_client = new RestClient(INNKEEPER_LSP_URL, LSP_PORT);
  	_rest_client->setContentType("application/json");
	_currentSSPId = SSPId;
  	// if first time ever connected to SSP do nothing
  	// otherwise retrive:
  	// - _prevDk1
  	// - _lastSSPId
  	getContextFromFlash();
}

/*
 if first time ever connected to SSP do nothing 
  	 otherwise retrive:
  	 - _prevDk1
  	 - _lastSSPId
*/
void lsp::getContextFromFlash() {
	P("GETCONTEXTFROMFLASH");
	String tmpSSPId = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION);
	for (uint8_t i = FLASH_LSP_START_SSPID; i < FLASH_LSP_END_SSPID; i++) {
		tmpSSPId += String(EEPROM.read(i), HEX);
	}
	PI("Read this SSPId from flash: ");
	P(tmpSSPId);
	if (tmpSSPId.substring(0, 4).equals("sym-")) {
		//got a valid SSP-id
		P("Found a SSPID valid in Flash!");
		for (uint8_t i = FLASH_LSP_START_PREV_DK1; i < FLASH_LSP_END_PREV_DK1; i++) {
			_prevDk1[i-FLASH_LSP_START_PREV_DK1] = EEPROM.read(i);
		}
		P("GOT this key from flash:");
		printBuffer(_prevDk1, sizeof(_prevDk1), "prev_DK1");
	} else {
		EEPROM.end();
		return;
	}
	
	_lastSSPId = tmpSSPId;
	EEPROM.end();
	return;
}

/*
 	Save in flash the currentSSPId and DK1
*/
void lsp::saveContextInFlash() {
	P("SAVECONTEXTINFLASH");
	EEPROM.begin(FLASH_MEMORY_RESERVATION);
	uint8_t j = 0;
	for (uint8_t i = FLASH_LSP_START_SSPID; i < FLASH_LSP_END_SSPID; i++) {
		EEPROM.write(i, _currentSSPId.charAt(j));
		j++;
		if(j >= _currentSSPId.length()) {
			P("WARN: wrote less character than expect in SSPid");
			break;
		}
	}
	for (uint8_t i = FLASH_LSP_START_PREV_DK1; i < FLASH_LSP_END_PREV_DK1; i++) {
			EEPROM.write(i,_dk1[i-FLASH_LSP_START_PREV_DK1]);
		}
	EEPROM.commit();
	EEPROM.end();
#ifdef DEBUG_SYM_CLASS
	// read back the content
	String tmpSSPId = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION);
	for (uint8_t i = FLASH_LSP_START_SSPID; i < FLASH_LSP_END_SSPID; i++) {
		tmpSSPId += String(EEPROM.read(i), HEX);
	}
	PI("Test flash data...\nSSPid from flash: ");
	P(tmpSSPId);
	PI("I expect: ");
	P(_currentSSPId);
	uint8_t tmpDK1[AES_KEY_LENGTH];
	for (uint8_t i = FLASH_LSP_START_PREV_DK1; i < FLASH_LSP_END_PREV_DK1; i++) {
			tmpDK1[i-FLASH_LSP_START_PREV_DK1] = EEPROM.read(i);
		}
	P("GOT this key from flash:");
	printBuffer(tmpDK1, sizeof(tmpDK1), "prev_DK1(from flash)");
	P("I expect:");
	printBuffer(_dk1, sizeof(_dk1), "DK1(as _dk1)");
#endif
}


/*
	Return a SHA1(symbiote-id||prevDK1). IN/OUT data should be intended as ascii hex rapresentation
*/
String lsp::getHashOfIdentity(String id) {
	// return all zeros if first time connect to a SSP
	if (_lastSSPId == "") return "00000000000000000000";
	else {
		String tmpString = id;
		for (uint8_t i = 0; i < AES_KEY_LENGTH; i++) tmpString = String(_prevDk1[i], HEX); 
		sha1.init();
		sha1.print(tmpString);
		uint8_t dataout[SHA1_KEY_SIZE];
		memcpy(dataout, sha1.result(), SHA1_KEY_SIZE);
		String retString = "";
		for (uint8_t i = 0; i < SHA1_KEY_SIZE; i++) retString = String(dataout[i], HEX);
		PI("Got this SHA-1(sym-id||prevDK1): "); 
		P(retString);
		return retString;
	}
}

uint8_t lsp::sendSDEVHelloToGW() {
	P("Enter sendSDEVHelloToGW\n");
	String resp = "";
	if (WiFi.status() == WL_CONNECTED) {
		_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.createObject();
	  	WiFi.macAddress(_SDEVmac);
	  	String mac_string;
	  	for (int j = 0; j < 6; j++) {
  			if (_SDEVmac[j] < 16) {
  				mac_string += "0";
  				} 
  			mac_string += String(_SDEVmac[j], HEX);
  			if (j!=5) mac_string += ":";
  		}
  		// FIXME, decomment
  		_SDEVNonce = random(0xFFFFFFFF);
  		String nonce = String(_SDEVNonce, HEX);
			_root["mti"] = STRING_MTI_SDEV_HELLO;
			_root["SDEVmac"] = mac_string.c_str();
			_root["cp"] = _cp.c_str();
			_root["kdf"] = _kdf.c_str();
			_root["nonce"] = nonce.c_str();
		String temp = "";
		P("Send this JSON:");
		_root.prettyPrintTo(Serial);
		_root.printTo(temp);
		// add this to respect the HTTP RFC
		temp = "\r\n" + temp;
		int statusCode = _rest_client->post(LSP_PATH, temp.c_str(), &resp);
		if (statusCode < 300 and statusCode >= 200){
			statusCode = elaborateInnkResp(resp);
			if (statusCode < 300 and statusCode >= 200) return NO_ERROR_LSP;
				else return statusCode;
		} else return statusCode;		
	} else return ERROR_NOT_CONNECTED;
}

void lsp::createAuthNPacket(uint8_t* dataout) {
	String gwNonceString = String(_GWNonce, HEX);
	while (gwNonceString.length() < 8) {
		// we need to add '0'
		gwNonceString = '0' + gwNonceString;

	}
	String dataToHash = String(_SDEVNonce, HEX) + gwNonceString;
	PI("\n**********\nSHA1(");
	PI(dataToHash);
	PI(")");
	sha1.init();
	sha1.print(dataToHash);
	memcpy(dataout, sha1.result(), 20);
}

uint8_t lsp::sendAuthN() {
	/* in this message SDEv should send (if TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA):
	{
		"mti": "0x30",
		"sn": "<HEX(sequence_number)>",
		"nonce": "<SDEVnonce2>",
		"sessionId": "abcdefgh",
		"authn": "<			      b64 ( ENC_dk1 ( SHA-1(HEX_STRING(SDEVnonce)||HEX_STRING(GWnonce))  || HEX_STRING(sn) ) )  >"
		"sign":  "<b64( SHA-1-HMAC_dk2( ENC_dk1 ( SHA-1(HEX_STRING(SDEVnonce)||HEX_STRING(GWnonce))  || HEX_STRING(sn) ) ) )>"
	}
	 */
	//"authn": <			     b64 ( ENC_dk1 ( SHA-1(HEX_STRING(SDEVnonce)||HEX_STRING(GWnonce))  || HEX_STRING(sn) ) )  >
	//"sign":  <b64( SHA-1-HMAC_dk2( ENC_dk1 ( SHA-1(HEX_STRING(SDEVnonce)||HEX_STRING(GWnonce))  || HEX_STRING(sn) ) ) )>

	P("\nEnter sendAuthN\n*********************\n");
	String resp = "";
	if (WiFi.status() == WL_CONNECTED) {
		_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.createObject();
		//initialize the sequence number
  		_sn = random(1000000);
  		// use DK1 to encrypt
  		String outdata;
  		String signedData;
  		uint8_t authNPacket[SHA1_KEY_SIZE];
  		memset(authNPacket, 0, SHA1_KEY_SIZE);
  		// crypt and sign data with the old SDEVnonce
  		// authNPacket should be SHA1(SDEVnonce||GWnonce)
  		createAuthNPacket(authNPacket);
  		printBuffer(authNPacket, 20, "");
  		encryptDataAndSign((char*)authNPacket, outdata, signedData);
  		//PI("B64 encoded data(outside): ");
		//P(outdata);
		// create the new SDEVnonce
		_SDEVNonce = random(0xFFFFFFFF);
			_root["mti"] = STRING_MTI_SDEV_AUTHN;
			_root["sn"] = String(_sn, HEX);
			_root["nonce"] = String(_SDEVNonce, HEX);
			_root["sessionId"] = _sessionId;
			_root["authn"] = outdata;
				// use DK2 to sign
			_root["sign"] = signedData;
		String temp = "";
		P("\n*********************\nSend this JSON:");
		_root.prettyPrintTo(Serial);
		P(" ");
		_root.printTo(temp);
		temp = "\r\n" + temp;
		int statusCode = _rest_client->post(LSP_PATH, temp.c_str(), &resp);
		if (statusCode < 300 and statusCode >= 200){
			//_sn++; must be incremented by the INNK
			statusCode = elaborateInnkResp(resp);
			if (statusCode < 300 and statusCode >= 200) return NO_ERROR_LSP;
				else return statusCode;
		} else return statusCode;		
	} else return ERROR_NOT_CONNECTED;
}


String lsp::getSessionId() {
	return _sessionId;
}

String lsp::getDK1() {
	String tmp = "";
	for (uint8_t i = 0; i < AES_KEY_LENGTH; i++) tmp += String(_dk1[i], HEX);
	return tmp;
}

void lsp::bufferSize(char* text, int &length) {
	int i = strlen(text);
	int buf = round(i / BLOCK_SIZE) * BLOCK_SIZE;
	length = (buf < i) ? buf + BLOCK_SIZE : length = buf;
}

void lsp::bufferSize(char* text, int text_len, int &length) {
	int i = text_len;
	int buf = round(i / BLOCK_SIZE) * BLOCK_SIZE;
	length = (buf < i) ? buf + BLOCK_SIZE : length = buf;
}

void lsp::bufferSize(unsigned char* text, int &length) {
	int i = strlen((const char*)text);
	int buf = round(i / BLOCK_SIZE) * BLOCK_SIZE;
	length = (buf < i) ? buf + BLOCK_SIZE : length = buf;
}

void lsp::signData(uint8_t* data, uint8_t data_len, String& output) {

	uint8_t dataToSign[data_len+4];
	memcpy(dataToSign, data, data_len);
	uint32_t tmpSn = ENDIAN_SWAP_32(_sn);
	memcpy(dataToSign+data_len, (uint8_t*)&tmpSn, 4);

	printBuffer(dataToSign, data_len+4, "DataToSign");

	sha1.initHmac(_dk2, DK2_KEY_LENGTH);
    sha1.write(dataToSign, (data_len+4));

    uint8_t* ret_data = sha1.resultHmac();

    printBuffer(ret_data, SHA1_KEY_SIZE, "BinarySign");
	base64 b64enc;
	String encoded = b64enc.encode(ret_data, SHA1_KEY_SIZE, false);
	output = encoded;
}

void lsp::encryptAndSign(char* plain_text, String& output, int length, String& signature) {
	
	byte enciphered[length];
	uint8_t iv[16];
	for (uint8_t k = 0; k < 16; k++) iv[k] = _iv.charAt(k);
	printBuffer(iv, 16, "IV\t");
	AES aesEncryptor(_dk1, iv, AES::AES_MODE_128, AES::CIPHER_ENCRYPT);
	//aesEncryptor.process((uint8_t*)plain_text, enciphered, length);
	aesEncryptor.processNoPad((uint8_t*)plain_text, enciphered, length);
	int encrypted_size = sizeof(enciphered);
	printBuffer((uint8_t*)enciphered, encrypted_size, "EncrypData");

	String signedData;
	signData(enciphered, encrypted_size, signedData);
	signature = signedData;

	base64 b64enc;
	String encoded = b64enc.encode(enciphered, encrypted_size, false);
	output = encoded;
}



/*
	Encrypts with DK1 (data || <sequence_number>) and returns
	the base64 rapresentation of the encrypted data and the b64 rapresentation of the signed 
	SHA1-HMAC_dk2(ENC_DATA|| <sequence_number>) 
*/
void lsp::encryptDataAndSign(char* plain_text, String& output, String& signature) {
	int length = 0;
	unsigned int tmpLen = 0;
	// todo fixme
	tmpLen = SHA1_KEY_SIZE + String(_sn, HEX).length();
	//String dataToEncrypt = String(plain_text) + String(_sn, HEX);
	PI("ADD this SN to encrypt:\t= ");
	P(String(_sn, HEX));
	uint8_t arrayOfDataToEncrypt[tmpLen];
	memset(arrayOfDataToEncrypt, 0, tmpLen);
	//dataToEncrypt.getBytes((byte*)arrayOfDataToEncrypt, (unsigned int)tmpLen); 
	memcpy(arrayOfDataToEncrypt, plain_text, SHA1_KEY_SIZE);
	for (uint8_t i = SHA1_KEY_SIZE; i < tmpLen; i++) arrayOfDataToEncrypt[i] = String(_sn, HEX).charAt(i-SHA1_KEY_SIZE);

	printBuffer((uint8_t*)arrayOfDataToEncrypt, tmpLen,"Data2Encrypt(array)");
	bufferSize((char*)arrayOfDataToEncrypt, tmpLen, length);;
	
	String encrypted;
	String tmpSign;
	uint8_t arrayOfDataToEncrypt_padded[length];
	memcpy(arrayOfDataToEncrypt_padded, arrayOfDataToEncrypt, tmpLen);
	if (tmpLen < length) {
		// we need to pad the data
		for (uint8_t i = tmpLen; i < length; i++) arrayOfDataToEncrypt_padded[i] = 0x55;
	}

	/// add 0x55 as pad if needed
	//for (uint8_t i = dataToEncrypt.length(); i < length; i++) dataToEncrypt.concat('U');
	//for (uint8_t i = tmpLen; i < length; i++) dataToEncrypt.concat('U');
	///printBuffer((uint8_t*)dataToEncrypt.c_str(), dataToEncrypt.length(),"DATA2ENCRYPT");
	printBuffer(arrayOfDataToEncrypt_padded, length,"DATA2ENCRYPT(padded)");
	PI("Lenght of the data without padding:\t");
	P(tmpLen);
	PI("Lenght of the data with padding:\t");
	P(length);

	//encryptAndSign((char*)dataToEncrypt.c_str(), encrypted, length, tmpSign);
	encryptAndSign((char*)arrayOfDataToEncrypt_padded, encrypted, length, tmpSign);
	output = encrypted;	
	signature = tmpSign;
}

/* TMP
		This is a GW INK AUTHN RESPONSE:
			{	
				"mti": "0x40",
				"sn": <HEX(SDEVsn+1)>,
				"nonce": "<GWnonce2>",
				"sessionId": <abCD123a>,
				"authn": "<b64(ENC_dk1(SHA-1(SDEVnonce2||GWnonce2)))>",
				"sign": "<b64(SHA-1-HMAC_dk2([ENC_dk1(SHA-1(SDEVnonce2||GWnonce2))]))>"
			}
*/
bool lsp::decryptAndVerify(String authn, String& decrypted, String GWsigned) {
	// Please note that you should invoke this method after SDEV and GW nonce are updated with 
	// SDEVnonce2 and GWnonce2

	// calculate the new ENC_dk1(SHA-1(SDEVnonce2||GWnonce2))
	String GWoutdata;
  	String signedData;
  	uint8_t GWauthNPacket[SHA1_KEY_SIZE];
  	memset(GWauthNPacket, 0, SHA1_KEY_SIZE);
  	// it creates the packet using the new SDEV and GW nonce savend in library
  	P(" ");
  	createAuthNPacket(GWauthNPacket);
  	printBuffer(GWauthNPacket, 20, " = CalculatedGWauthNPacket");
  	encryptDataAndSign((char*)GWauthNPacket, GWoutdata, signedData);
  	PI("Got this sign from INNK:\t");
  	P(GWsigned);
  	PI("Calculated sign:\t\t");
  	P(signedData);
  	// FIXME: uncomment
  	if (signedData == GWsigned) {
  		unsigned int binaryLength = decode_base64_length((unsigned char*)authn.c_str());
  		unsigned char decodedb64[binaryLength];
  		memset(decodedb64, 0, binaryLength);
  		P("Sign match found!");
  		decode_base64((unsigned char*)authn.c_str(), decodedb64);
  		printBuffer(decodedb64, binaryLength, "AUTHN(binary)");
  		String plainHex;
  		decrypt(decodedb64, binaryLength, plainHex);
  		PI("PlainHex decrypted: ");
  		P(plainHex);
  		return true;
  	}
}

/*
	Convert plain text to b64 of encrypted data
	NO use of sequence number
*/
void lsp::cryptData(String in, String& out) {
	P("CRYPTDATA");
	int length = 0;
	bufferSize((char*)in.c_str(), length);
	byte enciphered[length];
	uint8_t iv[16];
	for (uint8_t k = 0; k < 16; k++) iv[k] = _iv.charAt(k);
	printBuffer(iv, 16, "IV\t");

	AES aesEncryptor(_dk1, iv, AES::AES_MODE_128, AES::CIPHER_ENCRYPT);
	aesEncryptor.process((uint8_t*)in.c_str(), enciphered, length);
	int encrypted_size = sizeof(enciphered);
	printBuffer((uint8_t*)enciphered, encrypted_size, "EncrypData");
	base64 b64enc;
	String encoded = b64enc.encode(enciphered, encrypted_size, false);


	
	out = encoded;
}

/*
	Convert data b64 crypted data to plain text
	NO use of sequence number
*/
void lsp::decryptData(String in, String& out) {
	P("DECRYPTDATA");
	unsigned int binaryLength = decode_base64_length((unsigned char*)in.c_str());
  	unsigned char decodedb64[binaryLength];
  	memset(decodedb64, 0, binaryLength);
  	decode_base64((unsigned char*)in.c_str(), decodedb64);
  	printBuffer(decodedb64, binaryLength, "ENCRYPT_DATA(binary)");
  	String plainHex;
	decrypt(decodedb64, binaryLength, plainHex);
	out = plainHex;
}

void lsp::decrypt(unsigned char* crypted, uint8_t cryptedSize, String& output) {
	
	// TODO FIXME iv must be the same accorded with INNKEEPER
	int length = 0;
 	bufferSize((char*)crypted, cryptedSize, length);
  	byte deciphered[length];

  	uint8_t iv[16];
	for (uint8_t k = 0; k < 16; k++) iv[k] = _iv.charAt(k);
	printBuffer(iv, 16, "IV\t");

  	AES aesDencryptor(_dk1, iv, AES::AES_MODE_128, AES::CIPHER_DECRYPT);
  	aesDencryptor.process((uint8_t*)crypted, deciphered, length);
  	printBuffer(deciphered, length, "DECRYPT(BINARY)");
  	for (uint8_t i = 0; i< length; i++) output += String(deciphered[i], HEX);
}



/* base64_to_binary:
 *   Description:
 *     Converts a single byte from a base64 character to the corresponding binary value
 *   Parameters:
 *     c - Base64 character (as ascii code)
 *   Returns:
 *     6-bit binary value
 */
unsigned char lsp::base64_to_binary(unsigned char c) {
  // Capital letters - 'A' is ascii 65 and base64 0
  if('A' <= c && c <= 'Z') return c - 'A';

  // Lowercase letters - 'a' is ascii 97 and base64 26
  if('a' <= c && c <= 'z') return c - 71;

  // Digits - '0' is ascii 48 and base64 52
  if('0' <= c && c <= '9') return c + 4;

  // '+' is ascii 43 and base64 62
  if(c == '+') return 62;

  // '/' is ascii 47 and base64 63
  if(c == '/') return 63;

  return 255;
}

unsigned int lsp::decode_base64_length(unsigned char input[]) {

  unsigned char *start = input;
  while(base64_to_binary(input[0]) < 64) {
    ++input;
  }
  unsigned int input_length = input - start;
  unsigned int output_length = input_length/4*3;
  switch(input_length % 4) {

    default: return output_length;

    case 2: return output_length + 1;

    case 3: return output_length + 2;
  }
}


/* decode_base64:
 *   Description:
 *     Converts a base64 null-terminated string to an array of bytes
 *   Parameters:
 *     input - Pointer to input string
 *     output - Pointer to output array
 *   Returns:
 *     Number of bytes in the decoded binary
 */
unsigned int lsp::decode_base64(unsigned char input[], unsigned char output[]) {

  unsigned int output_length = decode_base64_length(input);
  // While there are still full sets of 24 bits...
  for(unsigned int i = 2; i < output_length; i += 3) {

    output[0] = base64_to_binary(input[0]) << 2 | base64_to_binary(input[1]) >> 4;
    output[1] = base64_to_binary(input[1]) << 4 | base64_to_binary(input[2]) >> 2;
    output[2] = base64_to_binary(input[2]) << 6 | base64_to_binary(input[3]);

    input += 4;
    output += 3;

  }
  switch(output_length % 3) {
    case 1:
      output[0] = base64_to_binary(input[0]) << 2 | base64_to_binary(input[1]) >> 4;
      break;

    case 2:
      output[0] = base64_to_binary(input[0]) << 2 | base64_to_binary(input[1]) >> 4;
      output[1] = base64_to_binary(input[1]) << 4 | base64_to_binary(input[2]) >> 2;
      break;
  }
  return output_length;
}