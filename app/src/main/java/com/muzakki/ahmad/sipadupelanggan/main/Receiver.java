package com.muzakki.ahmad.sipadupelanggan.main;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.muzakki.ahmad.sipadupelanggan.R;
import com.muzakki.ahmad.sipadupelanggan.service.MqttServiceConstants;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

/**
 * Created by jeki on 6/10/15.
 */
public class Receiver extends BroadcastReceiver {
    InternetConnection ic = null;
    Database db = null;
    Context context = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String id = intent.getStringExtra("id");
        if(this.context==null) {
            this.context = context;
            ic = new InternetConnection(context);
            db = new Database(context);
        }
        getTindakLanjut(id);
    }

    public void getTindakLanjut(String id){
        String url = Constants.getHost()+"/tindak_lanjut/";

        HashMap<String,String> param = new HashMap<>();
        param.put("action","get_tindak_lanjut");
        param.put("id", id);

        getTindakLanjut = new tindakLanjutHandler(id);
        ic.request(url, param, getTindakLanjut);
    }

    private void receiveTindaklanjut(String id,Message msg) {
        String response = msg.getData().getString("response");
        try {
            JSONObject obj = new JSONObject(response);
            String tindaklanjut = obj.getString("tindak_lanjut");
            String waktu_tindaklanjut = obj.getString("waktu_tindak_lanjut");

            String[] data = new String[]{tindaklanjut,waktu_tindaklanjut,id};
            String kategori = db.simpanTindaklanjut(data);

            Intent i = new Intent(MqttServiceConstants.CALLBACK_TO_ACTIVITY);
            i.putExtra(MqttServiceConstants.CALLBACK_ACTION, MqttServiceConstants.MESSAGE_ARRIVED_ACTION);
            i.putExtra("id",id);
            context.sendBroadcast(i);

            notifyAduan(id,kategori);
            getTindakLanjut = null;
            Log.i("jeki","tindak lanjut : "+tindaklanjut);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private synchronized void notifyAduan(String id,String kategori){
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);


        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setSmallIcon(R.drawable.ic_sipadu)
                        .setContentTitle("Sipadu Pelanggan")
                        .setContentText("Aduan anda tentang : \"" + kategori + "\" telah dibalas " +
                                "oleh manajemen.\n" +
                                "Tekan untuk membuka. ");
        Intent resultIntent = new Intent(context, Aduan.class);
        resultIntent.putExtra("ACTION",Constants.ACTION_NOTIFICATION);
        resultIntent.putExtra("id", id);

        PendingIntent pending = PendingIntent.getActivity(context, Integer.parseInt(id),
                resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(pending);
        mBuilder.setAutoCancel(true);

        mNotificationManager.notify(Integer.parseInt(id), mBuilder.build());
    }

    private Handler.Callback getTindakLanjut = null;

    private class tindakLanjutHandler implements Handler.Callback {
        String id = null;

        tindakLanjutHandler(String id) {
            this.id = id;
        }

        @Override
        public boolean handleMessage(Message message) {
            receiveTindaklanjut(this.id,message);
            return false;
        }
    }

}
