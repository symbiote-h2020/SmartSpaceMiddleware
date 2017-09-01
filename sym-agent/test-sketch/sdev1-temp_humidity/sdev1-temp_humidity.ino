#include <ArduinoJson.h>
#include <sym_agent.h>
#include <SI7021.h>

#define SDA 4
#define SCL 5

symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent Test1", "Temperature and Humidity");
SI7021 sensor;
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();

String readTemp()
{
  return String ((float)(sensor.getCelsiusHundredths()) / 100) + " °C";
}

String readHumidity()
{
  return String (sensor.getHumidityPercent()) + " %";
}

boolean setupBind(String* listResources, String (* functions[])() )
{
    //write all your resources
  listResources[0] = "temperature";
  listResources[1] = "humidity";

    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readHumidity;
  return true;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  sensor.begin(SDA, SCL);
  Serial.println(sensor.getHumidityPercent());
  Serial.println(sensor.getCelsiusHundredths());
  if (sdev1.begin() == true) {
    setupBind(listResources, functions);
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
  }
  sdev1.handleSSPRequest();
}
