package com.example.bluetoothapplication;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import com.example.android.bluetoothlegatt.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class DeviceControlActivity extends Activity {
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_NAME_TWO = "DEVICE_NAME_TWO";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRAS_DEVICE_ADDRESS_TWO = "DEVICE_ADDRESS_TWO";
    static String LIST_NAME = "NAME";
    static String LIST_UUID = "UUID";

    @SuppressLint("StaticFieldLeak")
    private static TextView ledStatus;
    @SuppressLint("StaticFieldLeak")
    private static TextView alsView;
    @SuppressLint("StaticFieldLeak")
    private static TextView capSenseView;
    @SuppressLint("StaticFieldLeak")
    private static TextView alsViewTwo;
    @SuppressLint("StaticFieldLeak")
    private static TextView capSenseViewTwo;
    @SuppressLint("StaticFieldLeak")
    private static TextView batteryView;
    @SuppressLint("StaticFieldLeak")
    private static TextView batteryViewTwo;
    private TextView mConnectionState;
    private TextView mConnectionStateTwo;
    private String mDeviceAddress;
    ExpandableListView mGattServicesList;
    private String mDeviceAddressTwo;
    private BluetoothLeService mBluetoothLeService;
    private boolean mConnected = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress, mDeviceAddressTwo);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                mConnectionState.setText(R.string.connected);
                mConnectionStateTwo.setText(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                mConnectionState.setText(R.string.disconnected);
                mConnectionStateTwo.setText(R.string.disconnected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices(), mBluetoothLeService.getSupportedGattServicesTwo());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();

        String mDeviceNameFirst = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        String mDeviceNameSecond = intent.getStringExtra(EXTRAS_DEVICE_NAME_TWO);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        mDeviceAddressTwo = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS_TWO);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        ((TextView) findViewById(R.id.device_address_two)).setText(mDeviceAddressTwo);
        ((TextView) findViewById(R.id.device_one)).setText(mDeviceNameFirst);
        ((TextView) findViewById(R.id.device_two)).setText(mDeviceNameSecond);
        mGattServicesList = findViewById(R.id.gatt_services_list);
        mConnectionState = findViewById(R.id.connection_state);
        mConnectionStateTwo = findViewById(R.id.connection_state_two);
        ledStatus = findViewById(R.id.LedStatus);
        alsView = findViewById(R.id.alsValue);
        capSenseView = findViewById(R.id.cpsValue);
        alsViewTwo = findViewById(R.id.alsValueTwo);
        capSenseViewTwo = findViewById(R.id.cpsValueTwo);
        batteryView = findViewById(R.id.batteryValue);
        batteryViewTwo = findViewById(R.id.batteryValueTwo);
        ledStatus.setText(R.string.Led_Status);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getActionBar()).setTitle("Control");
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect(mDeviceAddress, mDeviceAddressTwo);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress, mDeviceAddressTwo);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    public void displayGattServices(List<BluetoothGattService> gattServices, List<BluetoothGattService> gattServicesTwo) {
        if (gattServices == null || gattServicesTwo == null) return;
        String uuid;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData = new ArrayList<>();

        // Loops through the first device services
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(LIST_NAME, Attributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                HashMap<String, String> currentCharaData = new HashMap<>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, Attributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
        displayCharacteristics(gattServiceData, gattCharacteristicData);

    }

    public void displayCharacteristics(ArrayList<HashMap<String, String>> gattServiceData, ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData) {
        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2},
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[]{LIST_NAME, LIST_UUID},
                new int[]{android.R.id.text1, android.R.id.text2}
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    public void buttonOn(View view) {
        mBluetoothLeService.writeLedCharacteristic(true);
    }

    public void buttonOff(View view) {
        mBluetoothLeService.writeLedCharacteristic(false);
    }

    public static void changeLedStatus(int status) {
        if (status == 1) {
            ledStatus.setText(R.string.Led_On);
        } else {
            ledStatus.setText(R.string.Led_Off);
        }
    }

    public static void alsStatus(int status) {
        if (status >= 500) {
            alsView.setText(R.string.ALSSL); // Ambient light sensor sensing light
        } else {
            alsView.setText(R.string.AlSNSL); // Ambient light sensor not sensing light
        }
    }

    public static void capSenseStatus(byte[] status){
        if (status[0] == 0) {
            capSenseView.setText(R.string.no_touch);
        } else {
            capSenseView.setText(R.string.touch);
        }
    }

    public static void alsStatusTwo(int status) {
        if (status >= 500) {
            alsViewTwo.setText(R.string.ALSSL); // Ambient light sensor sensing light
        } else {
            alsViewTwo.setText(R.string.AlSNSL); // Ambient light sensor not sensing light
        }
    }

    public static void capSenseStatusTwo(byte[] status){
        if (status[0] == 0) {
            capSenseViewTwo.setText(R.string.no_touch);
        } else {
            capSenseViewTwo.setText(R.string.touch);
        }
    }

    public static void batteryUpdate(int batteryLevel) {
        batteryView.setText(batteryLevel);
    }

    public static void batteryUpdateTwo(int batteryLevel) {
        batteryViewTwo.setText(batteryLevel);
    }

}
