package com.appstacle.m3s.jmouruja.androidnavigator;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttClientStatusCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttManager;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttNewMessageCallback;
import com.amazonaws.mobileconnectors.iot.AWSIotMqttQos;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;


//public class MainActivity extends AppCompatActivity {

public class MainActivity extends AppCompatActivity  {


    static final String LOG_TAG = MainActivity.class.getCanonicalName();

    // AWS Iot CLI describe-endpoint call returns: XXXXXXXXXX.iot.<region>.amazonaws.com,
    private static final String CUSTOMER_SPECIFIC_IOT_ENDPOINT = BuildConfig.ENDPOINT;
    String message;
    String currentLocation = "defaultLocation";
    EditText txtSubscribe;
    EditText txtTopic;
    EditText txtMessage;
    TextView tvLastMessage;
    TextView tvClientId;
    TextView tvStatus;
    Button btnConnect;
    Intent mapIntent;
    AWSIotMqttManager mqttManager;
    String clientId;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // setContentView(R.layout.activity_maps);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //txtSubscribe = findViewById(R.id.txtSubscribe);
        txtSubscribe = findViewById(R.id.txtSubscribe);
        txtTopic = findViewById(R.id.txtTopic);
        txtMessage = findViewById(R.id.txtMessage);

        tvLastMessage = findViewById(R.id.tvLastMessage);
        tvClientId = findViewById(R.id.tvClientId);
        tvStatus = findViewById(R.id.tvStatus);

        btnConnect = findViewById(R.id.btnConnect);
        btnConnect.setEnabled(false);
        //final Intent mapIntent = new Intent(this, MapsActivity.class);

        // MQTT client IDs are required to be unique per AWS IoT account.
        // This UUID is "practically unique" but does not _guarantee_
        // uniqueness.
        //clientId = UUID.randomUUID().toString();
        clientId = "";
        txtSubscribe.setText("topic/loc1");
        tvClientId.setText(clientId);
        tvClientId.setText("#######-####-####-####-############");

        // Initialize the credentials provider
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(
                getApplicationContext(),
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails result) {
                        latch.countDown();
                    }

                    @Override
                    public void onError(Exception e) {
                        latch.countDown();
                        Log.e(LOG_TAG, "onError: ", e);
                    }
                }
        );

        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // MQTT Client
        mqttManager = new AWSIotMqttManager(clientId, CUSTOMER_SPECIFIC_IOT_ENDPOINT);

        // Enable button once all clients are ready
        btnConnect.setEnabled(true);
    }
    
    public void connect(final View view) {
        Log.d(LOG_TAG, "clientId = " + clientId);

        try {
            mqttManager.connect(AWSMobileClient.getInstance(), new AWSIotMqttClientStatusCallback() {
                @Override
                public void onStatusChanged(final AWSIotMqttClientStatus status,
                                            final Throwable throwable) {
                    Log.d(LOG_TAG, "Status = " + String.valueOf(status));

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                           tvStatus.setText(status.toString());
                            if(String.valueOf(status).equals("Connected")) {
                            subscribe(view);
                            }
                            if (throwable != null) {
                                Log.e(LOG_TAG, "Connection error.", throwable);
                            }
                        }
                    });
                }
            });
        } catch (final Exception e) {
            Log.e(LOG_TAG, "Connection error.", e);
            tvStatus.setText("Error! " + e.getMessage());
        }
    }
    private boolean isCallable(Intent intent) {
        List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY);
        return list.size() > 0;
    }

public void subscribe(final View view) {
    //final Intent mapIntent = new Intent(this, MapsActivity.class);
    final String topic = txtSubscribe.getText().toString();
    mapIntent = new Intent(this, MapsActivity.class);
    Toast.makeText(this, "subscribed to topic", Toast.LENGTH_SHORT).show();
    try {
        mqttManager.subscribeToTopic(topic, AWSIotMqttQos.QOS0,
            new AWSIotMqttNewMessageCallback() {

                @Override
                public void onMessageArrived(final String topic, final byte[] data) {
                    Log.d("onMessageArrived", new String(data));
                    runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                            message = new String(data);
                            Log.d(LOG_TAG, "Message arrived: Topic: "+ topic + " Message:" + message);
                            tvLastMessage.setText(message);
                            if (message.contains("GOTO:")) {

                                Log.d(LOG_TAG, "currentlocation is: " + currentLocation);

                            }

                        if (!message.equals(currentLocation) || currentLocation == "defaultLocation") {

                            String retrievedLocation = message.substring(message.indexOf("GOTO:") + 4, message.length());
                            Log.e(LOG_TAG, retrievedLocation);
                            mapIntent.putExtra("LOCATION_DATA", retrievedLocation);
                            currentLocation = message;
                            if (!isCallable(mapIntent)) {
                                Log.e("StartActivity", "Activity was callable! finishing...");
                                finish();
                                startActivity(mapIntent);
                            }else {

                                startActivity(mapIntent);
                            }

                        }
                    }
                    });

                }
            });
    } catch (Exception e) {
        Log.e(LOG_TAG, "Subscription error.", e);
    }
    Log.d(LOG_TAG, "Outside of subscribe method");
}

    public void publish(final View view) {
        final String topic = txtTopic.getText().toString();
        final String msg = txtMessage.getText().toString();

        try {
            mqttManager.publishString(msg, topic, AWSIotMqttQos.QOS0);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Publish error.", e);
        }
    }

    public void disconnect(final View view) {
        try {
            mqttManager.disconnect();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Disconnect error.", e);
        }
    }
}
