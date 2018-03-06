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

#ifndef SYM_SECURITY_H
#define SYM_SECURITY_H

#include <Arduino.h>
#include <ArduinoJson.h>
#include <RestClient.h>
#include <Crypto.h>
#include <Hash.h>
#include <sha1.h> // please be sure to use the forked version at https://github.com/bbx10/Cryptosuite
#include "base64.h"
#include <EEPROM.h>


#ifndef DEBUG_SYM_CLASS
#define DEBUG_SYM_CLASS 1
#if DEBUG_SYM_CLASS == 1
  // Print debug with carriege return
  #define P(__VA_ARGS__) Serial.println(__VA_ARGS__);
  // Print Inline without carriege return
  #define PI(__VA_ARGS__) Serial.print(__VA_ARGS__);
#else
  #define P(__VA_ARGS__)
  #define PI(__VA_ARGS__)
#endif 
#endif

#define FLASH_MEMORY_RESERVATION	512
#define FLASH_LSP_START_ADDR		0
// thought to be a 4 bytes identifier and 12 HEX byte
// like this: sym-00112233445566778899aabb
#define FLASH_LSP_START_SSPID		0
#define FLASH_LSP_END_SSPID			31
// should be 16 byte
#define FLASH_LSP_START_PREV_DK1	32
#define FLASH_LSP_END_PREV_DK1		47


#define ENDIAN_SWAP_32(l) ((l>>24) |((l>>16)<<8)&0xff00 | ((l>>8)<<16)&0xff0000 | (l << 24)) 

#define BLOCK_SIZE 16
#define AES_KEY_LENGTH 16
#define DK2_KEY_LENGTH 16
#define HMAC_DIGEST_SIZE    20
//#define SECURITY_JSON_SIZE 500
#define SECURITY_JSON_SIZE 400

#define NUM_ITERATIONS	1
#define NUM_Ti 1
//TODO FIXME: change static asdsignment, allocation should be done in dinamic mode
// depending on the 
#define NUM_Ti_BYTES 20

#define SHA256_KEY_SIZE SHA256_SIZE
#define SHA1_KEY_SIZE 20

#define DEFAULT_IV "1111111111111111"

#define MSK_SIZE	10
#define PSK_HASH_INPUT_SIZE (MSK_SIZE + 6)

// FOLLOWING DEFINE FOR Crypto Proposal
/*
	where PSK is used to set a pre-shared key, AES as symmetric 
	encryption algorithm with a 128 bit key, SHA256 as a 
	pseudorandom function (PRF) based on HMAC with the SHA-256 
	hash function
*/
#define TLS_PSK_WITH_AES_128_GCM_SHA256  		"0x00a8"


/*
	where PSK is used to set a pre-shared key, CHACHA20 as symmetric
	cipher with a 256 bit key, POLY1305 as a message authentication 
	code that requires a 256 bit key and a message and produces 
	a 128 bit tag
*/
#define TLS_PSK_WITH_CHACHA20_POLY1305_SHA256 	"0xccab"


/*
	where PSK is used to set a pre-shared key, AES as symmetric 
	encryption algorithm with a 128 bit key, SHA as a pseudorandom 
	function (PRF) based on HMAC with the SHA hash function or 
	as a message authentication code algorithm

	PRF = SHA1-HMAC 
*/
#define TLS_PSK_WITH_AES_128_CBC_SHA 			"0x008c"


/*
	which requires that the GW/INK  certificate’s contain an ECDH-capable 
	public key signed with ECDSA (both device and Gateway/Innkeeper 
	perform an ECDH operation and use the resultant shared secret as 
	the premaster secret), AES as symmetric encryption algorithm with 
	a 128 bit key, SHA as a pseudorandom function (PRF) based on HMAC 
	with the SHA hash function or as a message authentication code algorithm
*/
#define TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA 	"0xc004"


