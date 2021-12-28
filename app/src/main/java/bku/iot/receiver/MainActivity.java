package bku.iot.receiver;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.listener.OnChartGestureListener;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.Charset;
import java.sql.Time;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    LineChart mChart;
    ArrayList<Entry> tempValue = new ArrayList<Entry>();
    ArrayList<Entry> CO2Value = new ArrayList<Entry>();
    ArrayList<Entry> lightValue = new ArrayList<>();

    LineDataSet tempDataSet = null;
    LineDataSet CO2DataSet = null;
    LineDataSet LightDataSet = null;

    int tmpchartIndex=0;
    int CO2chartIndex=0;
    int lightIndex = 0;


    MQTTHelper mqttHelper;
    TextView txtTemp, txtco2, txtError,txtHummi;
    ToggleButton toggleButtonLed;
    ToggleButton toggleButtonPump;
    ProgressBar progressBar;
    CustomGauge gaugeHumid;
    TextView txtWind, txtMoist, txtLight;
    int sentCounter=0;
    int printerrorMessageCounter = 0;
    String printerrorMessage="";

    String topicSent = "";






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        txtTemp = findViewById(R.id.txtTemperature);
        txtco2 = findViewById(R.id.txtco2);
        toggleButtonLed = findViewById(R.id.ledButton);
        toggleButtonPump = findViewById(R.id.pumpButton);
        progressBar = findViewById(R.id.pBar);
        txtError = findViewById(R.id.errorText);
        gaugeHumid = findViewById(R.id.humidgauge);
        txtHummi = findViewById(R.id.txthumidgaugeLabel);
        txtMoist = findViewById(R.id.txtSoilmoist);
        txtWind = findViewById(R.id.txtWindspeed);
        txtLight = findViewById(R.id.txtLightlevel);
        gaugeHumid.setValue(69);


        mChart = findViewById(R.id.lineChart);

        toggleButtonLed.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isCheckedLed) {
                toggleButtonLed.setVisibility(View.INVISIBLE);
                if (isCheckedLed){
                    Log.d("mqtt", "Button On");
                    sendDataMQTT("VinhTien1612/feeds/led-channel", "1");
                    topicSent = "LED";
                    progressBar.setIndeterminate(true);
                }
                else {
                    Log.d("mqtt", "Button Off");
                    sendDataMQTT("VinhTien1612/feeds/led-channel", "0");
                    topicSent = "LED";
                    progressBar.setIndeterminate(true);
                }
            }
        });

        toggleButtonPump.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isCheckedPump) {
                toggleButtonPump.setVisibility(View.INVISIBLE);
                if (isCheckedPump){
                    Log.d("mqtt", "Button On");
                    sendDataMQTT("VinhTien1612/feeds/pump-channel", "2");
                    topicSent = "PUMP";
                    progressBar.setIndeterminate(true);
                }
                else {
                    Log.d("mqtt", "Button Off");
                    sendDataMQTT("VinhTien1612/feeds/pump-channel", "3");
                    topicSent = "PUMP";
                    progressBar.setIndeterminate(true);
                }
            }
        });

        setupScheduler();
        startMQTT();
        mChart.setNoDataText("No data");
        mChart.setDrawBorders(true);

        Description description = new Description();
        description.setText("Data");
        mChart.setDescription(description);
        mChart.setBackgroundColor(Color.WHITE);
        mChart.setVisibleXRangeMaximum(10);
        mChart.setDragEnabled(true);

    }


    int waitingPeriod = 0;
    boolean sendMessageAgain = false;
    List<MQTTMessage> list = new ArrayList<>();

    private void setupScheduler(){
        Timer aTimer = new Timer();
        TimerTask scheduler = new TimerTask() {
            @Override
            public void run() {
//                Log.d("mqtt", "Timer is executed");

                if (waitingPeriod > 0){
                    waitingPeriod--;
                    if (waitingPeriod == 0){
                        sendMessageAgain = true;
                    }
                }
                /// TODO: stop after 3 times
                if (sendMessageAgain){
                    if (sentCounter < 3){
                        sendDataMQTT(list.get(0).topic, list.get(0).message);
                        list.remove(0);
//                        sentCounter++;
                    }
                    else {
                        sentCounter=0;
                        sendMessageAgain=false;
                        list.clear();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressBar.setIndeterminate(false);
                                toggleButtonLed.setVisibility(View.VISIBLE);
                                toggleButtonPump.setVisibility(View.VISIBLE);
                                txtError.setText("Failed!");
                            }
                        });
                    }
                }
            }
        };
        aTimer.schedule(scheduler, 5000,1000); /// delay:executed afer 5 sec, and the after 1
    }


    private void sendDataMQTT(String topic, String value){
        waitingPeriod = 3;
        sentCounter++;
        sendMessageAgain = false;
        MQTTMessage buffer = new MQTTMessage();
        buffer.topic = topic; buffer.message = value;
        list.add(buffer);

        MqttMessage msg = new MqttMessage();
        msg.setId(16122001);
        msg.setQos(0); // qua ..... of service 0-4 cang cao cang tin cay
        msg.setRetained(true); // 1 packet with true retain - forward packet


        byte[] b = value.getBytes(Charset.forName("UTF-8"));
        msg.setPayload(b);

        try {
            mqttHelper.mqttAndroidClient.publish(topic, msg);
        }catch (MqttException e){
        }
    }

    private String randomID(){
        int result = (int) Math.random();
        return Integer.toString(result);
    }
    private void startMQTT(){
        mqttHelper = new MQTTHelper(getApplicationContext(), "16122222222");

        mqttHelper.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                Log.d("mqtt", "Connection is successful");

            }

            @Override
            public void connectionLost(Throwable cause) {

            }
            DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                Log.d("mqtt", "Receive " + message.toString() +" from" + topic);
                if(topic.equals("VinhTien1612/feeds/temp-channel")){
                    txtTemp.setText(message.toString()+"°C");
                    tempValue.add(new Entry(tmpchartIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    tempDataSet = new LineDataSet(tempValue,"Temperature");
                    tempDataSet.setLineWidth(5);
                    tempDataSet.setColor(Color.RED);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (CO2DataSet != null) dataSets.add(CO2DataSet);
                    if (LightDataSet != null) dataSets.add(LightDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();
                }
                if (topic.equals("VinhTien1612/feeds/co2-channel")){
                    txtco2.setText(message.toString() + " ppm");
                    CO2Value.add(new Entry(CO2chartIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    CO2DataSet = new LineDataSet(CO2Value,"CO2");
                    CO2DataSet.setLineWidth(5);
                    CO2DataSet.setColor(Color.BLUE);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (CO2DataSet != null) dataSets.add(CO2DataSet);
                    if (LightDataSet != null) dataSets.add(LightDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();
                }
                if(topic.equals("VinhTien1612/feeds/humid-channel")){
                    txtHummi.setText(message.toString()+"%");
                    gaugeHumid.setValue(Integer.parseInt(message.toString()));
                }
                if(topic.equals("VinhTien1612/feeds/error-control")){
                    txtError.setText("Updating");
                    progressBar.setIndeterminate(false);

                    Date date = new Date();
                    if (printerrorMessageCounter < 2){
                        printerrorMessage +=dateFormat.format(date) + '\n' + message.toString();
                        printerrorMessageCounter++;
                    }
                    else {
                        printerrorMessageCounter=0;
                        printerrorMessage = "";
                        printerrorMessage +=dateFormat.format(date) + '\n' + message.toString();
                        printerrorMessageCounter++;
                    }
                    txtError.setText(printerrorMessage);
                }
                if (topic.equals("VinhTien1612/feeds/light-level-channel")){
                    txtLight.setText(message.toString() + " lux");
                    lightValue.add(new Entry(lightIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    LightDataSet = new LineDataSet(lightValue,"Light");
                    LightDataSet.setLineWidth(5);
                    LightDataSet.setColor(Color.YELLOW);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (CO2DataSet != null) dataSets.add(CO2DataSet);
                    if (LightDataSet != null) dataSets.add(LightDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();

                }
                if (topic.equals("VinhTien1612/feeds/wind-speed-channel")){
                    txtWind.setText(message.toString() + " m/s");
                }
                if (topic.equals("VinhTien1612/feeds/moist-channel")){
                    txtMoist.setText(message.toString() + " %");
                }
                if (topic.equals("VinhTien1612/feeds/led-channel")){
                        if (message.toString().equals("1")){
                            toggleButtonLed.setChecked(true);
                        }
                        else  if (message.toString().equals("0")){
                            toggleButtonLed.setChecked(false);
                        }
                }
                if (topic.equals("VinhTien1612/feeds/pump-channel")){
                    if (message.toString().equals("2")){
                        toggleButtonPump.setChecked(true);
                    }
                    else  if (message.toString().equals("3")){
                        toggleButtonPump.setChecked(false);
                    }
                }

                if ((topic.equals("VinhTien1612/feeds/error-control") && ((message.toString().contains("LED")) || (message.toString().contains("PUMP")))) ){ //TODO: improve later
                    sendMessageAgain=false;
                    topicSent="";
                    progressBar.setIndeterminate(false);
                    sentCounter = 0;
                    waitingPeriod=0;
                    toggleButtonLed.setVisibility(View.VISIBLE);
                    toggleButtonPump.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public  class MQTTMessage {
        public String topic;
        public String message;

        public MQTTMessage (){
            this.topic = "";
            this.message = "";
        }
        public MQTTMessage(String topic, String message){
            this.message = message;
            this.topic = topic;
        }
    }
}