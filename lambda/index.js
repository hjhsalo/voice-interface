var AWS = require('aws-sdk');
var https = require('https');
var iotdata =  new AWS.IotData({endpoint:process.env.ENDPOINT});
exports.handler = function (event, context) {
    
    try {
        if (event.session.new) {
            onSessionStarted({requestId: event.request.requestId}, event.session);
        }

        if (event.request.type === "LaunchRequest") {
            onLaunch(event.request,
                event.session,
                function callback(sessionAttributes, speechletResponse) {
                    context.succeed(buildResponse(sessionAttributes, speechletResponse));
                });
        } else if (event.request.type === "IntentRequest") {
           onIntent(event.request,
                event.session,
                function callback(sessionAttributes, speechletResponse) {
                    context.succeed(buildResponse(sessionAttributes, speechletResponse));
                });
        } else if (event.request.type === "SessionEndedRequest") {
            onSessionEnded(event.request, event.session);
            context.succeed();
        }
    } catch (e) {
        context.fail("Exception: " + e);
        
    }
    
};
/** Called when the session starts */
function onSessionStarted(sessionStartedRequest, session) {
    // add any session init logic here
}

/** Called when the user invokes the skill without specifying what they want. */
function onLaunch(launchRequest, session, callback) {
    var speechOutput = "Welcome to the Route Navigation Skill. Please provide a location."
    var reprompt = "Please give movement commands by speaking out the location you want to navigate to."
    var header = "Map Interface"
    var shouldEndSession = false
    var sessionAttributes = {
        "speechOutput" : speechOutput,
        "repromptText" : reprompt
    }
    callback(sessionAttributes, buildSpeechletResponse(header, speechOutput, reprompt,speechOutput, shouldEndSession))

}

/** Called when the user specifies an intent for this skill. */
function onIntent(intentRequest, session, callback) {

    var intent = intentRequest.intent
    var intentName = intentRequest.intent.name;
    if (intentName == "MapIntent") {
        handleMapIntentRequest(intent,session,callback);
    }
    else if (intentName == "AMAZON.HelpIntent") {
                //handleHelpRequest(intent, session, callback);
    }else if (intentName == "AMAZON.StopIntent" || intentName == "AMAZON.CancelIntent") {
        //handleFinishSessionRequest(intent, session, callback);
    } else {
        throw "Invalid intent"
    }
    
    
    // dispatch custom intents to handlers here
}

/** Called when the user ends the session - is not called when the skill returns shouldEndSession=true. */
function onSessionEnded(sessionEndedRequest, session) {

}
// ------- Helper functions to build responses for Alexa -------
function buildSpeechletResponse(title, output, repromptText,cardOutput, shouldEndSession) {
    return {
        outputSpeech: {
            type: "PlainText",
            text: output
        },
        card: {
            type: "Simple",
            title: title,
            content: cardOutput
        },
        reprompt: {
            outputSpeech: {
                type: "PlainText",
                text: repromptText
            }
        },
        shouldEndSession: shouldEndSession
    };
}

function buildSpeechletResponseWithoutCard(output, repromptText, shouldEndSession) {
    return {
        outputSpeech: {
            type: "PlainText",
            text: output
        },
        reprompt: {
            outputSpeech: {
                type: "PlainText",
                text: repromptText
            }
        },
        shouldEndSession: shouldEndSession
    };
}

