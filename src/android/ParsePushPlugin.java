package org.apache.cordova.core;

import java.util.List;
import java.lang.Exception;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

import com.parse.Parse;
import com.parse.ParsePush;
import com.parse.ParseInstallation;

import android.util.Log;

public class ParsePushPlugin extends CordovaPlugin {
    
    public static final String ACTION_REGISTER = "register";
    public static final String ACTION_GET_INSTALLATION_ID = "getInstallationId";
    public static final String ACTION_GET_INSTALLATION_OBJECT_ID = "getInstallationObjectId";
    public static final String ACTION_GET_SUBSCRIPTIONS = "getSubscriptions";
    public static final String ACTION_SUBSCRIBE = "subscribe";
    public static final String ACTION_UNSUBSCRIBE = "unsubscribe";
    public static final String ACTION_RECEIVED = "received";
        
    private static CordovaWebView gWebView;
    
    private static String gECB;
    
    public static final String LOGTAG = "ParsePushPlugin";

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    	if (action.equals(ACTION_REGISTER)) {
            this.registerDevice(callbackContext, args);
            return true;
        }
        if (action.equals(ACTION_GET_INSTALLATION_ID)) {
            this.getInstallationId(callbackContext);
            return true;
        }

        if (action.equals(ACTION_GET_INSTALLATION_OBJECT_ID)) {
            this.getInstallationObjectId(callbackContext);
            return true;
        }
        if (action.equals(ACTION_GET_SUBSCRIPTIONS)) {
            this.getSubscriptions(callbackContext);
            return true;
        }
        if (action.equals(ACTION_SUBSCRIBE)) {
            this.subscribe(args.getString(0), callbackContext);
            return true;
        }
        if (action.equals(ACTION_UNSUBSCRIBE)) {
            this.unsubscribe(args.getString(0), callbackContext);
            return true;
        }
        if (action.equals(ACTION_RECEIVED)) {
            this.received(callbackContext);
            return true;
        }
        return false;
    }

    private void registerDevice(final CallbackContext callbackContext, final JSONArray args) {
    	try {
            
            Log.e(LOGTAG, "Loading Parse Push v1.8.0");

            //
            //
            
        	JSONObject jo = args.getJSONObject(0);
            String appId = jo.getString("appId");
            String clientKey = jo.getString("clientKey");
            
        	//
        	// Initialize Parse
            
            Parse.initialize(cordova.getActivity(), appId, clientKey);
            ParseInstallation.getCurrentInstallation().saveInBackground();
            
            //
            // Register callbacks for notification events
            
            gECB = jo.optString("ecb");
            
            callbackContext.success();
            
        } catch (JSONException e) {
            callbackContext.error("JSONException: " + e.toString());
        } catch(Exception e){
        	callbackContext.error(e.toString());
        }
    }

    private void getInstallationId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                String installationId = ParseInstallation.getCurrentInstallation().getInstallationId();
                callbackContext.success(installationId);
            }
        });
    }

    private void getInstallationObjectId(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
                String objectId = ParseInstallation.getCurrentInstallation().getObjectId();
                callbackContext.success(objectId);
            }
        });
    }

    private void getSubscriptions(final CallbackContext callbackContext) {
        cordova.getThreadPool().execute(new Runnable() {
            public void run() {
            	List<String> subscriptions = ParseInstallation.getCurrentInstallation().getList("channels");
                callbackContext.success(subscriptions.toString());
            }
        });
    }

    private void subscribe(final String channel, final CallbackContext callbackContext) {
    	ParsePush.subscribeInBackground(channel);
        callbackContext.success();
    }

    private void unsubscribe(final String channel, final CallbackContext callbackContext) {
    	ParsePush.unsubscribeInBackground(channel);
        callbackContext.success();
    }
    
    //
    //
    
    private void received(final CallbackContext callbackContext) {
        
        Context myContext = cordova.getActivity();

        String data = policyData(myContext);
        
        callbackContext.success(data);
        
    }
    
    //
    //
    
    public static String policyData(Context context) {
                            
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        
        String pushDataString = preferences.getString("PUSHDATA", "");
        
        Log.e(LOGTAG, pushDataString);

        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("PUSHDATA", ""); // Storing string
        editor.commit(); // Commit changes

        return pushDataString;
        
    }
    
    //
    // Use the cordova bridge to call the jsCB and pass it _json as param
    
    public static void javascriptECB (JSONObject _json) {
        
    	String snippet = "javascript:" + gECB + "(" + _json.toString() + ")";
        
    	Log.v(LOGTAG, "javascriptCB: " + snippet);
    	
    	if (gECB != null && !gECB.isEmpty() && gWebView != null) gWebView.sendJavascript(snippet);
        
    }
    
    //
    //
    
    @Override
    protected void pluginInitialize() {
        
    	gECB = null;
    	gWebView = this.webView;
    
    }
    
    @Override
    public void onDestroy() {
    
        super.onDestroy();
    	gECB = null;
    	gWebView = null;
    
    }
}
