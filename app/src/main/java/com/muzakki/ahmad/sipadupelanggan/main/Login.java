package com.muzakki.ahmad.sipadupelanggan.main;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.muzakki.ahmad.sipadupelanggan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;


public class Login extends ActionBarActivity{


    Database db = new Database(this);
    final int SETTING_ACTIVITY = 1;
    private String ip = null;
    InternetConnection ic = new InternetConnection(this);



    private final Handler.Callback callback = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            try {
                JSONObject result = new JSONObject(msg.getData().getString("response"));
                String response = result.getString("response");
                if(response.equals("OK")){
                    loginSuccess(result.getString("token"));
                }else{
                    loginError();
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        this.ip = db.getIP();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==SETTING_ACTIVITY && resultCode==RESULT_OK){
            ip = data.getStringExtra("ip");
            db.saveIP(ip);
            Constants.setHost(ip);
            Log.i("jeki",db.getIP());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.login_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.menu_setting_ip) {
            Intent i = new Intent(this,LoginSetting.class);
            i.putExtra("ip",ip);
            startActivityForResult(i, SETTING_ACTIVITY);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void login(View v){
        String url = Constants.getHost()+"/login_pelanggan/";
        if(ip==null){
            Toast.makeText(this,"IP belum disetting",Toast.LENGTH_SHORT).show();
            return;
        }
        findViewById(R.id.loading_overlay).setVisibility(View.VISIBLE);

        Map<String,String> params = new HashMap<>();
        params.put("auth","true");
        params.put("nosambungan",((EditText) findViewById(R.id.txtNosambungan)).getText().toString());
        params.put("password", ((EditText) findViewById(R.id.txtPassword)).getText().toString());

        ic.request(url,params,callback);
    }

    private void loginSuccess(String token){
        db.clearCache();
        String nosambungan = ((EditText) findViewById(R.id.txtNosambungan)).getText().toString();
        Constants.setNosambungan(nosambungan);
        db.saveLoginCache(nosambungan, token);
        db.saveIP(ip);

        Intent i = new Intent(this,Aduan.class);
        startActivity(i);
        finish();
    }

    private void loginError(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(Login.this, "Login gagal!", Toast.LENGTH_LONG).show();
                findViewById(R.id.loading_overlay).setVisibility(View.GONE);
            }
        });
    }

}
