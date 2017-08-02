//#define ARDUINOJSON_ENABLE_PROGMEM 0
#include <ArduinoJson.h>
#include <sym_agent.h>


Res_type resources[3] = { _1_1_1_trichloroethaneConcentration, _1_1_2_trichloroethaneConcentration, windPressure};
symAgent sdev1(agent_SDEV, conn_WIFI, resources, 2, 10000);
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;




void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  
  if (sdev1.begin() == true) {
    Serial.println("Success!");
    sdev1.join(&joinResp);
    printJoinResp(joinResp);
    Serial.println("\nSuccess2!");
  }
  else Serial.print("Failed!");

}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  if (keepAlive_triggered){
    sdev1.sendKeepAlive(resp);
    Serial.println(resp);
  }
  sdev1.handleSSPRequest();
}
