package cn.x1ongzhu.admgr;

import android.annotation.TargetApi;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
import com.transitionseverywhere.TransitionManager;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;

public class MainActivity extends AppCompatActivity {
    @BindView(R.id.video_player)
    VideoView videoPlayer;
    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.transitions_container)
    RelativeLayout transitionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
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
        File file = new File(Environment.getExternalStoragePublicDirectory("ads"), "19ec8243-c3c2-4b49-8bfb-37c27939fa9d.mp4");
        String url = file.getPath();
        videoPlayer.setUp("file://" + url, true, "");
        videoPlayer.startPlayLogic();
        videoPlayer.setVideoAllCallBack(new VideoAllCallBack() {
            @Override
            public void onStartPrepared(String url, Object... objects) {

            }

            @Override
            public void onPrepared(String url, Object... objects) {

            }

            @Override
            public void onClickStartIcon(String url, Object... objects) {

            }

            @Override
            public void onClickStartError(String url, Object... objects) {

            }

            @Override
            public void onClickStop(String url, Object... objects) {

            }

            @Override
            public void onClickStopFullscreen(String url, Object... objects) {

            }

            @Override
            public void onClickResume(String url, Object... objects) {

            }

            @Override
            public void onClickResumeFullscreen(String url, Object... objects) {

            }

            @Override
            public void onClickSeekbar(String url, Object... objects) {

            }

            @Override
            public void onClickSeekbarFullscreen(String url, Object... objects) {

            }

            @Override
            public void onAutoComplete(String url, Object... objects) {
                displayImage();
            }

            @Override
            public void onEnterFullscreen(String url, Object... objects) {

            }

            @Override
            public void onQuitFullscreen(String url, Object... objects) {

            }

            @Override
            public void onQuitSmallWidget(String url, Object... objects) {

            }

            @Override
            public void onEnterSmallWidget(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekVolume(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekPosition(String url, Object... objects) {

            }

            @Override
            public void onTouchScreenSeekLight(String url, Object... objects) {

            }

            @Override
            public void onPlayError(String url, Object... objects) {

            }

            @Override
            public void onClickStartThumb(String url, Object... objects) {

            }

            @Override
            public void onClickBlank(String url, Object... objects) {

            }

            @Override
            public void onClickBlankFullscreen(String url, Object... objects) {

            }
        });
    }

    private void displayImage() {
        videoPlayer.release();
        imageView.setImageURI(Uri.fromFile(new File(Environment.getExternalStoragePublicDirectory("ads"), "david-kovalenko-414249-unsplash.jpg")));
        Log.d(this.getClass().getName(), "Complete");
        TransitionManager.beginDelayedTransition(transitionsContainer);
        imageView.setVisibility(View.VISIBLE);
        videoPlayer.setVisibility(View.GONE);
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        File file = new File(Environment.getExternalStoragePublicDirectory("ads"), "19ec8243-c3c2-4b49-8bfb-37c27939fa9d.mp4");
                        String url = file.getPath();
                        videoPlayer.setUp("file://" + url, true, "");
                        videoPlayer.startPlayLogic();
                        TransitionManager.beginDelayedTransition(transitionsContainer);
                        videoPlayer.setVisibility(View.VISIBLE);
                        imageView.setVisibility(View.GONE);
                    }
                });
            }
        }, 5000);
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
