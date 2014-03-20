package com.thirtydaylabs.videoapp.app;


import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SearchView;

import com.android.vending.billing.IInAppBillingService;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.thirtydaylabs.pksongs.R;
import com.thirtydaylabs.videoapp.utilities.AlarmController;
import com.thirtydaylabs.videoapp.utilities.BaseBootReceiver;
import com.thirtydaylabs.videoapp.utilities.MenuFunctions;
import com.thirtydaylabs.videoapp.utilities.ZoomOutPageTransformer;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by HooMan on 5/12/13.
 */
public class CollectionActivity extends FragmentActivity {

    /**
     * Tag used on log messages.
     */
    public static final String TAG = "myApp";

    //Neccessary non-cloud messaging variables

    CollectionPagerAdapter mCollectionPagerAdapter;

    public static String PACKAGE_NAME;

    final Context context = this;
    ViewPager mViewPager;
    public static final String PREFS_NAME  = "PrefsFile";
    Dialog dialog;
    Intent intent = getIntent();
    static String query;
    /**
     * Push notification variables
     */
    public static final String PROPERTY_REG_ID = "registration_id";


    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "935888920614";

    GoogleCloudMessaging gcm;
    String regid;

    //Admob
    private InterstitialAd interstitial;
    private AdView adView;
    private RelativeLayout.LayoutParams rLParams;
    private  RelativeLayout rLayout;

