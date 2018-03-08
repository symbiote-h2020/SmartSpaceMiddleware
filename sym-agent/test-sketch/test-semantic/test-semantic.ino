#include <semantic_resources.h>
#include <ArduinoJson.h>
#include <Adafruit_MPL3115A2.h>
#include <Adafruit_NeoPixel.h>

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

Parameter paramPointer[3] = {Parameter("Red", "xsd:unsignedByte", "0", "255", &setRed), Parameter("Green", "xsd:unsignedByte", "0", "255", &setGreen), Parameter("Blue", "xsd:unsignedByte", "0", "255", &setBlue)};
	//	name, num
Capability c1("RGB", 3, paramPointer);
	//		internalID, name, url, capability_number, Capability* Class, observesProperty_number, Property* Class
Semantic s1("sym1", "aggeggio", "192.168.97.55", 1, &c1, 2, propertyPointer);

StaticJsonBuffer<2000> _jsonBuff;

//String (* functions[RES_NUMBER])();
//boolean (* actuatorsFunction[RES_NUMBER])(int);

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  pixels.begin();
  randomSeed(analogRead(0));
  if (! baro.begin()) {
    Serial.println("Couldnt find sensor");
  }
  String resp = s1.returnSemanticString();
  Serial.print(resp);
  _jsonBuff.clear();
  JsonObject& _root = _jsonBuff.parseObject(resp);
  if (!_root.success()) {
      Serial.println("parseObject() failed");
  }
  Serial.println("JSON:");
  _root.prettyPrintTo(Serial);
  Serial.println("\n\nTEST FUNCTION");
  Serial.print("Temp:\t");
  Serial.println(propertyPointer[0].getValue());
  Serial.print("Pressure:\t");
  Serial.println(propertyPointer[1].getValue());
  if (c1.actuateCapability("Green", 50)) {
    Serial.println("Actuate ok");
  } else Serial.println("Actuate KO");
}

void loop() {
  // put your main code here, to run repeatedly:
	uint32_t color = random(0xFFFFFFFF);
	pixels.setPixelColor(j, color);
	pixels.show();
  j++;
  if (j>=NUMPIXELS) j = 0;
	delay(5000);
}
