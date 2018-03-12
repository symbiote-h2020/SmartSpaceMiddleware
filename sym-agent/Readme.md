Symbiote Agent resources
===========================================
# warning: README outdated, need to be updated

This project contain a set of resources to use a symbiote agent in the symbiote ecosystem.
Symbiote is a platform cooperation and interoperability project found by the European commission. Learn more at: https://www.symbiote-h2020.eu/

# Version release

V. 0.2: add path ActuateResourceAgent to handle the actuation from SSP and various bugs fix.
V. 0.1: first release of the agent.

# Supported Platform

First release of the agent currently support ESP8266 arduino based platform like Adafruit HUZZAH and WEMOS.

# Directory descriptions

## src
It contains the library for the Arduino sym-agent. Please take in mind that you should create a directory in the `libraries/` path with the name `sym-agent` and you should copy the libraries files into this folder. 
Note that the `src` folder also contains a file named `symbiote-resources.h`. This is not actually used for firmware, but you should use the formalism enumerated inside it when describing the `observedProperties` of your agent.

## test-sketch
It contains a list of example sketch to test various functionality of the agent or define some sensor example.


# Library description

To correctly compile and use this library, you have to populate a couple of array. One represents the sensors measure you want to report using the convention exposed in the symbiotic semantic. The other should contain the pointers to the functions which return those measurments as a string. You can use something like this:

```boolean setupBind(String* listResources, String (* functions[])(), boolean (* actuatorsFunction[])(int) )
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
```
Remember that the list of the functions is ordered in the same way as you define the listResources String array; so you should map the list of the functions that read data from sensor at the index of the resources that are in read mode (not actuator) and the index of the functions array that doesn't correspond to a reading resource but to an actuator resource to point to a dummy funtion named dummyFunctionSensor (defined in the library). In the same way you should do that for the actuatorsFunction. You should map the index of the actuator resource to your actuator function and you should map the index of the resources that doesn't match to an actuator resource to a dummy function named dummyFunctionActuator (defined in the library).

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
And, for example, the actuateRele and actuateServo are the functions that should perform the actuation on the device.

You should also declare these extern variables in your code:

```extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
extern boolean (* actuatorsFunction[RES_NUMBER])(int);
```

To correctly start up the agent please use this behaviour:

- Define the number of your resources in the sym-agent.h ( RES_NUMBER )

Then call in your firmware the following method:

symAgent::begin()
symAgent::join()

This makes the agent to search some symbiotic wifi SSID (where wifi connection is used) and sends a message join to the ssp identified by the dns name `inkeeper.symbiote.org`. It also set up the keep alive timer update the global variable keepAlive_triggered to check where you need to send a keep alive message. Then to handle the ssp request you should call in your main loop the method symAgent::handleSSPRequest().
