package com.example.playmedia;

public class PlayMediaService {
    static {
        System.loadLibrary("playmedia-jni");
    }

    public static native int dlpioctl(int var0, int var1, int var2);

    public static native int dlpopen();

    public static native int hymcmd(int var0, int var1, int var2);

    public static native int hymopen();

    public static native int mspopen();

    public static native int msprecv(int var0);

    public static native int mspsend(int var0, int var1, int var2);

    public static native int sensorgetvalue(int var0);

    public static native int sensoropen();
}
