#include <ArduinoJson.h>
#include <sym_agent.h>

//Res_type resources[3] = { _1_1_1_trichloroethaneConcentration, _1_1_2_trichloroethaneConcentration, windPressure};
//symAgent sdev1(agent_SDEV, conn_WIFI, resources, 2, 10000);
symAgent sdev1(agent_SDEV, conn_WIFI, 10000);
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();

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
  listResources[0] = "temperature";
  listResources[1] = "pressure";

    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readPressure;
  return true;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  
  if (sdev1.begin() == true) {
    setupBind(listResources, functions);
    //sdev1.bind(createObservedPropertiesString, readSensorsJSON);
    sdev1.join(&joinResp);
    //printJoinResp(joinResp);
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
