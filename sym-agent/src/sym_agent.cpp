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
String listResources[RES_NUMBER];
String (* functions[RES_NUMBER])();
boolean (* actuatorsFunction[RES_NUMBER])(int);
uint8_t ppsk[HMAC_DIGEST_SIZE] = {0x46, 0x72, 0x31, 0x73, 0x80, 0x52, 0x78, 0x92, 0x52, 0x81, 0xad, 0xd7, 0x57, 0x2c, 0x04, 0xa5, 0xdd, 0x84, 0x16, 0x68};

void keepAliveISR(void){
	//KEEPALIVE_LED_TOGGLE
	keepAlive_triggered = true;
	volatile unsigned long next;
	noInterrupts();
	  		//set the keep_alive interval. The value is in msec
	#warning "fixthis in keepAliveISR"
	next=ESP.getCycleCount() + keep_alive_interval;
	//next=ESP.getCycleCount() + (2000 * TICK_MILLISECONDS);
	timer0_write(next);
	interrupts();
}

String dummyFunctionSensor(){
	return "NA";
}

boolean dummyFunctionActuator(int value){
	return true;
}

symAgent::symAgent()
{
		//create the json object, refers to https://github.com/bblanchon/ArduinoJson/blob/master/examples/JsonGeneratorExample/JsonGeneratorExample.ino
		// calculate the ssp-id based on the WiFi MAC. TODO: maybe this is possible only when it is connected by wifi, or maybe is better to create this
}
      //TODO please remember to add parameter for class BLE in the constructor
symAgent::symAgent(Agent_type agent_type, Conn_type conn_type, unsigned long keep_alive, String name, String description, bool isRoaming)
{
	pinMode(JOIN_LED, OUTPUT);
	pinMode(KEEPALIVE_LED, OUTPUT);
	JOIN_LED_OFF
	KEEPALIVE_LED_OFF
	_agent_type = agent_type;
	_conn_type = conn_type;
		//set internal keep alive interval to the correct value to be used in the timer0_write()
	_keep_alive = keep_alive * TICK_MILLISECONDS;
		// change the value of the global variable keep_alive_interval accordingly
	keep_alive_interval = _keep_alive;
	_name = name;
	_description = description;
	_security = new lsp(TLS_PSK_WITH_AES_128_CBC_SHA ,"PBKDF2", ppsk, HMAC_DIGEST_SIZE);
	_roaming = isRoaming;
		// if nothing is provided, initialize to NA the field comment of the obsProperty	
	for (int i = 0; i < RES_NUMBER; i++) {
		_obsPropertyComment[i] = "NA";
	}
	_server = new ESP8266WebServer();
	_regExpiration = 0; 
}
/*
symAgent::symAgent(Agent_type agent_type, Conn_type conn_type, unsigned long keep_alive, String name, String description, char** obsPropertyComment, bool isRoaming)
{
	pinMode(JOIN_LED, OUTPUT);
	pinMode(KEEPALIVE_LED, OUTPUT);
	JOIN_LED_OFF
	KEEPALIVE_LED_OFF
	_agent_type = agent_type;
	_conn_type = conn_type;
		//set internal keep alive interval to the correct value to be used in the timer0_write()
	_keep_alive = keep_alive * TICK_MILLISECONDS;
		// change the value of the global variable keep_alive_interval accordingly
	keep_alive_interval = _keep_alive;
	_name = name;
	_description = description;
	// FIXME
	_obsPropertyComment[0] = obsPropertyComment;

	_roaming = isRoaming;
	//_security = new lsp(TLS_PSK_WITH_AES_128_CBC_SHA ,"PBKDF2", ppsk, HMAC_DIGEST_SIZE);

	_server = new ESP8266WebServer();
	_regExpiration = 0;
		// read from flash if there is a valid symId stored, otherwise return "" String
	_id = getSymIdFromFlash();
	//_hash = readHashFromFlash(); 
	if (_hash != "none") {
		//saveHashInFlash();
	}
}
*/

String symAgent::getSymIdFromFlash() {
	String tmpID = "";
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		tmpID += String(EEPROM.read(i), HEX);
	}
	PI("Read this SYM-ID from flash: ");
	P(tmpID);
	EEPROM.end();
	if (tmpID != "ffffffffffffffffffffffffffffffff") {
		//valid sym-id
		return tmpID;
	} else return "";
}

