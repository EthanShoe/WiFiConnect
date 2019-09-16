package com.example.wificonnect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String networkSSID;
    String networkPass;
    int statusSymbol;
    int buttonMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkSSID = "NETGEAR56";
        networkPass = "vastflute432";

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

        finish();
    }

    public void OpenDoorClick(View v){
        if (buttonMode == 1){
            return;
        }
        OpenDoor();
    }

    private void OpenDoor() {
        SetButtonMode(1);
        try{
            URL url = new URL("http://192.168.1.10:8080/");
            WebView myWebView = (WebView) findViewById(R.id.webView);
            myWebView.setWebViewClient(new WebViewClient());
            myWebView.loadUrl("http://192.168.1.10:8080");
        } catch (MalformedURLException error){
            Log.d("STATUS", "MalformedURLException: " + error.toString());
        }
    }

    private void WifiCheck() {
        if (!isConnectedTo(networkSSID)){
            Log.d("STATUS", "WiFi was not connected.");
            SetButtonMode(0);
            SetStatusSymbol(0);

            ConnectToNetwork();

            final int interval = 7000; //7 Seconds
            Handler handler = new Handler();
            Runnable runnable = new Runnable(){
                public void run() {
                    if (!isConnectedTo(networkSSID)){
                        Toast.makeText(MainActivity.this, "The personal WiFi connection failed.", Toast.LENGTH_LONG).show();
                        SetStatusSymbol(0);
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
                        SetButtonMode(2);
                    }
                });

            }
        });
    }

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

                final int interval = 11000; //11 Seconds
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
