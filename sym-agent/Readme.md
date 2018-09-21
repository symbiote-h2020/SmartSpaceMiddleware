Symbiote Agent resources
===========================================

This project contain a set of resources to use a symbiote agent in the symbiote ecosystem.
Symbiote is a platform cooperation and interoperability project found by the European commission. Learn more at: https://www.symbiote-h2020.eu/

# Supported Platform

First release of the agent currently support ESP8266 arduino based platform like Adafruit HUZZAH and WEMOS.

# Set up the environment
Download the last Arduino software.
Add the ESP8266 board inside the software, enter http://arduino.esp8266.com/stable/package_esp8266com_index.json into Additional Board Manager URLs field in the Arduino v1.6.4+ preferences.
Add the correct external library:
* ArduinoJson
* RestClient
* Crypto
* Hash
* sha1 ( please be sure to use the forked version at https://github.com/bbx10/Cryptosuite )
* base64
* ESP8266WebServer
* WiFiUdp
* NTPClient

# Directory descriptions

## src
It contains the library for the Arduino sym-agent. Please take in mind that you should create a directory in the `libraries/` path with the name `sym-agent` and you should copy the libraries files into this folder. 
Note that the `src` folder also contains a file named `symbiote-resources.h`. This is not actually used for firmware, but you should use the formalism enumerated inside it when describing the `observedProperties` of your agent.

## test-sketch
It contains a list of example sketch to test various functionality of the agent or define some sensor example.


# Library description

To correctly compile and use this library

In order to properly interface with sensor readings, you should create a function that performs the reading and return a String  that includes the measurment + <a white space> + the unit of measurment. Something like this:
"10.4 °C"
This could be an example of readTemp and readPressure functions defined in your skecth in some way like this:

```String readTemp()
{
  <<get data from sensors>>
  <<convert binary data to string>>
  return <<string>>;
}

String readPressure()
{
  <get data from sensors>>
  <<convert binary data to string>>
  return <<string>>;
}
```
In the same way, if the device should also act as an actuator, you should declare a function that takes an int value as param and return a boolean for each actuator you want to define.
And, for example, the actuateRele and actuateServo are the functions that should perform the actuation on the device.
This could be an example of the actuation of a led:

```
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
```

After that you should declare the classes to handle the semantic description of the SDEV you are building. You have 3 choices:


1. SDEV with only sensor resources (mapped in the class Property)
2. SDEV with only actuator resources (mapped in the classes Parameter and Capability)
3. SDEV with both actuator and sensor resources

#### SDEV with only sensor resources

You should declare an array of Property, one instance for each sensor property. Every instance should be linked with the function that reads the property and give back the measurment. Like this:

```
Property propertyPointer[2] = {Property("temperature", "nc", &readTemp), Property("pressure", "nc", &readPr)};
```
Then declare an empty Capability class with 0 param_num:

```
Parameter* paramPointer = NULL;
Capability c1("none", 0, paramPointer);
```
After that, you can create a Semantic Class, linking all these steps together:

```
Semantic s1("aggeggio", 1, &c1, 2, propertyPointer);
```

#### SDEV with only actuator resources

You should declare an empty _**Property**_ Class. Then you should declare an array of _**Parameter**_ Class for each actuation that you want to be handled by the SDEV.

```
Parameter paramPointer[3] = {Parameter("r", "xsd:unsignedByte", "0", "255", &setRed), Parameter("g", "xsd:unsignedByte", "0", "255", &setGreen), Parameter("b", "xsd:unsignedByte", "0", "255", &setBlue)};
```
Then create a _**Capability**_ class and link the array of _**Parameter**_:

```
Capability c1("RGBCapability", 3, paramPointer);
```

After that, you can create a _**Semantic**_ Class passing 0 as the obsNumber and a NULL pointer for propertyPointer:

```
Property* propertyPointer = NULL;
Semantic s1("aggeggio", 1, &c1, 0, propertyPointer);
```

#### SDEV with both actuator and sensor resources

In this case you should merge the two previous step, so do something like this:

```
Property propertyPointer[2] = {Property("temperature", "nc", &readTemp), Property("pressure", "nc", &readPr)};
Parameter paramPointer[3] = {Parameter("r", "xsd:unsignedByte", "0", "255", &setRed), Parameter("g", "xsd:unsignedByte", "0", "255", &setGreen), Parameter("b", "xsd:unsignedByte", "0", "255", &setBlue)};
Capability c1("RGBCapability", 3, paramPointer);
```
After that, you can create a Semantic Class, linking all these steps together:

```
Semantic s1("aggeggio", 1, &c1, 2, propertyPointer);
```

Then create the _**symAgent**_ Class, passing the keepalive interval, the name, the roaming feature and the just created _**Semantic**_ class:

```
symAgent sdev1(20000, "RGB Leds HAT", false, &s1);
```

After that you should call the following method to correctly setup the agent:

```
sdev1.begin();
sdev1.registry();
sdev1.join();
```
Then remember to handle the expiration of the registration, if it is different from 0 (so no registration expiration).

In the main loop, remember to call periodically the handleSSPRequest() to handle the requests coming from RAP.

_**Further information:**_     
* To enable/disable the debug print on Serial, define the macro DEBUG_SYM_CLASS to 1/0 in the sym_agent.h and lsp.h
* Please note that at this point the crypto protocol supported from both SDEV and Innkeeper for the lightweight security protocol used in the sym_agent class are TLS_PSK_WITH_AES_128_CBC_SHA and PBKDF2.
* More details are available in the Wiki of this repo.
