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
package com.muzakki.ahmad.sipadupelanggan.main;

import android.content.Context;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;


/**
 * Handles call backs from the MQTT Client
 *
 */
public class MqttCallbackHandler implements MqttCallback {

    /** {@link Context} for the application used to format and import external strings**/
    private Context context;
    /** Client handle to reference the connection that this handler is attached to**/
    private String clientHandle;



    /**
    * Creates an <code>MqttCallbackHandler</code> object
    * @param context The application's context
    * @param clientHandle The handle to a Connection object
    */
    public MqttCallbackHandler(Context context, String clientHandle)
    {
        this.context = context;
        this.clientHandle = clientHandle;
    }

    /**
    * @see org.eclipse.paho.client.mqttv3.MqttCallback#connectionLost(Throwable)
    */
    @Override
    public void connectionLost(Throwable cause) {
        /*/	  cause.printStackTrace();
        if (cause != null) {
            String message = "lost koneksi";

            //build intent
            Intent intent = new Intent();
            intent.setClassName(context, "org.eclipse.paho.android.service.sample.ConnectionDetails");
            intent.putExtra("handle", clientHandle);

            //notify the user
            //Notify.notification(context, message, intent, 1);
        }*/
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // handled by Receiver
    }

    /**
    * @see org.eclipse.paho.client.mqttv3.MqttCallback#deliveryComplete(org.eclipse.paho.client.mqttv3.IMqttDeliveryToken)
    */
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
    // Do nothing
    }


}
