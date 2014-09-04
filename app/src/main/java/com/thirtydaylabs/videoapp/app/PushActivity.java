package com.thirtydaylabs.videoapp.app;

import android.app.ActionBar;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeIntents;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;
import com.thirtydaylabs.nigerianmovies.R;
import com.thirtydaylabs.videoapp.utilities.MenuFunctions;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by HooMan on 12/08/13.
 */


public class PushActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    // create string variables
    String YOUTUBE_API_KEY;
    String ID;
    String TITLE;

    VideoView videoView;
    String videoUrl;

    Context context = this;

    private InterstitialAd interstitial;
    // create object of view
    YouTubePlayerView youTubePlayerView;
    SharedPreferences prefs;

    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_layout);


        // connect view object and view id on xml
        youTubePlayerView = (YouTubePlayerView)findViewById(R.id.youtubeplayerview);

        myWebView = (WebView) findViewById( R.id.videoView );

        setTitle("More Here");
        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // For the main activity, make sure the app icon in the action bar
            // does not behave as a button
            getActionBar().setIcon(R.drawable.ic_action_previous_item);
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);

            int actionBarTitleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            if (actionBarTitleId > 0) {
                TextView title = (TextView) findViewById(actionBarTitleId);
                if (title != null) {
                    title.setTextColor(Color.YELLOW);
                }
            }

        }


//        videoView = (VideoView)findViewById(R.id.videoView);

        //Store the sharedprefrences
        prefs = getSharedPreferences(CollectionActivity.PREFS_NAME, Context.MODE_MULTI_PROCESS );
        ID = prefs.getString("votd", "");
        Log.i("Daily SP", ID);

        // display youtube player
        adMobInterstitialInitiate();


        if(YouTubeIntents.isYouTubeInstalled(context)){

            // display youtube player
            youTubePlayerView.initialize(YOUTUBE_API_KEY, this);
            youTubePlayerView.setVisibility(View.VISIBLE);

        }else{
            myWebView.setVisibility(View.VISIBLE);
            myWebView.setWebChromeClient(new WebChromeClient());
            myWebView.getSettings().setPluginState(WebSettings.PluginState.ON_DEMAND);

            //String playerURL="http://modernapps.biz/api/player.php?videoid="+ID;

            String playVideo ="<style>body{margin: 0px;margin-top: -30px;background-color: black;} .embed-container { position: relative; padding-bottom: 56.25%; height: 0; overflow: hidden; max-width: 100%; height: auto; } .embed-container iframe, .embed-container object, .embed-container embed { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }</style><div class='embed-container'><iframe src='http://www.youtube.com/embed/"+ID+"?rel=0&autoplay=1' frameborder='0' allowfullscreen></iframe></div>";

            myWebView.getSettings().setJavaScriptEnabled(true);
            myWebView.setVerticalScrollBarEnabled(false);
            myWebView.setHorizontalScrollBarEnabled(false);
            //myWebView.loadUrl(playerURL);
            myWebView.loadData(playVideo, "text/html", "utf-8");

        }
    }



    /**
     * Display interstitial admob ad
     */

    private void adMobInterstitialInitiate(){

        if(!getString(R.string.admob_id_interstitials_daily).equals("")){
            //AdMob Full Screen
            //Create the interstitial
            interstitial = new InterstitialAd(this);
            interstitial.setAdUnitId(getString(R.string.admob_id_interstitials_daily));
            // Create ad request
            AdRequest adRequest = new AdRequest.Builder().build();
            // Begin loading your interstitial
            interstitial.loadAd(adRequest);

            interstitial.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Save app state before going to the ad overlay.
                    // Create a simple intent that starts the hierarchical parent activity and
                    // use NavUtils in the Support Package to ensure proper handling of Up.
                    returnToParent();
                    finish();
                }

                @Override
                public void onAdFailedToLoad(int errorCode){
                    returnToParent();
                    finish();
                }
            });
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.player_activity_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                MenuFunctions.openVideoShare(context, ID);
                return true;
            case R.id.action_daily_video:
                MenuFunctions.openDailyVideo(context);
                return true;
            case R.id.action_mores:
                MenuFunctions.openMore(context);
            case android.R.id.home:
                // This is called when the Home (Up) button is pressed in the action bar.
                displayInterstitial();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.

    }


    @Override
    public void onStop() {
        super.onStop();
        // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
        finish();
    }


    @Override
    public void onDestroy()
    {
        super.onDestroy();
        finish();
    }

    //Override the back button
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        if (keyCode== KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            int interstitial_probability = (int)(Math.random() * (2 + 1));
            if(interstitial != null && interstitial_probability == 0 ){
                //RevMob Full Screen Ad
                displayInterstitial();
            }
            // return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    /**
    * Invoke displayInterstitial() when you are ready to display an interstitial.
    */
    public void displayInterstitial() {
        myWebView.loadUrl("about:blank");
        SharedPreferences settings = getSharedPreferences(CollectionActivity.PREFS_NAME, MODE_MULTI_PROCESS);
        Boolean premium_status = settings.getBoolean("premiumStatus", false);
        if (interstitial.isLoaded() && !premium_status) {
            interstitial.show();
        }
        this.finish();
    }

    private void returnToParent() {
        Intent upIntent = new Intent(this, CollectionActivity.class);
        if (NavUtils.shouldUpRecreateTask(this, upIntent)) {
            // This activity is not part of the application's task, so create a new task
            // with a synthesized back stack.
            TaskStackBuilder.from(this)
                    // If there are ancestor activities, they should be added here.
                    .addNextIntent(upIntent)
                    .startActivities();
            finish();
        } else {
            // This activity is part of the application's task, so simply
            // navigate up to the hierarchical parent activity.
            NavUtils.navigateUpTo(this, upIntent);
        }
    }



    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider,
                                        YouTubeInitializationResult result) {
        Toast.makeText(getApplicationContext(),
                "onInitializationFailure()",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean wasRestored) {
        try{
            if (!wasRestored) {
                player.loadVideo(ID);
            }
        }
        catch(IllegalStateException ise){
            //do nothing probably device go rotated
            return;
        }
    }


}
