#include <ArduinoJson.h>
#include <sym_agent.h>
#include <Adafruit_MPL3115A2.h>

#define SDA 4
#define SCL 5

symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent Test1", "Temperature, Humidity, Relé and Servo");
Adafruit_MPL3115A2 baro = Adafruit_MPL3115A2();
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
extern boolean (* actuatorsFunction[RES_NUMBER])(int);


String readTemp()
{
    return String (baro.getTemperature()) + " °C";
}

String readPressure()
{
    // return in kPa
  return String (baro.getPressure() * 0.01) + " mBar";
}

boolean actuateRele(int value){
  PI("RELEEEEE ");
  P(value);
  return true;
}

boolean actuateServo(int value){
  PI("SERVOOOO ");
  P(value);
  return true;
}

boolean setupBind(String* listResources, String (* functions[])(), boolean (* actuatorsFunction[])(int) )
{
    //write all your resources
  listResources[0] = "temperature";
  listResources[1] = "pressure";
  listResources[2] = "rele";
  listResources[3] = "servo";
    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readPressure;
  functions[2] = dummyFunctionSensor;
  functions[3] = dummyFunctionSensor;
  // assign to "functions" referencies to functions that actuate the things
  actuatorsFunction[0] = dummyFunctionActuator;
  actuatorsFunction[1] = dummyFunctionActuator;
  actuatorsFunction[2] = actuateRele;
  actuatorsFunction[3] = actuateServo;
  return true;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  if (! baro.begin()) {
    Serial.println("Couldn't find sensor");
    return;
  }
  Serial.print("Temperature: ");
  Serial.println(readTemp());
  Serial.print("Pressure: ");
  Serial.println(readPressure());
  if (sdev1.begin() == true) {
    setupBind(listResources, functions, actuatorsFunction);
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
    Serial.print("Temperature: ");
    Serial.println(readTemp());
    Serial.print("Pressure: ");
    Serial.println(readPressure());
  }
  sdev1.handleSSPRequest();
}

