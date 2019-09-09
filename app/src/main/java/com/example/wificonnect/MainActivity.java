package com.example.wificonnect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
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
import android.widget.ImageView;
import android.widget.Toast;

import java.io.Console;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    String networkSSID;
    String networkPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        networkSSID = "NETGEAR56";
        networkPass = "vastflute432";
    }

    @Override
    protected void onResume() {
        super.onResume();

        WifiCheck();

        final int interval = 5000; // 1 Second
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

    @Override
    protected void onPause() {
        super.onPause();

        finish();
    }

    public void OpenDoorClick(View v){
        Toast.makeText(getApplicationContext(), "Sorry, this button doesn't do anything yet.", Toast.LENGTH_LONG).show();
    }

    private void WifiCheck() {
        if (!isConnectedTo(networkSSID)){
            Log.d("STATUS", "WiFi was not connected upon opening app.");
            findViewById(R.id.openDoor).setEnabled(false);
            SetStatusSymbol(0);

            ConnectToNetwork();
        }
        else{
            Log.d("STATUS", "WiFi was connected upon opening app.");
            SetStatusSymbol(2);
            findViewById(R.id.openDoor).setEnabled(true);
        }
    }

    private void SetStatusSymbol(int status){
        ImageView statusView = findViewById(R.id.statusView);
        switch (status){
            case 0:
                statusView.setImageResource(R.drawable.wifi_red);
                break;
            case 1:
                statusView.setImageResource(R.drawable.wifi_yellow);
                break;
            case 2:
                statusView.setImageResource(R.drawable.wifi_green);
                break;
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
                        findViewById(R.id.openDoor).setEnabled(true);
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


}
