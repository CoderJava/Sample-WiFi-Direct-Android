/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect.transfer;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.ysn.examplewifidirect.BuildConfig;
import com.ysn.examplewifidirect.utils.Utility;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.Inet4Address;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class ConnectionListener extends Thread {

    private final String TAG = getClass().getSimpleName();

    private int mPort;
    private Context mContext;
    private ServerSocket mServerSocket;
    private boolean accessRequests = true;
    private long currentTimeMillisTemp = 0L;

    public ConnectionListener(Context context, int port) {
        this.mContext = context;
        this.mPort = port;
    }

    @Override
    public void run() {
        try {
            Log.d(TAG, Build.MANUFACTURER + ": connection listener: " + mPort);
            mServerSocket = new ServerSocket(mPort);
            mServerSocket.setReuseAddress(true);

            if (mServerSocket != null && !mServerSocket.isBound()) {
                mServerSocket.bind(new InetSocketAddress(mPort));
            }

            Log.d(TAG, "Inet4Address: " + Inet4Address.getLocalHost().getHostAddress());

            Socket socket = null;
            while (accessRequests) {
                // this is a blocking operation
                socket = mServerSocket.accept();
                handleData(socket.getInetAddress().getHostAddress(), socket.getInputStream());
            }
            Log.d(TAG, Build.MANUFACTURER + ": connection listener terminated. acceptRequests: " + accessRequests);
            socket.close();
            socket = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleData(String senderIP, InputStream inputStream) {
        try {
            byte[] input = Utility.getInputStreamByArray(inputStream);
            ObjectInput objectInput = null;
            try {
                objectInput = new ObjectInputStream(new ByteArrayInputStream(input));
                ITransferable transferObject = (ITransferable) objectInput.readObject();

                // processing incoming data text
                (new DataHandler(mContext, senderIP, transferObject)).process(currentTimeMillisTemp);
                objectInput.close();
                return;
            } catch (ClassNotFoundException cnfe) {
                cnfe.printStackTrace();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                if (objectInput != null) {
                    objectInput.close();
                }
            }

            // if control comes here that means the byte array sent is not the transfer object
            // that was expected. Processing it as a file (JPEG)
            currentTimeMillisTemp = System.currentTimeMillis();
            final File file = new File(Environment.getExternalStorageDirectory() +
                    "/ExampleWiFiDirect/" + currentTimeMillisTemp + ".jpg");

            File dirs = new File(file.getParent());
            if (!dirs.exists()) {
                boolean dirsSucess = dirs.mkdirs();
            }
            boolean fileCreationSuccess = file.createNewFile();

            Utility.copyFile(new ByteArrayInputStream(input), new FileOutputStream(file));
            // opening the received file. (if exists)
            if (file.exists() && file.length() > 0) {
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(Uri.parse(file.getAbsolutePath()), "image/*");
                intent.setDataAndType(Uri.parse("content://" + file.getAbsolutePath()), "image/*");
                Uri photoUri = Uri.parse("file://" + file.getAbsolutePath());
                if (Build.VERSION.SDK_INT >= 23) {
                    photoUri = FileProvider.getUriForFile(
                            mContext,
                            BuildConfig.APPLICATION_ID + ".provider",
                            file
                    );
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
                intent.setDataAndType(photoUri, "image/*");
                /*mContext.startActivity(intent);*/
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tearDown() {
        accessRequests = false;
    }
}
