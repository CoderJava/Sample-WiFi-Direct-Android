package com.ysn.examplewifidirect.wifidirect;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.util.Log;

import com.ysn.examplewifidirect.MainActivity;
import com.ysn.examplewifidirect.R;
import com.ysn.examplewifidirect.utils.Utility;

public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "WiFiDirect";

    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel channel;
    private MainActivity activity;

    public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                       MainActivity activity) {
        super();
        this.wifiP2pManager = manager;
        this.channel = channel;
        this.activity = activity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equalsIgnoreCase(action)) {
            // UI update to indicate wifi p2p status
            int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
            if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                // Wifi direct mode is enabled
                activity.setIsWifiP2pEnabled(true);
            } else {
                activity.setIsWifiP2pEnabled(false);
            }
            Log.d(TAG, "P2P state changed - " + state);
        } else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equalsIgnoreCase(action)) {
            // request available peers from the wifi p2p manager. This is an
            // asynchronous call and the calling activity is notified with a
            // callback on PeerListener.onPeersAvailable()
            if (wifiP2pManager != null) {
                wifiP2pManager.requestPeers(channel, activity);
            }
            Log.d(TAG, "P2P peers changed");
        } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equalsIgnoreCase(action)) {
            if (wifiP2pManager == null) {
                return;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            WifiP2pInfo p2pInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_INFO);

            if (p2pInfo != null && p2pInfo.groupOwnerAddress != null) {
                String goAddress = Utility.getDottedDecimalIP(p2pInfo.groupOwnerAddress.getAddress());
                boolean isGroupOwner = p2pInfo.isGroupOwner;
            }
            Log.d(TAG, "networkInfo.isConnected: " + networkInfo.isConnected());
            if (networkInfo.isConnected()) {
                // we are connected with the other device, request connection
                // info to find group owner IP
                wifiP2pManager.requestConnectionInfo(channel, activity);
            } else {
                // It's a disconnect
                // activity.resetData();
                activity.receivedDisconnectedFromServer();
            }
        } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equalsIgnoreCase(action)) {
//            Not needted for our use cast
//            DeviceListFragment fragment = activity.getFragmentManager()
//                    .findFragmentById(R.id.frag_list);
//            WifiP2pDevice device = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
//            fragment.updateThisDevice(device);
        }
    }
}
