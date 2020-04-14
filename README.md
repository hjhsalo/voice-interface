# voice-interface

This repository contains the source code used to design and develop the the Master's Thesis titled Voice-controlled in-vehicle infotainment system.

# Installation instructions

* Set yourself an Amazon AWS account for accessing the Alexa Voice Service and Amazon Lambda
* Create a customized intent that triggers the Lambda function provided in this github page
* Give persmissions to the lambda function to access IoT:publish to enable communication between Lambda and your navigation client.
* Create the IoS MQTT broker which the navigation client can be connected  to.
* Set up the navigation client by creating a MQTT interface that can be connected to the broker.
* Utilize the Android client java example provided here to carry out the data request and management from the google services API. 
* Request your own keys to use and enable those from the Google API services console.
* Speech interface can be tested by either using the test environment provided in the Alexa Voice Service, or by logging in to a Alexa enabled device with the account used to create the custom intent.
