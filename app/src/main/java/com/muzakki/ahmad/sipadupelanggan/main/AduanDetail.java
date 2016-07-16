package com.muzakki.ahmad.sipadupelanggan.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.TextView;

import com.muzakki.ahmad.sipadupelanggan.R;

import java.util.HashMap;
import java.util.List;

/**
 * Created by jeki on 6/8/15.
 */
public class AduanDetail extends ActionBarActivity {
    Database db = new Database(this);
    String msg_kosong = "<belum ada tindak lanjut>";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aduan_detail);
        Intent i = getIntent();
        String id = i.getStringExtra("id");
        showDetail(id);
    }

    private void showDetail(String id){

        List<HashMap<String,String>> result = db.getAduanDetail(id);
        ((TextView) findViewById(R.id.vwIsiAduan)).setText(result.get(0).get("aduan"));
        ((TextView) findViewById(R.id.vwTanggalAduan)).setText(result.get(0).get("waktu"));
        String tindak_lanjut = result.get(0).get("tindak_lanjut");
        if(tindak_lanjut!=null){
            ((TextView) findViewById(R.id.vwTindakLanjut)).setText(tindak_lanjut);
            ((TextView) findViewById(R.id.vwTanggalTindakLanjut)).setText(result.get(0).get("waktu_tindak_lanjut"));
        }else{
            ((TextView) findViewById(R.id.vwTindakLanjut)).setText(msg_kosong);
            //((TextView) findViewById(R.id.vwTindakLanjut)).setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        }

    }



}