    //In App Purchase
    boolean premium_status;
    private final static int DAYS_UNTIL_UPGRADE_PROMPT = 2;
    private final static int LAUNCHES_UNTIL_UPGRADE_PROMPT = 2;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_collection);

        PACKAGE_NAME = getApplicationContext().getPackageName();

        //handle search intent
        handleIntent(getIntent());

        //setting the adapter and the list view arraylist
        // Create an adapter that when requested, will return a fragment representing an object in
        // the collection.
        //
        // ViewPager and its adapters use support library fragments, so we must use
        // getSupportFragmentManager.
        mCollectionPagerAdapter = new CollectionPagerAdapter(getSupportFragmentManager(), context);

        // Set up action bar.

        // Specify that the Home button should show an "Up" caret, indicating that touching the
        // button will take the user one step up in the application's hierarchy.
        //actionBar.setDisplayHomeAsUpEnabled(true);


        // Set up the ViewPager, attaching the adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mCollectionPagerAdapter);
        mViewPager.setPageTransformer(true, new ZoomOutPageTransformer());

        mViewPager.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(view.getId());
            }
        });


        // Restore preferences
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS);
        boolean overlay_shown = settings.getBoolean("helpOverlay", false);
        if(!overlay_shown){
            //Call the set overlay, you can put the logic of checking if overlay is already called with a simple sharedpreference
            showOverLay();
        }

        //Initiate the admob banner
        adMobBannerInitiate();
        adMobInterstitialInitiate();


        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            regid = getRegistrationId(context);

            if (regid.isEmpty()) {
                registerInBackground();
            }

            Log.e("reg_id", regid);
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }

        //Start the daily video alarm - reboot persistant
        ComponentName receiver = new ComponentName(context, BaseBootReceiver.class);
        PackageManager pm = context.getPackageManager();


        //set daily pick alaram
        AlarmController.dailyVideoAlarm(context);


        //App Upgrade and rating Dialog
        appOfferDialog();


        //Bind the in-app service
        bindService(new Intent("com.android.vending.billing.InAppBillingService.BIND"),
                mServiceConn, Context.BIND_AUTO_CREATE);

    }



    /**
     * In-App purchase service
     */
    IInAppBillingService mService;

    ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name,
                                       IBinder service) {
            mService = IInAppBillingService.Stub.asInterface(service);
            try {
                int response = mService.isBillingSupported(3,getPackageName(),"inapp");
                if(response == 0) {
                    //has billing
                    checkPurchases();
                }else{
                    // no billing V3
                    MenuItem item = mMenu.findItem(R.id.action_upgrade);
                    item.setVisible(false);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }

        }
    };



    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);


        handleIntent(intent);
    }



    //handle search intent
    private void handleIntent(Intent intent) {
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            query = intent.getStringExtra(SearchManager.QUERY);
            mCollectionPagerAdapter.notifyDataSetChanged();
            mViewPager.setAdapter(mCollectionPagerAdapter);

        }
    }


    //Activity life cycles
    @Override
    protected void onStart() {
        super.onStart();
        EasyTracker.getInstance(this).activityStart(this);  // Add this method.
    }

    @Override
    public void onStop() {
        super.onStop();
        if (dialog != null) {
            dialog.dismiss();
            dialog = null;
        }
        EasyTracker.getInstance(this).activityStop(this);  // Add this method.
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {

        // Destroy the AdView.
        if (adView != null) {
            adView.destroy();
        }

        super.onDestroy();

        if (mService != null) {
            unbindService(mServiceConn);
        }
    }



    private MenuItem searchMenuItem; //search menu after submit collapse feature

    public MenuItem getSearchMenuItem() {
        return searchMenuItem;
    }

    /**
     * Create the actionbar menu
     */
    Menu mMenu;
    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu items for use in the action bar
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);
        mMenu = menu;
        //Get the search view to collape it later after submit
        searchMenuItem = menu.findItem(R.id.search);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager =
                (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView =
                (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(
                searchManager.getSearchableInfo(getComponentName()));

        //listener to close the search view after submit
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                MenuItem searchMenuItem = getSearchMenuItem();
                if (searchMenuItem != null) {
                    searchMenuItem.collapseActionView();
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                return true;
            }
        });


        if(premium_status) {
            MenuItem item = menu.findItem(R.id.action_upgrade);
            item.setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }




    /**
     * on actionbar item listener
     * @param item menuitem
     * @return boolean
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_share:
                MenuFunctions.openShare(context);
                return true;
            case R.id.action_daily_video:
                MenuFunctions.openDailyVideo(context);
                return true;
            case R.id.action_rate:
                MenuFunctions.openRate(context);
                return true;
            case R.id.action_upgrade:
//                MenuFunctions.upgrade(context);
                purchase();
                return true;
//            case R.id.action_about:
//                MenuFunctions.openAbout(context);
//                return true;
            case R.id.action_settings:
                MenuFunctions.settings(context);
                return true;
            case R.id.action_mores:
                MenuFunctions.openMore(context);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }



    /**
     * Show the overlay guide
     */
    private void showOverLay(){

        dialog = new Dialog(context, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setContentView(R.layout.help_overlay);
        LinearLayout layout = (LinearLayout) dialog.findViewById(R.id.overlayLayout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                dialog.dismiss();
                // We need an Editor object to make preference changes.
                // All objects are from android.context.Context
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
                SharedPreferences.Editor editor = settings.edit();
                editor.putBoolean("helpOverlay", true);

                // Commit the edits!
                editor.commit();
            }
        });

        dialog.show();

    }


    /**
     * Override the back button
     */
    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event)
    {
        if (keyCode== KeyEvent.KEYCODE_BACK && event.getRepeatCount() == 0)
        {

            if(!getString(R.string.admob_id_interstitials).equals("")){
                //RevMob Full Screen Ad
                displayInterstitial();
            }
            // return true;
        }
        return super.onKeyDown(keyCode, event);
    }



    // Invoke displayInterstitial() when you are ready to display an interstitial.
    public void displayInterstitial() {

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS);
        premium_status = settings.getBoolean("premiumStatus", false);
        if (interstitial.isLoaded() && !premium_status) {
            interstitial.show();
        }

    }



    /**
    * Reloading the ad view on screen rotation
    */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        adMobBannerInitiate();


    }



    /**
     * Initiate the adMob Banner
     */
    private void adMobBannerInitiate(){

        rLayout = (RelativeLayout) findViewById(R.id.main_layout);

        rLParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rLParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 1);

        rLayout = (RelativeLayout) findViewById(R.id.main_layout);

        //Remove the current banner
        AdView oldAdView = (AdView) findViewById(1);

        // Add the AdView to the view hierarchy. The view will have no size
        // until the ad is loaded.

        // Destroy the old AdView.
        if (oldAdView != null) {
            rLayout.removeView(oldAdView);
            oldAdView.destroy();
        }


        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_MULTI_PROCESS);
        premium_status = settings.getBoolean("premiumStatus", false);

        if(!premium_status && !getString(R.string.admob_id_home).equals("")) {
            adView = new AdView(this);
            adView.setAdSize(AdSize.SMART_BANNER);
            adView.setAdUnitId(getString(R.string.admob_id_home));
            adView.setId(1);

            rLayout.addView(adView, rLParams);

            // Create an ad request. Check logcat output for the hashed device ID to
            // get test ads on a physical device.
            AdRequest adRequest = new AdRequest.Builder().build();

            // Start loading the ad in the background.
            adView.loadAd(adRequest);
        }
    }



    /**
     *  Initiate the interstitial adMob
     */
    private void adMobInterstitialInitiate(){

        if(!getString(R.string.admob_id_interstitials).equals("")){
            //AdMob Full Screen
            //Create the interstitial
            interstitial = new InterstitialAd(this);
            interstitial.setAdUnitId(getString(R.string.admob_id_interstitials));
            // Create ad request
            AdRequest interAdRequest = new AdRequest.Builder().build();
            // Begin loading your interstitial
            interstitial.loadAd(interAdRequest);

            final Activity act = this;
            interstitial.setAdListener(new AdListener() {
                @Override
                public void onAdClosed() {
                    // Save app state before going to the ad overlay.
                    act.finish();
                }
            });
        }

    }



    /**
     * A {@link android.support.v4.app.FragmentStatePagerAdapter} that returns a fragment
     * representing an object in the collection.
     */
    public static class CollectionPagerAdapter extends FragmentStatePagerAdapter {

        private Context _context;
        String[] menuArray;

        public CollectionPagerAdapter(FragmentManager fm, Context c) {
            super(fm);
            _context = c;
            menuArray = _context.getResources().getStringArray(R.array.tab_titles);

        }


        @Override
        public Fragment getItem(int i) {
            Fragment fragment = new VideoListFragmentActivity();
            Bundle args = new Bundle();
            //ignore the search page if there is no query
            if(query == null){
                args.putInt(VideoListFragmentActivity.ARG_OBJECT, i+1 ); // Our object is just an integer :-P
            }else{
                args.putInt(VideoListFragmentActivity.ARG_OBJECT, i ); // Our object is just an integer :-P
            }
            args.putString(VideoListFragmentActivity.QUERY_OBJECT,query);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            // For this contrived example, we have a 100-object collection.
            int numberOfPages = menuArray.length;
            //ignore the search page if there is no query
            if(query == null){
                numberOfPages = numberOfPages - 1;
            }
            return numberOfPages;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            //check if there is a search query
            if(query == null){
                position = position + 1;
            }
            return menuArray[position];
        }

    }



    /**
     * Purchase Item
     */
    private void purchase(){


        try {
            int response = mService.isBillingSupported(3,getPackageName(),"inapp");
            if(response == 0) {
                //has billing
                String SKU = getString(R.string.premium_product_id);
                try {
                    Bundle buyIntentBundle = mService.getBuyIntent(3, context.getPackageName(),
                            SKU, "inapp", null);
                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    startIntentSenderForResult(pendingIntent.getIntentSender(),
                            1001, new Intent(), Integer.valueOf(0), Integer.valueOf(0),
                            Integer.valueOf(0));
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                } catch (Exception e){
                    purchaseErrorDialog();
                }
            }else{
                // no billing V3
                MenuItem item = mMenu.findItem(R.id.action_upgrade);
                item.setVisible(false);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e){
            purchaseErrorDialog();
        }


    }



    /**
     * on in-app purchase listener
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1001) {
            int responseCode = data.getIntExtra("RESPONSE_CODE", 0);
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");
            String dataSignature = data.getStringExtra("INAPP_DATA_SIGNATURE");

            if (resultCode == RESULT_OK) {
                try {

                    JSONObject jo = new JSONObject(purchaseData);
                    String sku = jo.getString("productId");
                    Log.i(TAG,"You have bought the " + sku + ". Excellent choice,adventurer!");

                    //Upgrade the app if the sku matched the upgrade package
                    if(sku.equals(getString(R.string.premium_product_id))) {
                        //Set the premium flag
                        SharedPreferences settings = getSharedPreferences(CollectionActivity.PREFS_NAME, MODE_MULTI_PROCESS);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putBoolean("premiumStatus", true);
                        editor.commit();

                        //Remove the upgrade menu
                        MenuItem item = mMenu.findItem(R.id.action_upgrade);
                        item.setVisible(false);

                        //Remove the ads
                        adMobBannerInitiate();

                        //Show complete dialog
                        purchaseCompleteDialog();
                    }

                }
                catch (JSONException e) {
                    Log.i(TAG,"Failed to parse purchase data.");
                    purchaseErrorDialog();
                    e.printStackTrace();
                }
                catch (Exception e){
                    purchaseErrorDialog();
                }
            }
        }
    }




    private void purchaseCompleteDialog(){

            //Create the upgrade dialog
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.upgrade_complete_dialog_title))
                    .setMessage(getString(R.string.upgrade_complete_dialog_body_text))
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            // continue with upgrade

                        }
                    })
                    .setIcon(R.drawable.ic_action_dark_important)
                    .show();

    }


    private void purchaseErrorDialog(){

        //Create the upgrade dialog
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade_error_dialog_title))
                .setMessage(getString(R.string.upgrade_error_dialog_body_text))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with upgrade

                    }
                })
                .setIcon(R.drawable.ic_action_dark_important)
                .show();

    }





    /**
     * check purchased items
     */
    private void checkPurchases() {

        Bundle ownedItems = null;
        try {
            ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        int response = ownedItems.getInt("RESPONSE_CODE");
        if (response == 0) {
            ArrayList<String> ownedSkus =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
            ArrayList<String>  purchaseDataList =
                    ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
            ArrayList<String>  signatureList =
                    ownedItems.getStringArrayList("INAPP_DATA_SIGNATURE");
            String continuationToken =
                    ownedItems.getString("INAPP_CONTINUATION_TOKEN");

            for (int i = 0; i < purchaseDataList.size(); ++i) {
                String purchaseData = purchaseDataList.get(i);
//                String signature = signatureList.get(i);
                String sku = ownedSkus.get(i);
                Log.i(TAG,sku);
                Log.i(TAG,purchaseData);
                if(sku.equals("android.test.purchased")){
                    try {

                        //Consume the order
                        int cResponse = mService.consumePurchase(3, getPackageName(), "inapp:com.mardox.mathtricks:android.test.purchased");
                        Log.i(TAG,"consume response: "+cResponse);

                        if(cResponse==0) {
                            //reset the sharedprefrences
                            SharedPreferences settings = getSharedPreferences(CollectionActivity.PREFS_NAME, MODE_MULTI_PROCESS);
                            SharedPreferences.Editor editor = settings.edit();
                            editor.putBoolean("premiumStatus", false);
                            editor.commit();

                            //Enable the ads
                            adMobBannerInitiate();
                            adMobInterstitialInitiate();
                        }

                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
                // do something with this purchase information
                // e.g. display the updated list of products owned by user
            }

            // if continuationToken != null, call getPurchases again
            // and pass in the token to retrieve more items
        }

    }


    /**
     * appOfferDialog Dialog
     */
    public void appOfferDialog() {

        SharedPreferences prefs = getSharedPreferences(CollectionActivity.PREFS_NAME, MODE_MULTI_PROCESS);
        premium_status = prefs.getBoolean("premiumStatus", false);
        Boolean dont_show_rate_again  = prefs.getBoolean("dontshowrateagain", false);


        SharedPreferences.Editor editor = prefs.edit();

        // Increment launch counter
        long launch_count = prefs.getLong("launch_count", 0) + 1;
        editor.putLong("launch_count", launch_count);

        // Get date of first launch
        Long date_firstLaunch = prefs.getLong("date_firstlaunch", 0);
        if (date_firstLaunch == 0) {
            date_firstLaunch = System.currentTimeMillis();
            editor.putLong("date_firstlaunch", date_firstLaunch);
        }


        // Wait at least n days before opening
        if (launch_count >= LAUNCHES_UNTIL_UPGRADE_PROMPT ) {
            if (System.currentTimeMillis() >= date_firstLaunch +
                    (DAYS_UNTIL_UPGRADE_PROMPT * 24 * 60 * 60 * 1000)) {

                //generate a random number [1,2]
                int randomDay = 1 + (int)(Math.random()*3);
                if(!premium_status && randomDay == 1) {
                    //Upgrade offer
                    upgradeDialog();
                }else if(!dont_show_rate_again && randomDay == 2){
                    rateDialog(editor);
                }

            }
        }

        editor.commit();

    }



    private void upgradeDialog(){

        //Create the upgrade dialog
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.upgrade_offer_title))
                .setMessage(getString(R.string.upgrade_offer_text))
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with upgrade
                        purchase();
                    }
                })
                .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })
                .setIcon(R.drawable.ic_action_dark_important)
                .show();

    }


    private void rateDialog(final SharedPreferences.Editor editor){

        final String APP_PNAME = getPackageName();

        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.rate_offer_title))
                .setItems(R.array.rating_response, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        switch (which) {
                            case 0:
                                //Rate Now
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + APP_PNAME)));
                            case 1:
                                //Rate Later
                            case 2:
                                //Never
                                if (editor != null) {
                                    editor.putBoolean("dontshowrateagain", true);
                                    editor.commit();
                                }
                        }

                    }

                })
                .setIcon(R.drawable.ic_action_dark_important)
                .show();

    }



    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }



    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }



    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }



    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    Log.i("reg_id",regid);
                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();
                    postRegID.start();
                    // For this demo: we don't need to send it because the device will send
                    // upstream messages to a server that echo back the message using the
                    // 'from' address in the message.

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
//                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);
    }



    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }



    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences(CollectionActivity.class.getSimpleName(),
                Context.MODE_PRIVATE);
    }



    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. Not needed for this demo since the device sends upstream messages
     * to a server that echoes back the message using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }



    Thread postRegID = new Thread( new Runnable() {

        @Override
        public void run() {

            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://30daylabs.com/cloud/gcm/device");

            //Determine the version code of the app
            int versionNumber = 0;
            try {
                PackageInfo pinfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                versionNumber = pinfo.versionCode;
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            // Create a new HttpClient and Post Header
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("package_name", context.getPackageName()));
            nameValuePairs.add(new BasicNameValuePair("reg_id", regid));
            nameValuePairs.add(new BasicNameValuePair("app_version", String.valueOf(versionNumber)));

            try {
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
                HttpResponse response = httpclient.execute(httppost);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    });



}
