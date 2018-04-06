// OUTDATED, useful only to take parameters for WEMOS platform, need to be adapted to newer library API

#include <ArduinoJson.h>
#include <sym_agent.h>
#include <Adafruit_NeoPixel.h>
#include <Metro.h>

#define WS2812_PIN 4
// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS 1

char* obsPropertyComment_[] = { "Red color of led", "Green color of led", "Blue color of led"};
symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent Test4", "RGB light", obsPropertyComment_);
// When we setup the NeoPixel library, we tell it how many pixels, and which pin to use to send signals.
// Note that for older NeoPixel strips you might need to change the third parameter--see the strandtest
// example for more information on possible values.
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, WS2812_PIN, NEO_GRB + NEO_KHZ800);
struct join_resp joinResp;
extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
extern boolean (* actuatorsFunction[RES_NUMBER])(int);
// Instanciate a metro object and set the interval to 250 milliseconds (0.25 seconds).
Metro registrationMetro = Metro();
int join_success = 0;

boolean actuateRed(int value){
  uint32_t color; 
  PI("RED: ");
  P(value);
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    P(color);
    color = ((color & 0xFF00FFFF) | (value << 16));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
}

boolean actuateGreen(int value){
  uint32_t color; 
  PI("GREEN: ");
  P(value);
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    P(color);
    color = ((color & 0xFFFF00FF) | (value << 8));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
}

boolean actuateBlue(int value){
  uint32_t color; 
  PI("BLUE: ");
  P(value);
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    P(color);
    color = ((color & 0xFFFFFF00) | (value));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
}

boolean setupBind(String* listResources, String (* functions[])(), boolean (* actuatorsFunction[])(int) )
{
    //write all your resources
  listResources[0] = "red";
  listResources[1] = "green";
  listResources[2] = "blue";
    // assign to "functions" referencies to functions that return sensors values
  functions[0] = dummyFunctionSensor;
  functions[1] = dummyFunctionSensor;
  functions[2] = dummyFunctionSensor;
  // assign to "functions" referencies to functions that actuate the things
  actuatorsFunction[0] = actuateRed;
  actuatorsFunction[1] = actuateGreen;
  actuatorsFunction[2] = actuateBlue;
  return true;
}


void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  pixels.begin(); // This initializes the NeoPixel library
  pixels.show();
  delay(200);
  pixels.setPixelColor(0,pixels.Color(0,150,0));
  pixels.show();
  if (sdev1.begin() == true) {
    setupBind(listResources, functions, actuatorsFunction);
    sdev1.join(&joinResp);
    Serial.println("Exit from join");
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
  else Serial.print("Failed!");
}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  if (keepAlive_triggered && join_success == 1){
  //if (keepAlive_triggered){
    sdev1.sendKeepAlive(resp);
  }
  sdev1.handleSSPRequest();
  if (registrationMetro.check() == 1 && join_success == 1){
    //need another new join request
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



