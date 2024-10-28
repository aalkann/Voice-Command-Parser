# Voice Command Parser

## Summary

This is a project that java, kafka and docker technologies are used together.

Here I try to demonstrate how to use java, kafka and docker to process a stream of voice data.

We use AssemblyAI library to convert auido data from byte array to text (You should change the API key inside SpechToTextClient.java. Existing one won't work)

**NOTE**: Translate service are just mock classes. and Google Cloud Translation API can be used instead of mock class.

## Architecture
├───kafka-streams-voice-command-consumer 

├───kafka-streams-voice-command-parser 

├───kafka-streams-voice-command-producer 

└───kafka-voice-command-common 


**kafka-streams-voice-command-consumer**: This system connects to kafka topic and print the message on the console. 

**kafka-streams-voice-command-producer**: This system connects to kafka topic and produce voice events.

**kafka-streams-voice-command-parser**: This system connects to kafka, consume events from voice-commands topic, process and save them to recognized-commands or unrecognized-commands topics.

**kafka-voice-command-common**: This is a library app that contains common models, serdes objects and configuration utils.

## Docker

docker-compose file is available for you to try it and see the result on you machine.

![image](https://github.com/user-attachments/assets/09da3607-4cca-400d-aa13-f5fc893b284a)




