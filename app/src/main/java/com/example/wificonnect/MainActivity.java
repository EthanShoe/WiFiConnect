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
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    String networkSSID;
    String networkPass;
    int statusSymbol;
    int buttonMode;
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
    }

    @Override
    protected void onResume() {
        super.onResume();

        SetButtonMode(0);

        WifiCheck();
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (statusSymbol != 2){ //if the process for connecting to wifi is still running
            this.finishAffinity();
            System.exit(0);//end the app
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        menu.findItem(R.id.autoOpen).setChecked(autoOpenChecked);
        menu.findItem(R.id.autoRetryConnect).setChecked(autoReconnectChecked);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
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
        editor.putBoolean("autoOpenChecked", autoOpenChecked);
        editor.putBoolean("autoReconnectChecked", autoReconnectChecked);
        editor.apply();

        return super.onOptionsItemSelected(item);
    }

    //when the open door button is clicked
    public void OpenDoorClick(View v){
        if (buttonMode == 1){
            return;
        }
        OpenDoor();
    }

    //method to open the door
    private void OpenDoor() {
        SetButtonMode(1);

        WebView myWebView = findViewById(R.id.webView);
        myWebView.setWebViewClient(new WebViewClient());
        myWebView.loadUrl("http://192.168.1.2:8080/open");
    }

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
                                SetButtonMode(2);

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
}
