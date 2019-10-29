package com.example.wificonnect;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.google.gson.Gson;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String networkSSID;
    String networkPass;
    String IPAddress;
    String userID;
    String lastIPNum;
    String roommateStatusString;
    int statusSymbol;
    int buttonMode;
    int currentUserID;
    int roommateUserID;
    boolean autoOpenChecked;
    boolean autoReconnectChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //set initial values
        networkSSID = "NETGEAR56";
        networkPass = "vastflute432";
        SharedPreferences sharedPreferences = getSharedPreferences("StoredValues", MODE_PRIVATE);
        userID = sharedPreferences.getString("userID", "1");
        lastIPNum = sharedPreferences.getString("lastIPNum", "2");
        IPAddress = String.format("http://192.168.1.%s:8080/", lastIPNum);
        autoOpenChecked = sharedPreferences.getBoolean("autoOpenChecked", true);
        autoReconnectChecked = sharedPreferences.getBoolean("autoReconnectChecked", true);

        findViewById(R.id.openDoor).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (buttonMode == 1){
                    OpenDoor();
                }
                return true;
            }
        });

        findViewById(R.id.statusView).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if (statusSymbol == 0){
                    Toast.makeText(getApplicationContext(), "Manual reconnected initiated.", Toast.LENGTH_LONG).show();
                    WifiCheck();
                }

                return false;
            }
        });

        RadioGroup radioGroup = findViewById(R.id.statusSelection);
        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int i) {
                int radioButtonID = radioGroup.getCheckedRadioButtonId();
                View radioButton = radioGroup.findViewById(radioButtonID);
                int buttonIndex = radioGroup.indexOfChild(radioButton);

                final int selectedStatus = buttonIndex - 1;

                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                MediaType JSON = MediaType.parse("application/json; charset=utf-8");
                                String content = String.format("{\"status\":%s}", selectedStatus);
                                RequestBody body = RequestBody.create(JSON, content);

                                Request request = new Request.Builder()
                                        .url(IPAddress + "api/users/" + currentUserID + "/")
                                        .addHeader("Content-Type", "application/json")
                                        .put(body) //PUT
                                        .build();

                                OkHttpClient client = new OkHttpClient();
                                Response response = client.newCall(request).execute();

                            } catch (IOException IO){

                            }
                        }
                    });

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        SetButtonMode(0);
        SetRoommateStatus(-1);

        WifiCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();

        final int interval = 10000; //10 Seconds
        Handler handler = new Handler();
        Runnable runnable = new Runnable(){
            public void run() {
                if (statusSymbol != 2){ //if the process for connecting to wifi is still running
                    System.exit(0); //end the app
                }
            }
        };
        handler.postDelayed(runnable, interval);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        menu.findItem(R.id.currentUser).setTitle(String.format("Current User: %s", userID));
        currentUserID = Integer.parseInt(userID);
        if(currentUserID == 1){
            roommateUserID = 2;
        } else{
            roommateUserID = 1;
        }
        menu.findItem(R.id.findIP).setTitle(IPAddress);
        menu.findItem(R.id.autoOpen).setChecked(autoOpenChecked);
        menu.findItem(R.id.autoRetryConnect).setChecked(autoReconnectChecked);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case R.id.currentUser:
                if(userID.equals("1")){
                    userID = "2";
                } else{
                    userID = "1";
                }
                item.setTitle("Current User: " + userID);
                Toast.makeText(this, "Please restart the app", Toast.LENGTH_LONG).show();
                break;

            case R.id.findIP:
                CycleIP();
                break;

            case R.id.autoOpen:
                if (item.isChecked()){
                    item.setChecked(false);
                    autoOpenChecked = false;
                }
                else{
                    item.setChecked(true);
                    autoOpenChecked = true;
                }
                break;

            case R.id.autoRetryConnect:
                if (item.isChecked()){
                    item.setChecked(false);
                    autoReconnectChecked = false;
                }
                else{
                    item.setChecked(true);
                    autoReconnectChecked = true;
                }
                break;
        }

        SharedPreferences sharedPreferences = getSharedPreferences("StoredValues", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("userID", userID);
        editor.putBoolean("autoOpenChecked", autoOpenChecked);
        editor.putBoolean("autoReconnectChecked", autoReconnectChecked);
        editor.apply();

        return super.onOptionsItemSelected(item);
    }

    //region Door Stuff

    //when the open door button is clicked
    public void OpenDoorClick(View v){
        if (buttonMode == 1){
            return;
        }
        OpenDoor();
        //set status to home
        try{
            RadioGroup radioGroup = findViewById(R.id.statusSelection);
            ((RadioButton)radioGroup.getChildAt(2)).setChecked(true);
        } finally {
            Toast.makeText(this, "Couldn't update status", Toast.LENGTH_LONG).show();
        }
    }

    //method to open the door
    private void OpenDoor() {
        SetButtonMode(1);

        WebView myWebView = findViewById(R.id.webView);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl(IPAddress + "open");
    }

    //endregion

    //region WiFi Network Stuff

    //checks whether or not the wifi is connected, and tries to connect if not connected
    private void WifiCheck() {
        if (!isConnectedTo(networkSSID)){
            Log.d("STATUS", "WiFi was not connected.");
            SetButtonMode(0);
            SetStatusSymbol(0);

            ConnectToNetwork();

            final int interval = 10000; //10 Seconds
            Handler handler = new Handler();
            Runnable runnable = new Runnable(){
                public void run() {
                    if (!isConnectedTo(networkSSID)){
                        Toast.makeText(MainActivity.this, "The personal WiFi connection failed.", Toast.LENGTH_LONG).show();
                        SetStatusSymbol(0);
                        if (autoReconnectChecked){
                            WifiCheck();
                        }
                    }
                }
            };
            handler.postDelayed(runnable, interval);
        }
        else{
            Log.d("STATUS", "WiFi is already connected.");
            SetStatusSymbol(2);
            SetButtonMode(2);

            GetRoommateStatuses();
        }
    }

    //starts connection attempt and checks for if and when successful
    private void ConnectToNetwork() {
        AttemptConnect(networkSSID, networkPass);

        //final boolean noConnection = true;
        SetStatusSymbol(1);

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                while (!isConnectedTo(networkSSID)){

                }

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), "Successfully connected to personal WiFi", Toast.LENGTH_LONG).show();
                        Log.d("STATUS", "WiFi was successfully connected.");
                        SetStatusSymbol(2);

                        final int interval = 1000; //1 Second
                        Handler handler = new Handler();
                        Runnable runnable = new Runnable(){
                            public void run() {
                                //set button mode and contact server
                                SetButtonMode(2);
                                GetRoommateStatuses();

                                //check if checkbox is checked and run OpenDoor();
                                if (autoOpenChecked){
                                    OpenDoor();
                                }
                            }
                        };
                        handler.postDelayed(runnable, interval);
                    }
                });

            }
        });
    }

    //method that connects to the network
    private void AttemptConnect(String networkSSID, String networkPass) {
        WifiConfiguration conf = new WifiConfiguration();
        conf.SSID = "\"" + networkSSID + "\"";   // Please note the quotes. String should contain ssid in quotes
        conf.preSharedKey = "\""+ networkPass +"\"";

        WifiManager wifiManager = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        wifiManager.addNetwork(conf);

        List<WifiConfiguration> list = wifiManager.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + networkSSID + "\"")) {
                wifiManager.disconnect();
                wifiManager.enableNetwork(i.networkId, true);
                wifiManager.reconnect();

                break;
            }
        }
    }

    //checks whether the phone is connected to specified SSID
    public boolean isConnectedTo(String ssid) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            //Request permission from user
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        }
        boolean isConnected = false;
        WifiManager wifi = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifi.getConnectionInfo();
        if (wifiInfo != null) {
            String currentConnectedSSID = wifiInfo.getSSID();
            if (currentConnectedSSID != null && ssid.equals(currentConnectedSSID.replaceAll("^\"|\"$", ""))) {
                isConnected = true;
            }
        }
        return isConnected;
    }

    //searches through IP addresses until it finds server
    public void CycleIP(){
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                boolean foundIP = false;
                for (int ipNumber = 0; ipNumber < 15; ipNumber++){
                    IPAddress = String.format("http://192.168.1.%s:8080/", ipNumber);

                    try{
                        String inputLine;
                        URL url = new URL(IPAddress + "api/users/");

                        //Create a connection
                        HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                        //Set methods and timeouts
                        connection.setRequestMethod("GET");
                        connection.setReadTimeout(5000);
                        connection.setConnectTimeout(5000);
                        connection.connect();

                        //Create a new InputStreamReader
                        InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

                        //Create a new buffered reader and String Builder
                        BufferedReader reader = new BufferedReader(streamReader);
                        StringBuilder stringBuilder = new StringBuilder();

                        //Check if the line we are reading is not null
                        while((inputLine = reader.readLine()) != null){
                            stringBuilder.append(inputLine);
                        }

                        //Close our InputStream and Buffered reader
                        reader.close();
                        streamReader.close();

                        foundIP = true;

                    } catch (IOException IO){

                    }

                    if (foundIP){
                        SharedPreferences sharedPreferences = getSharedPreferences("StoredValues", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("lastIPNum", Integer.toString(ipNumber));
                        editor.apply();

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), "Successfully found server IP Address", Toast.LENGTH_LONG).show();
                            }
                        });

                        break;
                    }
                }
            }
        });
    }

    //endregion

    //sets the wifi symbol
    private void SetStatusSymbol(int status){
        ImageView statusView = findViewById(R.id.statusView);
        switch (status){
            case 0:
                statusView.setImageResource(R.drawable.wifi_red);
                statusSymbol = 0;
                break;
            case 1:
                statusView.setImageResource(R.drawable.wifi_yellow);
                statusSymbol = 1;
                break;
            case 2:
                statusView.setImageResource(R.drawable.wifi_green);
                statusSymbol = 2;
                break;
        }
    }

    //sets the button mode
    private void SetButtonMode(int status){
        Button button = findViewById(R.id.openDoor);

        switch (status){
            case 0: //wifi not connected - disabled
                button.setBackgroundResource(R.drawable.button_disconnected);
                button.setEnabled(false);
                buttonMode = 0;
                break;
            case 1: //button pressed - wait
                button.setBackgroundResource(R.drawable.button_wait);
                button.setEnabled(true);
                buttonMode = 1;

                final int interval = 10000; //10 Seconds
                Handler handler = new Handler();
                Runnable runnable = new Runnable(){
                    public void run() {
                        SetButtonMode(2);
                    }
                };
                handler.postDelayed(runnable, interval);

                break;
            case 2: //button ready
                button.setBackgroundResource(R.drawable.button_ready);
                button.setEnabled(true);
                buttonMode = 2;
                break;
        }
    }

    //region Status functions

    //gets, parses, and uses retrieved statuses
    public void GetRoommateStatuses(){

        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                try{
                    String inputLine;

                    //Create a URL object holding our url
                    URL url = new URL(IPAddress + "api/users/");

                    //Create a connection
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                    //Set methods and timeouts
                    connection.setRequestMethod("GET");
                    connection.setReadTimeout(5000);
                    connection.setConnectTimeout(5000);
                    connection.connect();

                    //Create a new InputStreamReader
                    InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());

                    //Create a new buffered reader and String Builder
                    BufferedReader reader = new BufferedReader(streamReader);
                    StringBuilder stringBuilder = new StringBuilder();

                    //Check if the line we are reading is not null
                    while((inputLine = reader.readLine()) != null){
                        stringBuilder.append(inputLine);
                    }

                    //Close our InputStream and Buffered reader
                    reader.close();
                    streamReader.close();

                    // convert string to user object
                    Gson gson = new Gson();
                    final User[] userArray = gson.fromJson(stringBuilder.toString(), User[].class);

                    if (roommateUserID == 0) { //throw exception if no roommate status
                        throw new IOException("Didn't retrieve information from server");
                    }

                    //enable radio buttons
                    SetRadioButtonsEnabled(true);

                    //set both statuses from server
                    SetRoommateStatus(userArray[roommateUserID - 1].status);
                    if(userArray[roommateUserID - 1].status == 2){
                        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                        v.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            RadioGroup radioGroup = findViewById(R.id.statusSelection);
                            ((RadioButton)radioGroup.getChildAt(userArray[currentUserID - 1].status + 1)).setChecked(true);
                        }
                    });

                } catch (IOException IO){
                    roommateStatusString = "failedConnection";

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Runnable runnable = new Runnable(){
                                public void run() {
                                    GetRoommateStatuses();
                                }
                            };
                            Handler handler = new Handler();
                            handler.postDelayed(runnable, 1000);
                        }
                    });
                }
            }
        });

    }

    //sets status symbol for roommate
    public void SetRoommateStatus(final int status){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ImageView statusView = findViewById(R.id.roommateStatus);
                switch (status){
                    case -1:
                        statusView.setImageResource(R.drawable.server_disconect);
                        SetRadioButtonsEnabled(false);
                        break;
                    case 0:
                        statusView.setImageResource(R.drawable.roommate_away);
                        break;
                    case 1:
                        statusView.setImageResource(R.drawable.roommate_home);
                        break;
                    case 2:
                        statusView.setImageResource(R.drawable.roommate_sleeping);
                        break;
                }
            }
        });
    }

    //enables and disables the radio buttons
    public void SetRadioButtonsEnabled(final boolean value){
        runOnUiThread(new Runnable() {
        @Override
        public void run() {
            RadioGroup radioGroup = findViewById(R.id.statusSelection);
            for (int i = 0; i < (radioGroup.getChildCount()); i++) {
                (radioGroup.getChildAt(i)).setEnabled(value);
            }
        }
    });
    }

    //endregion
}
