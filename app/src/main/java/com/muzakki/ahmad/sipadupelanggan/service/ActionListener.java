package com.muzakki.ahmad.sipadupelanggan.service;

import android.content.Context;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;

/**
 * Created by jeki on 1/21/15.
 */
public class ActionListener implements IMqttActionListener {
    /**
     * Actions that can be performed Asynchronously <strong>and</strong> associated with a
     * {@link ActionListener} object
     */
    public enum Action {
        CONNECT,

        DISCONNECT,

        SUBSCRIBE,

        PUBLISH
    }

    private Action action;

    private String[] additionalArgs;

    private String clientHandle;

    private Context context;

    public ActionListener(Context context, Action action, String clientHandle, String... additionalArgs) {
        this.context = context;
        this.action = action;
        this.clientHandle = clientHandle;
        this.additionalArgs = additionalArgs;
    }

    @Override
    public void onSuccess(IMqttToken token) {
        switch (action) {
            case CONNECT :
                connect();
                break;
            case DISCONNECT :
                disconnect();
                break;
            case SUBSCRIBE :
                subscribe();
                break;
            case PUBLISH :
                publish();
                break;
        }
    }


    private void connect() {
        Log.i("jeki", "connected");
    }

    private void disconnect() {
        Log.i("jeki", "disconnected");
    }

    private void subscribe() {
        Log.i("jeki", "subscribe");
    }

    private void publish() {
        Log.i("jeki", "published");
    }


    @Override
    public void onFailure(IMqttToken token, Throwable exception) {
        switch (action) {
            case CONNECT :
                connect(exception);
                break;
            case DISCONNECT :
                disconnect(exception);
                break;
            case SUBSCRIBE :
                subscribe(exception);
                break;
            case PUBLISH :
                publish(exception);
                break;
        }
    }

    private void connect(Throwable exc) {
        Log.i("jeki", "failed to connect ");
    }

    private void disconnect(Throwable exc) {
        Log.i("jeki", "failed to disconnect "+exc.getMessage());
    }

    private void subscribe(Throwable exc) {
        Log.i("jeki", "failed to subscribe"+exc.getMessage());
    }

    private void publish(Throwable exc) {
        Log.i("jeki", "failed to publish "+exc.getMessage());
    }

}