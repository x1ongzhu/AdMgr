package cn.x1ongzhu.admgr;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        NanoHTTPD server = new WebServer(8080, this);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
