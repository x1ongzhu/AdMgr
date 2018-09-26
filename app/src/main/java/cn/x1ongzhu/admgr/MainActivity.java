package cn.x1ongzhu.admgr;

import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.bumptech.glide.Glide;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.VideoAllCallBack;
import com.transitionseverywhere.Slide;
import com.transitionseverywhere.TransitionManager;
import com.transitionseverywhere.TransitionSet;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import fi.iki.elonen.NanoHTTPD;
import io.realm.Realm;
import io.realm.RealmResults;

public class MainActivity extends AppCompatActivity implements VideoAllCallBack {
    private static final int UNKNOWN_FILE = 0;
    private static final int VIDEO_FILE = 1;
    private static final int IMAGE_FILE = 2;

    @BindView(R.id.video_player)
    VideoView videoPlayer;
    @BindView(R.id.image_view)
    ImageView imageView;
    @BindView(R.id.transitions_container)
    RelativeLayout transitionsContainer;
    private int index = -1;
    private Timer timer;
    private List<Adv> data;
    private boolean started = false;
    private Adv currentAd;
    private Timer imageTimer;
    private boolean pause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String command = intent.getStringExtra("command");
                if ("pause".equals(command)) {
                    pause();
                } else if ("resume".equals(command)) {
                    resume();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.playmedia.play");
        registerReceiver(receiver, filter);

        videoPlayer.setVideoAllCallBack(this);

        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Realm realm = Realm.getDefaultInstance();
                RealmResults<Adv> results = realm.where(Adv.class)
                        .beginGroup().isNull("startTime").and().isNull("endTime").endGroup()
                        .or()
                        .beginGroup().isNull("startTime").greaterThan("endTime", new Date()).endGroup()
                        .or()
                        .beginGroup().isNull("endTime").lessThan("startTime", new Date()).endGroup()
                        .sort("createTime").findAll();
                data = realm.copyFromRealm(results);
                realm.close();

                if (currentAd != null) {
                    int i = 0;
                    for (; i < data.size(); i++) {
                        if (data.get(i).getId().equals(currentAd.getId())) {
                            index = i;
                            break;
                        }
                    }
                    if (i == data.size()) {
                        index = -1;
                    }
                }
                if (!started) {
                    runOnUiThread(() -> next());
                }
            }
        }, 0, 10000);
    }

    private void next() {
        if (pause) {
            return;
        }
        if (index < data.size() - 1) {
            index++;
        } else {
            index = 0;
        }
        if (index >= 0 && index < data.size()) {
            started = true;
            Adv adv = data.get(index);
            currentAd = adv;
            int type = getFileType(adv);
            if (type == VIDEO_FILE) {
                playVideo(adv);
            } else if (type == IMAGE_FILE) {
                displayImage(adv);
            } else {
                next();
            }
        }
    }

    private void pause() {
        pause = true;
        int type = getFileType(currentAd);
        if (type == VIDEO_FILE) {
            videoPlayer.onVideoPause();
        } else if (type == IMAGE_FILE) {
            imageTimer.cancel();
        }
    }

    private void resume() {
        pause = false;
        int type = getFileType(currentAd);
        if (type == VIDEO_FILE) {
            videoPlayer.onVideoResume();
        } else if (type == IMAGE_FILE) {
            imageTimer = new Timer();
            imageTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runOnUiThread(() -> next());
                }
            }, currentAd.getDuration() * 1000);
        }
    }

    private void playVideo(Adv adv) {
        TransitionSet transitionSet = new TransitionSet();
        Slide right = new Slide(Gravity.RIGHT);
        right.addTarget(videoPlayer);
        Slide left = new Slide(Gravity.LEFT);
        left.addTarget(imageView);
        transitionSet.addTransition(left);
        transitionSet.addTransition(right);
        File file = new File(Environment.getExternalStoragePublicDirectory("ads"), adv.getFileName());
        String url = file.getPath();
        videoPlayer.setUp("file://" + url, true, "");
        videoPlayer.startPlayLogic();
//        TransitionManager.beginDelayedTransition(transitionsContainer, transitionSet);
        videoPlayer.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.GONE);
    }

    private void displayImage(Adv adv) {
        TransitionSet transitionSet = new TransitionSet();
        Slide right = new Slide(Gravity.RIGHT);
        right.addTarget(imageView);
        Slide left = new Slide(Gravity.LEFT);
        left.addTarget(videoPlayer);
        transitionSet.addTransition(left);
        transitionSet.addTransition(right);
        File file = new File(Environment.getExternalStoragePublicDirectory("ads"), adv.getFileName());
        videoPlayer.release();
        Glide.with(this).load(Uri.fromFile(file)).into(imageView);
//        TransitionManager.beginDelayedTransition(transitionsContainer, transitionSet);
        imageView.setVisibility(View.VISIBLE);
        videoPlayer.setVisibility(View.GONE);
        imageTimer = new Timer();
        imageTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> next());
            }
        }, adv.getDuration() * 1000);
    }

    private int getFileType(Adv adv) {
        String[] img = {"jpg", "jpeg", "png", "gif", "bmp"};
        String[] video = {"avi", "mp4", "flv", "mkv", "3gp", "wmv"};
        for (String ext : img) {
            if (adv.getFileName().toLowerCase().endsWith(ext.toLowerCase())) {
                return IMAGE_FILE;
            }
        }
        for (String ext : video) {
            if (adv.getFileName().toLowerCase().endsWith(ext.toLowerCase())) {
                return VIDEO_FILE;
            }
        }
        return UNKNOWN_FILE;
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
        runOnUiThread(this::next);
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
}
