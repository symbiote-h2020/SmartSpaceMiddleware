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


void printJoinResp(struct join_resp data){
	  PI("\n{\n\tresult: ");
	  PI(data.result);
	  P(",");
	  PI("\tid: ");
	  PI(data.id);
	  P(",");
	  PI("\thash: ");
	  PI(data.hash);
	  P(",");
	  PI("\tregistrationExpiration: ");
	  PI(data.registrationExpiration);
	  P("\n}");
}

void keepAliveISR(void){
	KEEPALIVE_LED_TOGGLE
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

symAgent::symAgent()
{
		//create the json object, refers to https://github.com/bblanchon/ArduinoJson/blob/master/examples/JsonGeneratorExample/JsonGeneratorExample.ino
		// calculate the ssp-id based on the WiFi MAC. TODO: maybe this is possible only when it is connected by wifi, or maybe is better to create this
}
      //TODO please remember to add parameter for class BLE in the constructor
symAgent::symAgent(Agent_type agent_type, Conn_type conn_type, Res_type* res_type, int num_resources, unsigned long keep_alive)
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

	//_hash = readHashFromFlash(); 
	if (_hash != "none") {
		//saveHashInFlash();
	}

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

     //search for well-known symbiotic ssid and try to connect to it.
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
  				//this could be a symbiotic ssid
  				// calculate the psw  				
  				String wifi_psw = calculateWifiPSW(WiFi.SSID(i));
  				P("WiFi psw: "+ wifi_psw);
  				
  				if (connect(WiFi.SSID(i), wifi_psw)){
  					P("Connected!");
  					_wifi_psw = wifi_psw;
  					_wifi_ssid = WiFi.SSID(i);
  						// get as ssp identifier the symbiotic code exposed by wifi ssid
  					_ssp_id = WiFi.SSID(i).substring(4);
  					byte mac[6];
  					WiFi.macAddress(mac);
  					_id = "";
  						// write the agent id based on the union of MAC + SSP_ID
  						// EG: if the ssid is "sym-2e4467f2a7b03255a2a4" and the MAC is 55:ee:45:ef:51:01 then the _id is "55ee45ef51012e4467f2a7b03255a2a4"
  					for (int j = 0; j < 6; j++) {
  						_id += String(mac[j], HEX);
  						_mac += String(mac[j], HEX);
  						_mac += ":";
  					}
  					_mac = _mac.substring(0, 17);
  					_id += _ssp_id;
  						//create a new http client class 
  					_rest_client = new RestClient(JOIN_URL);
  					_rest_client->setContentType("application/json");
  					PI("Got this IDs:\nssp_id: ");
  					P(_ssp_id);
  					PI("id: ");
  					P(_id);
  					PI("mac: ");
  					P(_mac);
  					P("-------------");
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
int symAgent::join(struct join_resp * result)
{
	P("JOIN");
	//struct join_resp;
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	_root["id"] = _id;
	_root["hash"] = _hash;
	_root["observesProperty"] = "TBD"; // TODO: FIXME
	/*
		now create a JSON like this:
		{
		    String id,
		    String hash,
		    deviceDescriptor{
		        String mac,
		        Boolean sleeping,
		        String agentType,
		        Integer readingInterval,
		    }
		    String[] observesProperty
		 }
	*/
	JsonObject& deviceDescriptor = _root.createNestedObject("deviceDescriptor");
		//TODO: add mac to private variable
	deviceDescriptor["mac"] = _mac;
		//TODO insert sleeping device as constructor parameter
	deviceDescriptor["sleeping"] = true;
	if (_agent_type == agent_SDEV) {
		deviceDescriptor["agentType"] = "SDEV";
	} else if (_agent_type == agent_PLAT) {
			deviceDescriptor["agentType"] = "Platform";
		} 
	deviceDescriptor["readingInterval"] = "TBD"; // TODO: FIXME
	String temp = "";
	String resp = "";
	_root.printTo(temp);
	temp = "\r\n" + temp;
	int statusCode = _rest_client->post(JOIN_PATH, temp.c_str(), &resp);
	PI("Status code from server: ");
  	P(statusCode);
  	//P("Response body from server: ");
	//P(resp);
	if (statusCode < 300 and statusCode >= 200){
		//got a valid response
		_jsonBuff.clear();
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		return statusCode;
		}
#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
#endif
			//write result on the struct
		result->result = _root2["result"].as<String>();
		result->id = _root2["id"].as<String>();
		result->hash = _root2["hash"].as<String>();
		result->registrationExpiration = _root2["registrationExpiration"].as<unsigned int>();
	}
	if (_keep_alive > 0) {
		volatile unsigned long next;
		P("Setting timer")
		noInterrupts();
	  	timer0_isr_init();
	  	timer0_attachInterrupt(keepAliveISR);
	  		//set the keep_alive interval. The value is in msec
	  	next=ESP.getCycleCount() + _keep_alive;
	  	timer0_write(next);
	  	interrupts();
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
      //set the agent type if a platform agent or a SDEV agent. In the first case, specify also the number of resources available inside the platform
void symAgent::setAgentType(Agent_type agent_type, int num_resources)
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
	//struct join_resp;
	_jsonBuff.clear();
	JsonObject& _root = _jsonBuff.createObject();
	_root["id"] = _id;
	String temp = "";
	String resp = "";
	_root.printTo(temp);
	temp = "\r\n" + temp;
	int statusCode = _rest_client->post(KEEPALIVE_PATH, temp.c_str(), &resp);
	PI("Status code from server: ");
  	P(statusCode);
  	//P("Response body from server: ");
	//P(resp);
	if (statusCode < 300 and statusCode >= 200){
		//got a valid response
		_jsonBuff.clear();
		JsonObject& _root2 = _jsonBuff.parseObject(resp);
		if (!_root2.success()) {
    		P("parseObject() failed");
    		keepAlive_triggered = false;
    		return statusCode;
		}
	#if DEBUG_SYM_CLASS == 1
		_root2.prettyPrintTo(Serial);
		P(" ");
	#endif
		response = _root2["result"].as<String>();
		keepAlive_triggered = false;
		return statusCode;
	}
	keepAlive_triggered = false;
	return statusCode;
}


void symAgent::sendValue(float* value)
{

}
void symAgent::sendValue(int* value)
{

}

String symAgent::calculateWifiPSW(String ssid)
{
	// the algoritm for retrive psw is simply repeat two times the ssp_id.
	// Remember that max 32 characters are allowed for wifi ssid
	// EG: if the ssid is "sym-2e4467f2a7b03255a2a4" then the psw is "2e4467f2a7b03255a2a42e4467f2a7b03255a2a4"
	String temp;
		//get only the ssp_id
	temp = ssid.substring(4);
	//temp += temp;
	return temp;
}