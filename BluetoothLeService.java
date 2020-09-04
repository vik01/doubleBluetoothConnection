package com.example.bluetoothapplication;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private String mBluetoothDeviceAddressTwo;
    private static BluetoothGatt mBluetoothGatt;
    private static BluetoothGatt mBluetoothGattTwo;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetoothapplication.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetoothapplication.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetoothapplication.ACTION_GATT_SERVICES_DISCOVERED";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        private int mState = 0;

        private void reset() {
            mState = 0;
        }

        private void advance() {
            mState++;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                boolean serviceDiscover = gatt.discoverServices();
                Log.d(TAG, "Discover services: " + serviceDiscover);
                broadcastUpdate(ACTION_GATT_CONNECTED);
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                Log.d(TAG, "onServicesDiscovered not received: " + status);
                reset();
                readAllCharacteristics(gatt);
            } else if (status == BluetoothGatt.GATT_FAILURE){
                Log.d(TAG, "onServicesDiscovered not received: " + status);
            }
        }

        private void readAllCharacteristics(BluetoothGatt gatt) {
            // reads characteristics one by one as a state machine
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "reading ALS characteristic");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.ALS_STRING));
                    break;
                case 1:
                    Log.d(TAG, "reading Capsense characteristic");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.CAP_SENSE_STRING));
                    break;
                case 2:
                    Log.d(TAG, "reading Battery characteristic");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.BATTERY_STRING));
                    break;
                default:
                    Log.d(TAG, "All characteristics read");
                    return;
            }
            gatt.readCharacteristic(characteristic);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            // called when a characteristic is read
            if (gatt == mBluetoothGatt) { // if reading first device
                characteristicUpdate(characteristic);
            } else if (gatt == mBluetoothGattTwo) { // if reading second device
                characteristicUpdateTwo(characteristic);
            }
            setNotifications(gatt);
        }

        /*
         * Enable notification of changes on the data characteristic for each sensor
         * by writing the ENABLE_NOTIFICATION_VALUE flag to that characteristic's
         * configuration descriptor.
         */
        private void setNotifications(BluetoothGatt gatt) {
            BluetoothGattCharacteristic characteristic;
            switch (mState) {
                case 0:
                    Log.d(TAG, "Enabling Ambient Light Sensor notification");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.ALS_STRING));
                    break;
                case 1:
                    Log.d(TAG, "Enabling Cap Sense notification");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.CAP_SENSE_STRING));
                    break;
                case 2:
                    Log.d(TAG, "Enabling Battery characteristic");
                    characteristic = gatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.BATTERY_STRING));
                    break;
                default:
                    Log.d(TAG, "All notifications enabled");
                    return;
            }
            // Enable Local notification
            gatt.setCharacteristicNotification(characteristic, true);

            //Enable remote notifications
            Log.d(TAG, "Getting Descriptor");
            BluetoothGattDescriptor desc = characteristic.getDescriptor(UUID.fromString(Attributes.CONFIG_DESCRIPTOR));
            desc.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            Log.d(TAG, Arrays.toString(desc.getValue()));
            gatt.writeDescriptor(desc);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            advance();
            readAllCharacteristics(gatt);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            // After notifications are enabled, all updates from the device on characteristic value changes will be posted here.
            if (gatt == mBluetoothGatt) { // if reading first device
                characteristicUpdate(characteristic);
            } else if (gatt == mBluetoothGattTwo) { // if reading second device
                characteristicUpdateTwo(characteristic);
            }
        }
    };

    public void writeLedCharacteristic(boolean value) {
        byte[] byteVal = new byte[1];
        if (value) {
            byteVal[0] = (byte) (1);
        }
        // get the LED characteristic of both devices
        BluetoothGattCharacteristic bluetoothGattCharacteristic =
                mBluetoothGatt.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.LED_STRING));

        bluetoothGattCharacteristic.setValue(byteVal); // sets the value of the LED characteristic for the first device

        BluetoothGattCharacteristic bluetoothGattCharacteristicTwo =
                mBluetoothGattTwo.getService(UUID.fromString(Attributes.SERVICE_STRING)).getCharacteristic(UUID.fromString(Attributes.LED_STRING));

        bluetoothGattCharacteristicTwo.setValue(byteVal); // sets the value if the LED characteristic for the second device

        // Writes the value of the characteristic (1 or 0) for both the devices
        mBluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
        mBluetoothGattTwo.writeCharacteristic(bluetoothGattCharacteristicTwo);
        DeviceControlActivity.changeLedStatus(byteVal[0]);
    }


    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void characteristicUpdate(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        if (uuid.equals(Attributes.ALS_STRING)) {
            int valueOfChar = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            DeviceControlActivity.alsStatus(valueOfChar);
        } else if (uuid.equals(Attributes.CAP_SENSE_STRING)) {
            byte[] mCapSenseValue = characteristic.getValue();
            DeviceControlActivity.capSenseStatus(mCapSenseValue);
        } else if (uuid.equals(Attributes.BATTERY_STRING)) {
            int mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            DeviceControlActivity.batteryUpdate(mBatteryLevel);
        }
    }

    private void characteristicUpdateTwo(BluetoothGattCharacteristic characteristic) {
        String uuid = characteristic.getUuid().toString();
        if (uuid.equals(Attributes.ALS_STRING)) {
            int valueOfChar = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            DeviceControlActivity.alsStatusTwo(valueOfChar);
        } else if (uuid.equals(Attributes.CAP_SENSE_STRING)) {
            byte[] mCapSenseValue = characteristic.getValue();
            DeviceControlActivity.capSenseStatusTwo(mCapSenseValue);
        } else if (uuid.equals(Attributes.BATTERY_STRING)) {
            int mBatteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
            DeviceControlActivity.batteryUpdateTwo(mBatteryLevel);
        }
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.i(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.i(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /*
     * Connects to the GATT server hosted on the Bluetooth LE device. Returns true if the connection is initiated successfully.
     * The connection result is reported asynchronously through the
     *  {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback. */
    public void connect(final String address, final String addressTwo) {
        if (mBluetoothAdapter == null || (address == null || addressTwo == null)) {
            Log.i(TAG, "BluetoothAdapter not initialized or unspecified address.");
        } else if ((address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) && (addressTwo.equals(mBluetoothDeviceAddressTwo) && mBluetoothGattTwo != null)) {
            // Previously connected first device. Try to reconnect.
            Log.i(TAG, "connecting to already connected devices");
            mBluetoothGatt.connect();
            mBluetoothGattTwo.connect();
        } else {
            Log.i(TAG, "Trying to create a new connection.");
            final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
            final BluetoothDevice deviceTwo = mBluetoothAdapter.getRemoteDevice(addressTwo);
            if (device == null || deviceTwo == null) {
                Log.i(TAG, "Device not found.  Unable to connect.");
            }
            mBluetoothGatt = device.connectGatt(this, true, mGattCallback); // this is BluetoothGatt = new BluetoothGatt connected to device
            mBluetoothGattTwo = deviceTwo.connectGatt(this, true, mGattCallback); // this is BluetoothGatt = new BluetoothGatt connected to device
            mBluetoothDeviceAddress = address;
            mBluetoothDeviceAddressTwo = addressTwo;
        }
    }

    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null || mBluetoothGattTwo == null) {
            Log.i(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
        mBluetoothGattTwo.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null || mBluetoothGattTwo == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
        mBluetoothGattTwo.close();
        mBluetoothGattTwo = null;
    }

    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }

    public List<BluetoothGattService> getSupportedGattServicesTwo() {
        if (mBluetoothGattTwo == null) return null;

        return mBluetoothGattTwo.getServices();
    }

}
