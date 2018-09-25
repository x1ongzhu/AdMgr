package cn.x1ongzhu.admgr;

import android.app.Application;

import java.io.FileNotFoundException;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class App extends Application {
    public static RealmConfiguration configuration;

    @Override
    public void onCreate() {
        super.onCreate();
        Realm.init(this);
        configuration = new RealmConfiguration.Builder()
                .schemaVersion(0)
                .migration(new DbMigration())
                .build();
        try {
            Realm.migrateRealm(configuration);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}
