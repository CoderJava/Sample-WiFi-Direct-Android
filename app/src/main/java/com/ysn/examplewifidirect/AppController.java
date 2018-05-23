/*
 * Created by YSN Studio on 5/23/18 2:52 PM
 * Copyright (c) 2018. All rights reserved.
 *
 * Last modified 5/23/18 2:52 PM
 */

package com.ysn.examplewifidirect;

import android.app.Application;

import com.ysn.examplewifidirect.transfer.ConnectionListener;
import com.ysn.examplewifidirect.utils.ConnectionUtils;

public class AppController extends Application {

    private ConnectionListener connectionListener;
    private int myPort;
    private boolean isConnectionListenerRunning = false;

    @Override
    public void onCreate() {
        super.onCreate();
        myPort = ConnectionUtils.getPort(getApplicationContext());
        connectionListener = new ConnectionListener(getApplicationContext(), myPort);
    }

    public void stopConnectionListener() {
        if (!isConnectionListenerRunning) {
            return;
        }
        if (connectionListener != null) {
            connectionListener.tearDown();
            connectionListener = null;
        }
        isConnectionListenerRunning = false;
    }

    public void startConnectionListener() {
        if (isConnectionListenerRunning) {
            return;
        }
        if (connectionListener == null) {
            connectionListener = new ConnectionListener(getApplicationContext(), myPort);
        }
        if (!connectionListener.isAlive()) {
            connectionListener.interrupt();
            connectionListener.tearDown();
            connectionListener = null;
        }
        connectionListener = new ConnectionListener(getApplicationContext(), myPort);
        connectionListener.start();
        isConnectionListenerRunning = true;
    }

    public void startConnectionListener(int port) {
        myPort = port;
        startConnectionListener();
    }

    public void restartConnectionListenerWith(int port) {
        stopConnectionListener();
        startConnectionListener(port);
    }

    public boolean isConnectionListenerRunning() {
        return isConnectionListenerRunning;
    }

    public int getPort() {
        return myPort;
    }
}
