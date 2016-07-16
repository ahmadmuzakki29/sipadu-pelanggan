package com.muzakki.ahmad.sipadupelanggan.main;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.muzakki.ahmad.sipadupelanggan.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;

/**
 * Created by jeki on 6/3/15.
 */
public class AduanTambah extends ActionBarActivity implements LocationListener, Handler.Callback{
    private static final int OPEN_LOCATION_SETTINGS = 1;
    Location location = null;
    CountDownLatch latch = null;
    boolean gps_enabled = false;
    boolean network_enabled = false;
    LocationManager lm = null;
    String kategori = null;
    String aduan = null;
    InternetConnection ic = new InternetConnection(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aduan_tambah);
        initView();
        lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        requestLocation();
    }

    private void initView(){
        Spinner kategori = (Spinner) findViewById(R.id.kategori);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.kategori, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        kategori.setAdapter(adapter);
    }

    public void requestLocation(){
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        if(!gps_enabled && !network_enabled) {
            AlertDialog.Builder dialog = new AlertDialog.Builder(this);
            dialog.setMessage("Apa anda ingin mengaktifkan GPS?");
            dialog.setPositiveButton("ya", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(myIntent, OPEN_LOCATION_SETTINGS);
                }
            });
            dialog.setNegativeButton("tidak", new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
        }else {
            getLocation();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode==OPEN_LOCATION_SETTINGS){
            try {
                gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
                network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            } catch(Exception ex) {
                ex.printStackTrace();
            }

            getLocation();
        }

    }

    public void getLocation(){
        if(gps_enabled) lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,this);
        if(network_enabled) lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        lm.removeUpdates(this);
    }

    public void kirim(View v){
        kategori = ((Spinner) findViewById(R.id.kategori)).getSelectedItem().toString();
        aduan = ((EditText) findViewById(R.id.isiAduan)).getText().toString();

        v.setVisibility(View.GONE);
        findViewById(R.id.loadingKirim).setVisibility(View.VISIBLE);


        HashMap<String,String> param = new HashMap<>();
        param.put("simpan_aduan","1");
        param.put("nosambungan",Constants.getNosambungan());
        param.put("kategori", kategori);
        param.put("aduan", aduan);

        if(location!=null){
            param.put("lat",String.valueOf(location.getLatitude()));
            param.put("long",String.valueOf(location.getLongitude()));
        }
        ic.request(Constants.getHost() + "/simpan_aduan/", param,this);
    }

    @Override
    public void onLocationChanged(Location location) {
        this.location = location;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {}

    @Override
    public void onProviderDisabled(String s) {}

    @Override
    public boolean handleMessage(Message message) {
        int resp = message.getData().getInt("response_code");
        if(resp!=200){
            Toast.makeText(this,"Connection Lost",Toast.LENGTH_SHORT).show();
            findViewById(R.id.loadingKirim).setVisibility(View.GONE);
            findViewById(R.id.btnKirim).setVisibility(View.VISIBLE);
        }
        String response = message.getData().getString("response");
        if(response==null) return false;
        try {
            JSONObject json = new JSONObject(response);
            String id = json.getString("id");
            String waktu = json.getString("waktu");

            Database db = new Database(this);
            db.simpanAduan(new String[]{id, aduan, waktu, kategori});

            Intent i = new Intent();
            i.putExtra("id",id);
            i.putExtra("aduan",aduan);
            i.putExtra("kategori",kategori);
            i.putExtra("waktu",waktu);
            setResult(RESULT_OK, i);
            finish();
        } catch (JSONException e) {}

        return true;
    }
}
