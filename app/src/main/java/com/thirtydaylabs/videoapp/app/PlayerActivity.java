package com.thirtydaylabs.videoapp.app;


/**
 * Created by HooMan on 12/08/13.
 */

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.MediaController;
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
import com.google.android.youtube.player.YouTubePlayer.Provider;
import com.google.android.youtube.player.YouTubePlayerView;
import com.thirtydaylabs.gymnasticstipsandtricks.R;
import com.thirtydaylabs.videoapp.utilities.MenuFunctions;


public class PlayerActivity extends YouTubeBaseActivity implements YouTubePlayer.OnInitializedListener {

    // create string variables
    String YOUTUBE_API_KEY;
    String ID;
    String TITLE;

    Context context = this;

    private InterstitialAd interstitial;
    // create object of view
    YouTubePlayerView youTubePlayerView;

    // In app purchase flag
    Boolean premium_status;

    WebView myWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.player_layout);

        SharedPreferences settings = getSharedPreferences(CollectionActivity.PREFS_NAME, MODE_MULTI_PROCESS);

        premium_status = settings.getBoolean("premiumStatus", false);

        // connect view object and view id on xml
        youTubePlayerView = (YouTubePlayerView)findViewById(R.id.youtubeplayerview);
        myWebView = (WebView) findViewById( R.id.videoView );

        // get YOUTUBE APIKEY

        YOUTUBE_API_KEY = CollectionActivity.YOUTUBE_KEY;

        // get video id from previous page
        Intent i = getIntent();
        ID = i.getStringExtra("id");
        TITLE = i.getStringExtra("title");

        setTitle(TITLE);

        // Make sure we're running on Honeycomb or higher to use ActionBar APIs
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // For the main activity, make sure the app icon in the action bar
            // does not behave as a button
            getActionBar().setIcon(R.drawable.ic_action_previous_item);
            ActionBar actionBar = getActionBar();
            actionBar.setHomeButtonEnabled(true);
        }


        //Initiate admob if the premium package does not exists
        if(!premium_status){
            adMobInterstitialInitiate();
        }


        initiatePlayer();


    }



    private void initiatePlayer(){

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
            WebViewClient client = new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    Log.d("MYAPP", "Page loaded");
                }
            };
            myWebView.setWebViewClient(client);

        }

    }




    private void adMobInterstitialInitiate(){

        if(!getString(R.string.admob_id_interstitials).equals("")){
            //AdMob Full Screen
            //Create the interstitial
            interstitial = new InterstitialAd(this);
            interstitial.setAdUnitId( getString(R.string.admob_id_interstitials));
            // Create ad request
            AdRequest adRequest = new AdRequest.Builder().build();
            // Begin loading your interstitial
            interstitial.loadAd(adRequest);

            interstitial.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Save app state before going to the ad overlay.
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
                // Create a simple intent that starts the hierarchical parent activity and
                // use NavUtils in the Support Package to ensure proper handling of Up.
                //RevMob Full Screen Ad
                displayInterstitial();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void returnToParent() {
        finish();
    }

    @Override
    public void onStart() {
        super.onStart();
        // The rest of your onStart() code.
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    protected void onResume() {
        super.onResume();
        initiatePlayer();
    }

    @Override
    public void onStop() {
        super.onStop();
        // The rest of your onStop() code.
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    protected void onPause() {
        super.onPause();
        myWebView.loadUrl("about:blank");
    }

    //COverride the back button
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        if (keyCode== KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {
            //RevMob Full Screen Ad
            displayInterstitial();
        }
        // return true;
        return super.onKeyDown(keyCode, event);
    }

    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {
        myWebView.loadUrl("about:blank");
        if(interstitial != null && randomBooleanSelector() ) {
            if (interstitial.isLoaded()) {
                interstitial.show();
            }
        }else{
            returnToParent();
        }
    }


    public boolean randomBooleanSelector(){

        int probability = (int)(Math.random() * (3));
        Log.i(CollectionActivity.TAG,"probability is : " + probability);
        if(probability > 0)
            return true;
        else
            return false;
    }



    @Override
    public void onInitializationFailure(Provider provider,
                                        YouTubeInitializationResult result) {
        Toast.makeText(getApplicationContext(),
                "onInitializationFailure()",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onInitializationSuccess(Provider provider, YouTubePlayer player,
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
