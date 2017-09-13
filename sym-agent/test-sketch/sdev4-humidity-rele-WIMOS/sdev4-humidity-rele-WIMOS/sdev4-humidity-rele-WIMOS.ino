#include <ArduinoJson.h>
#include <sym_agent.h>
#include <DHT.h>
#include <Metro.h>

#define RELE_PIN 5
#define DHTTYPE DHT11
#define DHT_PIN 2

symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent Test1", "Temperature, Humidity and Relé");
DHT dht(DHT_PIN, DHTTYPE);
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
extern boolean (* actuatorsFunction[RES_NUMBER])(int);
// Instanciate a metro object and set the interval to 250 milliseconds (0.25 seconds).
Metro registrationMetro = Metro();
int join_success = 0;

String readTemp()
{
  float h = dht.readTemperature();
  return String (h) + " °C";
}

String readHumidity()
{
  float h = dht.readHumidity();
  return String (h) + " %";
}

String readRele()
{
  if (digitalRead(RELE_PIN) == HIGH) return "1 Digital";
  else return "0 Digital";
}

boolean actuateRele(int value){
  if (value == 1) digitalWrite(RELE_PIN, HIGH);
  else if (value == 0) digitalWrite(RELE_PIN, LOW);
}

boolean setupBind(String* listResources, String (* functions[])(), boolean (* actuatorsFunction[])(int) )
{
    //write all your resources
  listResources[0] = "temperature";
  listResources[1] = "humidity";
  listResources[2] = "rele";
    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readHumidity;
  functions[2] = readRele;
  // assign to "functions" referencies to functions that actuate the things
  actuatorsFunction[0] = dummyFunctionActuator;
  actuatorsFunction[1] = dummyFunctionActuator;
  actuatorsFunction[2] = actuateRele;
  return true;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  dht.begin();
  pinMode(RELE_PIN, OUTPUT);
  if (sdev1.begin() == true) {
    setupBind(listResources, functions, actuatorsFunction);
    sdev1.join(&joinResp);
    printJoinResp(joinResp);
    if (joinResp.result == "OK") join_success = 1;
    else if (joinResp.result == "ALREADY_REGISTERED"){
      join_success = 1;
      Serial.println("I'm already registered!");
    }
    else{
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
    registrationMetro.interval(floor(joinResp.registrationExpiration * 0.9));
  }
  else Serial.println("Failed!");
}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  //if (keepAlive_triggered && join_success == 1){
  if (keepAlive_triggered){
    sdev1.sendKeepAlive(resp);
    Serial.print("Temperatura: ");
    Serial.println(readTemp());
    Serial.print("Humidity: ");
    Serial.println(readHumidity());
  }
  sdev1.handleSSPRequest();
  if (registrationMetro.check() == 1 && join_success == 1){
    //need another new join request
    Serial.println("****************");
    Serial.println("*** NEW JOIN ***");
    Serial.println("****************");
    sdev1.join(&joinResp);
    printJoinResp(joinResp);
    if (joinResp.result == "OK") join_success = 1;
    else if (joinResp.result == "ALREADY_REGISTERED"){
      join_success = 1;
      Serial.println("I'm already registered!");
    }
    else{
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
      /// stai under the 90% of the registration expiration
    registrationMetro.interval(floor(joinResp.registrationExpiration * 0.9));
  }
}

