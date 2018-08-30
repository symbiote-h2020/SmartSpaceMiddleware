#include <ArduinoJson.h>
#include <lsp.h>
#include <sym_agent.h>
#include <Adafruit_NeoPixel.h>
#include <Adafruit_MPL3115A2.h>
#include <Metro.h>
#include <semantic_resources.h>

String tmpTestJson = "{\"resourceInfo\":[{\"symbioteId\":\"\",\"internalIdResource\":\"5c:cf:7f:3a:6b:76\",\"type\":\"Light\"}],\"body\":{\"RGBCapability\":[{\"r\":20},{\"g\":5},{\"b\":5}]},\"type\" : \"SET\"}";
String tmpTestJson2 = "{\"resourceInfo\":[{\"symbioteId\":\"\",\"internalIdResource\":\"5c:cf:7f:3a:6b:76\",\"type\":\"Light\"},{\"type\" :\"Observation\"}],\"type\":\"GET\"}";
String tmpTestJson3 = "{\"resourceInfo\":[{\"symbioteId\":\"\",\"internalIdResource\":\"5c:cf:7f:3a:6b:76\",\"type\":\"Light\"},{\"type\" :\"Observation\"}],\"type\":\"HISTORY\"}";

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
  return String(baro.getPressure() * 0.01) + " mBar";
}

bool setRed(int in)
{
  uint32_t color;
  for (int i = 0; i < NUMPIXELS; i++) {
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
  for (int i = 0; i < NUMPIXELS; i++) {
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
  for (int i = 0; i < NUMPIXELS; i++) {
    color = pixels.getPixelColor(i);
    //set red color in the RGB 32 bit color variable
    color = ((color & 0xFFFFFF00) | (in));
    pixels.setPixelColor(i, color); // Moderately bright green color.
  }
  pixels.show();
  return true;
}

uint8_t count = 0;
Property propertyPointer[2] = {Property("temperature", "nc", &readTemp), Property("pressure", "nc", &readPr)};

Parameter paramPointer[3] = {Parameter("r", "xsd:unsignedByte", "0", "255", &setRed), Parameter("g", "xsd:unsignedByte", "0", "255", &setGreen), Parameter("b", "xsd:unsignedByte", "0", "255", &setBlue)};

Capability c1("RGBCapability", 3, paramPointer);
//    internalID, name, url, capability_number, Capability* Class, observesProperty_number, Property* Class
Semantic s1("SDEV", 1, &c1, 2, propertyPointer);

symAgent sdev1(120000, "RGB Leds HAT", true, &s1);

extern volatile boolean keepAlive_triggered;
extern NTPClient timeClient;
Metro registrationMetro = Metro();
int join_success = 0;

void restartSdev() {
  join_success = 0;
  if (sdev1.begin() == true) {
    int joinresp = sdev1.registry();
    if (joinresp < 300 and joinresp >= 200) {
      join_success = 1;
    } else {
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
    sdev1.join();
    // if 0 no RegExpiration
    if (sdev1.getRegExpiration() != 0) {
      Serial.print("\nREG expiration: ");
      Serial.println(sdev1.getRegExpiration());
      registrationMetro.interval(floor(sdev1.getRegExpiration() * 0.9));
    }
  }
  else Serial.print("Failed!");
  if (join_success) Serial.println("\nJoin success!");
}

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  if (! baro.begin()) {
    Serial.println("Couldnt find sensor");
    return;
  }
  Serial.println("Start...");
  pixels.begin(); // This initializes the NeoPixel library
  if (sdev1.begin() == true) {
    int joinresp = sdev1.registry();
    if (joinresp < 300 and joinresp >= 200) {
      join_success = 1;
    } else {
      join_success = 0;
      Serial.println("Error in JOIN message");
    }
    sdev1.join();
    // if 0 no RegExpiration
    if (sdev1.getRegExpiration() != 0) {
      Serial.print("\nREG expiration: ");
      Serial.println(sdev1.getRegExpiration());
      Serial.print("\nNext Keep-Alive: ");
      Serial.println((unsigned long int)floor(sdev1.getRegExpiration() * 0.9));
      registrationMetro.interval((unsigned long int)floor(sdev1.getRegExpiration() * 0.9));
    }
  }
  else Serial.print("Failed!");
  Serial.println("\n\n***************\n");
  Serial.print("Pressure:\t");
  Serial.println(readPr());

  Serial.print("Temperature:\t");
  Serial.println(readTemp());
  if (join_success) Serial.println("\nJoin success!");
  //delay(5000);
  //sdev1.TestelaborateQuery(tmpTestJson2);
  //delay(3000);
  //sdev1.TestelaborateQuery(tmpTestJson3);
}

void loop() {
  // put your main code here, to run repeatedly:
  String resp;

  delay(10);
  if (keepAlive_triggered && join_success == 1) {
    if (timeClient.forceUpdate()) {
      //Serial.println("NTP update OK");
    } else {
      //Serial.println("NTP update KO");
    }
    int sCode = sdev1.sendKeepAlive(resp);
    Serial.print("Keep-Alive code: ");
    Serial.println(sCode);
      // keep trace of the failed keepalive
    if (sCode >= 300) count++;
    else count = 0;
    if (count > 9) {
      Serial.print("Too many keep-alive failed, restarting SDEV...");
      restartSdev();
    }
  }
  sdev1.handleSSPRequest();
}

