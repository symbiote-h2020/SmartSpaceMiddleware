#include <ArduinoJson.h>
#include <sym_agent.h>

#define RES_NUMBER 2

Res_type resources[3] = { _1_1_1_trichloroethaneConcentration, _1_1_2_trichloroethaneConcentration, windPressure};
symAgent sdev1(agent_SDEV, conn_WIFI, resources, 2, 10000);
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;

String listResources[RES_NUMBER];
String (* functions[RES_NUMBER])();

String readTemp()
{
  return "32.5°C";
}

String readPressure()
{
  return "1014 mBar";
}

boolean setupBind(String* listResources, String (* functions[])() )
{
    //write all your resources
  listResources[0] = "temperature\0";
  listResources[1] = "pressure\0";

    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readPressure;
  return true;
}

String createObservedPropertiesString()
{
  String ret = "[";
  for (int i = 0; i < RES_NUMBER; i++) {
    ret += listResources[i] + ", ";
  }
    // delete the last comma
  ret = ret.substring(0, ret.length() - 2);
  ret += "]";
  return ret;
}

String readSensorsJSON()
{
  String ret = "[";
  for (int i = 0; i < RES_NUMBER; i++) {
    ret += "\"" + listResources[i] + "\"" + ": \"" + functions[i]() + "\", ";
  }
    // delete the last comma
  ret = ret.substring(0, ret.length() - 2);
  ret += "]";
  return ret;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  
  if (sdev1.begin() == true) {
    setupBind(listResources, functions);
    sdev1.bind(createObservedPropertiesString, readSensorsJSON);
    sdev1.join(&joinResp);
    printJoinResp(joinResp);
  }
  else Serial.print("Failed!");

}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  if (keepAlive_triggered){
    sdev1.sendKeepAlive(resp);
    //Serial.println(resp);
  }
  sdev1.handleSSPRequest();
}