void symAgent::saveIdInFlash() {
	P("SAVE SYM-ID IN FLASH");
	EEPROM.begin(FLASH_MEMORY_RESERVATION_AGENT);
	uint8_t j = 0;
	for (uint8_t i = FLASH_AGENT_START_SYMID; i < FLASH_AGENT_END_SYMID; i++) {
		EEPROM.write(i, _id.charAt(j));
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
	P(_id);
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
		if (_root["symbioteId"] == _id) {
			if (type == "SET") setResource(resp);
			else if (type == "GET") getResource();
			else if (type == "HISTORY") getResource();
			// TODO FIXME
			//else if (type == "SUBSCRIBE") subScribe();
			//else if (type == "UNSUBSCRIBE") unSubscribe();
			else {
				P("Wrong TYPE");
				String tmpResp = "{ \"id\":\"" + _id + "\", \"value\":\"WrongType\"}";
				_server->send(200, "application/json", tmpResp);
				return false;
			}
		} else {
			P("Wrong SymId");
			String tmpResp = "{ \"id\":\"" + _id + "\", \"value\":\"WrongSymId\"}";
			_server->send(200, "application/json", tmpResp);
			return false;
		}
	return true;
}

void symAgent::setResource(String rapRequest) {
	P("SET RESOURCE");
	//String resp = _server->arg(0);
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
		if (_root["resourceInfo"][0]["symbioteId"] == _id) {
			// check only the first if the array, because the other should be the same
			for (uint8_t i = 0; i< RES_NUMBER; i++) {
				//check if any of my resources should be changed
				for (uint8_t j = 0; j < RES_NUMBER; j++) {
					if (_root["resourceInfo"][i]["internalId"] == listResources[j]) {
						String field_value = "";
						// search for something like this:
						// _root["body"][1]["red"]["value"] = 200
						/*
							{
							  	"resourceInfo" : [ {
							    "symbioteId" : "12345",
							    "internalId" : "red",
							    "type" : "light"
							  }, {
							    "symbioteId" : "12345",
							    "internalId" : "blue",
							    "type" : "light"
							  }],
							  "body" : {
							    "red": [
							      { "{restriction}": "{value}" }
							    ] 
							  },
							  "type" : "SET"
							}
						*/
							//FIXME: wait a concrete example to prse the action
						field_value = _root["body"][i][(char *)listResources[j].c_str()]["value"].as<String>();
						actuatorsFunction[j](field_value.toInt());
					}
				}
				
			}
		} else {
			PI("Mismatch in symId.\nWhat I got: ");
			P(_root["resourceInfo"][0]["symbioteId"].as<String>());
			PI("What I expect: ");
			P(_id);
			return;
		}



		String id = _root["id"].as<String>();
		if (id == _id){
			String field = "";
			String field_value = "";
			for (int i = 0; i < RES_NUMBER; i++) {

				//TODO CHECK "NA" field
				field = listResources[i];
				field_value = _root["action"][field].as<String>();
				PI(field);
				PI(" value[");
				PI(i);
				PI("]=");
				P(field_value);
				actuatorsFunction[i](field_value.toInt());
			}

			String tmpResp = "{ \"id\":\"" + _id + "\", \"response\": \"OK\"}";
			_server->send(200, "application/json", tmpResp);
			return;
		} else {
			P("Wrong id");
			String tmpResp = "{ \"id\":\"" + _id + "\", \"value\":\"Error\"}";
			_server->send(200, "application/json", tmpResp);
			return;
		}
	return;
}


void symAgent::getResource() {
	// push resource to RAP
	// right now push all the resources
	P("GET RESOURCE");
	int res_index = 0;
			DynamicJsonBuffer dinamicJsonBuffer;
				//create main array
			JsonArray& root = dinamicJsonBuffer.createArray();
			while (res_index < RES_NUMBER) {
					// this return something like "33 °C"
				String tmpString = functions[res_index]();
					//create the nested object for each resource
				JsonObject& root_internal = root.createNestedObject();
					//this save only the value before the " ", so in this case "33"
				root_internal["value"] = tmpString.substring(0, tmpString.indexOf(" "));
				JsonObject& obsProperty = root_internal.createNestedObject("obsProperty");
					obsProperty["label"] = listResources[res_index];
					obsProperty["comment"] = _obsPropertyComment[res_index];
				JsonObject& uom = root_internal.createNestedObject("uom");
					uom["symbol"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["label"] = "NA";
					uom["comment"] = "NA";
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
			P("\n*************+\nPACKET SENT TO RAP:");
#if DEBUG_SYM_CLASS == 1
		root.prettyPrintTo(Serial);
		P(" ");
#endif
			_server->send(200, "application/json", resp);
			dinamicJsonBuffer.clear();
}

/*
// callback function to handle the resource request from the ssp
boolean symAgent::elaborateRequest()
{
	P("ELABORATE_REQUEST");
	String resp = _server->arg(0);
	_jsonBuff.clear();
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		return false;
		}
#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
		P(" ");
#endif
		String id = _root2["id"].as<String>();
		P(" ");
		if (id == _id){
			int res_index = 0;
			DynamicJsonBuffer dinamicJsonBuffer;
				//create main array
			JsonArray& root = dinamicJsonBuffer.createArray();
			//JsonObject& main = root.createNestedObject();
			while (res_index < RES_NUMBER) {
					// this return something like "33 °C"
				String tmpString = functions[res_index]();
					//create the nested object for each resource
				JsonObject& root_internal = root.createNestedObject();
					//this save only the value before the " ", so in this case "33"
				root_internal["value"] = tmpString.substring(0, tmpString.indexOf(" "));
				JsonObject& obsProperty = root_internal.createNestedObject("obsProperty");
					obsProperty["label"] = listResources[res_index];
					obsProperty["comment"] = _obsPropertyComment[res_index];
				JsonObject& uom = root_internal.createNestedObject("uom");
					uom["symbol"] = tmpString.substring((tmpString.indexOf(" ") + 1));
					uom["label"] = "NA";
					uom["comment"] = "NA";
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
			P("\n*************+\nPACKET SENT TO RAP:");
#if DEBUG_SYM_CLASS == 1
		root.prettyPrintTo(Serial);
		P(" ");
#endif
			_server->send(200, "application/json", resp);
			dinamicJsonBuffer.clear();
			return true;
		} else {
			P("Wrong id");
			String tmpResp = "{ \"id\":\"" + _id + "\", \"value\":\"WrongId\"}";
			_server->send(200, "application/json", tmpResp);
			return false;
		}
	return true;
}


// callback function to handle the actuation request coming from the ssp(RAP)
boolean symAgent::actuateRequest()
{
	P("ACTUATE_REQUEST");
	String resp = _server->arg(0);
	_jsonBuff.clear();
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		return false;
		}
#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
		P(" ");
#endif
		String id = _root2["id"].as<String>();
		if (id == _id){
			String field = "";
			String field_value = "";
			for (int i = 0; i < RES_NUMBER; i++) {

				//TODO CHECK "NA" field
				field = listResources[i];
				field_value = _root2["action"][field].as<String>();
				PI(field);
				PI(" value[");
				PI(i);
				PI("]=");
				P(field_value);
				actuatorsFunction[i](field_value.toInt());
			}

			String tmpResp = "{ \"id\":\"" + _id + "\", \"response\": \"OK\"}";
			_server->send(200, "application/json", tmpResp);
			return true;
		} else {
			P("Wrong id");
			String tmpResp = "{ \"id\":\"" + _id + "\", \"value\":\"Error\"}";
			_server->send(200, "application/json", tmpResp);
			return false;
		}
	return true;
}
*/
    // search for well-known symbiotic ssid and try to connect to it.
	// return true if found a symbiotic ssid and so ssp and connect to it, false otherwise

boolean symAgent::begin()
{
	P("BEGIN");
	if (this->_conn_type == conn_WIFI){
			// ssp-id based on a id sent from the innkeeper.
		int networksFound = WiFi.scanNetworks();
		for (int i = 0; i < networksFound; i++)
  		{
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
  					//_ssp_id = WiFi.SSID(i).substring(4);
  					_ssp_id = WiFi.SSID(i);
  					byte mac[6];
  					WiFi.macAddress(mac);
  					//String tmpId = "";
  						// write the agent id based on the union of MAC + SSP_ID
  						// EG: if the ssid is "sym-2e4467f2a7b03255a2a4" and the MAC is 55:ee:45:ef:51:01 
  						//then the _id is "55ee45ef51012e4467f2a7b03255a2a4"
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
  					PI("Got this IDs:\n\tssp_id: ");
  					P(_ssp_id);
  					PI("\tid: ");
  					P(_id);
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
					_security->begin(_ssp_id);
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
	} else if (this->_conn_type == conn_BLE) {
			//TODO: manage BLE
		return false;
	}
	
}

	//join the ssp, return a valid join response struct if success
int symAgent::join()
{
	P("JOIN");
	//struct join_resp;
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	/*
		Create a JSON like this:
		{
		   String sym-id,
		   String pluginId,
		   String pluginURL,
		   String semanticDescription
		   String dk1,
		   String hashField
 		}
	*/
	_root["sym-id"] = _id;
	_root["pluginId"] = _mac;
	_root["pluginURL"] = "http://" + String(WiFi.localIP()[0]) + "." + String(WiFi.localIP()[1]) + "." + String(WiFi.localIP()[2]) + "." + String(WiFi.localIP()[3]) + ":80";
	_root["dk1"] = _security->getDK1();
		//Regarding the hashField could be (i) all 0 when the SDEV joins for the first time
		// or (ii) hashField = H(sym-id || previous dk1)
	_root["hashField"] = _security->getHashOfIdentity(_id);
	//_root["hash"] = _hash;
	//_root["observesProperty"] = _createObservedPropertiesString(); // TODO: FIXME


	//JsonArray& data = _root.createNestedArray("observesProperty");
  	//for (int i=0; i< RES_NUMBER; i++ ) data.add(listResources[i]);

	//JsonObject& deviceDescriptor = _root.createNestedObject("deviceDescriptor");
		//TODO: add mac to private variable
	//deviceDescriptor["mac"] = _mac;
	
		//TODO insert sleeping device as constructor parameter
	//deviceDescriptor["sleeping"] = true;
	//deviceDescriptor["name"] = _name;
	//deviceDescriptor["description"] = _description;
	//Serial.println(WiFi.localIP());
	//deviceDescriptor["url"] = "http://" + String(WiFi.localIP()[0]) + "." + String(WiFi.localIP()[1]) + "." + String(WiFi.localIP()[2]) + "." + String(WiFi.localIP()[3]) +":80";
	//if (_agent_type == agent_SDEV) {
	//	deviceDescriptor["agentType"] = "SDEV";
	//} else if (_agent_type == agent_PLAT) {
	//		deviceDescriptor["agentType"] = "Platform";
	//	} 
	//deviceDescriptor["readingInterval"] = 1500; // TODO: FIXME
	String tempClearData = "";
	String tempCryptData = "";
	String tempJsonPacket = "";
	String resp = "";
	_root.printTo(tempClearData);

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
	P("Join message: ");
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
		 if (!(_rootClearResp["result"].as<String>() == "REJECTED")) {
			//kick away fro the SSP
			return ERR_KICKED_OUT_FROM_JOIN;
		} else if (_rootClearResp["result"].as<String>() == "OK" || _rootClearResp["result"].as<String>() == "ALREADY_REGISTERED") {
			_regExpiration = _rootClearResp["registrationExpiration"].as<unsigned int>();
			if (_id == "" || _id == _rootClearResp["sym-id"].as<String>()) {
				// everything ok
				_id = _rootClearResp["sym-id"].as<String>();
			} else {
				return ERR_SYMID_MISMATCH_FROM_JOIN;
			}
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

      //set the agent connection type
void symAgent::setConnectionType(Conn_type conn_type)
{
	if (_conn_type != conn_type) {
		_conn_type = conn_type;
		// TODO: understand if we need to reconnect the agent
	}
}
      //get back the agent connection type
Conn_type symAgent::getConnectionType()
{
	return _conn_type;
}
      //set the agent type if a platform agent or a SDEV agent.
void symAgent::setAgentType(Agent_type agent_type)
{
	_agent_type = agent_type;
}
      //get back the platform agent type
Agent_type symAgent::getAgentType()
{
	return _agent_type;
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
	KEEPALIVE_LED_ON
	delay(50);
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	_root["id"] = _id;

	//String temp = "";
	//String resp = "";
	///_root.printTo(temp);
	//temp = "\r\n" + temp;

	String tempClearData = "";
	String tempCryptData = "";
	String resp = "";
	String tempJsonPacket = "";
	_root.printTo(tempClearData);

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
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		keepAlive_triggered = false;
    		KEEPALIVE_LED_OFF
    		return ERR_PARSE_JSON;
		}
	#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
		P(" ");
	#endif
		//response = _root2["result"].as<String>();
		// TODO: parse response from innkeper
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
		//get only the ssp_id
	temp = temp.substring(4);
	temp.replace("f", "9");
	temp.replace("5", "a");
	return temp;
}