package com.ysn.examplewifidirect.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.ByteOrder;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

public class Utility {

    private final static String sharedPreferencesName = "kkd";

    public static boolean copyFile(InputStream inputStream, OutputStream outputStream) {
        byte buf[] = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buf)) != -1) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static byte[] getInputStreamByArray(InputStream inputStream) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        try {
            while ((len = inputStream.read(buffer)) > -1) {
                baos.write(buffer, 0, len);
            }
            baos.flush();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return (baos.toByteArray());
    }

    public static String getMyMacAddress() {
        try {
            List<NetworkInterface> networkInterfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface networkInterface : networkInterfaces) {
                if (!networkInterface.getName().equalsIgnoreCase("wlan0")) {
                    continue;
                }
                byte[] macBytes = networkInterface.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder stringBuilder = new StringBuilder();
                for (byte macByte : macBytes) {
                    String hex = Integer.toHexString(macByte & 0xFF);
                    if (hex.length() == 1) {
                        hex = "0" + hex;
                    }
                    stringBuilder.append(hex + ":");
                }
                if (stringBuilder.length() > 0) {
                    stringBuilder.deleteCharAt(stringBuilder.length() - 1);
                }
                return stringBuilder.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "02:00:00:00:00:00";
    }

    public static String getMyIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface networkInterface = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = networkInterface.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getWiFiIPAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        String ip = getDottedDecimalIP(wifiManager.getConnectionInfo().getIpAddress());
        return ip;
    }

    public static String getDottedDecimalIP(int ipAddr) {
        if (ByteOrder.nativeOrder().equals(ByteOrder.LITTLE_ENDIAN)) {
            ipAddr = Integer.reverseBytes(ipAddr);
        }
        byte[] ipByteArray = BigInteger.valueOf(ipAddr).toByteArray();

        // convert to dotted decimal annotation
        String ipAddrStr = getDottedDecimalIP(ipByteArray);
        return ipAddrStr;
    }

    public static String getDottedDecimalIP(byte[] ipAddr) {
        // convert to dotted decimal notation
        String ipAddrStr = "";
        for (int a = 0; a < ipAddr.length; a++) {
            if (a > 0) {
                ipAddrStr += ".";
            }
            ipAddrStr += ipAddr[a] & 0xFF;
        }
        return ipAddrStr;
    }

    public static boolean isWifiConnected(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            if (wifiInfo != null && wifiInfo.getNetworkId() == -1) {
                return false; // not connected to an access point
            }
            return true; // connected to an Access Point
        } else {
            return false; // WiFi adapter is off
        }

    }

    public static boolean isWiFiEnable(Context context) {
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static void requestPermission(String strPermission, int perCode, Activity activity) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, strPermission)) {
            Toast.makeText(activity, "GPS permission allows us to access location data." +
                    " Please allow in App Settings for additional functionality.", Toast.LENGTH_SHORT)
                    .show();
        } else {
            ActivityCompat.requestPermissions(activity, new String[]{strPermission}, perCode);
        }
    }

    public static boolean checkPermission(String strPermission, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            int result = ContextCompat.checkSelfPermission(context, strPermission);
            if (result == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    public static void deletePersistentGroups(WifiP2pManager wifiP2pManager, WifiP2pManager.Channel channel) {
        try {
            Method[] methods = WifiP2pManager.class.getMethods();
            for (int a = 0; a < methods.length; a++) {
                if (methods[a].getName().equals("deletePersistentGroup")) {
                    // Delete any persistent group
                    for (int netid = 0; netid < 32; netid++) {
                        methods[a].invoke(wifiP2pManager, channel, netid, null);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void clearKey(Context context, String key) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit();
        prefsEditor.remove(key);
        prefsEditor.commit();
    }

    public static void saveString(Context context, String key, String value) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit();
        prefsEditor.putString(key, value);
        prefsEditor.commit();
    }

    public static String getString(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        String val = prefs.getString(key, null);
        return val;
    }

    public static void saveInt(Context context, String key, int value) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit();
        prefsEditor.putInt(key, value);
        prefsEditor.commit();
    }

    public static int getInt(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        int val = prefs.getInt(key, -1);
        return val;
    }

    public static void saveBool(Context context, String key, boolean value) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit();
        prefsEditor.putBoolean(key, value);
        prefsEditor.commit();
    }

    public static boolean getBool(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE);
        boolean val = sharedPreferences.getBoolean(key, false);
        return val;
    }

    public static void clearPreferences(Context context) {
        SharedPreferences.Editor prefsEditor = context.getSharedPreferences(sharedPreferencesName, Context.MODE_PRIVATE)
                .edit();
        prefsEditor.clear().commit();
    }

}
