package com.example.cy.bleservicetest;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private static String TAG = "bleServiceTest";
    private Intent bleSvcIntent;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "onCreate");

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        bleSvcIntent = new Intent(this, bleService.class);
    }


    public void startBleService(View v){

        startService(bleSvcIntent);
        Log.i(TAG, "startBleService");
    }

    public void stopBleService(View v){
        stopService(bleSvcIntent);
        Log.i(TAG, "stopBleService");
    }


    public void chkServiceRunning(View v){

        Log.i(TAG, "chkServiceRunning");

        String status = "isServiceRunning: " + isServiceRunning(bleService.class);

        tvStatus.setText(status);
    }

    private boolean isServiceRunning(Class serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        Log.d(TAG, "target: " + serviceClass.getName());

        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            //Log.d(TAG, "service: " + service.service.getClassName());
            if (serviceClass.getName().equals(service.service.getClassName())) {
                Log.d(TAG, "service: " + service.service.getClassName() + "found!");
                return true;
            }
        }
        return false;
    }
}
