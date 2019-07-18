package cn.x1ongzhu.admgr;

import android.app.Application;

import com.pgyersdk.crash.PgyCrashManager;
import com.shuyu.gsyvideoplayer.player.PlayerFactory;

import java.io.FileNotFoundException;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import tv.danmaku.ijk.media.exo2.Exo2PlayerManager;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
//        PlayerFactory.setPlayManager(new Exo2PlayerManager());
        PgyCrashManager.register();
        Realm.init(this);
        RealmConfiguration configuration = new RealmConfiguration.Builder()
                .schemaVersion(1)
                .build();
        Realm.setDefaultConfiguration(configuration);
        try {
            RealmConfiguration migrationConfig = new RealmConfiguration.Builder()
                    .schemaVersion(1)
                    .migration(new DbMigration())
                    .build();
            Realm.migrateRealm(migrationConfig);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
