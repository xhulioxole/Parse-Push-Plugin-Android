Cordova Parse.com Plugin
=========================

Cordova v3+ plugin for Parse.com push (Android SDK v1.8.0)

[Parse.com's](http://parse.com) Javascript API has no mechanism to register a device for or receive push notifications, which
makes it fairly useless for PN in Phonegap/Cordova. This plugin bridges the gap by leveraging native Parse.com SDKs
to register/receive PNs and allow a few essential methods to be accessible from Javascript. 

For Android, Parse SDK v1.8.0 is used. This means GCM support and no more background process `PushService` unnecessarily
taps device battery to duplicate what GCM already provides.

This plugin exposes the four native Android API push services to JS:
* **register**( options, successCallback, errorCallback )   -- register the device + a JS event callback (when a PN is received)
* **getInstallationId**( successCallback, errorCallback )
* **getSubscriptions**( successCallback, errorCallback )
* **subscribe**( channel, successCallback, errorCallback )
* **unsubscribe**( channel, successCallback, errorCallback )
* **received**( successCallback, errorCallback )

Installation
------------

    cordova plugin add https://github.com/4ndywilliamson/Parse-Push-Plugin-Android

####Android devices without Google Cloud Messaging:
If you only care about GCM devices, you're good to go. Move on to the [Usage](#usage) section. 

The automatic setup above does not work for non-GCM devices. To support them, the `ParseBroadcastReceiver`
must be setup to work properly. My guess is this receiver takes care of establishing a persistent connection that will
handle push notifications without GCM. Follow these steps for `ParseBroadcastReceiver` setup:

1. Add the following to your AndroidManifest.xml, inside the `<application>` tag
    ```xml
    <receiver android:name="com.parse.ParseBroadcastReceiver">
       <intent-filter>
          <action android:name="android.intent.action.BOOT_COMPLETED" />
          <action android:name="android.intent.action.USER_PRESENT" />
       </intent-filter>
    </receiver>
    ```
    
2. Add the following permission to AndroidManifest.xml, as a sibling of the `<application>` tag
    ```xml
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    ```
On the surface, step 1 & 2 should be enough. However, when one of the actions `BOOT_COMPLETED` or
`USER_PRESENT` (on screen unlock) occurs, `ParseBroadastReceiver` gets invoked well before your Javascript
code or this plugin's Java code gets a chance to call `Parse.initialize()`. The Parse SDK then barfs, causing
your app to crash. Continue with steps 3 & 4 to fix this.

3. Phonegap/Cordova doesn't seem to define its own android.app.Application, it only defines an android Activity.
We'll need to define an application class to override the default `onCreate` behavior and call `Parse.initialize()`
so the crash described above does not occur. In your application's Java source path, e.g., `platforms/android/src/com/example/app`, create a file
named MainApplication.java and define it this way

    package com.example.app;  //REPLACE THIS WITH YOUR package name

    import android.app.Application;
    import com.parse.Parse;
    import com.parse.ParseCrashReporting;

    public class MainApplication extends Application {
	    @Override
        public void onCreate() {
            super.onCreate();

            ParseCrashReporting.enable(this);
            Parse.initialize(this, "YOUR_PARSE_APPID", "YOUR_PARSE_CLIENT_KEY");
        }
    }
    
4. The final step is to register MainApplication in AndroidManifest.xml so it's used instead of the default.
In the `<application>` tag, add the attribute `android:name="MainApplication"`. Obviously, you don't have
to name your application class this way, but you have to use the same name in 3 and 4. 

Usage
-----
Once the device is ready, call ```ParsePushPlugin.register()```. This will register the device with Parse, you should see this reflected in your Parse control panel.
You can optionally specify an event callback to be invoked when a push notification is received.
After successful registration, you can call any of the other available methods.

    <script type="text/javascript">

    //
    // Registers the device for push

    function registerForParsePush() {
        ParsePushPlugin.register({ appId:"PARSE_APPID", clientKey:"PARSE_CLIENT_KEY", ecb:"onNotification"}, function() {
            console.log('Successfully registered device!');
            getInstallationData();
            getPushNotificationData();
        }, function(e) {
            console.log('Error registering device: ' + e);
        });
    }
       
    //
    // Retrieves the Parse.com push installation ID

    function getInstallationData() {
	    ParsePushPlugin.getInstallationId(function(id) {
		    console.log(id);
	    }, function(e) {
		    console.log('Error');
	    });
    }
    
    //
    // Gets all Channels
    
    function getAllSubscriptionsData() {
        ParsePushPlugin.getSubscriptions(function(subscriptions) {
		    console.log(subscriptions);
	    }, function(e) {
		    console.log('Error');
	    });
    }
	
    //
    // Adds a Channel
    
    function subscribePushNotification() {
	    ParsePushPlugin.subscribe('Channel', function() {
		    console.log('OK');
	    }, function(e) {
		    console.log('Error');
	    });
    }
    
    //
    // Removes a Channel
    
    function unsubscribePushNotification() {
	    ParsePushPlugin.unsubscribe('Channel', function(msg) {
		    console.log('OK');
	    }, function(e) {
		    console.log('Error');
	    });    
	}

    //
    // Gets the push notification payload data when the app opens from a push notification

    function getPushNotificationData() {
        ParsePushPlugin.received(function(data) {
            if (data.length > 0) {
                notificationPayload(JSON.parse(data));
                console.log('Successfully Obtained Push Data: ' + data);
            }
        }, function(e) { 
            console.log('Error Obtaining Push Data: ' + e);
        });
    }

    //
    // Gets the push notification payload data when a push is sent whilst in the app, ParsePushPlugin.register sets up the callback
    
	function onNotification(data){
    	console.log("Received pn: " + JSON.stringify(data));
	}
    
    </script>

Silent Notifications
--------------------
For Android, a silent notification can be sent if you omit the `title` and `alert` fields in the
JSON payload. If you register an **ecb** as shown above, it will still be invoked and you can
do whatever processing needed on the other fields of the payload.


Compatibility
-------------
Cordova v3+
