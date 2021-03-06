package edu.mit.tabtracker.android;


import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.facebook.android.AsyncFacebookRunner;
import com.facebook.android.Facebook;


public class Session {

    private static final String TOKEN = "access_token";
    private static final String EXPIRES = "expires_in";
    private static final String KEY = "facebook-session";
    private static final String UID = "uid";
    private static final String NAME = "name";
    private static final String APP_ID = "app_id";

    private static Session singleton;
    private static Facebook fbLoggingIn;

    // The Facebook object
    private Facebook fb;

    // The user id of the logged in user
    private String uid;

    // The user name of the logged in user
    private String name;

    /**
     * Constructor
     * 
     * @param fb
     * @param uid
     * @param name
     */
    public Session(Facebook fb, String uid, String name) {
        this.fb = fb;
        this.uid = uid;
        this.name = name;
    }

    /**
     * Returns the Facebook object
     */
    public Facebook getFb() {
        return fb;
    }

    /**
     * Returns the session user's id
     */
    public String getUid() {
        return uid;
    }

    /**
     * Returns the session user's name 
     */
    public String getName() {
        return name;
    }

    /**
     * Stores the session data on disk.
     * 
     * @param context
     * @return
     */
    public boolean save(Context context) {

        Editor editor =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.putString(TOKEN, fb.getAccessToken());
        editor.putLong(EXPIRES, fb.getAccessExpires());
        editor.putString(UID, uid);
        editor.putString(NAME, name);
        editor.putString(APP_ID, fb.getAppId());
        if (editor.commit()) {
            singleton = this;
            return true;
        }
        return false;
    }

    /**
     * Loads the session data from disk.
     * 
     * @param context
     * @return
     */
    public static Session restore(Context context) {
        if (singleton != null) {
            if (singleton.getFb().isSessionValid()) {
                return singleton;
            } else {
                return null;
            }
        }

        SharedPreferences prefs =
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE);
        
        String appId = prefs.getString(APP_ID, null);
        
        if (appId == null) {
        	return null;
        }
        
        Facebook fb = new Facebook(appId);
        fb.setAccessToken(prefs.getString(TOKEN, null));
        fb.setAccessExpires(prefs.getLong(EXPIRES, 0));
        String uid = prefs.getString(UID, null);
        String name = prefs.getString(NAME, null);
        if (!fb.isSessionValid() || uid == null || name == null) {
            return null;
        }

        Session session = new Session(fb, uid, name);
        singleton = session;
        return session;
    }

    /**
     * Clears the saved session data.
     * 
     * @param context
     */
    public static void clearSavedSession(Context context) {
        Editor editor = 
            context.getSharedPreferences(KEY, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.commit();
        singleton = null;
    }

    /**
     * Freezes a Facebook object while it's waiting for an auth callback.
     */
    public static void waitForAuthCallback(Facebook fb) {
        fbLoggingIn = fb;
    }

    /**
     * Returns a Facebook object that's been waiting for an auth callback.
     */
    public static Facebook wakeupForAuthCallback() {
        Facebook fb = fbLoggingIn;
        fbLoggingIn = null;
        return fb;
    }
    
    public void fetchFriends(final AppSessionListener listener) {
    	new AsyncFacebookRunner(fb).request("me/friends", 
         	new AsyncRequestListener() {
             	public void onComplete(JSONObject dataObj, final Object state) {
					JSONArray friends = dataObj.optJSONArray("data");
					String[] friendNames = new String[friends.length()];
					int[] friendIds = new int[friends.length()];
					for (int i=0; i<friends.length(); i++) {
					    friendNames[i] = friends.optJSONObject(i).optString("name");
					    friendIds[i] = friends.optJSONObject(i).optInt("id");
					}
					FriendsListSingleton.getInstance().store(friendIds, friendNames);
					listener.onFetchFriendsComplete(friendIds, friendNames);
             	}
             	
             	public void onFail(String message) {
             		String[] friendNames = new String[0];
					int[] friendIds = new int[0];
             		FriendsListSingleton.getInstance().store(friendIds, friendNames);
					listener.onFetchFriendsComplete(friendIds, friendNames);
             	}
             	
         	}, null);
    }
    
}