package org.apache.cordova.core;

import android.app.Application;
import com.parse.Parse;
import com.parse.ParseInstallation;

public class ParseInit extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        // TODO - Change these with your own APP_ID AND CLIENT_KEY
        Parse.initialize(this, "APP_ID", "CLIENT_KEY");
        ParseInstallation.getCurrentInstallation().saveInBackground();
    }
}