/*******************************************************************************
* sym-agent Library
* Version: 0.1
* Date: 25/07/2017
* Author: Unidata
* Company: Unidata
*
* This is a lightweight symbiote agent implementation for Arduino
*
*
* Revision  Description
* ========  ===========
* 
* 0.1      Initial release
*******************************************************************************/

#include "sym_agent.h"

volatile boolean keepAlive_triggered = false;
volatile unsigned long keep_alive_interval = 0;
uint8_t ppsk[HMAC_DIGEST_SIZE] = {0x46, 0x72, 0x31, 0x73, 0x80, 0x52, 0x78, 0x92, 0x52, 0x81, 0xad, 0xd7, 0x57, 0x2c, 0x04, 0xa5, 0xdd, 0x84, 0x16, 0x68};

void keepAliveISR(void){
	//KEEPALIVE_LED_TOGGLE
	keepAlive_triggered = true;
	volatile unsigned long next;
	noInterrupts();
	  		//set the keep_alive interval. The value is in msec
	#warning "fixthis in keepAliveISR"
	next=ESP.getCycleCount() + keep_alive_interval;
	timer0_write(next);
	interrupts();
}

symAgent::symAgent()
{

}

symAgent::symAgent(unsigned long keep_alive, String description, bool isRoaming)
{
	pinMode(JOIN_LED, OUTPUT);
	pinMode(KEEPALIVE_LED, OUTPUT);
	JOIN_LED_OFF
	KEEPALIVE_LED_OFF
		//set internal keep alive interval to the correct value to be used in the timer0_write()
	_keep_alive = keep_alive * TICK_MILLISECONDS;
		// change the value of the global variable keep_alive_interval accordingly
	keep_alive_interval = _keep_alive;
	_description = description;
	_security = new lsp(TLS_PSK_WITH_AES_128_CBC_SHA ,"PBKDF2", ppsk, HMAC_DIGEST_SIZE);
	_roaming = isRoaming;
	_server = new ESP8266WebServer();
	_regExpiration = 0; 
	_subscribe = false;
	_firstTimeEverConnect = true;
}

symAgent::symAgent(unsigned long keep_alive, String description, bool isRoaming, Semantic* semantic)
{
	pinMode(JOIN_LED, OUTPUT);
	pinMode(KEEPALIVE_LED, OUTPUT);
	JOIN_LED_OFF
	KEEPALIVE_LED_OFF
		//set internal keep alive interval to the correct value to be used in the timer0_write()
	_keep_alive = keep_alive * TICK_MILLISECONDS;
		// change the value of the global variable keep_alive_interval accordingly
	keep_alive_interval = _keep_alive;
	_description = description;
	_security = new lsp(TLS_PSK_WITH_AES_128_CBC_SHA ,"PBKDF2", ppsk, HMAC_DIGEST_SIZE);
	_roaming = isRoaming;
	_server = new ESP8266WebServer();
	_semantic = semantic;
	_regExpiration = 0; 
	_subscribe = false;
	_firstTimeEverConnect = true;
}



String symAgent::getSymIdFromFlash() {
	String tmpID = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		tmpID += String((char)EEPROM.read(i));
	}
	PI("Read this SYM-ID from flash: ");
	P(tmpID);
	EEPROM.end();
	if (tmpID != "ffffffffffffffffffffffffffffffffffffffffffffffff") {
		//valid sym-id
		P("Valid sym-id!");
		return tmpID;
	} else {
		P("No symId found in flash");
		return "";
	}
}

void symAgent::saveIdInFlash() {
	P("SAVE SYM-ID IN FLASH");
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	uint8_t j = 0;
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		EEPROM.write(i, _symId[j]);
		j++;
	}
	EEPROM.commit();
	EEPROM.end();
#ifdef DEBUG_SYM_CLASS
	String tmpID = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		tmpID += String(EEPROM.read(i), HEX);
	}
	PI("Read back SYM-ID from flash: ");
	P(tmpID);
	PI("What I expect:");
	P(_symId);
	EEPROM.end();
#endif
}

void symAgent::forceSymIdInFlash(String value) {
	P("FORCE A VALUE SYM-ID in flash");
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	uint8_t j = 0;
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		EEPROM.write(i, value[j]);
		j++;
	}
	EEPROM.commit();
	EEPROM.end();
#ifdef DEBUG_SYM_CLASS
	String tmpID = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		tmpID += String(EEPROM.read(i), HEX);
	}
	PI("Read back SYM-ID from flash: ");
	P(tmpID);
	PI("What I expect:");
	P(_symId);
	EEPROM.end();
#endif
}


symAgent::~symAgent()
{

}

boolean symAgent::connect(String ssid, String psw){
	int count = 60;
	WiFi.begin(ssid.c_str(), psw.c_str());
  	while (WiFi.status() != WL_CONNECTED) {
  		JOIN_LED_TOGGLE
		delay(200);
		PI(".");
		count--;
		if (count == 0) {
			JOIN_LED_OFF
			return false;
		} 
	}
	JOIN_LED_OFF
	return true; 
}

