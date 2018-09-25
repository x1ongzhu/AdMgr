package cn.x1ongzhu.admgr;

import android.annotation.TargetApi;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.shuyu.gsyvideoplayer.GSYVideoManager;

import java.io.File;
import java.io.IOException;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.video_player)
    VideoView videoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        init();
        NanoHTTPD server = new WebServer(8080, this);
        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        File file = new File(Environment.getExternalStoragePublicDirectory("adv"), "3a95877e-4a02-4bc1-b699-b0208b03468e.mp4");
        String url = file.getPath();

        videoPlayer.setUp(url, true, "");
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    protected void onDestroy() {
        super.onDestroy();
        videoPlayer.release();
    }

    @Override
    public void onBackPressed() {
        //释放所有
        videoPlayer.setVideoAllCallBack(null);
        GSYVideoManager.releaseAllVideos();
        super.onBackPressed();
    }
}
