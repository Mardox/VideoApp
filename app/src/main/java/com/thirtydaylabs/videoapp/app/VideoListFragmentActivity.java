package com.thirtydaylabs.videoapp.app;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ProgressBar;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.MapBuilder;
import com.thirtydaylabs.pksongs.R;
import com.thirtydaylabs.videoapp.utilities.EndlessScrollListener;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by HooMan on 5/12/13.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class VideoListFragmentActivity extends Fragment{

    public static final String ARG_OBJECT = "object";
    public static final String QUERY_OBJECT = "query";

    static final String ITEM_TYPE = "type";
    static final String KEY_ID = "id";
    static final String KEY_TITLE = "title";
    static final String KEY_THUMB = "thumbnail";
    static final String KEY_DURATION = "duration";
    static final String KEY_VIEW_COUNT = "viewCount";

    GridView videoList;
    ProgressBar prgLoading;

    ArrayList<HashMap<String, String>> itemsList = new ArrayList<HashMap<String,String>>();

    ListAdapter adapter;

    final Context context = getActivity();
    View rootView;

    int current_page;
    String search_query;
    String[] api_query;
    String[] query_duration;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_collection_object, container, false);
        Bundle args = getArguments();

        current_page = args.getInt(CollectionActivity.ARG_OBJECT);

        search_query = args.getString(CollectionActivity.QUERY_OBJECT);
        api_query = getResources().getStringArray(R.array.queries);
        query_duration = getResources().getStringArray(R.array.query_duration);

        adapter= new listViewAdapter(getActivity(), itemsList, getActivity());
        prgLoading = (ProgressBar) rootView.findViewById(R.id.prgLoading);
        videoList = (GridView) rootView.findViewById(R.id.list);
        videoList.setAdapter(adapter);

        //Action bar
        setHasOptionsMenu(true);

        //Endless scroll functionality
        // Attach the listener to the AdapterView onCreate
        videoList.setOnScrollListener(new EndlessScrollListener() {
            @Override
            public void onLoadMore(int page, int totalItemsCount) {
                //Preserve the list view's location
                int index = videoList.getFirstVisiblePosition();
                View v = videoList.getChildAt(0);
                int top = (v == null) ? 0 : v.getTop();
                //Call to append new data to the list
                Log.i(CollectionActivity.TAG,"Scroll been triggered");
                searchVideos(page, true);
                //scroll back to the listvie's location
                videoList.setVerticalScrollbarPosition(index);// setSelectionFromTop(index, top);
            }
        });

        searchVideos(0, true);

        return rootView;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_refresh:
                searchVideos(0, false);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    String query ="";
    String orderby ="";

    public void searchVideos(int page, boolean append){

        //Check if the internet connection is available
        if(!isOnline()){
            networkErrorDialog();
            return;
        }


        try{

            int offset = page * 50;
            //encode in case user has included symbols such as spaces etc
            String searchDuration = (query_duration[current_page].length()>0)? "&duration="
                    + query_duration[current_page] :"";
            String startIndex = (offset > 0)? "&start-index=" + Integer.toString(offset):"";
            String genre = (getString(R.string.genre).length()>0)? "&genre=" +getString(R.string.genre) :"";

            if(search_query!=null && (current_page == query_duration.length-1)){
                //Setup the search parameters
                if(offset == 0){

                    // Send data to Analytics
                    EasyTracker easyTracker = EasyTracker.getInstance(context);

                    easyTracker.send(MapBuilder
                                    .createEvent("user_action",     // Event category (required)
                                            "video_search",  // Event action (required)
                                            search_query,   // Event label
                                            null)            // Event value
                                    .build()
                    );

                    //Send the search to the backend
                    postData.start();

                }
                String compiled_search_query = search_query;
                query = "videos?q="+compiled_search_query.trim()+"&";
            }else if(api_query[current_page].substring(0,5).equals("users")){
                query = api_query[current_page]+"?";
                orderby = "&orderby=published";
            }else if(api_query[current_page].substring(0,5).equals("video")){
                query = api_query[current_page]+"&" ;
            }else if(api_query[current_page].substring(0,5).equals("playl")){
                query = api_query[current_page]+"?" ;
            }else if(api_query[current_page].substring(0,5).equals("daily")){
                query = api_query[current_page].substring(5)+"&";
                orderby = "&orderby=viewCount";
                int randomStartIndex = (int)(Math.random() * (100 + 1));
                startIndex = "&start-index=" + Integer.toString(offset + randomStartIndex);
            }

            query = query.replace(" ","%20");

            if(!append){
                itemsList.clear();
                ArrayList<String> orderbyArray = new ArrayList<String>();
                orderbyArray.add("relevance");
                orderbyArray.add("published");
                orderbyArray.add("viewCount");
                int random = (int)(Math.random() * (2 + 1));
                orderby = "&orderby=" + orderbyArray.get(random);
            }

            //append encoded user search term to search URL
            String searchURL = "https://gdata.youtube.com/feeds/api/"
                    +query+"max-results=50&v=2&alt=jsonc&safeSearch=strict"
                    +startIndex+searchDuration+orderby+genre;
            //instantiate and execute AsyncTask
            Log.i(CollectionActivity.TAG,searchURL);
            if(!prgLoading.isShown()){
                prgLoading.setVisibility(View.VISIBLE);
            }


            new GetVideos().execute(searchURL);

        }
        catch(Exception e){
            Log.e("myApp", "Whoops - something went wrong getting the videos!"+ e.toString());
            e.printStackTrace();
        }
    }

    private class GetVideos extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... apiURL) {
            //start building result which will be json string
            StringBuilder apiFeedBuilder = new StringBuilder();
            //should only be one URL, receives array
            for (String searchURL : apiURL) {
                HttpClient apiClient = new DefaultHttpClient();
                try {
                    //pass search URL string to fetch
                    HttpGet apiGet = new HttpGet(searchURL);
                    //execute request
                    HttpResponse apiResponse = apiClient.execute(apiGet);
                    //check status, only proceed if ok
                    StatusLine searchStatus = apiResponse.getStatusLine();
                    //Log.e("myApp", "I am here ");
                    if (searchStatus.getStatusCode() == 200) {
                        //get the response
                        HttpEntity apiEntity = apiResponse.getEntity();
                        InputStream entityContent = apiEntity.getContent();
                        //process the results
                        InputStreamReader readerInput = new InputStreamReader(entityContent);
                        BufferedReader tweetReader = new BufferedReader(readerInput);
                        String lineIn;
                        while ((lineIn = tweetReader.readLine()) != null) {
                            apiFeedBuilder.append(lineIn);
                        }
                    }
                    else
                        Log.e("myApp","Whoops - something went wrong with status code!"
                                + searchStatus.getStatusCode());
                }
                catch(Exception e){
                    Log.e("myApp", "Whoops - something went wrong with httpObject!");
                    // e.printStackTrace();
                }
            }
            //return result string
            return apiFeedBuilder.toString();
        }


        protected void onPostExecute(String result) {
            //start preparing result string for display
            if(isAdded()) {
                try {
                    //get JSONObject from result
                    JSONObject resultObject = new JSONObject(result);
                    //get JSONArray contained within the JSONObject retrieved - "results"
                    JSONObject data = resultObject.getJSONObject("data");
                    JSONArray items = data.getJSONArray("items");


                    //loop through each item in the tweet array
                    for (int t = 0; t < items.length(); t++) {

                        HashMap<String, String> map = new HashMap<String, String>();
                        //each item is a JSONObject
                        JSONObject item;

                        if (query.substring(0, 5).equals("playl")) {
                            JSONObject tempitem = items.getJSONObject(t);
                            item = tempitem.getJSONObject("video");
                        } else {
                            item = items.getJSONObject(t);
                        }

                        //get the username and text content for each tweet
                        if(item.has("viewCount")) {

                            String id = item.getString("id");
                            String title = item.getString("title");
                            String thumbnail = "http://img.youtube.com/vi/" + id + "/mqdefault.jpg";
                            String duration = item.getString("duration");
                            String views = item.getString("viewCount");

                            // adding each child node to HashMap key =&gt; value
                            map.put(ITEM_TYPE, "video");
                            map.put(KEY_ID, id);
                            map.put(KEY_TITLE, title);
                            map.put(KEY_THUMB, thumbnail);
                            map.put(KEY_DURATION, duration);
                            map.put(KEY_VIEW_COUNT, views);

                            itemsList.add(map);
                        }

                    }

                } catch (Exception e) {
                    Log.e(CollectionActivity.TAG, "Whoops:"+current_page + e.toString());
                    noResultErrorDialog();
                    e.printStackTrace();
                }

                //inflating the list view
                //check result exists
                if (!itemsList.isEmpty()) {

                    prgLoading.setVisibility(View.GONE);
                    videoList.invalidateViews();
                    videoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        HashMap<String, String> item;

                        @Override
                        public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
                            item = itemsList.get(position);
                            String item_type = item.get("type");
                            if (item_type.equals("video")) {
                                String video_id = item.get("id");
                                String video_title = item.get("title");
                                Intent youtubeActivity = new Intent(getActivity(), PlayerActivity.class);
                                youtubeActivity.putExtra("id", video_id);
                                youtubeActivity.putExtra("title", video_title);
                                startActivity(youtubeActivity);
                            }
                        }

                    });

                } else {
                    prgLoading.setVisibility(View.GONE);
                }
            }

        }

    }


    /**
     * Check if Internet connectuion is available
     */

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        if (netInfo != null && netInfo.isConnectedOrConnecting()) {
            return true;
        }
        return false;
    }



    /**
     * Network connection error dialog
     */
    private void networkErrorDialog() {

        final Context context = getActivity();
        //Create the upgrade dialog
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.error_dialog_title))
                .setMessage(R.string.no_internet_message)
                .setPositiveButton(R.string.retry_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // reset the request
                        searchVideos(0, true);
                    }
                })
                .setIcon(R.drawable.ic_action_dark_error)
                .show();
    }



    /**
     * Network connection error dialog
     */
    private void noResultErrorDialog() {

        final Context context = getActivity();
        //Create the upgrade dialog
        new AlertDialog.Builder(context)
                .setTitle(getString(R.string.error_dialog_title))
                .setMessage(R.string.no_search_results)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // reset the request

                    }
                })
                .setIcon(R.drawable.ic_action_dark_error)
                .show();
    }





    Thread postData = new Thread(new Runnable() {

        @Override
        public void run() {
            HttpClient httpclient = new DefaultHttpClient();
            HttpPost httppost = new HttpPost("http://30daylabs.com/cloud/api/search");
            // Create a new HttpClient and Post Header
            // Add your data
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            nameValuePairs.add(new BasicNameValuePair("package_name", CollectionActivity.PACKAGE_NAME));
            nameValuePairs.add(new BasicNameValuePair("search_query", search_query));
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
            // Execute HTTP Post Request
        }


    });



}
