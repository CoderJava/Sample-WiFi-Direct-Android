package com.ysn.examplewifidirect.transfer;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.ysn.examplewifidirect.ChatActivity;
import com.ysn.examplewifidirect.MainActivity;
import com.ysn.examplewifidirect.db.DBAdapter;
import com.ysn.examplewifidirect.model.ChatDTO;
import com.ysn.examplewifidirect.model.DeviceDTO;

import java.io.File;

public class DataHandler {

    public static final String DEVICE_LIST_CHANGED = "device_list_updated";
    public static final String CHAT_REQUEST_RECEIVED = "chat_request_received";
    public static final String CHAT_RESPONSE_RECEIVED = "chat_response_received";
    public static final String KEY_CHAT_REQUEST = "chat_requester_or_responder";
    public static final String KEY_IS_CHAT_REQUEST_ACCEPTED = "is_chat_request_accept";

    private ITransferable data;
    private Context mContext;
    private String senderIP;
    private LocalBroadcastManager broadcastManager;
    private DBAdapter dbAdapter = null;
    private long currentTimeMillisTemp;

    DataHandler(Context context, String senderIP, ITransferable data) {
        this.mContext = context;
        this.data = data;
        this.senderIP = senderIP;
        this.dbAdapter = DBAdapter.getInstance(mContext);
        this.broadcastManager = LocalBroadcastManager.getInstance(mContext);
    }

    public void process(long currentTimeMillisTemp) {
        this.currentTimeMillisTemp = currentTimeMillisTemp;
        if (data.getRequestType().equalsIgnoreCase(TransferConstants.TYPE_REQUEST)) {
            processRequest();
        } else {
            processResponse();
        }
    }

    private void processRequest() {
        switch (data.getRequestCode()) {
            case TransferConstants.CLIENT_DATA:
                processPeerDeviceInfo();
                DataSender.sendCurrentDeviceData(mContext, senderIP,
                        dbAdapter.getDevice(senderIP).getPort(), false);
                break;
            case TransferConstants.CLIENT_DATA_WD:
                processPeerDeviceInfo();
                Intent intent = new Intent(MainActivity.FIRST_DEVICE_CONNECTED);
                intent.putExtra(MainActivity.KEY_FIRST_DEVICE_IP, senderIP);
                broadcastManager.sendBroadcast(intent);
                break;
            case TransferConstants.CHAT_REQUEST_SENT:
                processChatRequestReceipt();
                break;
        }
    }

    private void processResponse() {
        switch (data.getRequestCode()) {
            case TransferConstants.CLIENT_DATA:
            case TransferConstants.CLIENT_DATA_WD:
                processPeerDeviceInfo();
                break;
            case TransferConstants.CHAT_DATA:
                processChatData();
                break;
            case TransferConstants.CHAT_DATA_IMAGE:
                processChatDataImage();
                break;
            case TransferConstants.CHAT_REQUEST_ACCEPTED:
                processChatRequestResponse(true);
                break;
            case TransferConstants.CHAT_REQUEST_REJECTED:
                processChatRequestResponse(false);
                break;
        }
    }

    private void processChatRequestReceipt() {
        String chatRequesterDeviceJson = data.getData();
        DeviceDTO chatRequesterDevice = DeviceDTO.fromJSON(chatRequesterDeviceJson);
        chatRequesterDevice.setIp(senderIP);

        Intent intent = new Intent(CHAT_REQUEST_RECEIVED);
        intent.putExtra(KEY_CHAT_REQUEST, chatRequesterDevice);
        broadcastManager.sendBroadcast(intent);
    }

    private void processChatRequestResponse(boolean isRequestAccepted) {
        String chatResponderDeviceJson = data.getData();
        DeviceDTO chatResponderDevice = DeviceDTO.fromJSON(chatResponderDeviceJson);
        chatResponderDevice.setIp(senderIP);

        Intent intent = new Intent(CHAT_RESPONSE_RECEIVED);
        intent.putExtra(KEY_CHAT_REQUEST, chatResponderDevice);
        intent.putExtra(KEY_IS_CHAT_REQUEST_ACCEPTED, isRequestAccepted);
        broadcastManager.sendBroadcast(intent);
    }

    private void processChatData() {
        String chatJson = data.getData();
        ChatDTO chatObject = ChatDTO.fromJSON(chatJson);
        chatObject.setFromIP(senderIP);

        // save in db if needed here
        Intent chatReceiverIntent = new Intent(ChatActivity.ACTION_CHAT_RECEIVED);
        chatReceiverIntent.putExtra(ChatActivity.KEY_CHAT_DATA, chatObject);
        broadcastManager.sendBroadcast(chatReceiverIntent);
    }

    private void processChatDataImage() {
        String chatJson = data.getData();
        ChatDTO chatObject = ChatDTO.fromJSON(chatJson);
        chatObject.setFromIP(senderIP);
        if (currentTimeMillisTemp != 0L) {
            File fileImage = new File(Environment.getExternalStorageDirectory() + "/ExampleWiFiDirect/" + currentTimeMillisTemp + ".jpg");
            chatObject.setMessage(fileImage.getAbsolutePath());
        }
        Log.d("DataHandler", "message: " + chatObject.getMessage());

        Intent chatReceiverIntent = new Intent(ChatActivity.ACTION_CHAT_RECEIVED);
        chatReceiverIntent.putExtra(ChatActivity.KEY_CHAT_DATA, chatObject);
        broadcastManager.sendBroadcast(chatReceiverIntent);
    }

    private void processPeerDeviceInfo() {
        String deviceJson = data.getData();
        DeviceDTO device = DeviceDTO.fromJSON(deviceJson);
        device.setIp(senderIP);
        long rowid = dbAdapter.addDevice(device);
        if (rowid > 0) {
            Log.d("DataHandler", Build.MANUFACTURER + " received: " + deviceJson);
        } else {
            Log.d("DataHandler", Build.MANUFACTURER + " can't save: " + deviceJson);
        }

        Intent intent = new Intent(DEVICE_LIST_CHANGED);
        broadcastManager.sendBroadcast(intent);
    }

}
