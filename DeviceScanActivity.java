package com.example.bluetoothapplication;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.example.android.bluetoothlegatt.R;

import java.util.LinkedList;
import java.util.Objects;

public class DeviceScanActivity extends Activity {
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private Handler mHandler;
    private LinkedList<BluetoothDevice> mLeDevices;

    private static final String DEVICE_NAME = "nRF52832 LED";
    private static final int REQUEST_ENABLE_BT = 1;
    // Stops scanning after 1 seconds.
    private static final long SCAN_PERIOD = 1000;
    private static final int REQUEST_ENABLE_LOCATION = 1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listitem_device);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Objects.requireNonNull(getActionBar()).setTitle(R.string.title_devices);
        }
        mHandler = new Handler();
        mLeDevices = new LinkedList<>();

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    public void displayToast(String message) {
        if (message.length() >= 50) {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        findViewById(R.id.show).setVisibility(View.INVISIBLE);
        findViewById(R.id.connect).setVisibility(View.INVISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        clear();
    }

    public void versionCheck() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            scanLeDevice(true);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void checkPermission() {
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            scanLeDevice(true);
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                Toast.makeText(this, "Location permission is required if you want to scan for BLE devices", Toast.LENGTH_SHORT).show();
            }
            requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_ENABLE_LOCATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ENABLE_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                scanLeDevice(true);

            } else {
                Toast.makeText(this, "Permission was not granted", Toast.LENGTH_SHORT).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            displayToast("Scanning for Device");
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);
            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            displayToast("Stopping Scan");
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    public void addDevice(BluetoothDevice device) {
        if (!mLeDevices.contains(device) && DEVICE_NAME.equals(device.getName())) {
            mLeDevices.add(device);
            displayToast("Added device: " + device.getName());
        }
    }

    public void clear() {
        mLeDevices.clear();
        ((TextView) findViewById(R.id.device_name)).setText("");
        ((TextView) findViewById(R.id.device_address)).setText("");
        ((TextView) findViewById(R.id.device_name2)).setText("");
        ((TextView) findViewById(R.id.device_address2)).setText("");

    }

    // Sets up the device name and address on the app. Real one
    public void setUpDevices(BluetoothDevice device, BluetoothDevice deviceTwo) {
        ((TextView) findViewById(R.id.device_name)).setText(device.getName());
        ((TextView) findViewById(R.id.device_address)).setText(device.getAddress());
        ((TextView) findViewById(R.id.device_name2)).setText(deviceTwo.getName());
        ((TextView) findViewById(R.id.device_address2)).setText(deviceTwo.getAddress());
    }


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addDevice(device);
                }
            });
        }
    };

    public void scanButton(View view) {
        clear();
        versionCheck();
        findViewById(R.id.show).setVisibility(View.VISIBLE);
    }

    public void showButton(View view) {
        if (mLeDevices != null) {
            BluetoothDevice getFirstDevice = mLeDevices.getFirst();
            BluetoothDevice getSecondDevice = mLeDevices.getLast();
            setUpDevices(getFirstDevice , getSecondDevice);
        } else {
            displayToast("Did not find the correct device, check and see if device is on, then try again");
        }
        findViewById(R.id.connect).setVisibility(View.VISIBLE);
    }

    public void connectButton(View view) {
        BluetoothDevice deviceFirst = mLeDevices.getFirst();
        BluetoothDevice deviceLast = mLeDevices.getLast();
        if (deviceFirst == null || deviceLast == null) return;
        final Intent intent = new Intent(DeviceScanActivity.this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, deviceFirst.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, deviceFirst.getAddress());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME_TWO, deviceLast.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS_TWO, deviceLast.getAddress());
        if (mScanning) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanning = false;
        }
        startActivity(intent);
    }

}