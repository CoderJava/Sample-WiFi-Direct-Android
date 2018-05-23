package com.ysn.examplewifidirect.transfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.ysn.examplewifidirect.ChatActivity;
import com.ysn.examplewifidirect.model.ChatDTO;
import com.ysn.examplewifidirect.model.DeviceDTO;
import com.ysn.examplewifidirect.utils.ConnectionUtils;
import com.ysn.examplewifidirect.utils.Utility;

public class DataSender {

    private static final String TAG = "DataSender";

    public static void sendData(Context context, String destIP, int destPort, ITransferable data) {
        Log.d(TAG, "sendData with following data");
        Log.d(TAG, "context: " + context);
        Log.d(TAG, "destIP: " + destIP);
        Log.d(TAG, "destPort: " + destPort);
        Log.d(TAG, "data: " + data);
        Intent serviceIntent = new Intent(context, DataTransferService.class);
        serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA);
        serviceIntent.putExtra(DataTransferService.DEST_IP_ADDRESS, destIP);
        serviceIntent.putExtra(DataTransferService.DEST_PORT_NUMBER, destPort);
        serviceIntent.putExtra(DataTransferService.EXTRAS_SHARE_DATA, data);
        context.startService(serviceIntent);
    }

    public static void sendFile(Context context, String destIP, int destPort, Uri fileUri) {
        Intent serviceIntent = new Intent(context, DataTransferService.class);
        serviceIntent.setAction(DataTransferService.ACTION_SEND_FILE);
        serviceIntent.putExtra(DataTransferService.DEST_IP_ADDRESS, destIP);
        serviceIntent.putExtra(DataTransferService.DEST_PORT_NUMBER, destPort);
        serviceIntent.putExtra(DataTransferService.EXTRAS_FILE_PATH, fileUri.toString());
        context.startService(serviceIntent);
    }

    public static void sendDataImageChat(Context context, String destIP, int destPort, ITransferable data,
                                         Uri contentUri) {
        Intent serviceIntent = new Intent(context, DataTransferService.class);
        serviceIntent.setAction(DataTransferService.ACTION_SEND_DATA_IMAGE);
        serviceIntent.putExtra(DataTransferService.DEST_IP_ADDRESS, destIP);
        serviceIntent.putExtra(DataTransferService.DEST_PORT_NUMBER, destPort);
        serviceIntent.putExtra(DataTransferService.EXTRAS_SHARE_DATA, data);
        serviceIntent.putExtra(DataTransferService.EXTRAS_FILE_PATH, contentUri.toString());
        context.startService(serviceIntent);
    }

    public static void sendCurrentDeviceData(Context context, String destIP, int destPort, boolean isRequest) {
        DeviceDTO currentDevice = new DeviceDTO();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, TransferConstants.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, TransferConstants.KEY_MY_IP));

        ITransferable transferData = null;
        if (!isRequest) {
            transferData = TransferModelGenerator.generateDeviceTransferModelResponse(currentDevice);
        } else {
            transferData = TransferModelGenerator.generateDeviceTransferModelRequest(currentDevice);
        }
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendCurrentDeviceDataWD(Context context, String destIP, int destPort, boolean isRequest) {
        Log.d(TAG, "sendCurrentDeviceDataWD");
        DeviceDTO currentDevice = new DeviceDTO();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, TransferConstants.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, TransferConstants.KEY_MY_IP));

        ITransferable transferData = null;
        if (!isRequest) {
            transferData = TransferModelGenerator.generateDeviceTransferModelResponseWD(currentDevice);
        } else {
            transferData = TransferModelGenerator.generateDeviceTransferModelRequestWD(currentDevice);
        }
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatRequest(Context context, String destIP, int destPort) {
        DeviceDTO currentDevice = new DeviceDTO();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, TransferConstants.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, TransferConstants.KEY_MY_IP));
        ITransferable transferData = TransferModelGenerator.generateChatRequestModel(currentDevice);
        Log.d(TAG, "sendChatRequest");
        Log.d(TAG, "destIP: " + destIP);
        Log.d(TAG, "destPort: " + destPort);
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatResponse(Context context, String destIP, int destPort, boolean isAccepted) {
        DeviceDTO currentDevice = new DeviceDTO();
        currentDevice.setPort(ConnectionUtils.getPort(context));
        String playerName = Utility.getString(context, TransferConstants.KEY_USER_NAME);
        if (playerName != null) {
            currentDevice.setPlayerName(playerName);
        }
        currentDevice.setIp(Utility.getString(context, TransferConstants.KEY_MY_IP));
        ITransferable transferData = TransferModelGenerator.generateChatResponseModel(currentDevice, isAccepted);
        sendData(context, destIP, destPort, transferData);
    }

    public static void sendChatInfo(Context context, String destIP, int destPort, ChatDTO chat) {
        ITransferable transferableData = TransferModelGenerator.generateChatTransferModel(chat);
        Log.d(TAG, "transferableData.requestCode: " + transferableData.getRequestCode());
        sendData(context, destIP, destPort, transferableData);
    }

    public static void sendChatInfoImage(Context context, String destIP, int destPort, ChatDTO chat,
                                         Uri contentUri) {
        ITransferable transferableData = TransferModelGenerator.generateChatImageTransferModel(chat);
        sendDataImageChat(context, destIP, destPort, transferableData, contentUri);
    }
}