// callback function to handle the resource request from the ssp
boolean symAgent::elaborateQuery()
{
	P("RESOURCE-QUERY");
	String resp = _server->arg(0);
	_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.parseObject(resp);
		if (!_root.success()) {
    		P("parseObject() failed");
    		return false;
		}
#if DEBUG_SYM_CLASS == 1
		_root.prettyPrintTo(Serial);
		P(" ");
#endif
		/*
			Parse a JSON like this:
			== GET ==
			{
			  "resourceInfo": [
			    {
			      "sspIdResource": "1",
			      "internalIdResource": "5c:cf:7f:3a:6b:76",
			      "symIdParent": "pippo",
			      "sspIdParent": "1",
			      "session_expiration": 1522854128610,
			      "observedProperties": [
			        "temperature",
			        "pressure"
			      ]
			    }
			  ],
			  "type": "GET"
			} 

			== SET ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "{symbioteId}",
			    "internalId" : "{internalId}",
			    "type" : "{Model}"
			  } ],
			  "body" : {
			    "{capability}": [
			      { "{restriction}": "{value}" }
			    ] 
			  },
			  "type" : "SET"
			}

			== HISTORY ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "EnvSensor"
			  }, {
			    "type" : "Observation"
			  } ],
			  "filter" : {
			    "type" : "expr",
			    "param" : "temperature",
			    "cmp" : "EQ",
			    "val" : "20"
			  },
			  "type" : "HISTORY"
			}

			== SUBSCRIBE ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "Light"

			  }, {
			    "type" : "Observation"
			  } ],
			  "type" : "SUBSCRIBE"
			}

			== UNSUBSCRIBE ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "Light"

			  }, {
			    "type" : "Observation"
			  } ],
			  "type" : "UNSUBSCRIBE"
			}
		*/
		String type = _root["type"].as<String>();
		if (_root["resourceInfo"][0]["internalIdResource"] == _internalId ) {
			if (type == "SET") setResource(resp);
			else if (type == "GET") getResource();
			else if (type == "HISTORY") getResource();
			else if (type == "SUBSCRIBE") subscribe();
			else if (type == "UNSUBSCRIBE") unsubscribe();
			else {
				P("Wrong TYPE");
				String tmpResp = "{ \"id\":\"" + _symId + "\", \"value\":\"WrongType\"}";
				_server->send(200, "application/json", tmpResp);
				return false;
			}
		} else {
			P("Wrong SymId");
			String tmpResp = "{ \"id\":\"" + _symId + "\", \"value\":\"WrongSymId\"}";
			_server->send(200, "application/json", tmpResp);
			return false;
		}
	return true;
}
////////////////////////////////////////////////////////////////////////
/// test function to delete
// callback function to handle the resource request from the ssp
boolean symAgent::TestelaborateQuery(String resp)
{
	P("TEST-RESOURCE-QUERY");
	_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.parseObject(resp);
		if (!_root.success()) {
    		P("parseObject() failed");
    		return false;
		}
#if DEBUG_SYM_CLASS == 1
		_root.prettyPrintTo(Serial);
		P(" ");
#endif
		/*
			Parse a JSON like this:
			== GET ==
			{
  				"resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "Light"

			  }, {
			    "type" : "Observation"
			  } ],
			  "type" : "GET"
			}

			== SET ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "{symbioteId}",
			    "internalId" : "{internalId}",
			    "type" : "{Model}"
			  } ],
			  "body" : {
			    "{capability}": [
			      { "{restriction}": "{value}" }
			    ] 
			  },
			  "type" : "SET"
			}

			== HISTORY ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "EnvSensor"
			  }, {
			    "type" : "Observation"
			  } ],
			  "filter" : {
			    "type" : "expr",
			    "param" : "temperature",
			    "cmp" : "EQ",
			    "val" : "20"
			  },
			  "type" : "HISTORY"
			}

			== SUBSCRIBE ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "Light"

			  }, {
			    "type" : "Observation"
			  } ],
			  "type" : "SUBSCRIBE"
			}

			== UNSUBSCRIBE ==
			{
			  "resourceInfo" : [ {
			    "symbioteId" : "abcdefgh",
			    "internalId" : "123456",
			    "type" : "Light"

			  }, {
			    "type" : "Observation"
			  } ],
			  "type" : "UNSUBSCRIBE"
			}
		*/
		String type = _root["type"].as<String>();
		//if (_root["symbioteId"] == _symId) {
			if (type == "SET") setResource(resp);
			else if (type == "GET") getResource();
			else if (type == "HISTORY") getResource();
			else if (type == "SUBSCRIBE") subscribe();
			else if (type == "UNSUBSCRIBE") unsubscribe();
			else {
				P("Wrong TYPE");
				String tmpResp = "{ \"id\":\"" + _symId + "\", \"value\":\"WrongType\"}";
				//_server->send(200, "application/json", tmpResp);
				return false;
			}
	return true;
}

