package com.example.bluetoothapplication;

import java.util.HashMap;
import java.util.UUID;

public class Attributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String LED_STRING = "00001207-0000-1000-8000-00805f9b34fb";
    public static String SERVICE_STRING = "00001234-0000-1000-8000-00805f9b34fb";
    public static String CAP_SENSE_STRING = "00001235-0000-1000-8000-00805f9b34fb";
    public static String ALS_STRING = "00001236-0000-1000-8000-00805f9b34fb";
    public static String BATTERY_STRING = "00001238-0000-1000-8000-00805f9b34fb";
    public static String CONFIG_DESCRIPTOR = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Services
        attributes.put(SERVICE_STRING, "nRF52832 Services");

        // Characteristics
        attributes.put(LED_STRING, "LED Characteristic");
        attributes.put(ALS_STRING, "Ambient Light Sensor Characteristic");
        attributes.put(CAP_SENSE_STRING, "Capsense Characteristic");
        attributes.put(CONFIG_DESCRIPTOR, "Descriptor");
        attributes.put(BATTERY_STRING, "Battery Characteristic");
    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
