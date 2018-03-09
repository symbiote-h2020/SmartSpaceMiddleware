#include <ArduinoJson.h>
#include <lsp.h>
#include <sym_agent.h>
#include <Adafruit_NeoPixel.h>
#include <Adafruit_MPL3115A2.h>
#include <Metro.h>
#include <semantic_resources.h>

String tmpTestJson = "{\"resourceInfo\":[{\"symbioteId\":\"\",\"internalId\":\"sym1\",\"type\":\"Light\"}],\"body\":{\"RGBCapability\":[{\"r\":20},{\"g\":40}]},\"type\" : \"SET\"}";

#define SDA 4
#define SCL 5
#define WS2812_PIN 2
// How many NeoPixels are attached to the Arduino?
#define NUMPIXELS 32

Adafruit_MPL3115A2 baro = Adafruit_MPL3115A2();
Adafruit_NeoPixel pixels = Adafruit_NeoPixel(NUMPIXELS, WS2812_PIN, NEO_GRB + NEO_KHZ800);
uint8_t j = 0;

String readTemp()
{
  return String (baro.getTemperature()) + " °C";
}

String readPr()
{
    // return in kPa
  return String (baro.getPressure() * 0.01) + " mBar";
}

bool setRed(int in)
{
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    color = ((color & 0xFF00FFFF) | (in << 16));
    pixels.setPixelColor(i, color);  //Moderately bright green color.
  }
  pixels.show();
  return true;
}

bool setGreen(int in)
{
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    color = ((color & 0xFFFF00FF) | (in << 8));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
  return true;
}

bool setBlue(int in)
{
  uint32_t color; 
  for(int i=0;i<NUMPIXELS;i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    color = ((color & 0xFFFFFF00) | (in));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
  return true;
}


Property propertyPointer[2] = {Property("temperature", "nc", &readTemp), Property("pressure", "nc", &readPr)};

Parameter paramPointer[3] = {Parameter("r", "xsd:unsignedByte", "0", "255", &setRed), Parameter("g", "xsd:unsignedByte", "0", "255", &setGreen), Parameter("b", "xsd:unsignedByte", "0", "255", &setBlue)};

Capability c1("RGBCapability", 3, paramPointer);
  //    internalID, name, url, capability_number, Capability* Class, observesProperty_number, Property* Class
Semantic s1("sym1", "aggeggio", "192.168.97.55", 1, &c1, 2, propertyPointer);

symAgent sdev1(10000, "sym-Agent on HUZZAH", "RGB Leds HAT", false, &s1);

extern volatile boolean keepAlive_triggered;
Metro registrationMetro = Metro();
int join_success = 0;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  Serial.println("Start...");
  pixels.begin(); // This initializes the NeoPixel library
  if (sdev1.begin() == true) {
    //setupBind(listResources, functions, actuatorsFunction);
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
  Serial.println("\n\n***************\n\n");
  sdev1.TestelaborateQuery(tmpTestJson);
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



