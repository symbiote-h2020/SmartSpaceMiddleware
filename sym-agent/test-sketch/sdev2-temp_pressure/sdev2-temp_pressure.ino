#include <ArduinoJson.h>
#include <sym_agent.h>
#include <Adafruit_MPL3115A2.h>

#define SDA 4
#define SCL 5

symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent Test1", "Temperature and Pressure");
Adafruit_MPL3115A2 baro = Adafruit_MPL3115A2();
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();

String readTemp()
{
    return String (baro.getTemperature()) + " °C";
}

String readPressure()
{
    // return in kPa
  return String (baro.getPressure() * 0.01) + " mBar";
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
  if (! baro.begin()) {
    Serial.println("Couldnt find sensor");
    return;
  }
  Serial.print("Temperatura: ");
  Serial.println(readTemp());
  Serial.print("Pressione: ");
  Serial.println(readPressure());
  if (sdev1.begin() == true) {
    setupBind(listResources, functions);
    sdev1.join(&joinResp);
  }
  else Serial.print("Failed!");

}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  if (keepAlive_triggered){
    sdev1.sendKeepAlive(resp);
    Serial.print("Temperatura: ");
    Serial.println(readTemp());
    Serial.print("Pressione: ");
    Serial.println(readPressure());
  }
  sdev1.handleSSPRequest();
}
