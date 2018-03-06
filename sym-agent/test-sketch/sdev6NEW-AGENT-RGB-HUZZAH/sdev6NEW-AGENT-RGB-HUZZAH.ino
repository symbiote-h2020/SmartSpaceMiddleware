#include <ArduinoJson.h>
#include <lsp.h>
#include <sym_agent.h>
#include <Adafruit_NeoPixel.h>
#include <Metro.h>
#include <semantic_resources.h>

String tmpTestJson = "{\"resourceInfo\":[{\"symbioteId\":\"\",\"internalId\":\"green\",\"type\":\"Light\"}],\"body\":{\"RGBCapability\":[{\"r\":20},{\"g\":40}]},\"type\" : \"SET\"}";

#define WS2812_PIN 5
// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS 32

//uint8_t ppsk[HMAC_DIGEST_SIZE] = {0x46, 0x72, 0x31, 0x73, 0x80, 0x52, 0x78, 0x92, 0x52, 0x81, 0xad, 0xd7, 0x57, 0x2c, 0x04, 0xa5, 0xdd, 0x84, 0x16, 0x68};

//symAgent sdev1(agent_SDEV, conn_WIFI, 10000, "sym-Agent on HUZZAH", "RGB Leds HAT", false);
symAgent sdev1(10000, "sym-Agent on HUZZAH", "RGB Leds HAT", false);

// When we setup the NeoPixel library, we tell it how many pixels, and which pin to use to send signals.
// Note that for older NeoPixel strips you might need to change the third parameter--see the strandtest
// example for more information on possible values.
//Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, WS2812_PIN, NEO_GRB + NEO_KHZ800);

extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
extern boolean (* actuatorsFunction[RES_NUMBER])(int);

Metro registrationMetro = Metro();

int join_success = 0;

boolean actuateRed(int value){
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    //color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    //color = ((color & 0xFF00FFFF) | (value << 16));
    //pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  //pixels.show();
}

boolean actuateGreen(int value){
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    //color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    //color = ((color & 0xFFFF00FF) | (value << 8));
    //pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  //pixels.show();
}

boolean actuateBlue(int value){
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    //color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    //color = ((color & 0xFFFFFF00) | (value));
    //pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  //pixels.show();
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
  //pixels.begin(); // This initializes the NeoPixel library
  ///sec1.begin("sym-12345678");
  if (sdev1.begin() == true) {
    setupBind(listResources, functions, actuatorsFunction);
  int joinresp = sdev1.join();
  if (joinresp < 300 and joinresp >= 200) {
    join_success = 1;
  } else {
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
    // if 0 no RegExpiration
    if (sdev1.getRegExpiration() != 0) {
      registrationMetro.interval(floor(sdev1.getRegExpiration() * 0.9));
    }
  }
  else Serial.print("Failed!");

  //sdev1.TestelaborateQuery(tmpTestJson);
}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;
  delay(10);
  if (keepAlive_triggered && join_success == 1){
    sdev1.sendKeepAlive(resp);
  }
  sdev1.handleSSPRequest();
  if (registrationMetro.check() == 1 && join_success == 1){
    //need another new join request
    int joinresp = sdev1.join();
    if (joinresp < 300 and joinresp >= 200) {
    join_success = 1;
  } else {
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
      /// stay under the 90% of the registration expiration
    registrationMetro.interval(floor(sdev1.getRegExpiration() * 0.9));
  }
}


