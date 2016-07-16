package com.muzakki.ahmad.sipadupelanggan.main;



/**
 * Created by jeki on 6/1/15.
 */
public class Constants{
    private final static String PROTOCOL = "http://";
    private final static String PORT = ":88";

    private static String ip = "10.0.2.2";
    private static String host = PROTOCOL+ip+PORT;
    private static String nosambungan = "";


    static int timeout = 60;
    static int keepalive = 200;
    static String username = "user";
    static String password = "rahasia";

    static String ACTION_NOTIFICATION = Constants.class.getPackage().getName();

    static void setHost(String ip){
        Constants.ip = ip;
        host = PROTOCOL+ip+PORT;
    }

    static String getHost() {
        return host;
    }

    static void setNosambungan(String nosambungan) {
        Constants.nosambungan = nosambungan;
    }

    static String getNosambungan() {
        return nosambungan;
    }

    public static String getIp() {
        return ip;
    }
}
