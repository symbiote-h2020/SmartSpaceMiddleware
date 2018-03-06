#include <semantic_resources.h>
#include <ArduinoJson.h>

Property propertyPointer[2] = {Property("temperature", "nc"), Property("humidity", "nc")};

Parameter paramPointer[3] = {Parameter("Red", "xsd:unsignedByte", "0", "255"), Parameter("Green", "xsd:unsignedByte", "0", "255"), Parameter("Blue", "xsd:unsignedByte", "0", "255")};

Capability c1("RGB", 3, paramPointer);

Semantic s1("sym1", "aggeggio", "192.168.97.55", 1, &c1, 2, propertyPointer);

StaticJsonBuffer<2000> _jsonBuff;

void setup() {
  // put your setup code here, to run once:
  Serial.begin(115200);
  String resp = s1.returnSemanticString();
  Serial.print(resp);
  _jsonBuff.clear();
  JsonObject& _root = _jsonBuff.parseObject(resp);
  if (!_root.success()) {
      Serial.println("parseObject() failed");
  }
  Serial.println("JSON:");
  _root.prettyPrintTo(Serial);
}

void loop() {
  // put your main code here, to run repeatedly:

}
