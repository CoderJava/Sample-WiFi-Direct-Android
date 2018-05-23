/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.ysn.examplewifidirect.adapter.AdapterDevice;
import com.ysn.examplewifidirect.db.DBAdapter;
import com.ysn.examplewifidirect.model.DeviceDTO;
import com.ysn.examplewifidirect.transfer.DataHandler;
import com.ysn.examplewifidirect.transfer.DataSender;
import com.ysn.examplewifidirect.transfer.TransferConstants;
import com.ysn.examplewifidirect.utils.ConnectionUtils;
import com.ysn.examplewifidirect.utils.DialogUtils;
import com.ysn.examplewifidirect.utils.Utility;
import com.ysn.examplewifidirect.wifidirect.WiFiDirectBroadcastReceiver;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements WifiP2pManager.PeerListListener, WifiP2pManager.ConnectionInfoListener {

    private final String TAG = getClass().getSimpleName();
    public static final String WRITE_PERMISSION = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    public static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;
    public static final int WRITE_PERM_REQ_CODE = 19;
    public static final int CAMERA_PERM_REQ_CODE = 20;
    public static final String FIRST_DEVICE_CONNECTED = "first_device_connected";
    public static final String KEY_FIRST_DEVICE_IP = "first_device_ip";

    private boolean isConnectionInfoSent = false;
    private DeviceDTO selectedDevice;
    private AppController appController;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifip2pChannel;
    private WiFiDirectBroadcastReceiver wiFiDirectBroadcastReceiver;

    private boolean isWifiP2pEnabled = false;
    private boolean isWDConnected = false;

    @BindView(R.id.text_view_username_activity_main)
    TextView textViewUsername;
    @BindView(R.id.text_view_port_activity_main)
    TextView textViewPort;
    @BindView(R.id.progress_bar_activity_main)
    ProgressBar progressBar;
    @BindView(R.id.recycler_view_activity_main)
    RecyclerView recyclerViewActivityMain;

    private ArrayList<DeviceDTO> deviceDTOS;
    private AdapterDevice adapterDevice;
    private boolean isDeviceListUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        initialize();
    }

    private void initialize() {
        String usernameHint = Build.MANUFACTURER;
        textViewUsername.setText(usernameHint);
        checkWritePermission();
        printInterfaces();

        progressBar.setVisibility(View.VISIBLE);
        String myIP = Utility.getWiFiIPAddress(this);
        Utility.saveString(this, TransferConstants.KEY_MY_IP, myIP);

        wifiP2pManager = (WifiP2pManager) getSystemService(WIFI_P2P_SERVICE);
        wifip2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);

        // starting connection listener with default port for now
        appController = (AppController) getApplicationContext();
        appController.startConnectionListener(TransferConstants.INITIAL_DEFAULT_PORT);

        progressBar.setVisibility(View.GONE);
        recyclerViewActivityMain.setVisibility(View.VISIBLE);
        deviceDTOS = new ArrayList<>();

        // connection request succeeded. No code needed here
        adapterDevice = new AdapterDevice(deviceDTOS, new AdapterDevice.ListenerAdapterDevice() {
            @Override
            public void onClickItemDevice(DeviceDTO deviceDTO) {
                if (!isWDConnected) {
                    Toast.makeText(MainActivity.this, "Please wait for connection", Toast.LENGTH_SHORT)
                            .show();
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = deviceDTO.getIp();
                    config.wps.setup = WpsInfo.PBC;
                    config.groupOwnerIntent = 4;
                    wifiP2pManager.connect(wifip2pChannel, config, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            // connection request succeeded. No code needed here
                            /*Toast.makeText(MainActivity.this, "Connection success", Toast.LENGTH_SHORT)
                                    .show();*/
                        }

                        @Override
                        public void onFailure(int reasonCode) {
                            Toast.makeText(MainActivity.this, "Connection failed, try again. reason: " + reasonCode, Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                } else {
                    selectedDevice = deviceDTO;
                    DialogUtils.getServiceSelectionDialog(MainActivity.this, deviceDTO).show();
                }
            }
        });
        recyclerViewActivityMain.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewActivityMain.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        recyclerViewActivityMain.setAdapter(adapterDevice);
    }

    @OnClick({
            R.id.fab_search_activity_main
    })
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.fab_search_activity_main:
                if (!isWDConnected) {
                    wifiP2pManager.discoverPeers(wifip2pChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            progressBar.setVisibility(View.VISIBLE);
                            recyclerViewActivityMain.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Peer discovery started", Toast.LENGTH_SHORT)
                                    .show();
                        }

                        @Override
                        public void onFailure(int i) {
                            Toast.makeText(MainActivity.this, "Peer discovery failure", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                }
        }
    }

    private void printInterfaces() {
        try {
            Enumeration<NetworkInterface> x = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface networkInterface : Collections.list(x)) {
                Log.d(TAG, "display name: " + networkInterface.getDisplayName());
                Log.d(TAG, "name: " + networkInterface.getName());
                Log.d(TAG, "is up and running: " + networkInterface.isUp());
                Log.d(TAG, "Loopback: " + networkInterface.isLoopback());
                Log.d(TAG, "Support multicast: " + networkInterface.supportsMulticast());
                Log.d(TAG, "Is virtual: " + networkInterface.isVirtual());
                Log.d(TAG, "Hardware address: " + Arrays.toString(networkInterface.getHardwareAddress()));
                Log.d(TAG, "Sub interfaces...");
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    Log.d(TAG, "sub network interface inetaddress: " + inetAddress.getHostAddress());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    private void checkWritePermission() {
        boolean isGrantedWritePermission = Utility.checkPermission(WRITE_PERMISSION, this);
        boolean isGrantedCameraPermission = Utility.checkPermission(CAMERA_PERMISSION, this);
        if (!isGrantedWritePermission) {
            Utility.requestPermission(WRITE_PERMISSION, WRITE_PERM_REQ_CODE, this);
        } else if (!isGrantedCameraPermission) {
            Utility.requestPermission(CAMERA_PERMISSION, CAMERA_PERM_REQ_CODE, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Toast.makeText(this, "This permissions is needed for file sharing.", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }
    }

    @SuppressLint("StringFormatMatches")
    @Override
    protected void onResume() {
        super.onResume();
        DBAdapter.getInstance(this).clearDatabase();
        textViewPort.setText(String.format(getString(R.string.port_info), ConnectionUtils.getPort(this)));

        IntentFilter localFilter = new IntentFilter();
        localFilter.addAction(DataHandler.DEVICE_LIST_CHANGED);
        localFilter.addAction(FIRST_DEVICE_CONNECTED);
        localFilter.addAction(DataHandler.CHAT_REQUEST_RECEIVED);
        localFilter.addAction(DataHandler.CHAT_RESPONSE_RECEIVED);
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver, localFilter);

        IntentFilter wifip2pFilter = new IntentFilter();
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        wifip2pFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        wiFiDirectBroadcastReceiver = new WiFiDirectBroadcastReceiver(wifiP2pManager, wifip2pChannel, this);
        registerReceiver(wiFiDirectBroadcastReceiver, wifip2pFilter);
        LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DataHandler.DEVICE_LIST_CHANGED));
    }

    @Override
    protected void onDestroy() {
        disconnect();
        super.onDestroy();
    }

    public void disconnect() {
        if (appController.isConnectionListenerRunning()) {
            appController.stopConnectionListener();
            Utility.clearPreferences(this);
            Utility.deletePersistentGroups(wifiP2pManager, wifip2pChannel);
            DBAdapter.getInstance(this).clearDatabase();
            wifiP2pManager.removeGroup(wifip2pChannel, new WifiP2pManager.ActionListener() {
                @Override
                public void onSuccess() {
                    isWDConnected = false;
                    isConnectionInfoSent = false;
                    isDeviceListUpdated = false;
                    if (!isFinishing()) {
                        Toast.makeText(MainActivity.this, "Disconnected device success", Toast.LENGTH_SHORT)
                                .show();
                    }
                }

                @Override
                public void onFailure(int i) {
                    if (!isFinishing()) {
                        Toast.makeText(MainActivity.this, "Disconnected device failed", Toast.LENGTH_SHORT)
                                .show();
                    }
                }
            });
        }
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
        Log.d(TAG, "onConnectionInfoAvailable");
        if (wifiP2pInfo.groupFormed && !wifiP2pInfo.isGroupOwner && !isConnectionInfoSent) {
            isWDConnected = true;
            appController.restartConnectionListenerWith(ConnectionUtils.getPort(MainActivity.this));
            String groupOwnerAddress = wifiP2pInfo.groupOwnerAddress.getHostAddress();
            DataSender.sendCurrentDeviceDataWD(MainActivity.this, groupOwnerAddress, TransferConstants.INITIAL_DEFAULT_PORT, true);
            isConnectionInfoSent = true;

            if (!isDeviceListUpdated) {
                LocalBroadcastManager.getInstance(this).sendBroadcast(new Intent(DataHandler.DEVICE_LIST_CHANGED));
                Toast.makeText(MainActivity.this, "Connection to another device success", Toast.LENGTH_SHORT)
                        .show();
            }
        }
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList wifiP2pDeviceList) {
        List<WifiP2pDevice> devices = new ArrayList<>();
        devices.addAll(wifiP2pDeviceList.getDeviceList());
        deviceDTOS.clear();
        for (WifiP2pDevice device : devices) {
            DeviceDTO deviceDTO = new DeviceDTO();
            deviceDTO.setIp(device.deviceAddress);
            deviceDTO.setPlayerName(device.deviceName);
            deviceDTO.setDeviceName(new String());
            deviceDTO.setOsVersion(new String());
            deviceDTO.setPort(-1);
            deviceDTOS.add(deviceDTO);
        }

        progressBar.setVisibility(View.GONE);
        recyclerViewActivityMain.setVisibility(View.VISIBLE);
        adapterDevice.notifyDataSetChanged();
    }

    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        unregisterReceiver(wiFiDirectBroadcastReceiver);
        super.onPause();
    }

    private BroadcastReceiver localReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case FIRST_DEVICE_CONNECTED:
                    appController.restartConnectionListenerWith(ConnectionUtils.getPort(MainActivity.this));
                    String senderIP = intent.getStringExtra(KEY_FIRST_DEVICE_IP);
                    int port = DBAdapter.getInstance(MainActivity.this).getDevice(senderIP).getPort();
                    DataSender.sendCurrentDeviceData(MainActivity.this, senderIP, port, true);
                    isWDConnected = true;
                    break;
                case DataHandler.DEVICE_LIST_CHANGED:
                    ArrayList<DeviceDTO> devices = DBAdapter.getInstance(MainActivity.this)
                            .getDeviceList();
                    int peerCount = (devices == null) ? 0 : devices.size();
                    if (peerCount > 0) {
                        isDeviceListUpdated = true;
                        Toast.makeText(MainActivity.this, "Connection has been accepted", Toast.LENGTH_SHORT)
                                .show();
                        progressBar.setVisibility(View.GONE);
                        deviceDTOS.clear();
                        deviceDTOS.addAll(devices);
                        adapterDevice.notifyDataSetChanged();
                    }
                    break;
                case DataHandler.CHAT_REQUEST_RECEIVED:
                    DeviceDTO chatRequesterDevice = (DeviceDTO) intent.getSerializableExtra(DataHandler.KEY_CHAT_REQUEST);
                    DialogUtils.getChatRequestDialog(MainActivity.this, chatRequesterDevice).show();
                    break;
                case DataHandler.CHAT_RESPONSE_RECEIVED:
                    boolean isChatRequestedAccepted = intent.getBooleanExtra(DataHandler.KEY_IS_CHAT_REQUEST_ACCEPTED, false);
                    if (!isChatRequestedAccepted) {
                        Toast.makeText(MainActivity.this, "Chat request rejected", Toast.LENGTH_SHORT)
                                .show();
                    } else {
                        DeviceDTO chatDevice = (DeviceDTO) intent.getSerializableExtra(DataHandler.KEY_CHAT_REQUEST);
                        DialogUtils.openChatActivity(MainActivity.this, chatDevice);
                        Toast.makeText(MainActivity.this, chatDevice.getPlayerName() + " Accepted chat request", Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case DialogUtils.CODE_PICK_IMAGE:
                if (resultCode == RESULT_OK) {
                    Uri imageUri = data.getData();
                    DataSender.sendFile(MainActivity.this, selectedDevice.getIp(), selectedDevice.getPort(), imageUri);
                }
                break;
        }
    }

    public void receivedDisconnectedFromServer() {
        if (isWDConnected || isConnectionInfoSent) {
            appController.stopConnectionListener();
            Utility.clearPreferences(this);
            Utility.deletePersistentGroups(wifiP2pManager, wifip2pChannel);
            DBAdapter.getInstance(this).clearDatabase();
            isWDConnected = false;
            isConnectionInfoSent = false;
            isDeviceListUpdated = false;
            Toast.makeText(MainActivity.this, "Another device has been disconnected", Toast.LENGTH_SHORT)
                    .show();
        }
    }
}
