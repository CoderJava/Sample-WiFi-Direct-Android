package com.ysn.examplewifidirect.transfer;

import android.app.IntentService;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.ysn.examplewifidirect.utils.Utility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.Socket;

public class DataTransferService extends IntentService {

    private static final int SOCKET_TIMEOUT = 5000;
    public static final String ACTION_SEND_FILE = "send_file";
    public static final String ACTION_SEND_DATA = "send_data";
    public static final String ACTION_SEND_DATA_IMAGE = "send_data_image";
    public static final String EXTRAS_FILE_PATH = "file_url";
    public static final String DEST_IP_ADDRESS = "host";
    public static final String DEST_PORT_NUMBER = "port";
    public static final String EXTRAS_SHARE_DATA = "sharedata";

    public DataTransferService(String name) {
        super(name);
    }

    public DataTransferService() {
        super("DataTransferService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equalsIgnoreCase(ACTION_SEND_DATA_IMAGE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            ITransferable transferObject = (ITransferable) intent.getExtras().getSerializable(EXTRAS_SHARE_DATA);
            String host = intent.getExtras().getString(DEST_IP_ADDRESS);
            int port = intent.getExtras().getInt(DEST_PORT_NUMBER);
            Socket socket = null;
            try {
                socket = new Socket(host, port);

                // send file
                OutputStream outputStreamImage = socket.getOutputStream();
                ContentResolver contentResolver = context.getContentResolver();
                InputStream inputStream = null;
                try {
                    inputStream = contentResolver.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Utility.copyFile(inputStream, outputStreamImage);
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // send text
                socket = new Socket(host, port);
                OutputStream outputStreamText = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(outputStreamText);
                oos.writeObject(transferObject);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (intent.getAction().equalsIgnoreCase(ACTION_SEND_FILE)) {
            String fileUri = intent.getExtras().getString(EXTRAS_FILE_PATH);
            String host = intent.getExtras().getString(DEST_IP_ADDRESS);
            int port = intent.getExtras().getInt(DEST_PORT_NUMBER);
            Socket socket = null;
            try {
                socket = new Socket(host, port);

                Log.d("DataTransferService", "Client socket - " + socket.isConnected());
                OutputStream outputStream = socket.getOutputStream();
                ContentResolver contentResolver = context.getContentResolver();
                InputStream inputStream = null;
                try {
                    inputStream = contentResolver.openInputStream(Uri.parse(fileUri));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                Utility.copyFile(inputStream, outputStream);
                Log.d("DataTransferService", "Client: Data written");
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        } else if (intent.getAction().equalsIgnoreCase(ACTION_SEND_DATA)) {
            String host = intent.getExtras().getString(DEST_IP_ADDRESS);
            int port = intent.getExtras().getInt(DEST_PORT_NUMBER);
            Socket socket = null;
            ITransferable transferObject = (ITransferable) intent.getExtras().getSerializable(EXTRAS_SHARE_DATA);
            try {
                socket = new Socket(host, port);
                OutputStream outputStream = socket.getOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(outputStream);
                oos.writeObject(transferObject);
                oos.close();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }
}
