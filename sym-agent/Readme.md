Symbiote Agent resources
===========================================

This project contain a set of resources to use a symbiote agent in the symbiote ecosystem.
Symbiote is a platform cooperation and interoperability porject found by the European commission. Learn more at: https://www.symbiote-h2020.eu/

# Directory descriptions

## src

## test-sketch

# Library description

To correctly compile and use this library, you have to populate a couple of array. One represents the sensors measure you want ot report using the convention exposed in the symbiotic semantic. The other should contain the pointers to the functions which return those measurments as a string. You can use something like this:

```boolean setupBind(String* listResources, String (* functions[])() )
{
    //write all your resources
  listResources[0] = "temperature";
  listResources[1] = "pressure";

    // assign to "functions" referencies to functions that return sensors values
  functions[0] = readTemp;
  functions[1] = readPressure;
  return true;
}
```
Where readtTemp and readPressure are function defined in your skecth in some way like this:

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
You should also declare these extern variables in your code:

```extern volatile boolean keepAlive_triggered;
extern String listResources[RES_NUMBER];
extern String (* functions[RES_NUMBER])();
```

To correctly start up the agent please use this behaviour:

symAgent::begin()
setupBind();
symAgent::join()

This makes the agent to search some symbiotic wifi SSID (where wifi connection is used) and sends a message join to the ssp identified by the dns name `inkeeper.symbiote.org`. It also set up the keep alive timer update the global variable keepAlive_triggered to check where you need to send a keep alive message. Then to handle the ssp request you should call in your main loop the method symAgent::handleSSPRequest().
