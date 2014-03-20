package com.thirtydaylabs.videoapp.videoplayer;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;

import com.thirtydaylabs.videoapp.app.listViewAdapter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;


/**
 * Created by HooMan on 24/10/13.
 */
public class YouTubeAPI {


    static final String KEY_ID = "id";
    static final String KEY_TITLE = "title";
    static final String KEY_THUMB = "thumbnail";

    ListView videoList;
    ArrayList<HashMap<String, String>> videosList = new ArrayList<HashMap<String,String>>();

    listViewAdapter adapter;

    public void YouTubeCall(String searchTerm ){

        try{
            //encode in case user has included symbols such as spaces etc
            String encodedSearch = URLEncoder.encode(searchTerm, "UTF-8");
            //append encoded user search term to search URL
            String searchURL = "https://gdata.youtube.com/feeds/api/videos?q="+encodedSearch+"&orderby=relevance&max-results=50&v=2&alt=jsonc";
            //instantiate and execute AsyncTask
            new GetVideos().execute(searchURL);


        }
        catch(Exception e){
            Log.e("myApp", "Whoops - something went wrong searchTwitter!");
            e.printStackTrace();
        }
    }


    private class GetVideos extends AsyncTask<String, Void, String> {
        /*
         * Carry out fetching task in background
         * - receives search URL via execute method
         */
        @Override
        protected String doInBackground(String... youtubeURL) {
            //start building result which will be json string
            StringBuilder youtubeFeedBuilder = new StringBuilder();
            //should only be one URL, receives array
            for (String searchURL : youtubeURL) {
                HttpClient tweetClient = new DefaultHttpClient();
                try {
                    //pass search URL string to fetch
                    HttpGet tweetGet = new HttpGet(searchURL);
                    //execute request
                    HttpResponse tweetResponse = tweetClient.execute(tweetGet);
                    //check status, only proceed if ok
                    StatusLine searchStatus = tweetResponse.getStatusLine();
                    //Log.e("myApp", "I am here ");
                    if (searchStatus.getStatusCode() == 200) {
                        //get the response
                        HttpEntity tweetEntity = tweetResponse.getEntity();
                        InputStream tweetContent = tweetEntity.getContent();
                        //process the results
                        InputStreamReader tweetInput = new InputStreamReader(tweetContent);
                        BufferedReader tweetReader = new BufferedReader(tweetInput);
                        String lineIn;
                        while ((lineIn = tweetReader.readLine()) != null) {
                            youtubeFeedBuilder.append(lineIn);
                        }
                    }
                    else
                        Log.e("myApp","Whoops - something went wrong !");
                }
                catch(Exception e){
                    Log.e("myApp", "Whoops - something went wrong!");
                    e.printStackTrace();
                }
            }
            //return result string
            return youtubeFeedBuilder.toString();
        }

        /*
         * Process result of search query
         * - this receives JSON string representing tweets with search term included
         */
        protected void onPostExecute(String result) {
            //start preparing result string for display

            try {
                //get JSONObject from result
                JSONObject resultObject = new JSONObject(result);
                //get JSONArray contained within the JSONObject retrieved - "results"
                JSONObject data = resultObject.getJSONObject("data");
                JSONArray items = data.getJSONArray("items");
                //loop through each item in the tweet array
                for (int t=0; t<items.length(); t++) {

                    HashMap<String, String> map = new HashMap<String, String>();
                    //each item is a JSONObject
                    JSONObject item = items.getJSONObject(t);
                    //get the username and text content for each tweet
                    String id = item.getString("id");
                    String title = item.getString("title");
                    String thumbnail = item.getJSONObject("thumbnail").getString("hqDefault");

                    // adding each child node to HashMap key =&gt; value
                    map.put(KEY_ID, id);
                    map.put(KEY_TITLE, title);
                    map.put(KEY_THUMB, thumbnail);

                    videosList.add(map);
                }
            }
            catch (Exception e) {
                Log.e("myApp", "Whoops - something went wrong!");
                e.printStackTrace();
            }

//            return videoList;
//            //check result exists
//            if(!videosList.isEmpty()){
//                videoList = (ListView) findViewById(R.id.list);
//                prgLoading.setVisibility(View.GONE);
//                adapter= new listViewAdapter(MainActivity.this, videosList);
//                videoList.setAdapter(adapter);
//                videoList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
//                    HashMap<String, String> video;
//
//                    @Override
//                    public void onItemClick(AdapterView<?> arg0, View arg1, int position,long arg3) {
//                        video = videosList.get(position);
//                        String id = video.get(MainActivity.KEY_ID);
//
//                        Intent youtubeActivity = new Intent(MainActivity.this, PlayerActivity.class);
//                        youtubeActivity.putExtra("id", id);
//                        startActivity(youtubeActivity);
//                    }
//
//
//                });
//
//            }else
//                Log.e("myApp", "No Results!");
        }

    }


}
