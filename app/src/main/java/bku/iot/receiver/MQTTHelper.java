package bku.iot.receiver;


import android.content.Context;
import android.util.Log;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Created by Le Trong Nhan on 18/02/2020.
 */

public class MQTTHelper {

    final String serverUri = "tcp://io.adafruit.com:1883";


    private String clientId = "";
//    final String subscriptionTopic = "VinhTien1612/f/+";


//    final String username = "locthanh2001";
//    final String password = "aio_PGQB79bInpzPc4JOf6vmG8pLRqnV";
    final String username = "VinhTien1612";
    final String password = "aio_LYpa43WgloRSLKb9v6cmE19NGtzL";

    public MqttAndroidClient mqttAndroidClient;

    public MQTTHelper(Context context, String _clientID){
        clientId = _clientID;
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                Log.w("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    private void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {

                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }

    private void subscribeToTopic() {
        try {

            String[] topics = new String[]{
                    /// TODO:
//                 "locthanh2001/feeds/tds"
                    "VinhTien1612/feeds/pump-channel",
                    "VinhTien1612/feeds/led-channel",
                    "VinhTien1612/feeds/humid-channel",
                    "VinhTien1612/feeds/co2-channel",
                    "VinhTien1612/feeds/temp-channel",
                    "VinhTien1612/feeds/error-control",
                    "VinhTien1612/feeds/moist-channel",
                    "VinhTien1612/feeds/wind-speed-channel",
                    "VinhTien1612/feeds/light-level-channel"
            };
            for(int i = 0; i < topics.length; i++) {
                mqttAndroidClient.subscribe(topics[i], 0, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        Log.w("Mqtt", "Subscribed!");

                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        Log.w("Mqtt", "Subscribed fail!");
                    }
                });
            }


        } catch (MqttException ex) {
            System.err.println("Exceptionst subscribing");
            ex.printStackTrace();
        }
    }
}
