/*--------------------------------------------------------------------
  This file is part of the SymbIoTe project. 
  --------------------------------------------------------------------*/

#ifndef SYM_AGENT_H
#define SYM_AGENT_H

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <RestClient.h>


#if defined ESP8266
  #include <ESP8266WiFi.h> 
  #warning "You are using the ESP2866 Arduino platform"
#else
  #error "Platform not defined"
#endif

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

#define MAX_JSON_SIZE 500
#define JOIN_URL "innkeeper.symbiote.org"
#define JOIN_PATH "/innkeeper/join"
#define KEEPALIVE_PATH "/innkeeper/keep_alive"

#define SSP_PORT 8080

  // This is the pin led definition to led on board
#define JOIN_LED 0
#define KEEPALIVE_LED 2

  // this is the define for the systick of the esp8266 to seconds and milliseconds
#define TICK_SECONDS 80000000
#define TICK_MILLISECONDS 80000

#define JOIN_LED_ON       digitalWrite(JOIN_LED, LOW);
#define JOIN_LED_OFF      digitalWrite(JOIN_LED, HIGH);
#define JOIN_LED_TOGGLE   digitalWrite(JOIN_LED, !digitalRead(JOIN_LED));

#define KEEPALIVE_LED_ON       digitalWrite(KEEPALIVE_LED, LOW);
#define KEEPALIVE_LED_OFF      digitalWrite(KEEPALIVE_LED, HIGH);
#define KEEPALIVE_LED_TOGGLE   digitalWrite(KEEPALIVE_LED, !digitalRead(KEEPALIVE_LED));

#define RES_NUMBER 3

enum Conn_type { conn_WIFI, conn_BLE, conn_HTTP };

enum Agent_type { agent_SDEV, agent_PLAT };

struct join_resp
{
  String result;
  String id;
  String hash;
  int registrationExpiration;
};

  // This function handle the creation of the JSON String that represent the resources exposed by the agent
String createObservedPropertiesString();
  // This function create the JSON with the key: value of the reading
String readSensorsJSON();

String getProperty(int i);

void printJoinResp(struct join_resp data);
void keepAliveISR(void);

String dummyFunctionSensor();
boolean dummyFunctionActuator(int value);


class symAgent
{
  public:
    symAgent();
      //TODO please remember to add parameter for class BLE in the constructor
    symAgent(Agent_type agent_type, Conn_type conn_type, unsigned long keep_alive, String name, String description);
    ~symAgent();

      // search for well-known symbiotic ssid and try to connect to it.
      // return true if found a symbiotic ssid and so ssp and connect to it, false otherwise
    boolean begin();
      //join the ssp, return  status code of the request and do side effect of the response from the innkeeper into the join_resp struct
    int join(struct join_resp * result);
      //set the agent connection type
    void setConnectionType(Conn_type conn_type);
      //get back the agent connection type
    Conn_type getConnectionType();
      //set the agent type if a platform agent or a SDEV agent. 
    void setAgentType(Agent_type agent_type);
      //get back the agent type
    Agent_type getAgentType();
      //set the keep alive interval for the agent
    void setKeepAlive(unsigned long keep_alive);
      //get back the keep alive interval for the agent
    unsigned long getKeepAlive();

    boolean getKeepAliveTrigger();
    void clearKeepAliveTrigger();

    int sendKeepAlive(String& response);

    void sendValue(float* value);
    void sendValue(int* value);

    boolean elaborateRequest();
    boolean actuateRequest();

    void handleSSPRequest();

    //void bind(String (* createObservedPropertiesString)(), String (* readSensorsJSON)());
    void bind(String (* getProperty)(int), String (* readSensorsJSON)());

    String (* _createObservedPropertiesString)();
    String (* _getProperty) (int);
    String (* _readSensorsJSON)();

  private:
      //This function calculate the password of the symbiotic ssid.
      // Remember that max 32 characters are allowed for wifi ssid
      // EG: if the ssid is "sym-2e4467f2a7b03255a2a4" then the psw is "2e4467f2a7b03255a2a42e4467f2a7b03255a2a4"
    String calculateWifiPSW(String ssid);
      //Try to connect to wifi for 15 seconds and send back response
    boolean connect(String ssid, String psw);

    //volatile boolean _keepAlive_triggered;
    String _wifi_ssid;
    String _wifi_psw;
    String _mac;
      //this is the SSP identifier
    String _ssp_id;
      //this is the agent identifier
    String _id;
    String _hash;
    String _name;
    String _description;

    Agent_type _agent_type;
    Conn_type _conn_type;

    unsigned long _keep_alive;

    /**
      StaticJsonBuffer and DynamicJsonBuffer are designed to be throwaway memory pools,
      they are not intended to be reused. As a consequence, using a global JsonBuffer is not recommended.
    */
    StaticJsonBuffer<MAX_JSON_SIZE> _jsonBuff;

    RestClient* _rest_client;
    ESP8266WebServer* _server;

};


#endif