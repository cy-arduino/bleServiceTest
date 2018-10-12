package com.example.cy.bleservicetest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class bleService extends Service {
    private static String TAG = "bleAdvtService";

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;

    private Set<BluetoothDevice> mConnectedDevices = new HashSet<>();

    byte[] readResp = new byte[1];

    byte[] readWriteResp = new byte[1];

    byte[] readWriteNotifyResp = new byte[1];

    public static final UUID MY_BT_SERVICE_UUID = UUID.fromString("0000A101-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_BT_CHAR_FOR_READ_UUID = UUID.fromString("0000AAA0-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_BT_CHAR_FOR_READ_WRITE_UUID = UUID.fromString("0000AAA1-0000-1000-8000-00805f9b34fb");
    public static final UUID MY_BT_CHAR_FOR_READ_WRITE_NOTIFY_UUID = UUID.fromString("0000AAA2-0000-1000-8000-00805f9b34fb");

    public static UUID CLIENT_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");


    Boolean isBleSupported;


    public bleService() {

    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "onBind()");
        return null;
    }

    @Override
    public void onCreate() {
        Log.i(TAG, "onCreate()");

        mBluetoothManager = (BluetoothManager) getApplicationContext().getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();

        // We can't continue without proper Bluetooth support
        isBleSupported = checkBleSupport(bluetoothAdapter);

        readResp[0]=0;
        readWriteResp[0]=0;
        readWriteNotifyResp[0]=0;

        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStart()");
        if(!isBleSupported){
            Log.e(TAG, "BLE is not supported! Do nothing...");
        }else{
            startAdvertising();
            startServer();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy()");

        stopServer();
        stopAdvertising();

        super.onDestroy();
    }


    private void showToast(String toastMsg){

        final String m = toastMsg;
        new Handler(Looper.getMainLooper()).post(new Runnable(){
            public void run(){
                Toast.makeText(getApplicationContext(), m, Toast.LENGTH_LONG).show();
            }
        });

    }

    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {

        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: " + device);
                mConnectedDevices.add(device);
                showToast("BluetoothDevice CONNECTED:");
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: " + device);
                //Remove device from any active subscriptions
                mConnectedDevices.remove(device);
                showToast("BluetoothDevice DISCONNECTED:");
            }
            super.onConnectionStateChange(device, status, newState);
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

            if(MY_BT_CHAR_FOR_READ_UUID.equals(characteristic.getUuid()) ){
                readResp[0] += 1;
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        readResp);
            }

            if(MY_BT_CHAR_FOR_READ_WRITE_UUID.equals(characteristic.getUuid())){
                readWriteResp[0] += 2;
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        readWriteResp);
            }

            if(MY_BT_CHAR_FOR_READ_WRITE_NOTIFY_UUID.equals(characteristic.getUuid())){
                readWriteNotifyResp[0] +=3;
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        readWriteNotifyResp);
            }
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
            Log.d(TAG, "onCharacteristicWriteRequest");


            if(MY_BT_CHAR_FOR_READ_WRITE_UUID.equals(characteristic.getUuid()) ){
                Log.d(TAG, "uuid: " + MY_BT_CHAR_FOR_READ_WRITE_UUID);
                Log.d(TAG, "device: " + device);
                Log.d(TAG, "requestId: " + requestId);
                Log.d(TAG, "characteristic: " + characteristic);
                Log.d(TAG, "preparedWrite: " + preparedWrite);
                Log.d(TAG, "responseNeeded: " + responseNeeded);
                Log.d(TAG, "offset: " + offset);
                Log.d(TAG, "value: " + value);

                readWriteResp[0] = value[0];
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        readWriteResp);
            }




        }
    };

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
            showToast("LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: "+errorCode);
        }
    };

    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(MY_BT_SERVICE_UUID))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }


    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(getMyBleService());
    }
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }


    public static BluetoothGattService getMyBleService() {

        //service
        BluetoothGattService service = new BluetoothGattService(MY_BT_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        //characteristic 1: for read
        BluetoothGattCharacteristic forRead = new BluetoothGattCharacteristic(MY_BT_CHAR_FOR_READ_UUID,
                //Read-only characteristic
                BluetoothGattCharacteristic.PROPERTY_READ ,
                BluetoothGattCharacteristic.PERMISSION_READ);
/*        forRead.addDescriptor(
                new BluetoothGattDescriptor(CLIENT_CONFIG,
                    //Read-only descriptor
                    BluetoothGattDescriptor.PERMISSION_READ));
*/
        //characteristic 2: for write
        BluetoothGattCharacteristic forReadWrite = new BluetoothGattCharacteristic(MY_BT_CHAR_FOR_READ_WRITE_UUID,
                //Read-only characteristic
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
/*        forReadWrite.addDescriptor(
                new BluetoothGattDescriptor(CLIENT_CONFIG,
                    //Read/write descriptor
                    BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
*/
        //characteristic 3: for write
        BluetoothGattCharacteristic forReadWriteNotify = new BluetoothGattCharacteristic(MY_BT_CHAR_FOR_READ_WRITE_NOTIFY_UUID,
                //Read-only characteristic
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        /*forReadWriteNotify.addDescriptor(
                new BluetoothGattDescriptor(CLIENT_CONFIG,
                        //Read/write descriptor
                        BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
*/

        service.addCharacteristic(forRead);
        service.addCharacteristic(forReadWrite);
        service.addCharacteristic(forReadWriteNotify);

        return service;
    }


    private boolean checkBleSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported");
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported");
            return false;
        }

        return true;
    }
}