void symAgent::setResource(String rapRequest) {
	P("SET RESOURCE");
	_jsonBuff.clear();
		JsonObject& _root = _jsonBuff.parseObject(rapRequest);
		if (!_root.success()) {
    		P("parseObject() failed");
    		return;
		}
#if DEBUG_SYM_CLASS == 1
		_root.prettyPrintTo(Serial);
		P(" ");
#endif
		if (_root["resourceInfo"][0]["internalIdResource"] == _internalId) {
			JsonObject& capNameJson = _root["body"];
					// return somwthing like "RGBCapability"
			String capNameString = capNameJson.begin()->key;
			uint8_t capabilityIndex = 0;
				//now search the index of the capability with that name
			for (uint8_t i = 0; i < _semantic->getCapabilityNum(); i++) {
				if (_semantic->getCapabilityName(i) == capNameString) capabilityIndex = i;
			}
			for( uint8_t j = 0; j< 3; j++) {
					// attach to the head of the JSON to the inside array position
				JsonObject& insideCapabilityJSON = _root["body"][capNameString][j];
					// point the first two element key-value of the array
				JsonObject::iterator it = insideCapabilityJSON.begin();
				String propName = it->key;
				uint8_t propValue = it->value;
					PI("Actuating: ");
					PI(capNameString);
					PI(" => ");
					PI(propName);
					PI(" : ");
					PI(propValue);
				if (_semantic->actuateParameterOfCapability(capabilityIndex, propName, propValue)) {
					P(" OK");
				} else {
					P("KO");
				}
			}
			String tmpResp = "{ \"result\":\"done\"}";
			_server->send(200, "application/json", tmpResp);
		} else {
			PI("Mismatch in symId.\n*What I got*\nSym-Id:\t");
			P(_root["resourceInfo"][0]["symbioteId"].as<String>());
			PI("InternalId:\t");
			P(_root["resourceInfo"][0]["internalId"].as<String>());
			PI("*What I expect*\nSym-Id:\t");
			P(_symId);
			PI("InternalId:\t");
			P(_internalId);
			P("Wrong SymId");
			String tmpResp = "{ \"internalIdResource\":\"" + _internalId + "\", \"value\":\"WrongSymId\"}";
			_server->send(200, "application/json", tmpResp);
			return;
		}
	return;
}

void symAgent::subscribe()
{
	_subscribe = true;
}

void symAgent::unsubscribe()
{
	_subscribe = false;
}

