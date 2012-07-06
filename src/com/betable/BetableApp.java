package com.betable;

import android.app.Application;
import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class BetableApp extends Application {
    protected static final String TAG = "BetableApp";

    public static Betable BETABLE;
    public static Properties betableProperties = new Properties();
    public static final String CLIENT_ID_KEY = "client_id",
        CLIENT_SECRET_KEY = "client_secret",
        REDIRECT_URI_KEY = "redirect_uri",
        GAME_ID_KEY = "game_id";

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            this.loadBetableProperties();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public static String getBetableProperty(String key) {
        return betableProperties.getProperty(key);
    }

    private void loadBetableProperties() throws IOException {
        InputStream is = null;
        try {
            AssetManager assetManager = this.getResources().getAssets();
            is = assetManager.open("betable.properties");
            betableProperties.load(is);
        } finally {
            if (is != null) is.close();;
        }
    }
}
