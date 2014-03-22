package com.thirtydaylabs.videoapp.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import com.thirtydaylabs.mechanicalengineering.R;
import com.thirtydaylabs.videoapp.utilities.Notification;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by HooMan on 1/03/14.
 */
public class DailyVideoService extends BroadcastReceiver{


    String packageName;
    Context contextVariable;

    @Override
    public void onReceive(Context context, Intent intent) {

        ApplicationInfo packageInfo = context.getApplicationContext().getApplicationInfo();
        packageName = packageInfo.packageName;
        contextVariable = context;
        if(!context.getString(R.string.daily_pick_query).equals(null)){
            getVideo.start();
        }

    }



    Thread getVideo = new Thread(new Runnable() {

        @Override
        public void run() {

            String output = null;
            int random;
            try {

                //External daily video source
                //URI url = new URI("http://30daylabs.com/cloud/api/video?format=json&package_name="+ packageName);
                //String vidID = responseJSON.getString("video_id");
                //String title = responseJSON.getString("title");
                //String subtitle = responseJSON.getString("subtitle");
                //String externalIcon = responseJSON.getString("thumbnail");

                //Daily video pick from youtube
//                Log.i("Daily","Thread  triggered");
//                int offset = (int)(Math.random() * (15 + 1));
                //String query = "https://gdata.youtube.com/feeds/api/videos?q="+ contextVariable.getString(R.string.daily_pick_query)+"&max-results=50&v=2&alt=jsonc&safeSearch=strict&orderby=relevance&duration=short&start-index="+offset;
                String query = "https://gdata.youtube.com/feeds/api/"+ contextVariable.getString(R.string.daily_pick_query) +"?v=2&alt=jsonc";
                query = query.replace(" ","%20");
                URI url = new URI(query);


                DefaultHttpClient httpClient = new DefaultHttpClient();
                HttpGet httpGet = new HttpGet(url);

                HttpResponse httpResponse = httpClient.execute(httpGet);
                HttpEntity httpEntity = httpResponse.getEntity();
                output = EntityUtils.toString(httpEntity);
//                Log.i("Daily", url.toString());
//                Log.i("Daily", output);

                // Create a HashMap which stores Strings as the keys and values
                Map<String,Object> push = new HashMap<String,Object>();


                JSONObject responseJSON = new JSONObject(output);

                //get JSONArray contained within the JSONObject retrieved - "results"
                JSONObject data = responseJSON.getJSONObject("data");
                JSONArray items = data.getJSONArray("items");

                //random number
                random = (int)(Math.random() * (items.length()));
//                Log.i("Daily random",Integer.toString(random));
//                Log.i("Daily array length",Integer.toString(items.length()));
                //each item is a JSONObject
                JSONObject tempitem =items.getJSONObject(random);
                JSONObject item = tempitem.getJSONObject("video");


                //item = items.getJSONObject(random);

                String[] notification_titles = contextVariable.getResources().getStringArray(R.array.notification_titles);
                random = (int)(Math.random() * (notification_titles.length));
                Log.i("Daily random title count",Integer.toString(notification_titles.length));
                Log.i("Daily random title",Integer.toString(random));
                String title = notification_titles[random];

                String vidID = item.getString("id");
                String subtitle = item.getString("title");
                String externalIcon = item.getJSONObject("thumbnail").getString("hqDefault");

                // Adding some values to the HashMap
                push.put("title", title);
                push.put("subtitle", subtitle);
                push.put("externalIcon", externalIcon);


                SharedPreferences settings = contextVariable.getSharedPreferences(CollectionActivity.PREFS_NAME, Context.MODE_MULTI_PROCESS );
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("votd", vidID);
                editor.commit();

//                Log.i("Daily - new ID", vidID);
                if(!settings.getBoolean("noDailyAlert",false)) {
                    Notification.sendNotification(contextVariable, push);
                }


            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (URISyntaxException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            // Execute HTTP Post Request
        }


    });



}
