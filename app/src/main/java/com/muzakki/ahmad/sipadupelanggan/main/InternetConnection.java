package com.muzakki.ahmad.sipadupelanggan.main;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Created by jeki on 5/30/15.
 */
public class InternetConnection {
    private Context context = null;


    public InternetConnection(Context context){
        this.context = context;
    }

    public void request(final String url,final Map<String,String> params,
                        final Handler.Callback callback){


        new Thread() {
            public void run() {
            Message msg = Message.obtain();
            msg.what = 1;
            Bundle b;
            String response = null;
            try {
                response = openHttpConnection(url, params, "POST");

                b = new Bundle();
                b.putString("response", response);
                b.putString("url", url);
                b.putInt("response_code", 200);
            } catch (IOException e) {
                e.printStackTrace();
                b = new Bundle();
                b.putInt("response_code", 408);
            } catch (NullPointerException e){
                e.printStackTrace();
                b = new Bundle();
                b.putInt("response_code", 404);
            }

            msg.setData(b);
            if(callback!=null) {
                callback.handleMessage(msg);
            }
            // if null dont handle anything
            }
        }.start();
    }

    private String openHttpConnection(String urlStr,Map<String,String> params,String method)
            throws IOException, NullPointerException {
        InputStream in = null;

        int resCode = -1;
        StringBuilder total = new StringBuilder();
        if(!isConnected() && context!=null){
            ((Activity) context).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(context, " Your Phone Is Not Connected ", Toast.LENGTH_LONG).show();
                }
            });
            throw new NullPointerException("Not Connected");
        }
        HttpURLConnection httpConn = null;
        try {

            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();
            if (!(urlConn instanceof HttpURLConnection)) {
                throw new IOException("URL is not an Http URL");
            }
            httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod(method);
            httpConn.setConnectTimeout(5000);

            OutputStream os = httpConn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(os, "UTF-8"));
            String param = encodeParams(params);
            writer.write(param);
            writer.flush();
            writer.close();

            httpConn.connect();
            resCode = httpConn.getResponseCode();
            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(in));

                String line;
                while ((line = r.readLine()) != null) {
                    total.append(line);
                }
            }else{
                throw new NullPointerException("link not found "+urlStr+"?"+param);
            }

            if(total==null) throw new NullPointerException("respon kosong "+urlStr+"?"+param);
        }catch (MalformedURLException e) {
            Log.i("jeki","malformed "+e.getMessage());
        }
        finally {
            if(httpConn!=null){
                httpConn.disconnect();
            }
        }

        return total.toString();
    }

    private String encodeParams(Map<String,String> params){
        String  strParam = "";
        for (Map.Entry<String, String> p : params.entrySet()){
            strParam += p.getKey() + "=" + p.getValue() + "&";
        }
        strParam = strParam.substring(0,strParam.length()-1);
        return strParam;
    }

    public boolean isConnected() {
        ConnectivityManager connec =(ConnectivityManager) context.getSystemService(context.CONNECTIVITY_SERVICE);


        if ( connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTED ||

                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTING ||
                connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.CONNECTED ) {

            return true;
        }else if (
                connec.getNetworkInfo(0).getState() == android.net.NetworkInfo.State.DISCONNECTED ||
                        connec.getNetworkInfo(1).getState() == android.net.NetworkInfo.State.DISCONNECTED  ) {
            return false;
        }
        return false;
    }
}
