package com.example.wificonnect;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.widget.ImageView;
import android.widget.Toast;

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

        WifiCheck();
    }

    @Override
    protected void onResume() {
        super.onResume();

        WifiCheck();
    }

    public void OpenDoorClick(View v){
        Toast.makeText(getApplicationContext(), "Sorry, this button doesn't do anything yet.", Toast.LENGTH_LONG).show();
    }

    private void WifiCheck() {
        if (!isConnectedTo(networkSSID)){
            findViewById(R.id.openDoor).setEnabled(false);
            ConnectToNetwork();
        }
        else{
            SetStatusSymbol(2);
            findViewById(R.id.openDoor).setEnabled(true);
        }
    }

    private void SetStatusSymbol(int status){
        ImageView statusView = findViewById(R.id.statusView);
        switch (status){
            case 0:
                statusView.setImageResource(R.drawable.wifi_red);
            case 1:
                statusView.setImageResource(R.drawable.wifi_yellow);
            case 2:
                statusView.setImageResource(R.drawable.wifi_green);
        }
    }

    private void ConnectToNetwork() {
        attemptConnect(networkSSID, networkPass);

        boolean noConnection = true;
        SetStatusSymbol(1);
        while (noConnection){
            noConnection = !isConnectedTo(networkSSID);
        }

        Toast.makeText(getApplicationContext(), "Successfully connected to personal WiFi", Toast.LENGTH_LONG).show();
        SetStatusSymbol(2);
        findViewById(R.id.openDoor).setEnabled(true);
    }

    private void attemptConnect(String networkSSID, String networkPass) {
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
