package com.muzakki.ahmad.sipadupelanggan.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.EditText;

import com.muzakki.ahmad.sipadupelanggan.R;

/**
 * Created by jeki on 6/4/15.
 */
public class LoginSetting extends ActionBarActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getSupportActionBar().setTitle("Setting");
        setContentView(R.layout.setting_login);

        Intent i = getIntent();
        String  ip = i.getStringExtra("ip");
        ((EditText) findViewById(R.id.txtIP)).setText(ip);
    }

    public void save(View v){
        String ip = ((EditText) findViewById(R.id.txtIP)).getText().toString();
        Intent i = new Intent();
        i.putExtra("ip", ip);

        setResult(RESULT_OK, i);
        finish();
    }
}
