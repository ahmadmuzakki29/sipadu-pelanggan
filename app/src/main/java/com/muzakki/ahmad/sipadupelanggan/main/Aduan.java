package com.muzakki.ahmad.sipadupelanggan.main;

import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.muzakki.ahmad.sipadupelanggan.R;
import com.muzakki.ahmad.sipadupelanggan.service.ActionListener;
import com.muzakki.ahmad.sipadupelanggan.service.MqttClient;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jeki on 5/31/15.
 */
public class Aduan extends ActionBarActivity implements AdapterView.OnItemClickListener {
    private static final int TAMBAH_ADUAN = 1;
    Database db = new Database(this);
    MqttClient client = null;
    InternetConnection ic = new InternetConnection(this);
    ArrayList<String> storeID = null;
    private Handler.Callback syncAll = new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            syncAllCallback(message);
            return true;
        }
    };

    ArrayAdapter<HashMap<String,String>> adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.aduan);
        String[] result = db.getLoginCache();
        if(result==null){
            showLogin();
        }else {
            showLoading();
            initList();
            connectAction();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        showAduan();
        Intent i = getIntent();
        String action = i.getStringExtra("ACTION");
        if(action!=null && action.equals(Constants.ACTION_NOTIFICATION)) {
            String id = i.getStringExtra("id");
            i.removeExtra("ACTION");
            i.removeExtra("id");
            showAduanDetail(id);
        }
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        String action = i.getStringExtra("ACTION");

        if(action!=null && action.equals(Constants.ACTION_NOTIFICATION)) {
            String id = i.getStringExtra("id");
            showAduanDetail(id);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("jeki", "onpause");

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("jeki", "ondestroy");
        client.unregisterResources();
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int i, long l) {
        String id = storeID.get(i);
        showAduanDetail(id);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.aduan_action, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.menu_tambah_aduan:
                Intent i = new Intent(this,AduanTambah.class);
                startActivityForResult(i, TAMBAH_ADUAN);
                return true;
            case R.id.menu_logout:
                dialogLogout();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode==TAMBAH_ADUAN && resultCode==RESULT_OK){
            HashMap<String,String> item = new HashMap<>();
            Bundle data = intent.getExtras();
            item.put("aduan",data.getString("aduan"));
            item.put("tanggal",data.getString("waktu"));
            item.put("tindak_lanjut","0");
            addList(data.getString("id"),item);
        }
    }

    private void initList(){
        List<HashMap<String,String>> result = db.getLatestAduan();
        if(result.isEmpty()) {
            if(!ic.isConnected()) return;
            String url = Constants.getHost()+"/sync_aduan/";
            HashMap<String,String> param = new HashMap<>();
            param.put("sync","all");
            param.put("nosambungan",Constants.getNosambungan());

            ic.request(url, param, syncAll);
        }
    }

    private void showAduanDetail(String id){
        Intent intent = new Intent(this,AduanDetail.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("id", id);

        startActivity(intent);
    }

    private void syncAllCallback(Message msg){
        String response = msg.getData().getString("response");
        try {
            JSONArray json_array = new JSONArray(response);
            db.insertAduan(json_array);
            showAduan();
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addList(String id,HashMap<String,String> item){
        if(adapter==null){
            showAduan();
            return;
        }
        storeID.add(0,id);
        adapter.insert(item, 0);
        adapter.notifyDataSetChanged();
    }

    public void updateList(String id){
        int pos = -1;
        for(int i=0;i<storeID.size();i++){
            String _id = storeID.get(i);
            if(_id.equals(id)){
                pos = i;
                break;
            }
        }
        if(pos==-1){
            Log.i("jeki","cannot update list");
            return;
        }
        HashMap<String,String> item = adapter.getItem(pos);
        adapter.remove(item);
        item.put("tindak_lanjut", "1");
        adapter.insert(item, pos);
        adapter.notifyDataSetChanged();
    }

    public void showAduan(){ // called only first time
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                List<HashMap<String, String>> result = db.getAduan();
                if (result.size() == 0) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(Aduan.this, R.layout.aduan_empty, R.id.txtAduanEmpty);
                    adapter.add("Data Kosong");
                    ListView content = (ListView) findViewById(R.id.content);
                    content.setAdapter(adapter);
                    return;
                }
                List<HashMap<String, String>> list = new ArrayList<>();
                int i = 0;
                storeID = new ArrayList<>();
                for (HashMap<String, String> entry : result) {
                    HashMap<String, String> values = new HashMap<>();
                    storeID.add(entry.get("id"));
                    values.put("aduan", entry.get("aduan"));
                    values.put("waktu", entry.get("waktu"));
                    values.put("tindak_lanjut", entry.get("tindak_lanjut"));

                    list.add(values);
                }
                adapter = new AduanAdapter(Aduan.this, R.layout.aduan_list, list);
                ListView content = (ListView) findViewById(R.id.content);
                content.setAdapter(adapter);
                content.setOnItemClickListener(Aduan.this);
            }
        });
    }

    private void showLoading(){
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,R.layout.aduan_loading,R.id.txtAduanLoading);
        adapter.add("Loading...");
        ListView content = (ListView) findViewById(R.id.content);
        content.setAdapter(adapter);
    }

    private void connectAction() {

        MqttConnectOptions conOpt = new MqttConnectOptions();

        String clientId = Constants.getNosambungan();
        boolean cleanSession = true;
        String uri = "tcp://"+Constants.getIp()+":1883";

        client = new MqttClient(this, uri, clientId,Constants.getNosambungan());


        String clientHandle = uri + clientId;

        // connection options
        String[] actionArgs = new String[1];
        actionArgs[0] = clientId;

        conOpt.setCleanSession(cleanSession);
        conOpt.setConnectionTimeout(Constants.timeout);
        conOpt.setKeepAliveInterval(Constants.keepalive);
        conOpt.setUserName(Constants.username);
        conOpt.setPassword(Constants.password.toCharArray());

        client.setCallback(new MqttCallbackHandler(this, clientHandle));

        final ActionListener callback = new ActionListener(this,
                ActionListener.Action.CONNECT, clientHandle, actionArgs);

        boolean doConnect = true;
        if (doConnect) {
            try {
                client.connect(conOpt, null, callback);
            }
            catch (MqttException e) {
                Log.e(this.getClass().getCanonicalName(),
                        "MqttException Occured", e);
            }
        }
    }

    public void requestNotif(){
        String url = Constants.getHost()+"/notification/";
        HashMap<String,String> param = new HashMap<>();
        param.put("action", "get_notif");
        param.put("nosambungan", Constants.getNosambungan());

        ic.request(url, param, null);
    }

    private void dialogLogout(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Anda yakin untuk keluar?");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                logout();
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                Log.i("jeki", "cancel");
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void logout(){
        NotificationManager nMgr = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nMgr.cancelAll();
        db.clearCache();
        try{
            client.disconnect();
        }catch(MqttException e){}
    }

    public void showLogin(){
        Intent i = new Intent(this,Login.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        finish();
    }

    public MqttClient getClient() {
        return client;
    }

    private class AduanAdapter extends ArrayAdapter<HashMap<String,String>>{

        public AduanAdapter(Context context, int resource, List<HashMap<String,String>> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View rowView = convertView;
            if (rowView == null) {
                LayoutInflater inflater = Aduan.this.getLayoutInflater();
                rowView = inflater.inflate(R.layout.aduan_list, null);

                ViewHolder viewHolder = new ViewHolder();

                viewHolder.aduan = (TextView) rowView.findViewById(R.id.aduan);
                viewHolder.tanggal = (TextView) rowView.findViewById(R.id.tanggal);
                viewHolder.tindak_lanjut = (ImageView) rowView.findViewById(R.id.checktindaklanjut);
                rowView.setTag(viewHolder);
            }
            ViewHolder holder = (ViewHolder) rowView.getTag();

            holder.aduan.setText(getItem(position).get("aduan"));
            holder.tanggal.setText(getItem(position).get("waktu"));

            String check = getItem(position).get("tindak_lanjut");
            if(check.equals("0")){
                holder.tindak_lanjut.setImageResource(R.drawable.ic_wait);
            }else{
                holder.tindak_lanjut.setImageResource(R.drawable.ic_check);
            }
            return rowView;
        }
    }

    static class ViewHolder {
        public TextView aduan;
        public TextView tanggal;
        public ImageView tindak_lanjut;

    }

}
