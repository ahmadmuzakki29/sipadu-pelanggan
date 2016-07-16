/*******************************************************************************
 * Copyright (c) 1999, 2014 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution. 
 *
 * The Eclipse Public License is available at 
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at 
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 */
package com.muzakki.ahmad.sipadupelanggan.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.util.SparseArray;

import com.muzakki.ahmad.sipadupelanggan.main.Aduan;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Enables an android application to communicate with an MQTT server using non-blocking methods.
 * <p>
 * Implementation of the MQTT asynchronous client interface {@link IMqttAsyncClient} , using the MQTT
 * android service to actually interface with MQTT server. It provides android applications a simple programming interface to all features of the MQTT version 3.1
 * specification including:
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 * </p>
 */
public class MqttClient extends BroadcastReceiver {


    private static final String SERVICE_NAME = MqttService.class.getName();

    private static final int BIND_SERVICE_FLAG = 0;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        
        public void onReceive(Context context, Intent intent) {

        }
    };

    private static ExecutorService pool = Executors.newCachedThreadPool();

    private static boolean subscribed = false; // diganti saat subscribe dan conenction lost

    /**
     * ServiceConnection to process when we bind to our service
     */
    private final class MyServiceConnection implements ServiceConnection {

        
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mqttService = ((MqttServiceBinder) binder).getService();
            bindedService = true;
            doConnect();
        }

        
        public void onServiceDisconnected(ComponentName name) {
            mqttService = null;
        }
    }


    private MyServiceConnection serviceConnection = new MyServiceConnection();

    private MqttService mqttService;

    private String clientHandle;

    Context myContext;
    private SparseArray<IMqttToken> tokenMap = new SparseArray<IMqttToken>();
    private int tokenNumber = 0;

    // Connection data
    private String serverURI;
    private String clientId;
    private MqttConnectOptions connectOptions;
    private IMqttToken connectToken;

    // The MqttCallback provided by the application
    private MqttCallback callback;
    private MqttTraceHandler traceCallback;


    private boolean traceEnabled = false;

    private volatile boolean registerReceiver = false;
    private volatile boolean bindedService = false;


    private String nosambungan = "";
    public MqttClient(){}

    /**
     * Constructor- create an MqttAndroidClient that can be used to communicate
     * with an MQTT server on android
     *
     * @param context
     *            used to pass context to the callback.
     * @param serverURI
     *            specifies the protocol, host name and port to be used to
     *            connect to an MQTT server
     * @param clientId
     *            specifies the name by which this connection should be
     *            identified to the server
     */
    public MqttClient(Context context, String serverURI,
                             String clientId,String nosambungan) {

        myContext = context;
        this.serverURI = serverURI;
        this.clientId = clientId;
        this.nosambungan = nosambungan;
    }

    /**
     * Determines if this client is currently connected to the server.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise.
     */
    public boolean isConnected() {
        if (mqttService != null)
            return mqttService.isConnected(clientHandle);
        else
            return false;
    }
    
    public IMqttToken connect(MqttConnectOptions options, Object userContext,
                              IMqttActionListener callback) throws MqttException {

        IMqttToken token = new MqttTokenAndroid(this, userContext,
                callback);

        connectOptions = options;
        connectToken = token;

		/*
		 * The actual connection depends on the service, which we start and bind
		 * to here, but which we can't actually use until the serviceConnection
		 * onServiceConnected() method has run (asynchronously), so the
		 * connection itself takes place in the onServiceConnected() method
		 */
        if (mqttService == null) { // First time - must bind to the service
            Intent serviceStartIntent = new Intent();
            serviceStartIntent.setClassName(myContext, SERVICE_NAME);
            Object service = myContext.startService(serviceStartIntent);
            if (service == null) {
                IMqttActionListener listener = token.getActionCallback();
                if (listener != null) {
                    listener.onFailure(token, new RuntimeException(
                            "cannot start service " + SERVICE_NAME));
                }
            }

            // We bind with BIND_SERVICE_FLAG (0), leaving us the manage the lifecycle
            // until the last time it is stopped by a call to stopService()
            myContext.startService(serviceStartIntent);
            myContext.bindService(serviceStartIntent, serviceConnection,
                    Context.BIND_AUTO_CREATE);

            registerReceiver(this);
        }
        else {
            pool.execute(new Runnable() {

                
                public void run() {
                    doConnect();

                    //Register receiver to show shoulder tap.
                    registerReceiver(MqttClient.this);
                }

            });
        }

        return token;
    }


    private void registerReceiver(BroadcastReceiver receiver) {
        IntentFilter filter = new IntentFilter();
        filter.addAction(MqttServiceConstants.CALLBACK_TO_ACTIVITY);
        myContext.registerReceiver(receiver, filter);
        registerReceiver = true;
    }

    /**
     * Actually do the mqtt connect operation
     */
    private void doConnect() {
        if (clientHandle == null) {
            clientHandle = mqttService.getClient(serverURI, clientId,myContext.getApplicationInfo().packageName);
        }
        mqttService.setTraceEnabled(traceEnabled);
        mqttService.setTraceCallbackId(clientHandle);
        Log.i("jeki","clienthandle "+clientHandle);

        String activityToken = storeToken(connectToken);
        try {
            mqttService.connect(clientHandle, connectOptions, null,
                    activityToken);
        }
        catch (MqttException e) {
            IMqttActionListener listener = connectToken.getActionCallback();
            if (listener != null) {
                listener.onFailure(connectToken, e);
            }
        }
    }

    /**
     * Disconnects from the server.
     * <p>
     * An attempt is made to quiesce the client allowing outstanding work to
     * complete before disconnecting. It will wait for a maximum of 30 seconds
     * for work to quiesce before disconnecting. This method must not be called
     * from inside {@link MqttCallback} methods.
     * </p>
     *
     * @return token used to track and wait for disconnect to complete. The
     *         token will be passed to any callback that has been set.
     * @throws MqttException
     *             for problems encountered while disconnecting
     */
    public IMqttToken disconnect() throws MqttException {
        IMqttToken token = new MqttTokenAndroid(this, null,
                (IMqttActionListener) null);
        String activityToken = storeToken(token);
        Log.i("jeki","client handle disconnect "+clientHandle);
        mqttService.disconnect(clientHandle, null, activityToken);
        return token;
    }

    public IMqttToken subscribe(String topic, int qos) throws MqttException,
            MqttSecurityException {
        return subscribe(topic, qos, null, null);
    }

    public IMqttToken subscribe(String topic, int qos, Object userContext,
                                IMqttActionListener callback) throws MqttException {
        IMqttToken token = new MqttTokenAndroid(this, userContext,
                callback, new String[]{topic});
        String activityToken = storeToken(token);
        mqttService.subscribe(clientHandle, topic, qos, null, activityToken);
        return token;
    }

    public void setCallback(MqttCallback callback) {
        this.callback = callback;
    }

    
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();

        String action = data.getString(MqttServiceConstants.CALLBACK_ACTION);

        if (MqttServiceConstants.CONNECT_ACTION.equals(action)) {
            connectAction(data);
        }
        else if (MqttServiceConstants.MESSAGE_ARRIVED_ACTION.equals(action)) {
            //messageArrivedAction(data);
            String id = data.getString("id");
            ((Aduan) myContext).updateList(id);
        }
        else if (MqttServiceConstants.SUBSCRIBE_ACTION.equals(action)) {
            subscribeAction(data);
        }
        else if (MqttServiceConstants.UNSUBSCRIBE_ACTION.equals(action)) {
            unSubscribeAction(data);
        }
        else if (MqttServiceConstants.SEND_ACTION.equals(action)) {
            sendAction(data);
        }
        else if (MqttServiceConstants.MESSAGE_DELIVERED_ACTION.equals(action)) {
            messageDeliveredAction(data);
        }
        else if (MqttServiceConstants.ON_CONNECTION_LOST_ACTION
                .equals(action)) {
            connectionLostAction(data);
        }
        else if (MqttServiceConstants.DISCONNECT_ACTION.equals(action)) {
            disconnected(data);
        }
        else if (MqttServiceConstants.TRACE_ACTION.equals(action)) {
            traceAction(data);
        }else{
            mqttService.traceError(MqttService.TAG, "Callback action doesn't exist.");
        }

    }



    /**
     * Process the results of a connection
     *
     * @param data
     */
    private void connectAction(Bundle data) {
        IMqttToken token = connectToken;
        removeMqttToken(data);

        simpleAction(token, data);
        if(!isConnected()) return;
        ((Aduan) myContext).requestNotif();

        try {
            if(subscribed){
                Log.i("jeki","not subscribe bcoz already subscribed");
                return;
            }
            subscribe(Constants.TOPIC + nosambungan, 0);

            subscribed = true;
        } catch (MqttException e) {
            e.printStackTrace();
            Log.i("jeki","cannot subscribe to "+Constants.TOPIC);
        }
    }

    /**
     * Process a notification that we have disconnected
     *
     * @param data
     */
    private void disconnected(Bundle data) {
        clientHandle = null; // avoid reuse!
        IMqttToken token = removeMqttToken(data);
        if (token != null) {
            ((MqttTokenAndroid) token).notifyComplete();
        }
        if (callback != null) {
            callback.connectionLost(null);
        }
        subscribed = false;
        ((Aduan) myContext).showLogin();
    }

    /**
     * Process a Connection Lost notification
     *
     * @param data
     */
    private void connectionLostAction(Bundle data) {
        if (callback != null) {
            Exception reason = (Exception) data
                    .getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
            callback.connectionLost(reason);
        }
        subscribed = false;
    }

    /**
     * Common processing for many notifications
     *
     * @param token
     *            the token associated with the action being undertake
     * @param data
     *            the result data
     */
    private void simpleAction(IMqttToken token, Bundle data) {
        if (token != null) {
            Status status = (Status) data
                    .getSerializable(MqttServiceConstants.CALLBACK_STATUS);
            if (status == Status.OK) {
                ((MqttTokenAndroid) token).notifyComplete();
            }
            else {
                Exception exceptionThrown = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
                ((MqttTokenAndroid) token).notifyFailure(exceptionThrown);
            }
        }
    }

    /**
     * Process notification of a publish(send) operation
     *
     * @param data
     */
    private void sendAction(Bundle data) {
        IMqttToken token = getMqttToken(data); // get, don't remove - will
        // remove on delivery
        simpleAction(token, data);
    }

    /**
     * Process notification of a subscribe operation
     *
     * @param data
     */
    private void subscribeAction(Bundle data) {
        IMqttToken token = removeMqttToken(data);
        simpleAction(token, data);
    }

    /**
     * Process notification of an unsubscribe operation
     *
     * @param data
     */
    private void unSubscribeAction(Bundle data) {
        IMqttToken token = removeMqttToken(data);
        simpleAction(token, data);
    }

    /**
     * Process notification of a published message having been delivered
     *
     * @param data
     */
    private void messageDeliveredAction(Bundle data) {
        IMqttToken token = removeMqttToken(data);
        if (token != null) {
            if (callback != null) {
                Status status = (Status) data
                        .getSerializable(MqttServiceConstants.CALLBACK_STATUS);
                if (status == Status.OK) {
                    callback.deliveryComplete((IMqttDeliveryToken) token);
                }
            }
        }
    }

    /**
     * Process notification of a message's arrival
     *
     * @param data
     */
    private void messageArrivedAction(Bundle data) {
        if (callback != null) {
            String topic = data
                    .getString(MqttServiceConstants.CALLBACK_DESTINATION_NAME);
            ParcelableMqttMessage message = data
                    .getParcelable(MqttServiceConstants.CALLBACK_MESSAGE_PARCEL);
            try {
                    callback.messageArrived(topic, message);
            }
            catch (Exception e) {
                // Swallow the exception
            }
        }
    }

    /**
     * Process trace action - pass trace data back to the callback
     *
     * @param data
     */
    private void traceAction(Bundle data) {

        if (traceCallback != null) {
            String severity = data.getString(MqttServiceConstants.CALLBACK_TRACE_SEVERITY);
            String message =  data.getString(MqttServiceConstants.CALLBACK_ERROR_MESSAGE);
            String tag = data.getString(MqttServiceConstants.CALLBACK_TRACE_TAG);
            if (MqttServiceConstants.TRACE_DEBUG.equals(severity))
                traceCallback.traceDebug(tag, message);
            else if (MqttServiceConstants.TRACE_ERROR.equals(severity))
                traceCallback.traceError(tag, message);
            else
            {
                Exception e = (Exception) data.getSerializable(MqttServiceConstants.CALLBACK_EXCEPTION);
                traceCallback.traceException(tag, message, e);
            }
        }
    }

    /**
     * @param token
     *            identifying an operation
     * @return an identifier for the token which can be passed to the Android
     *         Service
     */
    private synchronized String storeToken(IMqttToken token) {
        tokenMap.put(tokenNumber, token);
        return Integer.toString(tokenNumber++);
    }

    /**
     * Get a token identified by a string, and remove it from our map
     *
     * @param data
     * @return the token
     */
    private synchronized IMqttToken removeMqttToken(Bundle data) {

        String activityToken = data.getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
        if (activityToken!=null){
            int tokenNumber = Integer.parseInt(activityToken);
            IMqttToken token = tokenMap.get(tokenNumber);
            tokenMap.delete(tokenNumber);
            return token;
        }
        return null;
    }

    /**
     * Get a token identified by a string, and remove it from our map
     *
     * @param data
     * @return the token
     */
    private synchronized IMqttToken getMqttToken(Bundle data) {
        String activityToken = data
                .getString(MqttServiceConstants.CALLBACK_ACTIVITY_TOKEN);
        IMqttToken token = tokenMap.get(Integer.parseInt(activityToken));
        return token;
    }

    /**
     * Get the SSLSocketFactory using SSL key store and password
     * <p>
     * A convenience method, which will help user to create a SSLSocketFactory
     * object
     * </p>
     *
     * @param keyStore
     *            the SSL key store which is generated by some SSL key tool,
     *            such as keytool in Java JDK
     * @param password
     *            the password of the key store which is set when the key store
     *            is generated
     * @return SSLSocketFactory used to connect to the server with SSL
     *         authentication
     * @throws MqttSecurityException
     *             if there was any error when getting the SSLSocketFactory
     */
    public SSLSocketFactory getSSLSocketFactory (InputStream keyStore, String password) throws MqttSecurityException {
        try{
            SSLContext ctx = null;
            SSLSocketFactory sslSockFactory=null;
            KeyStore ts;
            ts = KeyStore.getInstance("BKS");
            ts.load(keyStore, password.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ts);
            TrustManager[] tm = tmf.getTrustManagers();
            ctx = SSLContext.getInstance("SSL");
            ctx.init(null, tm, null);

            sslSockFactory=ctx.getSocketFactory();
            return sslSockFactory;

        } catch (KeyStoreException e) {
            throw new MqttSecurityException(e);
        } catch (CertificateException e) {
            throw new MqttSecurityException(e);
        } catch (FileNotFoundException e) {
            throw new MqttSecurityException(e);
        } catch (IOException e) {
            throw new MqttSecurityException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new MqttSecurityException(e);
        } catch (KeyManagementException e) {
            throw new MqttSecurityException(e);
        }
    }

    /**
     * Unregister receiver which receives intent from MqttService avoids
     * IntentReceiver leaks.
     */
    public void unregisterResources(){
        if(myContext != null && registerReceiver){
            synchronized (this) {
                myContext.unregisterReceiver(this);
                registerReceiver = false;
            }
            if(bindedService){
                try{
                    myContext.unbindService(serviceConnection);
                    bindedService = false;
                }catch(IllegalArgumentException e){
                    //Ignore unbind issue.
                }
            }
        }
    }

    /**
     * Register receiver to receiver intent from MqttService. Call this method
     * when activity is hidden and become to show again.
     *
     * @param context
     *            - Current activity context.
     */
    public void registerResources(Context context){
        if(context != null){
            this.myContext = context;
            if(!registerReceiver){
                registerReceiver(this);
            }
        }
    }
}
