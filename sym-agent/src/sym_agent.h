/*--------------------------------------------------------------------
  This file is part of the SymbIoTe project. 
  --------------------------------------------------------------------*/

#ifndef SYM_AGENT_H
#define SYM_AGENT_H

#include <Arduino.h>
#include <ESP8266WiFi.h>
#include <ESP8266WebServer.h>
#include <ArduinoJson.h>
#include <WiFiUdp.h>
#include <NTPClient.h>
#include <RestClient.h>
#include <EEPROM.h>
#include <Time.h>
#include <lsp.h>
#include <semantic_resources.h>


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

#define NTP_OFFSET   7200 //60 * 60 * 2      // In seconds
#define NTP_INTERVAL 60 * 1000    // In miliseconds
#define NTP_ADDRESS  "europe.pool.ntp.org"

#define FLASH_MEMORY_RESERVATION_AGENT  512
//#define FLASH_LSP_START_ADDR    0
// thought to be a 4 bytes identifier and 12 HEX byte
// like this: sym-00112233445566778899aabb
#define FLASH_LSP_START_SSPID   0
#define FLASH_LSP_END_SSPID     31
// should be 16 byte
#define FLASH_LSP_START_PREV_DK1  32
#define FLASH_LSP_END_PREV_DK1    47 

#define FLASH_AGENT_START_SYMID 48
  // 12 HEX bytes for sym-Id -> mapped to 24 ascii char
#define FLASH_AGENT_END_SYMID   72

#define FLASH_AGENT_START_SENSOR_RESOURCE_SYMID 73
  // 12 HEX bytes for sym-Id -> mapped to 24 ascii char
#define FLASH_AGENT_END_SENSOR_RESOURCE_SYMID 97

#define MAX_JSON_SIZE 2500
//#define JOIN_URL "10.20.30.1"
#define JOIN_URL "innkeeper.symbiote.org"
//#define RAP_URL "192.168.97.105"
#define REGISTRY_PATH "/innkeeper/sdev/register"
#define JOIN_PATH "/innkeeper/sdev/join"
#define RAP_PATH "/rap/v1/request"
#define KEEPALIVE_PATH "/innkeeper/keep_alive"
#define UNREGISTRY_PATH "/innkeeper/sdev/unregistry"

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


#define ERR_KICKED_OUT_FROM_JOIN 0x55
#define ERR_UNKNOWN_RESPONSE_FROM_JOIN 0x65
#define ERR_SYMID_MISMATCH_FROM_JOIN 0x75
#define ERR_PARSE_JSON 0x56

#define RES_NUMBER 3

void keepAliveISR(void);


class symAgent
{
  public:
    symAgent();
      //TODO please remember to add parameter for class BLE in the constructor
    symAgent(unsigned long keep_alive, String description, bool isRoaming);
    symAgent(unsigned long keep_alive, String description, bool isRoaming, Semantic* semantic);
    /* This second constructor instantiate also the value for field comment inside obsProperty
    */
    ~symAgent();

    boolean elaborateQuery();

    boolean TestelaborateQuery(String resp);

    String getSymIdFromFlash();
    void saveIdInFlash();
    void forceSymIdInFlash(String value);

    void saveSymIdResourceInFlash();
    String getSymIdResourceFromFlash();

    String TestgetSymIdResourceFromFlash();
    void TestsaveSymIdResourceInFlash(String symId);

    void setResource(String rapRequest);

    void subscribe();
    void unsubscribe();

    void history();
    void getResource();
      // search for well-known symbiotic ssid and try to connect to it.
      // return true if found a symbiotic ssid and so ssp and connect to it, false otherwise
    boolean begin();
      //join the ssp, return  status code of the request and do side effect of the response from the innkeeper into the join_resp struct
    int registry();
    int unregistry();
    int join();
    String createSensorSemanticDescription();
    String createActuatorSemanticDescription();
      //set the keep alive interval for the agent
    void setKeepAlive(unsigned long keep_alive);
      //get back the keep alive interval for the agent
    unsigned long getKeepAlive();

    boolean getKeepAliveTrigger();
    void clearKeepAliveTrigger();

    int sendKeepAlive(String& response);

    boolean elaborateRequest();
    boolean actuateRequest();

    String getResourceAsString();
    void handleSSPRequest();

    uint32_t getRegExpiration();

  private:
    /************************* FUNCTIONS *************************/
      //This function calculate the password of the symbiotic ssid.
      // Remember that max 32 characters are allowed for wifi ssid
      // EG: if the ssid is "sym-2e4467f2a7b03255a2a4" then the psw is "2e4467f2a7b03255a2a42e4467f2a7b03255a2a4"
    String calculateWifiPSW(String ssid);
      //Try to connect to wifi for 15 seconds and send back response
    boolean connect(String ssid, String psw);


    /************************* VARIABLES *************************/
    String _wifi_ssid;
    String _wifi_psw;
    String _mac;
      //this is the SSP identifier
    String _sspId;
      //this is the agent identifier
    String _symId;
    //String _symIdResource;
      // valid as internal id for both sensor and actuator
    String _internalId;
      // this is the sspId of the SDEV
    String _symIdInternal;

    String _sspIdSensorResource;
    String _symIdSensorResource;

    String _sspIdActuatorResource;
    String _symIdActuatorResource;

    bool _firstTimeEverConnect;
    String _description;

    unsigned long _keep_alive;
    bool _roaming;
    bool _subscribe;
    uint32_t _regExpiration;

    /**
      StaticJsonBuffer and DynamicJsonBuffer are designed to be throwaway memory pools,
      they are not intended to be reused. As a consequence, using a global JsonBuffer is not recommended.
    */
    StaticJsonBuffer<MAX_JSON_SIZE> _jsonBuff;

    RestClient* _rest_client;
    ESP8266WebServer* _server;

    lsp* _security;
    Semantic* _semantic;

};


#endif