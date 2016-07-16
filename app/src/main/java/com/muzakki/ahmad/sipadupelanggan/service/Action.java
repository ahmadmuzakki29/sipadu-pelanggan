package com.muzakki.ahmad.sipadupelanggan.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * Created by jeki on 5/28/15.
 */
public class Action extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle data = intent.getExtras();

        messageArrivedAction(data.getBundle("data"));
    }

    private void messageArrivedAction(Bundle data){
        Log.i("jeki",
                "topik: "+data.getString("TOPIC")+
                "\nmessage: "+data.getString("MESSAGE"));
    }
}
