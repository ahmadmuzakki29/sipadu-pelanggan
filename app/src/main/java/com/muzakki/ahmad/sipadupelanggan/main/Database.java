package com.muzakki.ahmad.sipadupelanggan.main;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by jeki on 5/31/15.
 */
public class Database extends SQLiteOpenHelper{
    // If you change the database schema, you must increment the database version.
    public static final int DATABASE_VERSION = 7;
    public static final String DATABASE_NAME = "sipadu.db";

    private static final String[] TABLES = new String[]{"login","aduan","host"};
    private static final String[] SQL_CREATE_ENTRIES = new String[]{
            "CREATE TABLE login (id INTEGER PRIMARY KEY,nosambungan text,token text)",
            "CREATE TABLE aduan (id INTEGER PRIMARY KEY,aduan text," +
                    "waktu text,tindak_lanjut text, waktu_tindak_lanjut text,kategori text)",
            "create table host(ip text)"
    };

    public Database(Context context){
        super(context, DATABASE_NAME, null, DATABASE_VERSION);

    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        for(String entry : SQL_CREATE_ENTRIES){
            db.execSQL(entry);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for(String tb: TABLES){
            String delete = "DROP TABLE IF EXISTS "+tb;
            db.execSQL(delete);
        }
        onCreate(db);
    }

    private List<HashMap<String,String>> getResult(Cursor c){
        List<HashMap<String,String>> result = new ArrayList<>();
        if(c.getCount()>0) {
            while (c.moveToNext()) {
                HashMap<String, String> row = new HashMap<>();
                for(int col=0;col<c.getColumnCount();col++){
                    row.put(c.getColumnName(col),c.getString(col));
                }
                result.add(row);
            }
        }
        return result;
    }


    public String getIP(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select ip from host",null);

        try{
            if(c.moveToFirst()) {
                return c.getString(0);
            }else {
                return null;
            }
        }finally{
            db.close();
            c.close();
        }
    }

    public String[] getLoginCache(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select nosambungan,token from login",null);

        try{
            if(c.moveToFirst()) {
                return new String[]{c.getString(0),c.getString(1)};
            }else {
                return null;
            }
        }finally{
            db.close();
            c.close();
        }
    }

    public void clearCache(){
        SQLiteDatabase db = getReadableDatabase();
        onUpgrade(db,0,0);
    }

    public void saveIP(String ip){
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("delete from host");
        db.execSQL("insert into host(ip) values(?)",new String[]{ip});
        db.close();
    }

    public void saveLoginCache(String nosambungan,String token){
        SQLiteDatabase db = getReadableDatabase();
        db.execSQL("insert into login(nosambungan,token) values(?,?)",new String[]{nosambungan,token});
        db.close();
    }

    public List<HashMap<String,String>> getLatestAduan(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select waktu from aduan order by datetime(waktu) desc limit 1", null);

        try{
            return getResult(c);
        }finally {
            db.close();
            c.close();
        }
    }

    public List<HashMap<String,String>> getAduan(){
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("select id,aduan,strftime('%d/%m/%Y',waktu) as waktu, " +
                "(case when tindak_lanjut is null then 0 else 1 end) as tindak_lanjut " +
                "from aduan order by datetime(waktu) desc",
                null);

        try{
            return getResult(c);
        }finally {
            db.close();
            c.close();
        }
    }

    public int insertAduan(JSONArray json_array)throws JSONException{ /* saat pertama kali login */
        if(json_array.length()==0) return 0;
        String[] column = new String[]{"id","aduan","waktu","tindak_lanjut","waktu_tindak_lanjut","kategori"};
        String[] data = new String[json_array.length()*column.length];
        int a = 0;
        String field = "";
        String values = "";
        for(int i=0;i<json_array.length();i++){
            JSONObject obj = json_array.getJSONObject(i);
            values += "(";
            for(String col : column){
                if(i==0) field += col+",";
                String value = obj.getString(col);
                data[a++] = value.equals("None")? null:value;
                values += "?,";
            }
            values = values.substring(0,values.length()-1);
            values += "),";
        }
        values = values.substring(0,values.length()-1);
        field = field.substring(0,field.length()-1);
        String query = "insert into aduan("+field+") values"+values;
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(query,data);
        db.close();
        return json_array.length();
    }

    public void simpanAduan(String[] data){
        SQLiteDatabase db = getReadableDatabase();
        String query = "insert into aduan(id,aduan,waktu,kategori) values(?,?,?,?)";
        db.execSQL(query,data);
        db.close();
    }

    public String simpanTindaklanjut(String[] data){
        SQLiteDatabase db = getReadableDatabase();
        String query = "update aduan set tindak_lanjut=?, waktu_tindak_lanjut=? where id=?";
        db.execSQL(query,data);
        String id = data[2];
        Cursor c = db.rawQuery("select kategori from aduan where id=?",new String[]{id});

        List<HashMap<String,String>> r = getResult(c);
        String kategori = r.get(0).get("kategori");
        try{
            return kategori;
        }finally {
            db.close();
            c.close();
        }
    }

    public List<HashMap<String,String>> getAduanDetail(String id){
        SQLiteDatabase db = getReadableDatabase();
        String query = "select aduan,tindak_lanjut,strftime('%d/%m/%Y',waktu) as waktu, " +
                "strftime('%d/%m/%Y',waktu_tindak_lanjut) as waktu_tindak_lanjut " +
                "from aduan where id=?";
        Cursor c = db.rawQuery(query, new String[]{id});

        try{
            return getResult(c);
        }finally {
            db.close();
            c.close();
        }

    }
}
