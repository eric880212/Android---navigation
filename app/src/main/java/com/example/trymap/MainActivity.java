/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.trymap;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
//ui import
import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Handler;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
//speech import
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeech.OnInitListener;
//geo_coding
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.Address;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;


import java.io.InputStreamReader;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;


/**
 * This shows how to create a simple activity with a map and a marker on the map.
 */
public class MainActivity extends AppCompatActivity implements GoogleMap.OnMapClickListener, GoogleMap.OnMapLongClickListener,
        OnMapReadyCallback, OnInitListener, SensorEventListener {

    private GoogleMap mMap;
    public UiSettings mUiSettings;
    public EditText address_dest, address_origin;
    private Button speedtest,end_direction;
    private TextToSpeech tts;
    private LinearLayout layout;
    private Polyline polyline;

//    private Location location;

    LatLng Taiwan = new LatLng(25.02, 121.32);
    LatLng AcPos;
    LatLng test = new LatLng(24.968483, 121.193276);
    Marker ac, ph;
    public List<Marker> Accident = new ArrayList<Marker>();
    public List<Marker> Photo = new ArrayList<Marker>();

    String msg = null;
    //加速度監測
    private TextView tvSensors;
    private SensorManager mSensorManager;
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];

    //偵測甩動
    private long mLastUpdateTime;
    private static final long SPEED_SHRESHOLD = 3000;
    private static final int UPTATE_INTERVAL_TIME = 70;
    private float mLastX;                    //x軸體感(Sensor)偏移
    private float mLastY;                    //y軸體感(Sensor)偏移
    private float mLastZ;                    //z軸體感(Sensor)偏移
    private double mSpeed;                 //甩動力道數度
    private boolean is_direction = false;
    Location location = null;

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //speech
        tts = new TextToSpeech(this, this);
        setContentView(R.layout.activity_main);
        address_origin = (EditText) findViewById(R.id.addr_origin);
        address_dest = (EditText) findViewById(R.id.addr_dest);
        Button enter = findViewById(R.id.enter);
        Button accidentB = findViewById(R.id.accident);
        layout = (LinearLayout)findViewById(R.id.linearlayout);
        end_direction = findViewById(R.id.end_direction);

        end_direction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                is_direction = false;
                layout.setVisibility(View.VISIBLE);
                end_direction.setVisibility(View.GONE);
                polyline.remove();
            }
        });

        address_origin.setHint("出發地");
        address_dest.setHint("目的地");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);

