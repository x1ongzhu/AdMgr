package cn.x1ongzhu.admgr;

import android.app.Application;

import java.io.FileNotFoundException;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
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
