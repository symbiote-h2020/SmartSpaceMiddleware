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
	memset(_dk1, 0, sizeof(_dk1));
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
  Serial.print("\n"+label);
  Serial.print(" = {");
  for (uint8_t j = 0; j < len - 1; j++) {
      Serial.print(buff[j],HEX);
      Serial.print(", ");
   }
   Serial.print(buff[len - 1],HEX);
   Serial.println("}\n");
}


void lsp::calculateDK1(uint8_t num_iterations) {
	P("Start calculating DK1 key....");
	if (_kdf == "PBKDF2") {
		if (_cp == TLS_PSK_WITH_AES_128_CBC_SHA) {
			Serial.print("Size of PSK: ");
			P(_psk_len);
			printBuffer(_psk, _psk_len, "PSK");
			/*
			Serial.print("\nPSK");
		  	Serial.print(" = {");
		  	for (uint8_t j = 0; j < _psk_len-1; j++) {
		      Serial.print(_psk[j],HEX);
		      Serial.print(", ");
		   	}
		   	Serial.print(_psk[_psk_len-1],HEX);
		   	Serial.println("}\n");
			*/
			uint8_t salt[8];
			memset(salt, 0, sizeof(salt));
			uint32_t tmpnonce = ENDIAN_SWAP_32(0x98ec4);
			memcpy(salt, (uint8_t*)&tmpnonce, 4);
			tmpnonce = ENDIAN_SWAP_32(_GWNonce);
			memcpy(salt+4, (uint8_t*)&_GWNonce, 4);

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
	P("Start calculating DK2 key....");
	if (_kdf == "PBKDF2") {
		if (_cp == TLS_PSK_WITH_AES_128_CBC_SHA) {
			// size of salt should be 4 + 4 + 10 bytes
			uint8_t salt[8+(_psk_len/2)];
			memset(salt, 0, sizeof(salt));

			memcpy(salt, (uint8_t*)_psk, (_psk_len/2));

			uint32_t tmpnonce = ENDIAN_SWAP_32(0x98ec4);
			memcpy(salt+10, (uint8_t*)&tmpnonce, 4);
			tmpnonce = ENDIAN_SWAP_32(_GWNonce);
			memcpy(salt+14, (uint8_t*)&_GWNonce, 4);

			printBuffer(salt, sizeof(salt), "DK2salt");

			PBKDF2function( _psk, _psk_len, salt, sizeof(salt), _dk2, sizeof(_dk2), num_iterations );
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
	_root.prettyPrintTo(Serial);
	String mti = _root["mti"].as<String>();
	if (mti == STRING_MTI_GW_INK_HELLO) {
		// GWInnkeeperHello code, everything ok
		// get the crypto choice
		P("GOT MTI GW INK HELLO");
		String _cc = _root["cc"].as<String>();
		if (_cc != _cp) {
			P("Crypto Choice different from Crypto Proposal, process degraded, I continue usign CP");
		}
		_iv = _root["iv"].as<String>();
		_GWNonce = _root["nonce"].as<String>().toInt();
		if (_iv != "") {
			// we need to use the init vector for the key calculation
			P("Init vector found");
			_needInitVector == true;
		} else {
			P("Init vector not found");
			_needInitVector == false;
		}
		return COMMUNICATION_OK;
	} else if (mti == STRING_MTI_GW_INK_AUTHN) {
		P("GOT MTI GW INK AUTHN");
		return COMMUNICATION_ERROR;
	} else {
		P("ERR: enter default from elaborate InnkResp");
		return COMMUNICATION_ERROR;
	}
}


void lsp::begin() {
	randomSeed(analogRead(0));
	// setup the rest client and web server listen port/path
	_rest_client = new RestClient(INNKEEPER_LSP_URL, LSP_PORT);
  	_rest_client->setContentType("application/json");
}

uint8_t lsp::sendSDEVHelloToGW() {
	P("Enter sendSDEVHelloToGW");
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
		PI("Got this status code:");
		P(statusCode);
		if (statusCode < 300 and statusCode >= 200){
			statusCode = elaborateInnkResp(resp);
			if (statusCode < 300 and statusCode >= 200) return NO_ERROR_LSP;
				else return statusCode;
		} else return statusCode;		
	} else return ERROR_NOT_CONNECTED;
}

uint8_t lsp::sendAuthN(String clearData) {
	P("Enter sendAuthN");
	String resp = "";
	if (WiFi.status() == WL_CONNECTED) {
		_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.createObject();
		//initialize the sequence number
  		_sn = random(1000000);
  		PI("SN: ");
		P(_sn);
  		// use DK1 to encrypt
  		//uint8_t testdata[32] = {100,100,100,188,188,188,188,144,144,144,144,111,111,111,111,111,100,100,100,188,188,188,188,144,144,144,144,111,111,111,111,111};
  		char testdata[] = "this can be text of any length or information you want it to be";
  		String outdata;
  		String signedData;
  		//encryptData(testdata, outdata);
  		encryptDataAndSign(testdata, outdata, signedData);
  		PI("B64 encoded data(outside): ");
		P(outdata);
			_root["mti"] = STRING_MTI_SDEV_AUTHN;
			_root["sn"] = String(_sn);
			_root["authn"] = outdata;
				// use DK2 to sign
			_root["sign"] = signedData;
		String temp = "";
		P("Send this JSON:");
		_root.prettyPrintTo(Serial);
		_root.printTo(temp);
		int statusCode = _rest_client->post(LSP_PATH, temp.c_str(), &resp);
		if (statusCode < 300 and statusCode >= 200){
			_sn++;
			statusCode = elaborateInnkResp(resp);
			if (statusCode < 300 and statusCode >= 200) return NO_ERROR_LSP;
				else return statusCode;
		} else return statusCode;		
	} else return ERROR_NOT_CONNECTED;
}

void lsp::bufferSize(char* text, int &length) {
	int i = strlen(text);
	int buf = round(i / BLOCK_SIZE) * BLOCK_SIZE;
	length = (buf < i) ? buf + BLOCK_SIZE : length = buf;
}


void lsp::encrypt(char* plain_text, String& output, int length) {
	
	byte enciphered[length];
	uint8_t iv[16] = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
	AES aesEncryptor(_dk1, iv, AES::AES_MODE_128, AES::CIPHER_ENCRYPT);
	aesEncryptor.process((uint8_t*)plain_text, enciphered, length);
	int encrypted_size = sizeof(enciphered);
	printBuffer((uint8_t*)enciphered, encrypted_size, "EncrypData");
	base64 b64enc;
	String encoded = b64enc.encode(enciphered, encrypted_size, false);
	output = encoded;
}

void lsp::encryptAndSign(char* plain_text, String& output, int length, String& signature) {
	
	byte enciphered[length];
	uint8_t iv[16] = {1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1};
	AES aesEncryptor(_dk1, iv, AES::AES_MODE_128, AES::CIPHER_ENCRYPT);
	aesEncryptor.process((uint8_t*)plain_text, enciphered, length);
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
	the base64 rapresentation of the encrypted data
*/
void lsp::encryptData(char* plain_text, String& output) {
	int length = 0;
	String dataToEncrypt = String(plain_text) + String(_sn, HEX);
	PI("This is what I'm going to ENCRYPT: ");
	P(dataToEncrypt);
	//bufferSize(plain_text, length);
	bufferSize((char*)dataToEncrypt.c_str(), length);
	//char encrypted[length];
	String encrypted;
	///encrypt(plain_text, encrypted, length);
	encrypt((char*)dataToEncrypt.c_str(), encrypted, length);
	output = encrypted;	
	//PI("B64 encoded data(intermediate): ");
	//P(encrypted);
}

/*
	Encrypts with DK1 (data || <sequence_number>) and returns
	the base64 rapresentation of the encrypted data and the b64 rapresentation of the signed 
	SHA1-HMAC_dk2(ENC_DATA|| <sequence_number>) 
*/
void lsp::encryptDataAndSign(char* plain_text, String& output, String& signature) {
	int length = 0;
	String dataToEncrypt = String(plain_text) + String(_sn, HEX);
	PI("This is what I'm going to ENCRYPT: ");
	P(dataToEncrypt);
	bufferSize((char*)dataToEncrypt.c_str(), length);
	//char encrypted[length];
	String encrypted;
	String tmpSign;
	///encrypt(plain_text, encrypted, length);
	encryptAndSign((char*)dataToEncrypt.c_str(), encrypted, length, tmpSign);
	output = encrypted;	
	signature = tmpSign;
	//PI("B64 encoded data(intermediate): ");
	//P(encrypted);
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
	//PI("B64 signed data: ");
	//P(encoded);
	output = encoded;
}