/*
	which requires that the GW/INK  certificate’s contain an ECDH-capable 
	public key signed with ECDSA (both device and GW/Innkeeper perform 
	an ECDH operation and use the resultant shared secret as the premaster secret), 
	AES as symmetric encryption algorithm with a 256 bit key, SHA as a 
	pseudorandom function (PRF) based on HMAC with the SHA hash function 
	or as a message authentication code algorithm
*/
#define TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA 	"0xC005"


// Message Type Indicator (MTI)
#define STRING_MTI_SDEV_HELLO 		"0x10"
#define STRING_MTI_GW_INK_HELLO 	"0x20"
#define STRING_MTI_SDEV_AUTHN 		"0x30"
#define STRING_MTI_GW_INK_AUTHN 	"0x40"
#define STRING_MTI_SDEV_DATA_UPLINK	"0x50"
#define STRING_MTI_GW_DATA_DOWNLINK	"0x60"

// Exit status code
#define ERROR_NOT_CONNECTED 0x55
#define NO_ERROR_LSP 		0x01
#define COMMUNICATION_ERROR	0x10
#define COMMUNICATION_OK	0xEE
#define JSON_PARSE_ERR		0xED



// Network stuff
#define INNKEEPER_LSP_URL "192.168.97.105"
#define LSP_PORT 8080
#define LSP_PATH "/innkeeper/registry"


class lsp {
public:

	lsp(char* cp, char* kdf, uint8_t* psk, uint8_t psk_len);
	~lsp();
	void begin(String SSPId);
	void getContextFromFlash();
	void saveContextInFlash();
	void calculateDK1(uint8_t num_iterations);
	void calculateDK2(uint8_t num_iterations);
	void PBKDF2function(uint8_t *pass, uint32_t pass_len, uint8_t *salt, uint32_t salt_len,uint8_t *output, uint32_t key_len, uint32_t rounds );
	uint8_t elaborateInnkResp( String& resp);
	uint8_t sendSDEVHelloToGW();
	void sendHelloToGW();
	void printBuffer(uint8_t* buff, uint8_t len, String label);
	void createAuthNPacket(uint8_t* dataout);
	uint8_t sendAuthN();
	String getSessionId();
	String getDK1();
	String getHashOfIdentity(String id);
	void cryptData(String in, String& out);
	/*
		Convert data b64 crypted data to plain text
	*/
	void decryptData(String in, String& out);
	void encryptDataAndSign(char* plain_text, String& output, String& signature);
	void signData(uint8_t* data, uint8_t data_len, String& output);
	bool decryptAndVerify(String authn, String& decrypted, String GWsigned);
	void decrypt(unsigned char* crypted, String& output);

/* decode_base64:

 *   Description:

 *     Converts a base64 null-terminated string to an array of bytes

 *   Parameters:

 *     input - Pointer to input string
 *     output - Pointer to output array

 *   Returns:

 *     Number of bytes in the decoded binary

 */
	unsigned int decode_base64(unsigned char input[], unsigned char output[]);


private:
	void bufferSize(char* text, int &length);
	void bufferSize(unsigned char* text, int &length);
	void encryptAndSign(char* plain_text, String& output, int length, String& signature);
	//void encrypt(char* plain_text, String& output, int length);
	unsigned char base64_to_binary(unsigned char c);
	unsigned int decode_base64_length(unsigned char input[]);
	uint32_t HEX2Int(String in);


	StaticJsonBuffer<SECURITY_JSON_SIZE> _jsonBuff;
	RestClient* _rest_client;

	bool _needInitVector;
	String _iv;
	uint32_t _SDEVNonce;
	uint32_t _GWNonce;
	uint32_t _sn;
	uint8_t _SDEVmac[6];
	String _lastSSPId;
	String _currentSSPId;
	String _kdf;
	String _cp;
	String _sessionId;
	uint8_t *hmac;
	Sha1Class sha1;

	uint8_t _dk1[AES_KEY_LENGTH];
	uint8_t _prevDk1[AES_KEY_LENGTH];
	uint8_t _dk2[DK2_KEY_LENGTH];
	uint8_t* _psk;
	uint8_t _psk_len;

};


#endif // SYM_SECURITY_H