//        tvSensors = (TextView) findViewById(R.id.tv_sensors);
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        assert mSensorManager != null;
        Sensor mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        //Thread mythread = new Thread(new MyServer());
        //mythread.start();
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we
     * just add a marker near Africa.
     */
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onMapReady(GoogleMap googlemap) {

        this.mMap = googlemap;
//        mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);

//        Taipei = mMap.addMarker(new MarkerOptions().position(Taiwan).title("Marker").draggable(true));
        getgeocode();
        getspeed();

        address_origin.setText("現在位置");
        address_dest.setText("中壢車站");
        if (checkPermissions()) {
            googlemap.setMyLocationEnabled(true);
            init(googlemap);
//            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//                return;
//            }

        }

        mMap.setOnMapClickListener(this);
        mMap.setOnMapLongClickListener(this);
        // Keep the UI Settings state in sync with the checkboxes.
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setCompassEnabled(true);
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);
        mMap.getUiSettings().setTiltGesturesEnabled(true);
        mMap.getUiSettings().setRotateGesturesEnabled(true);

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /**
     * 感測器資料變化時回撥
     */
    @Override
    public void onSensorChanged(SensorEvent event) {
// 在本例中，alpha 由 t / (t + dT)計算得來，
// 其中 t 是低通濾波器的時間常數，dT 是事件報送頻率
        final float alpha = (float) 0.8;
// 用低通濾波器分離出重力加速度
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
// 用高通濾波器剔除重力幹擾
        String accelerometer = "加速度感測器\n" + "x:" +
                (event.values[0] - gravity[0]) + "\n" + "y:" +
                (event.values[1] - gravity[1]) + "\n" + "z:" +
                (event.values[2] - gravity[2]);
//        tvSensors.setText(accelerometer);
        //當前觸發時間
        long mCurrentUpdateTime = System.currentTimeMillis();
        //觸發間隔時間 = 當前觸發時間 - 上次觸發時間
        long mTimeInterval = mCurrentUpdateTime - mLastUpdateTime;
        //若觸發間隔時間< 70 則return;
        if (mTimeInterval < UPTATE_INTERVAL_TIME) return;
        mLastUpdateTime = mCurrentUpdateTime;

        //取得xyz體感(Sensor)偏移
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        //甩動偏移速度 = xyz體感(Sensor)偏移 - 上次xyz體感(Sensor)偏移
        float mDeltaX = x - mLastX;
        float mDeltaY = y - mLastY;
        float mDeltaZ = z - mLastZ;

        mLastX = x;
        mLastY = y;
        mLastZ = z;

        //體感(Sensor)甩動力道速度公式
        mSpeed = Math.sqrt(mDeltaX * mDeltaX + mDeltaY * mDeltaY + mDeltaZ * mDeltaZ) / mTimeInterval * 10000;

        //若體感(Sensor)甩動速度大於等於甩動設定值則進入 (達到甩動力道及速度)
        if (mSpeed >= SPEED_SHRESHOLD) {
            tts.speak("震動注意", TextToSpeech.QUEUE_FLUSH, null);//發音
            //達到搖一搖甩動後要做的事
            client b = new client();
            String location_Now = String.valueOf((double) location.getLatitude()) + "," + String.valueOf((double)location.getLongitude());

            b.execute(location_Now);
        }

    }

    /**
     * 介面獲取焦點，按鈕可以點選時回撥
     */
    protected void onResume() {
        super.onResume();
//註冊加速度感測器
        mSensorManager.registerListener((SensorEventListener) this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),//感測器TYPE型別
                SensorManager.SENSOR_DELAY_NORMAL);//採集頻率

    }

    /**
     * 暫停Activity，介面獲取焦點，按鈕可以點選時回撥
     */
    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onMapClick(LatLng point) {

//        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
//        try {
//            List<Address> ADDR = geocoder.getFromLocation(point.latitude, point.longitude, 1);
//            getAddress(ADDR);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    String getAddress(List<Address> ADDR) {
        if (ADDR != null && ADDR.size() > 0) {
            Address address = ADDR.get(0);
            String addressText = String.format("%s-%s%s%s%s",
                    address.getCountryName(), //國家
                    address.getAdminArea(), //城市
                    address.getLocality(), //區
                    address.getThoroughfare(), //路
                    address.getSubThoroughfare() //巷號
            );
            return addressText;
//            tts.speak(addressText, TextToSpeech.QUEUE_FLUSH, null);//發音
        }
        return null;
    }

    void getgeocode() {

        for (int i = 0; i < 18; i++) {
            Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
            List<Address> addressLocation = null;
            try {
                addressLocation = geoCoder.getFromLocationName(String.valueOf(getResources().getStringArray(R.array.roadname)[i]), 1);
                AcPos = new LatLng(addressLocation.get(0).getLatitude(), addressLocation.get(0).getLongitude());
                ac = mMap.addMarker(new MarkerOptions().position(AcPos).title(getResources().getStringArray(R.array.roadname)[i]).visible(false)
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));
                Accident.add(ac);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    void getspeed() {

        for (int i = 0; i < 113; i++) {
            Geocoder geoCoder = new Geocoder(this, Locale.getDefault());
            double photoposx, photoposy;
            photoposx = Double.parseDouble(getResources().getStringArray(R.array.lat)[i]);
            photoposy = Double.parseDouble(getResources().getStringArray(R.array.lng)[i]);
            AcPos = new LatLng(photoposx, photoposy);

            ph = mMap.addMarker(new MarkerOptions().position(AcPos).title("測速").visible(false)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
            Photo.add(ph);

        }
    }

    @Override
    public void onMapLongClick(LatLng point) {

    }

    public void onInit(int status) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINESE);    //設定語言為英文
            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            } else {
                tts.setPitch(1);    //語調(1為正常語調；0.5比正常語調低一倍；2比正常語調高一倍)
                tts.setSpeechRate(1);    //速度(1為正常速度；0.5比正常速度慢一倍；2比正常速度快一倍)
            }
        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }

    public void onDestroy() {
        // shutdown tts
        super.onDestroy();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }


    public void ButtonClick(View view) {
        showLocation(mMap,location);
        if (mMap == null) {
            return;
        }
        final Dialog dialog = ProgressDialog.show(this, "計算路徑", "計算中");
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                dialog.dismiss();
            }
        }, 1500);
        String origin = address_origin.getText().toString();
        String dest = address_dest.getText().toString();
        String location_Now = String.valueOf((double) location.getLatitude()) + "," + String.valueOf((double)location.getLongitude());
        if(origin.equals("現在位置")){
            origin = location_Now;
        }
        String url = getDirectionsUrl(origin, dest);

        DownloadTask downloadTask = new DownloadTask();
        //Start downloading json data from Google Directions
        //API
        downloadTask.execute(url);

        layout.setVisibility(View.GONE);
        end_direction.setVisibility(View.VISIBLE);
        is_direction = true;
    }


    public void AccidentClick(View view){
        if(Accident.get(0).isVisible()==false){
            for(int i=0;i<18;i++)Accident.get(i).setVisible(true);
        }
        else
            for(int i=0;i<18;i++)Accident.get(i).setVisible(false);
    }

    public void SpeedClick(View view){
        if(Photo.get(0).isVisible()==false){
            for(int i=0;i<113;i++)Photo.get(i).setVisible(true);
        }
        else
            for(int i=0;i<113;i++)Photo.get(i).setVisible(false);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public void SituationClick(View view) {
        for(int i=0;i<4;i++){
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            HttpURLConnection connection = null;
            String APIUrl =
                    "https://traffic.transportdata.tw/MOTC/v1/Road/Traffic/Road/Taoyuan?$filter=RouteID%20eq%20'68000L21432'&$top=30&$format=JSON";
            // 申請的APPID
            // （FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF 為 Guest
            // 帳號，以IP作為API呼叫限制，請替換為註冊的APPID & APPKey）
            String APPID = "MQA4ADYANQBmAGYAMwAwAC0AOAA5ADYAMgAtADQAYwBmADgALQBiADUAOQA0AC0ANABkADMAMQBlAGMAOQBiAGIAMAA4AGEA";
            // 申請的APPKey
            String APPKey = "ZgBmADUAZABhADEAZAA2AC0ANgA2ADQAMgAtADQAYwBmADkALQBiAGEAOABhAC0AOAAwAGMAMABhAGQAYgBlAGMANgAxADYA";

            // 取得當下的UTC時間，Java8有提供時間格式DateTimeFormatter.RFC_1123_DATE_TIME
            // 但是格式與C#有一點不同，所以只能自行定義
            String xdate = getServerTime();
            System.out.println("中文"+xdate);
            String SignDate = "x-date: " + xdate;
            String respond = "";
            boolean isgzip = true;

            String Signature = "";
            try {
                // 取得加密簽章
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Signature = HMAC_SHA1.Signature(SignDate, APPKey);
                }
            } catch (    SignatureException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            System.out.println("Signature :" + Signature);
            String sAuth = "hmac username=\"" + APPID + "\", algorithm=\"hmac-sha1\", headers=\"x-date\", signature=\""
                    + Signature + "\"";
            System.out.println(sAuth);
            try {
                URL url = new URL(APIUrl);
                if ("https".equalsIgnoreCase(url.getProtocol())) {
                    SslUtils.ignoreSsl();
                }
                connection = (HttpURLConnection) url.openConnection();
                //connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.87 Safari/537.36");
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Authorization", sAuth);
                connection.setRequestProperty("x-date", xdate);
                connection.setRequestProperty("Accept-Encoding", "gzip");
                connection.setRequestProperty("Content-Type", "application/json ; charset=utf-8");
                connection.setDoInput(true);

                respond = connection.getResponseCode() + " " + connection.getResponseMessage();
                System.out.println("回傳狀態:" + respond);
                BufferedReader in;

                //判斷來源是否為gzip
                if ("gzip".equals(connection.getContentEncoding())) {
                    InputStreamReader reader = new InputStreamReader(new GZIPInputStream(connection.getInputStream()));
                    in = new BufferedReader(reader);
                } else {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    in = new BufferedReader(reader);
                }

                // 返回的數據已經過解壓
                StringBuffer buffer = new StringBuffer();
                // 讀取回傳資料
                String line = "";
                while ((line = in.readLine()) != null) {
                    System.out.println(line);
                    buffer.append(line).append("\r\n");
                }

                in.close();
                System.out.println(buffer.toString());

            } catch (ProtocolException e) {
                e.printStackTrace();
            }

            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public static String getServerTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        return dateFormat.format(calendar.getTime());
    }

    private String getDirectionsUrl(String origin, String dest) {
//         Origin of route
        String str_origin = "origin=" + origin;

        // Destination of route
        String str_dest = "destination=" + dest;

        // Sensor enabled
        String key = "key=AIzaSyB6U_yNrQmuqJhts2V1Y5CQWhkYYQ2s3uo";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + key;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + parameters;

        return url;
    }

    class MyServer implements Runnable
    {
        ServerSocket ss;
        Socket mysocket;
        DataInputStream dis;
        String message;
        Handler handler = new Handler();

        @Override
        public void run() {
            try {
                ss = new ServerSocket(9700);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(),"waiting for client: "+ message, Toast.LENGTH_SHORT).show();
                    }
                });
                while(true){
                    mysocket = ss.accept();
                    dis = new DataInputStream(mysocket.getInputStream());
                    message = dis.readUTF();
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"message received from client: "+ message, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class client extends AsyncTask<String,Void,Void> {
        Socket s;
        DataOutputStream dos;
        PrintWriter pw;
        @Override
        protected Void doInBackground(String... voids) {
            String message = voids[0];

            try {
                s = new Socket("172.20.10.2",1234);
                pw = new PrintWriter(s.getOutputStream());
                pw.write(message);
                pw.flush();
                pw.close();
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    //我的IP
    private String getMyIp(){
        //新增一個WifiManager物件並取得WIFI_SERVICE
        WifiManager wifi_service = (WifiManager)getApplicationContext().getSystemService(WIFI_SERVICE);
        //取得wifi資訊
        WifiInfo wifiInfo = wifi_service.getConnectionInfo();
        //取得IP，但這會是一個詭異的數字，還要再自己換算才行
        int ipAddress = wifiInfo.getIpAddress();
        //利用位移運算和AND運算計算IP
        String ip = String.format("%d.%d.%d.%d",(ipAddress & 0xff),(ipAddress >> 8 & 0xff),(ipAddress >> 16 & 0xff),(ipAddress >> 24 & 0xff));
        return ip;
    }


    private class DownloadTask extends AsyncTask<String, Void, String> {

        // Downloading data in non-ui thread
        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        // Executes in UI thread, after the execution of
        // doInBackground()
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();

            br.close();

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    private class ParserTask extends
            AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DirectionsJSONParser parser = new DirectionsJSONParser();

                // Starts parsing data
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points = null;
            PolylineOptions lineOptions = null;
            MarkerOptions markerOptions = new MarkerOptions();

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<LatLng>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(5);  //導航路徑寬度
                lineOptions.color(Color.BLUE); //導航路徑顏色

            }

            // Drawing polyline in the Google Map for the i-th route
            polyline = mMap.addPolyline(lineOptions);
        }
    }

    private boolean checkPermissions() {
        final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 0;
        final int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 0;

        boolean ACCESS_FINE_LOCATION = false;
        boolean ACCESS_COARSE_LOCATION = false;

        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            ACCESS_FINE_LOCATION = true;
        }
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);
                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        } else {
            // Permission has already been granted
            ACCESS_COARSE_LOCATION = true;
        }
        return ACCESS_FINE_LOCATION && ACCESS_COARSE_LOCATION;
    }

    private LocationManager locationManager = null;
    private String provider;

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void init(GoogleMap googleMap) {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        List<String> providerList = locationManager.getProviders(true);
        if (providerList.contains(LocationManager.GPS_PROVIDER)) {
            provider = LocationManager.GPS_PROVIDER;
        } else if (providerList.contains(LocationManager.NETWORK_PROVIDER)) {
            provider = LocationManager.NETWORK_PROVIDER;
        } else {
            Toast.makeText(this, "沒有位置提供器可使用", Toast.LENGTH_LONG).show();
            return;
        }
//        Toast.makeText(this, provider + "可使用", Toast.LENGTH_LONG).show();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            showLocation(googleMap,location);
        } else {
            String info = "無法獲得當前位置";
            while(location  == null)
            {
                locationManager.requestLocationUpdates("gps", 60000, 1, locationListener);
            }
            Toast.makeText(this, info, Toast.LENGTH_SHORT).show();

        }
        locationManager.requestLocationUpdates(provider, 100, 0, locationListener);
    }

    LocationListener locationListener = new LocationListener() {
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onProviderEnabled(String provider) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onProviderDisabled(String provider) {
            // TODO Auto-generated method stub
        }
        @Override
        public void onLocationChanged(Location location) {
            if(is_direction){
                String origin = String.valueOf((double) location.getLatitude()) + "," + String.valueOf((double)location.getLongitude());
                String dest = address_dest.getText().toString();
                String url = getDirectionsUrl(origin, dest);
                polyline.remove();
                DownloadTask downloadTask = new DownloadTask();
                //Start downloading json data from Google Directions
                //API
                downloadTask.execute(url);
                showLocation(mMap,location);
            }
            else{
                if(polyline != null)
                    polyline.remove();
            }


        }
    };

    private void showLocation(GoogleMap googleMap, Location location) {
        LatLng now = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(now));
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(18));
    }

    private final static String TAG = "MainActivity";
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0) {
                    for (int i = 0; i < permissions.length; i++) {
                        if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                            Log.d(TAG, permissions[i] + "allow");
                            init(mMap);
                        } else {
                            checkPermissions();
                            Log.d(TAG, permissions[i] + "not allow");
                        }
                    }
                } else {
                    Log.d(TAG, "no pm allow");
                }
                return;
        }
    }
}
