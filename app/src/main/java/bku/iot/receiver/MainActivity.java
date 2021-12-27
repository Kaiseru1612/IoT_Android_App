package bku.iot.receiver;

import androidx.appcompat.app.AppCompatActivity;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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

//import pl.pawelkleczkowski.customgauge.CustomGauge;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    LineChart mChart;
    ArrayList<Entry> tempValue = new ArrayList<Entry>();
    ArrayList<Entry> TDSValue = new ArrayList<Entry>();
    ArrayList<Entry> humidValue = new ArrayList<>();

    LineDataSet tempDataSet = null;
    LineDataSet TDSDataSet = null;
    LineDataSet HumidDataSet = null;

    private final String MaxminFileName = "value.txt";

    int tmpchartIndex=0;
    int TDSchartIndex=0;
    int HumidchartIndex = 0;


    MQTTHelper mqttHelper;
    TextView txtTemp, txtTDS, txtHumid, txtTDS_Status, txtTemp_status, txtHumid_status, txtlastUpdate;

    EditText valueToset;
    Spinner spinner;
    Button setValueButton;


    SharedPreferences sharedPreferences;

    int sentCounter=0;
    int printerrorMessageCounter = 0;
    String printerrorMessage="";

    String topicSent = "";
    int MAX_TDS, MIN_TDS, MAX_TEMP, MIN_TEMP, MAX_HUMID, MIN_HUMID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_main);

        txtTDS = findViewById(R.id.txtTds);
        txtTemp = findViewById(R.id.txtTemperature);
        txtHumid = findViewById(R.id.txtHumid);
        txtTDS_Status = findViewById(R.id.txtTDS_status);
        txtTemp_status = findViewById(R.id.txtTemperature_status);
        txtHumid_status = findViewById(R.id.txtHumid_status);
        txtlastUpdate = findViewById(R.id.lastUpdate);

        setValueButton = findViewById(R.id.set_value_button);
        spinner = findViewById(R.id.spinner);
        valueToset = findViewById(R.id.ValueToSet);

        mChart = findViewById(R.id.lineChart);

        sharedPreferences = getSharedPreferences("VALUE", MODE_PRIVATE);

        initData();
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


        setValueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!valueToset.getText().toString().equals("")){
                    int val = Integer.parseInt(valueToset.getText().toString());
                    String name = spinner.getSelectedItem().toString();
                    SharedPreferences.Editor editor = sharedPreferences.edit();

                    if (name.equals("Max Temperature")){
                        if (val <= MIN_TEMP){
                            Toast.makeText(MainActivity.this, "Max value smaller than min value", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            MAX_TEMP = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MAX_TEMP", val);
                        }
                    }
                    else if (name.equals("Min Temperature")){
                        if (val >= MAX_TEMP){
                            Toast.makeText(MainActivity.this, "Min value bigger than max value", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            MIN_TEMP = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MIN_TEMP", val);
                        }
                    }
                    else if (name.equals("Max TDS")){
                        if (val <= MIN_TDS){
                            Toast.makeText(MainActivity.this, "Max value smaller than min value", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            MAX_TDS = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MAX_TDS", val);
                        }

                    }
                    else if (name.equals("Min TDS")){
                        if (val >= MAX_TDS){
                            Toast.makeText(MainActivity.this, "Min value bigger than max value", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            MIN_TDS = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MIN_TDS", val);
                        }
                    }
                    else if (name.equals("Max Humid")){
                        if (val <= MIN_HUMID){
                            Toast.makeText(MainActivity.this, "Max value smaller than min value", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            MAX_HUMID = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MAX_HUMID", val);
                        }
                    }
                    else if (name.equals("Min Humid")){
                        if (val >= MAX_HUMID){
                            Toast.makeText(MainActivity.this, "Min value bigger than max value", Toast.LENGTH_SHORT).show();
                        }
                        else {
                            MIN_HUMID = val;
                            Toast.makeText(MainActivity.this, "set " + name, Toast.LENGTH_SHORT).show();
                            editor.putInt("MIN_HUMID", val);
                        }
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Please select a value to set", Toast.LENGTH_SHORT).show();
                    }
                    editor.apply();

                    // reevaluate data
                    initData();
                }
                else {
                    Toast.makeText(MainActivity.this, "No value to set", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void initData(){
        MAX_TEMP = sharedPreferences.getInt("MAX_TEMP", 100);
        MIN_TEMP = sharedPreferences.getInt("MIN_TEMP", 10);
        MAX_TDS = sharedPreferences.getInt("MAX_TDS", 100);
        MIN_TDS = sharedPreferences.getInt("MIN_TDS", 10);
        MAX_HUMID = sharedPreferences.getInt("MAX_HUMID", 100);
        MIN_HUMID = sharedPreferences.getInt("MIN_HUMID", 10);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // temp init
                int tmpval = sharedPreferences.getInt("TEMP_VALUE", 0);
                txtTemp.setText(tmpval + "°C");
                if (tmpval > MAX_TEMP){
                    txtTemp_status.setBackgroundColor(Color.RED);
                    txtTemp_status.setText("HIGH");
                }
                else if (tmpval < MIN_TEMP){
                    txtTemp_status.setBackgroundColor(Color.BLUE);
                    txtTemp_status.setText("LOW");
                }
                else {
                    txtTemp_status.setBackgroundColor(Color.GREEN);
                    txtTemp_status.setText("NORMAL");
                }

                // TDS init
                tmpval = sharedPreferences.getInt("TDS_VALUE", 0);
                txtTDS.setText(tmpval + "ppm");
                if (tmpval > MAX_TDS){
                    txtTDS_Status.setBackgroundColor(Color.RED);
                    txtTDS_Status.setText("HIGH");
                }
                else if (tmpval < MIN_TDS){
                    txtTDS_Status.setBackgroundColor(Color.BLUE);
                    txtTDS_Status.setText("LOW");
                }
                else {
                    txtTDS_Status.setBackgroundColor(Color.GREEN);
                    txtTDS_Status.setText("NORMAL");
                }
                // HUMID init
                tmpval = sharedPreferences.getInt("HUMID_VALUE", 0);
                txtHumid.setText(tmpval + "%");
                if (tmpval > MAX_HUMID){
                    txtHumid_status.setBackgroundColor(Color.RED);
                    txtHumid_status.setText("HIGH");
                }
                else if (tmpval < MIN_HUMID){
                    txtHumid_status.setBackgroundColor(Color.BLUE);
                    txtHumid_status.setText("LOW");
                }
                else {
                    txtHumid_status.setBackgroundColor(Color.GREEN);
                    txtHumid_status.setText("NORMAL");
                }


                // Date init
                String date = sharedPreferences.getString("UPDATE_TIME", "Last update: Unknown");
                txtlastUpdate.setText(date);
            }
        });
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


        byte[] b = value.getBytes(StandardCharsets.UTF_8);
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
            final DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyy HH:mm:ss");
            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {

                Log.d("mqtt", "Receive " + message.toString() +" from" + topic);
                /// TODO: TEMP
                SharedPreferences.Editor editor = sharedPreferences.edit();
                if(topic.equals("VinhTien1612/feeds/temp-channel")){
                    int tempvalue = Integer.parseInt(message.toString());
                    editor.putInt("TEMP_VALUE", tempvalue);
                    txtTemp.setText(message.toString()+"°C");
                    if (tempvalue > MAX_TEMP){
                        txtTemp_status.setText("HIGH");
                        txtTemp_status.setBackgroundColor(Color.RED);
                    }
                    else if (tempvalue < MIN_TEMP){
                        txtTemp_status.setText("LOW");
                        txtTemp_status.setBackgroundColor(Color.BLUE);
                    }
                    else {
                        txtTemp_status.setText("NORMAL");
                        txtTemp_status.setBackgroundColor(Color.GREEN);
                    }
                    tempValue.add(new Entry(tmpchartIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    tempDataSet = new LineDataSet(tempValue,"Temperature");
                    tempDataSet.setLineWidth(5);
                    tempDataSet.setColor(Color.RED);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (TDSDataSet != null) dataSets.add(TDSDataSet);
                    if (HumidDataSet != null) dataSets.add(HumidDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();
                }
                // TDS
                if (topic.equals("VinhTien1612/feeds/wind-speed-channel")){
                    int TDSvalue = Integer.parseInt(message.toString());
                    editor.putInt("TDS_VALUE", TDSvalue);
                    txtTDS.setText(message.toString() + " ppm");
                    if (TDSvalue > MAX_TDS){
                        txtTDS_Status.setText("HIGH");
                        txtTDS_Status.setBackgroundColor(Color.RED);
                    }
                    else if (TDSvalue < MIN_TDS){
                        txtTDS_Status.setText("LOW");
                        txtTDS_Status.setBackgroundColor(Color.BLUE);
                    }
                    else {
                        txtTDS_Status.setText("NORMAL");
                        txtTDS_Status.setBackgroundColor(Color.GREEN);
                    }
                    TDSValue.add(new Entry(TDSchartIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    TDSDataSet = new LineDataSet(TDSValue,"TDS");
                    TDSDataSet.setLineWidth(5);
                    TDSDataSet.setColor(Color.BLUE);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (TDSDataSet != null) dataSets.add(TDSDataSet);
                    if (HumidDataSet != null) dataSets.add(HumidDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();
                }
                /// TODO: humid
                if (topic.equals("VinhTien1612/feeds/humid-channel")){
                    int humidvalue = Integer.parseInt(message.toString());
                    editor.putInt("HUMID_VALUE", humidvalue);
                    txtHumid.setText(message.toString() + " %");
                    if (humidvalue > MAX_HUMID){
                        txtHumid_status.setText("HIGH");
                        txtHumid_status.setBackgroundColor(Color.RED);
                    }
                    else if (humidvalue < MIN_HUMID){
                        txtHumid_status.setText("LOW");
                        txtHumid_status.setBackgroundColor(Color.BLUE);
                    }
                    else {
                        txtHumid_status.setText("NORMAL");
                        txtHumid_status.setBackgroundColor(Color.GREEN);
                    }
                    humidValue.add(new Entry(HumidchartIndex++, Integer.parseInt(message.toString())));
                    ArrayList<ILineDataSet> dataSets = new ArrayList<>();
                    HumidDataSet = new LineDataSet(humidValue,"humid");
                    HumidDataSet.setLineWidth(5);
                    HumidDataSet.setColor(Color.GREEN);
                    if (tempDataSet != null) dataSets.add(tempDataSet);
                    if (TDSDataSet != null) dataSets.add(TDSDataSet);
                    if (HumidDataSet != null) dataSets.add(HumidDataSet);
                    LineData data = new LineData(dataSets);
                    mChart.setData(data);
                    mChart.invalidate();
                }
                Date date = new Date();
                String printDate = "Last update: "  + dateFormat.format(date);
                txtlastUpdate.setText(printDate);
                editor.putString("UPDATE_TIME", printDate);

                /// TODO: errror conrtrol
/*                if(topic.equals("VinhTien1612/feeds/error-control")){
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
//                    txtError.setText(printerrorMessage);
                }
                if ((topic.equals("VinhTien1612/feeds/error-control") && (message.toString().contains("TDS") )) ){ //TODO: add temp, humid
                    sendMessageAgain=false;
                    topicSent="";
                    progressBar.setIndeterminate(false);
                    sentCounter = 0;
                    waitingPeriod=0;
                }*/



                // save
                editor.apply();
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