void symAgent::getResource() {
	// push resource to RAP
	// right now push all the resources
	P("GET RESOURCE");
	int res_index = 0;
			DynamicJsonBuffer dinamicJsonBuffer;
				//create main array
			JsonArray& root = dinamicJsonBuffer.createArray();
			while (res_index < _semantic->getObsPropertyNum()) {
					// this return something like "33 °C"
				String tmpString = _semantic->getObsPropertyValue(res_index);
					//create the nested object for each resource
				JsonObject& root_internal = root.createNestedObject();
					//this save only the value before the " ", so in this case "33"
				root_internal["value"] = tmpString.substring(0, tmpString.indexOf(" "));
				JsonObject& obsProperty = root_internal.createNestedObject("obsProperty");
					obsProperty["@c"] = ".Property";
					obsProperty["name"] = _semantic->getObsPropertyName(res_index);
					obsProperty["description"] = "";
				JsonObject& uom = root_internal.createNestedObject("uom");
					uom["@c"] = "UnitOfMeasurment";
					uom["symbol"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["name"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["description"] = "";
	#if DEBUG_SYM_CLASS == 1
					P(" ");
					root.prettyPrintTo(Serial);
					P(" ");
	#endif
				res_index++;
			}
			String resp = "";
			root.printTo(resp);
			resp = "\r\n" + resp;
			P("\n*************\nPACKET SENT TO RAP:");
#if DEBUG_SYM_CLASS == 1
		root.prettyPrintTo(Serial);
		P(" ");
#endif
			//P("Print packet as plain-text:");
			//P(resp);
			_server->send(200, "application/json", resp);
			dinamicJsonBuffer.clear();
}

// search for well-known symbiotic ssid and try to connect to it.
// return true if found a symbiotic ssid and so ssp and connect to it, false otherwise
boolean symAgent::begin()
{
	P("BEGIN");
			// ssp-id based on a id sent from the innkeeper.
	int networksFound = WiFi.scanNetworks();
	for (int i = 0; i < networksFound; i++) {
		P(WiFi.SSID(i));
		if (WiFi.SSID(i).startsWith("sym-") == true ){
				// this could be a symbiotic ssid
  				// calculate the psw  				
			String wifi_psw = calculateWifiPSW(WiFi.SSID(i));
			P("WiFi psw: "+ wifi_psw);
			if (connect(WiFi.SSID(i), wifi_psw)){
				P("Connected!");
				_wifi_psw = wifi_psw;
				_wifi_ssid = WiFi.SSID(i);
					// get as ssp identifier the symbiotic code exposed by wifi ssid
				_sspId = WiFi.SSID(i);
				byte mac[6];
				WiFi.macAddress(mac);
  						// write the agent id based on the union of MAC + SSPID
  						// EG: if the ssid is "sym-2e4467f2a7b03255a2a4" and the MAC is 55:ee:45:ef:51:01 
  						//then the _symId is "55ee45ef51012e4467f2a7b03255a2a4"
				for (int j = 0; j < 6; j++) {
					if (mac[j] < 16) {
						_mac += "0";
					} 
					_mac += String(mac[j], HEX);
					if (j!=5) _mac += ":";
				}
				_mac = _mac.substring(0, 17);
				//create a new http client class 
				_rest_client = new RestClient(JOIN_URL, SSP_PORT);
				_rest_client->setContentType("application/json");
				PI("Got this IDs:\n\tsspId: ");
				P(_sspId);
				PI("\tid: ");
				P(_symId);
				PI("\tmac: ");
				P(_mac);
				P("-------------");

				_server->on("/rap/v1/request", [this](){
				P("RESOURCEHANDLER");
					String message = "Args found:\n";
					for (uint8_t i=0; i < _server->args(); i++){
					   message += " " + _server->argName(i) + ": " + _server->arg(i) + "\n";
					}
					elaborateQuery();
			    });
				_server->onNotFound([this](){
					P("NOTFOUND");
					String message = "File Not Found\n\n";
					message += "URI: ";
					message += _server->uri();
					message += "\nMethod: ";
					message += (_server->method() == HTTP_GET)?"GET":"POST";
					message += "\nArguments: ";
					message += _server->args();
					message += "\n";
					for (uint8_t i=0; i<_server->args(); i++){
					  message += " " + _server->argName(i) + ": " + _server->arg(i) + "\n";
					}
					P(message)
					_server->send(404, "text/plain", String("Not Found :("));
			    });
				_server->begin();
				// initialize the security part
				_security->begin(_sspId);
				_security->sendSDEVHelloToGW();
				_security->calculateDK1(4);
				_security->calculateDK2(4);
				_security->sendAuthN();
  				return true;
  			}
  		}
  	}
  	// return that it not found a symbiotic ssid
  	return false;	
}

	//join the ssp, return a valid join response struct if success
int symAgent::registry()
{
	P("REGISTRY");
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	/*
		Create a JSON like this:
		{
		   String symIdSDEV,
		   String pluginId,
		   String pluginURL,
		   String dk1,
		   String hashField
 		}
	*/
 		//read from flash if there is stored a valid symId
	//_symId = getSymIdFromFlash();
 	_symId = ""; ///// TODO FIXME
	if (_symId == "") _firstTimeEverConnect = true;
	else _firstTimeEverConnect = false;
	//_firstTimeEverConnect = false;
	_root["symId"] = _symId;
	_root["pluginId"] = _mac;
	_root["sspId"] = "";
	_root["roaming"] = false;
	_internalId = _mac;
	String urlString = "http://" + String(WiFi.localIP()[0]) + "." + String(WiFi.localIP()[1]) + "." + String(WiFi.localIP()[2]) + "." + String(WiFi.localIP()[3]) + "/rap/v1/request";
	_root["pluginURL"] = urlString;
	_semantic->setURL(urlString);
	_root["dk1"] = _security->getDK1();
		//Regarding the hashField could be (i) all 0 when the SDEV joins for the first time
		// or (ii) hashField = H(sym-id || previous dk1)
	_root["hashField"] = _security->getHashOfIdentity(_symId);
	String tempClearData = "";
	String tempCryptData = "";
	String tempJsonPacket = "";
	String resp = "";
	_root.printTo(tempClearData);
	P("Registry message (CLEAR-DATA): ");
#if DEBUG_SYM_CLASS == 1
	_root.prettyPrintTo(Serial);
	P(" ");
#endif

	/*
		create a JSON like this:
		{
			"mti": 0x50,
			"sessionId": <value>,
			"data": "encryptJoinJSON"
		}
	*/
			// crypt the data using the cryptosuite from lightweightsecurity protocol
	_security->cryptData(tempClearData, tempCryptData);

	_jsonBuff.clear();
	JsonObject& jsonCrypt = _jsonBuff.createObject();
	jsonCrypt["mti"] = STRING_MTI_SDEV_DATA_UPLINK;
	jsonCrypt["sessionId"] = _security->getSessionId();
	jsonCrypt["data"] = tempCryptData;
	jsonCrypt.printTo(tempJsonPacket);

	tempJsonPacket = "\r\n" + tempJsonPacket;
	_rest_client->setContentType("application/encrypted");
	int statusCode = _rest_client->post(REGISTRY_PATH, tempJsonPacket.c_str(), &resp);
	P("Registry message (SDEVP): ");
#if DEBUG_SYM_CLASS == 1
	jsonCrypt.prettyPrintTo(Serial);
	P(" ");
#endif
	PI("Status code from server: ");
  	P(statusCode);
	if (statusCode < 300 and statusCode >= 200){
		//got a valid response
		_jsonBuff.clear();
		//remove any additional byte from response like dimension of the buffer
		resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
		JsonObject& _rootCryptResp = _jsonBuff.parseObject(resp);
		if (!_rootCryptResp.success()) {
    		P("parseObject() failed");
    		return ERR_PARSE_JSON;
		}
#if DEBUG_SYM_CLASS == 1
		_rootCryptResp.prettyPrintTo(Serial);
		P("");
#endif
		_security->decryptData( _rootCryptResp["data"].as<String>(), tempClearData);

		_jsonBuff.clear();
		JsonObject& _rootClearResp = _jsonBuff.parseObject(tempClearData);
		if (!_rootCryptResp.success()) {
    		P("parseObject() failed");
    		return ERR_PARSE_JSON;
		}
#if DEBUG_SYM_CLASS == 1
		P("Decrypted JSON");
		_rootClearResp.prettyPrintTo(Serial);
		P("");
#endif
		 if ((_rootClearResp["result"].as<String>() == "REJECTED")) {
			//kick away from the SSP
			P("JOIN REJECTED");
			return ERR_KICKED_OUT_FROM_JOIN;
		} else if (_rootClearResp["result"].as<String>() == "OK" || _rootClearResp["result"].as<String>() == "ALREADY_REGISTERED" || _rootClearResp["result"].as<String>() == "OFFLINE") {
			P("JOIN OK");
			_regExpiration = _rootClearResp["registrationExpiration"].as<unsigned int>();
			if (_symId == "" || _symId == _rootClearResp["symId"].as<String>()) {
				// everything ok
				P("JOIN SYMID OK");
				_symId = _rootClearResp["symId"].as<String>();
				_symIdInternal = _rootClearResp["sspId"].as<String>();
			} else {
				P("JOIN MISMATCH");
				return ERR_SYMID_MISMATCH_FROM_JOIN;
			}
		} else {
			P("JOIN RESPONSE UNKNOWN");
			return ERR_UNKNOWN_RESPONSE_FROM_JOIN;
		}
	} else {
			//error response from server
			P("GOT http error code from INNK");
			_jsonBuff.clear();
			//remove any additional byte from response like dimension of the buffer
			resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
			JsonObject& _root2 = _jsonBuff.parseObject(resp);
			if (!_root2.success()) {
	    		P("parseObject() failed");
	    		return ERR_PARSE_JSON;
			}
#if DEBUG_SYM_CLASS == 1
			_root2.prettyPrintTo(Serial);
			P("");
#endif
	}
	if (_keep_alive > 0) {
		//set up keepalive
		volatile unsigned long next;
		noInterrupts();
	  	timer0_isr_init();
	  	timer0_attachInterrupt(keepAliveISR);
	  		//set the keep_alive interval. The value is in msec
	  	next=ESP.getCycleCount() + _keep_alive;
	  	timer0_write(next);
	  	interrupts();
	}
	if (_roaming) {
		P("ROAMING SDEV, SAVE CONTEXT IN FLASH");
		_security->saveContextInFlash();
			// save the symbiote-ID in flash
		saveIdInFlash();
	}
	return statusCode;
}

// Unregistry the SDEV from the SSP, this also delete 
// all the entry in the DB of the innkeeper
int symAgent::unregistry()
{
	P("UNREGISTRY");
			_jsonBuff.clear();
			JsonObject& _root = _jsonBuff.createObject();
			/*
				Create a JSON like this:
				{
				   String sspId
		 		}
			*/
			_root["sspId"] = _symIdInternal;
			String tempClearData = "";
			String tempCryptData = "";
			String tempJsonPacket = "";
			String resp = "";
			_root.printTo(tempClearData);
			P("Join message (CLEAR-DATA): ");
		#if DEBUG_SYM_CLASS == 1
			_root.prettyPrintTo(Serial);
			P(" ");
		#endif

			/*
				create a JSON like this:
				{
					"mti": 0x50,
					"sessionId": <value>,
					"data": "encryptJoinJSON"
				}
			*/
					// crypt the data using the cryptosuite from lightweightsecurity protocol
			_security->cryptData(tempClearData, tempCryptData);

			_jsonBuff.clear();
			JsonObject& jsonCrypt = _jsonBuff.createObject();
			jsonCrypt["mti"] = STRING_MTI_SDEV_DATA_UPLINK;
			jsonCrypt["sessionId"] = _security->getSessionId();
			jsonCrypt["data"] = tempCryptData;
			jsonCrypt.printTo(tempJsonPacket);

			tempJsonPacket = "\r\n" + tempJsonPacket;
			_rest_client->setContentType("application/encrypted");
			int statusCode = _rest_client->post(UNREGISTRY_PATH, tempJsonPacket.c_str(), &resp);
			P("UNREGISTRY message (SDEVP): ");
		#if DEBUG_SYM_CLASS == 1
			jsonCrypt.prettyPrintTo(Serial);
			P(" ");
		#endif
			PI("Status code from server: ");
		  	P(statusCode);
			return statusCode;
}

int symAgent::join()
{
	P("JOIN");
	if (_firstTimeEverConnect) {

		if (_semantic->isAnActuator()) {
			P("JOIN-ACTUATOR");
			// send the join request only if first time connected to a SSP
			// join the actuator resource to the SSP
			_jsonBuff.clear();
			JsonObject& _root = _jsonBuff.createObject();
					
		 		//read from flash if there is stored a valid symId
		 	_root["internalIdResource"] = _internalId;
		 	_root["sspIdResource"] = "";
			_root["sspIdParent"] = _symIdInternal;
			_root["symIdParent"] = _symId;
			_root["accessPolicy"] = (char*)NULL;
			_root["filteringPolicy"] = (char*)NULL;

			String tmpSemanticJsonString = createActuatorSemanticDescription();
			DynamicJsonBuffer dinamicJsonBuffer; 
			JsonObject& dinamicRoot = dinamicJsonBuffer.parseObject(tmpSemanticJsonString);
			if (!dinamicRoot.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
			_root["resource"] = dinamicRoot;

			String tempClearData = "";
			String tempCryptData = "";
			String tempJsonPacket = "";
			String resp = "";
			_root.printTo(tempClearData);
			P("Join message (CLEAR-DATA): ");
		#if DEBUG_SYM_CLASS == 1
			_root.prettyPrintTo(Serial);
			P(" ");
		#endif

			/*
				create a JSON like this:
				{
					"mti": 0x50,
					"sessionId": <value>,
					"data": "encryptJoinJSON"
				}
			*/
					// crypt the data using the cryptosuite from lightweightsecurity protocol
			_security->cryptData(tempClearData, tempCryptData);

			_jsonBuff.clear();
			JsonObject& jsonCrypt = _jsonBuff.createObject();
			jsonCrypt["mti"] = STRING_MTI_SDEV_DATA_UPLINK;
			jsonCrypt["sessionId"] = _security->getSessionId();
			jsonCrypt["data"] = tempCryptData;
			jsonCrypt.printTo(tempJsonPacket);

			tempJsonPacket = "\r\n" + tempJsonPacket;
			_rest_client->setContentType("application/encrypted");
			int statusCode = _rest_client->post(JOIN_PATH, tempJsonPacket.c_str(), &resp);
			P("Join message (SDEVP): ");
		#if DEBUG_SYM_CLASS == 1
			jsonCrypt.prettyPrintTo(Serial);
			P(" ");
		#endif
			PI("Status code from server: ");
		  	P(statusCode);
			if (statusCode < 300 and statusCode >= 200){
				//got a valid response
				_jsonBuff.clear();
					//remove any additional byte from response like dimension of the buffer
				resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
				JsonObject& _rootCryptResp = _jsonBuff.parseObject(resp);
				if (!_rootCryptResp.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				_rootCryptResp.prettyPrintTo(Serial);
				P("");
		#endif
				_security->decryptData( _rootCryptResp["data"].as<String>(), tempClearData);
				_jsonBuff.clear();
				JsonObject& _rootClearResp = _jsonBuff.parseObject(tempClearData);
				if (!_rootCryptResp.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				P("Decrypted JSON");
				_rootClearResp.prettyPrintTo(Serial);
				P("");
		#endif
				 if ((_rootClearResp["result"].as<String>() == "REJECTED")) {
					//kick away fro the SSP
					P("JOIN REJECTED");
					return ERR_KICKED_OUT_FROM_JOIN;
				} else if (_rootClearResp["result"].as<String>() == "OK" || _rootClearResp["result"].as<String>() == "ALREADY_REGISTERED" || _rootClearResp["result"].as<String>() == "OFFLINE") {
					P("JOIN OK");
						_sspIdActuatorResource = _rootClearResp["symIdResource"].as<String>();
						_symIdActuatorResource = _rootClearResp["symIdResource"].as<String>();
				} else {
					return ERR_UNKNOWN_RESPONSE_FROM_JOIN;
				}
			} else {
					//error response from server
					P("GOT http error code from INNK");
					_jsonBuff.clear();
					//remove any additional byte from response like dimension of the buffer
					resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
					JsonObject& _root2 = _jsonBuff.parseObject(resp);
					if (!_root2.success()) {
			    		P("parseObject() failed");
			    		return ERR_PARSE_JSON;
					}
		#if DEBUG_SYM_CLASS == 1
					_root2.prettyPrintTo(Serial);
					P("");
		#endif
			}
			dinamicJsonBuffer.clear();
			//return statusCode;
		}

		if (_semantic->isASensor()) {
			P("JOIN-SENSOR");
			// join the sensor resource to the SSP
			// send the join request only if first time connected to a SSP
			_jsonBuff.clear();
			JsonObject& _root = _jsonBuff.createObject();
			/*
				Create a JSON like this:
				{
				   String sspId, // internal Id valid for the SSP, look at this as a "local" symId
				   String symId, // symId of the container SDEV class
				   String internalIdResource,
				   String semanticDescription
		 		}
			*/
		 		//read from flash if there is stored a valid symId
		 	_root["internalIdResource"] = _internalId;
		 	_root["sspIdResource"] = "";
			_root["sspIdParent"] = _symIdInternal;
			_root["symIdParent"] = _symId;
			_root["accessPolicy"] = (char*)NULL;
			_root["filteringPolicy"] = (char*)NULL;

			String tmpSemanticJsonString = createSensorSemanticDescription();
			DynamicJsonBuffer dinamicJsonBuffer; 
			JsonObject& dinamicRoot = dinamicJsonBuffer.parseObject(tmpSemanticJsonString);
			if (!dinamicRoot.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
			_root["resource"] = dinamicRoot;

			String tempClearData = "";
			String tempCryptData = "";
			String tempJsonPacket = "";
			String resp = "";
			_root.printTo(tempClearData);
			P("Join message (CLEAR-DATA): ");
		#if DEBUG_SYM_CLASS == 1
			_root.prettyPrintTo(Serial);
			P(" ");
		#endif

			/*
				create a JSON like this:
				{
					"mti": 0x50,
					"sessionId": <value>,
					"data": "encryptJoinJSON"
				}
			*/
					// crypt the data using the cryptosuite from lightweightsecurity protocol
			_security->cryptData(tempClearData, tempCryptData);

			_jsonBuff.clear();
			JsonObject& jsonCrypt = _jsonBuff.createObject();
			jsonCrypt["mti"] = STRING_MTI_SDEV_DATA_UPLINK;
			jsonCrypt["sessionId"] = _security->getSessionId();
			jsonCrypt["data"] = tempCryptData;
			jsonCrypt.printTo(tempJsonPacket);

			tempJsonPacket = "\r\n" + tempJsonPacket;
			_rest_client->setContentType("application/encrypted");
			int statusCode = _rest_client->post(JOIN_PATH, tempJsonPacket.c_str(), &resp);
			P("Join message (SDEVP): ");
		#if DEBUG_SYM_CLASS == 1
			jsonCrypt.prettyPrintTo(Serial);
			P(" ");
		#endif
			PI("Status code from server: ");
		  	P(statusCode);
			if (statusCode < 300 and statusCode >= 200){
				//got a valid response
				_jsonBuff.clear();
				//remove any additional byte from response like dimension of the buffer
				resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
				JsonObject& _rootCryptResp = _jsonBuff.parseObject(resp);
				if (!_rootCryptResp.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				_rootCryptResp.prettyPrintTo(Serial);
				P("");
		#endif
				_security->decryptData( _rootCryptResp["data"].as<String>(), tempClearData);
				_jsonBuff.clear();
				JsonObject& _rootClearResp = _jsonBuff.parseObject(tempClearData);
				if (!_rootCryptResp.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				P("Decrypted JSON");
				_rootClearResp.prettyPrintTo(Serial);
				P("");
		#endif
				 if ((_rootClearResp["result"].as<String>() == "REJECTED")) {
					//kick away fro the SSP
					P("JOIN REJECTED");
					return ERR_KICKED_OUT_FROM_JOIN;
				} else if (_rootClearResp["result"].as<String>() == "OK" || _rootClearResp["result"].as<String>() == "ALREADY_REGISTERED" || _rootClearResp["result"].as<String>() == "OFFLINE") {
					P("JOIN OK");
						_sspIdSensorResource = _rootClearResp["sspIdResource"].as<String>();
						_symIdSensorResource = _rootClearResp["symIdResource"].as<String>();
				} else {
					return ERR_UNKNOWN_RESPONSE_FROM_JOIN;
				}
			} else {
					//error response from server
					P("GOT http error code from INNK");
					_jsonBuff.clear();
					//remove any additional byte from response like dimension of the buffer
					resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
					JsonObject& _root2 = _jsonBuff.parseObject(resp);
					if (!_root2.success()) {
			    		P("parseObject() failed");
			    		return ERR_PARSE_JSON;
					}
		#if DEBUG_SYM_CLASS == 1
					_root2.prettyPrintTo(Serial);
					P("");
		#endif
			}
			dinamicJsonBuffer.clear();
			return statusCode;
		}
	} else {
		P("NO JOIN REQUIRED, RESOURCE ALREADY IN CORE");
		return 200;
	}
}

String symAgent::createSensorSemanticDescription() {
	return _semantic->returnSensorSemanticString();
}

String symAgent::createActuatorSemanticDescription() {
	return _semantic->returnActuatorSemanticString();
}

      //set the keep alive interval for the agent
void symAgent::setKeepAlive(unsigned long keep_alive)
{
		//set internal keep alive interval to the correct value to be used in the timer0_write()
	_keep_alive = keep_alive * TICK_MILLISECONDS;
		// change the value of the global variable keep_alive_interval accordingly
	keep_alive_interval = _keep_alive;
}
      //get back the keep alive interval for the agent
unsigned long symAgent::getKeepAlive()
{
	return _keep_alive;
}


int symAgent::sendKeepAlive(String& response)
{
	P("KEEPALIVE");
	P("Remember to change if needed the meaning of id field");
	KEEPALIVE_LED_ON
	delay(50);
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	_root["sspId"] = _symIdInternal;

	String tempClearData = "";
	String tempCryptData = "";
	String resp = "";
	String tempJsonPacket = "";
	_root.printTo(tempClearData);
#if DEBUG_SYM_CLASS == 1
		_root.prettyPrintTo(Serial);
		P(" ");
#endif
	/*
		create a JSON like this:
		{
			"mti": 0x50,
			"sessionId": <value>,
			"data": "encryptJoinJSON"
		}
	*/
	// crypt the data using the cryptosuite from lightweightsecurity protocol
	_security->cryptData(tempClearData, tempCryptData);

	_jsonBuff.clear();
	JsonObject& jsonCrypt = _jsonBuff.createObject();
	jsonCrypt["mti"] = STRING_MTI_SDEV_DATA_UPLINK;
	jsonCrypt["sessionId"] = _security->getSessionId();
	jsonCrypt["data"] = tempCryptData;
	jsonCrypt.printTo(tempJsonPacket);

	tempJsonPacket = "\r\n" + tempJsonPacket;

	_rest_client->setContentType("application/encrypted");
	int statusCode = _rest_client->post(KEEPALIVE_PATH, tempJsonPacket.c_str(), &resp);
	PI("Status code from server: ");
  	P(statusCode);
	if (statusCode < 300 and statusCode >= 200){
		//got a valid response
				_jsonBuff.clear();
				//remove any additional byte from response like dimension of the buffer
				resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
				JsonObject& _rootCryptResp = _jsonBuff.parseObject(resp);
				if (!_rootCryptResp.success()) {
		    		P("parseObject() failed");
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				_rootCryptResp.prettyPrintTo(Serial);
				P("");
		#endif
				_security->decryptData( _rootCryptResp["data"].as<String>(), tempClearData);
				_jsonBuff.clear();
				JsonObject& _rootClearResp = _jsonBuff.parseObject(tempClearData);
				if (!_rootCryptResp.success()) {
		    		keepAlive_triggered = false;
		    		KEEPALIVE_LED_OFF
		    		return ERR_PARSE_JSON;
				}
		#if DEBUG_SYM_CLASS == 1
				P("Keep-Alive Response from INNK:");
				_rootClearResp.prettyPrintTo(Serial);
				P("");
		#endif
				if ((_rootClearResp["result"].as<String>() == "OK" || _rootClearResp["result"].as<String>() == "OFFLINE")) {
					//kick away fro the SSP
					bool somethingChanged = false;
					P("KEEP-ALIVE OK");
					if (_rootClearResp["result"].as<String>() == "OK") {
						// maybe the innkeeper wants to update our symId?
						JsonArray& updateSymIdArray = _rootClearResp["updatedSymId"];
						for (uint8_t i = 0; i < updateSymIdArray.size(); i++) {
							// scan the element of the array
							JsonObject& updatedSymIdJSON = updateSymIdArray[i];
							if (updatedSymIdJSON["symIdResource"] != "") {
								// a new symIdResource is going to be assigned
								String tmpSSPIdRes = updatedSymIdJSON["sspIdResource"].as<String>();
								if (_sspIdSensorResource == tmpSSPIdRes) {
									P("NEW SYMID FOUND!\nOLD\t=>\tNEW");
									PI(_symIdSensorResource);
									PI("\t=>\t");
									P(updatedSymIdJSON["symIdResource"].as<String>());
									_symIdSensorResource = updatedSymIdJSON["symIdResource"].as<String>();
									somethingChanged = true;
								}
								if (_sspIdActuatorResource == tmpSSPIdRes) {
									P("NEW SYMID FOUND!\nOLD\t=>\tNEW");
									PI(_symIdActuatorResource);
									PI("\t=>\t");
									P(updatedSymIdJSON["symIdResource"].as<String>());
									_symIdActuatorResource = updatedSymIdJSON["symIdResource"].as<String>();
									somethingChanged = true;
								}
							}
						}
						if (somethingChanged && _roaming) {
							// need to save the new symId in flash. Actually no. The only symId stored in flash is the symId of the container SDEV.
							// the other ResourceSymId are not used by the SDEV
						}
					}
				} else {
					P("KEEP-ALIVE KO");
				}
				if (_subscribe) {
					String rapData = getResourceAsString();
					statusCode = _rest_client->post(RAP_PATH, rapData.c_str(), &resp);
				}
				keepAlive_triggered = false;
				KEEPALIVE_LED_OFF
				return statusCode;
	} else {
		// http error code from innkeeper
		_jsonBuff.clear();
			//remove any additional byte from response like dimension of the buffer
		resp = resp.substring(resp.indexOf("{"), (resp.lastIndexOf("}") + 1 ));
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		keepAlive_triggered = false;
    		KEEPALIVE_LED_OFF
    		return statusCode;
		}
	#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
		P(" ");
	#endif
		//response = _root2["result"].as<String>();
		keepAlive_triggered = false;
		KEEPALIVE_LED_OFF
		return statusCode;
	}
	//never arrive here
	keepAlive_triggered = false;
	KEEPALIVE_LED_OFF
	return statusCode;
}

// create the JSON to pash data to RAP
String symAgent::getResourceAsString()
{
	P("GET RESOURCE");
	int res_index = 0;
			DynamicJsonBuffer dinamicJsonBuffer;
				//create main array
			JsonArray& root = dinamicJsonBuffer.createArray();
			while (res_index < _semantic->getObsPropertyNum()) {
					// this return something like "33 °C"
				String tmpString = _semantic->getObsPropertyValue(res_index);
					//create the nested object for each resource
				JsonObject& root_internal = root.createNestedObject();
					//this save only the value before the " ", so in this case "33"
				root_internal["value"] = tmpString.substring(0, tmpString.indexOf(" "));
				JsonObject& obsProperty = root_internal.createNestedObject("obsProperty");
					obsProperty["@c"] = ".Property";
					obsProperty["name"] = _semantic->getObsPropertyName(res_index);
					obsProperty["description"] = "";
				JsonObject& uom = root_internal.createNestedObject("uom");
					uom["@c"] = "UnitOfMeasurment";
					uom["symbol"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["name"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["description"] = "";
	#if DEBUG_SYM_CLASS == 1
					P(" ");
					root.prettyPrintTo(Serial);
					P(" ");
	#endif
				res_index++;
			}
			String resp = "";
			root.printTo(resp);
			resp = "\r\n" + resp;
#if DEBUG_SYM_CLASS == 1
		root.prettyPrintTo(Serial);
		P(" ");
#endif
		dinamicJsonBuffer.clear();
		return resp;
}

void symAgent::handleSSPRequest()
{
	_server->handleClient();
}

uint32_t symAgent::getRegExpiration() {
	return _regExpiration;
}

String symAgent::calculateWifiPSW(String ssid)
{
	// the algoritm for retrive psw is simply to substitue all the hex value 0xf-> 0x9 and all the hex value 0x5-> 0xa.
	// Remember that max 32 characters are allowed for wifi ssid
	// EG: if the ssid is "sym-2e4467f2a7b03255a2a4" then the psw is "2e446792a7b032aaa2a4"
	String temp = ssid;
		//get only the sspId
	temp = temp.substring(4);
	temp.replace("f", "9");
	temp.replace("5", "a");
	return temp;
}