function buildResponse(sessionAttributes, speechletResponse) {
    return {
        version: "1.0",
        sessionAttributes: sessionAttributes,
        response: speechletResponse
    };
}
function handleMapIntentRequest(intent, session, callback) {
    //First we check the most valid intent slot, knownLocation which provides us a streetname + number
    //If not available, check the next possible slots such as the Location.
    var title = "Location response"
    var rText = "Please provide a location";
    var speechOutput = "Welcome to the route navigation skill"
    var command;
    //console.log(intent.slots.knownlocation.value)
    //if (intent.slots.knownLocation.value === null || intent.slots.knownLocation.value === undefined) {
      if (intent.slots.knownLocation.value) {
          command = intent.slots.knownLocation.value.toLowerCase();
      }else if(intent.slots.germanLocation.value) {
          command = intent.slots.germanLocation.value.toLowerCase();
      }else
      {
        if (!intent.slots.Location.value) {
            //if no location found
            speechOutput = "Please provide a location";
            console.log("Callbacking from 'no location!' ")
                    callback(session.attributes, buildSpeechletResponse(title, speechOutput, rText,speechOutput, false))
        } else {
            command = intent.slots.Location.value.toLowerCase();
            //IF a location has been found, there might be a number value in the 
            //addressNumber slot, which we check and try to apply if possible
            //if(intent.slots.addressNumber.value !== 'undefined' || intent.slots.addressNumber.value !== null) {
            //    command = command + " "+ intent.slots.addressNumber.value;
            //}
        }
      }
      if (intent.slots.Country.value) {
          command = command + " " + intent.slots.Country.value.toLowerCase();
      }
    var initPromise = getConfirmedLocation(command);
    initPromise.then(function(result) {
    var obj = result;
    var coords;
    console.log("Seeing if obj is null")
    if (obj !== null) {
        console.log("Obj was not null!")
        if (obj.status == "ZERO_RESULTS") {
            speechOutput = "I couldnt find a location for " + command;
            callback(session.attributes, buildSpeechletResponse(title, speechOutput, rText,speechOutput, false));
        }
        else {
            speechOutput = "Giving directions to: " + obj.results[0].formatted_address;
            coords = (obj.results[0].geometry.location.lat + "," + obj.results[0].geometry.location.lng);
            console.log("Going to sendLocation()");
            sendLocation(coords).then(function(result) {
            console.log("result:", result)
            if (result == "Success") {
            console.log("Sent the location successfully!")
            rText = "";
            callback(session.attributes, buildSpeechletResponse(title, speechOutput,rText,(speechOutput + ", " + coords), false));
            }else {
                console.log("Location sending failed, result was: ",  result)
                speechOutput = "Failed to send the location to the navigation device";
                callback(session.attributes, buildSpeechletResponse(title, speechOutput, rText,speechOutput, false));
                }
            });
        }
        }
    else {
        speechOutput = "Error! Location is null";
        callback(session.attributes, buildSpeechletResponse(title, speechOutput, rText,speechOutput, false));

    }
            //callback(session.attributes, buildSpeechletResponse(title, speechOutput, rText,speechOutput, false));
        });
}


async function sendLocation(coords) {
    return new Promise((resolve,reject) => {
    console.log("Sending location" + coords);
    console.log("location info:", "GOTO: "+ coords)
    var params = {
        topic: "topic/loc1",
        payload: "GOTO: "+ coords,
        qos: 0
        };
        
        iotdata.publish(params, function(err, data){
            
        if(err){
            console.log("Error occured : ",err);
            reject(err)
        }
        else {
            console.log("Publish success!")
            resolve("Success")
        }
        })
        })
    }
    

async function getConfirmedLocation(location) {
    return new Promise((resolve,reject) => {
        var escapedLocation = require('querystring').escape(location);
        const queryString = ("/maps/api/geocode/json?address=" + escapedLocation + "&key=ENV_KEY");
        const options = {
            hostname: "maps.googleapis.com",
            path: queryString,
            port: 443,
            method: 'GET',
            json: true
        };
        https.get(options, function (res) {
            var json = '';
            res.on('data', function (chunk) {
                json += chunk;
            });
            res.on('end', function () {
                if (res.statusCode === 200) {
                    try {
                        var data = JSON.parse(json);
                        console.log(data);
                        // data is available here:
                        resolve(data);
                    } catch (e) {
                        console.log('Error parsing JSON!');
                    }
                } else {
                    console.log('Status:', res.statusCode);
                }
            });
        }).on('error', function (err) {
            console.log('Error:', err);
        });

   });
}
