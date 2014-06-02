package com.thirtydaylabs.videoapp.app;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.thirtydaylabs.pksongs.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

/**
 * Created by HooMan on 12/08/13.
 */
public class listViewAdapter extends BaseAdapter{

    private Activity activity;
    private ArrayList<HashMap<String, String>> data;
    private static LayoutInflater inflater=null;


    public listViewAdapter(Activity a, ArrayList<HashMap<String, String>> d, Context context) {
        activity = a;
        data=d;
        inflater = (LayoutInflater)activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public int getCount() {
        return data.size();
    }

    public Object getItem(int position) {
        return position;
    }

    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

            final View vi = inflater.inflate(R.layout.video_card_row, null);
            assert vi != null;
            TextView title = (TextView)vi.findViewById(R.id.title); // title
            TextView sponsored = (TextView)vi.findViewById(R.id.sponsored); // title
            ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image
            TextView duration = (TextView)vi.findViewById(R.id.duration); // title
            TextView view_count = (TextView)vi.findViewById(R.id.views); // title

            final ImageView favoriteBT = (ImageView) vi.findViewById(R.id.favorite_image);

            final HashMap<String, String> video;
            video = data.get(position);

            // Setting all values in listview
//            sponsored.setVisibility(sponsored.GONE);
//            if(video.get(VideoListFragmentActivity.ITEM_TYPE)=="ad"){
//                sponsored.setVisibility(sponsored.VISIBLE);
//            }
            title.setText(video.get(VideoListFragmentActivity.KEY_TITLE));

            String commaSeparatedViewCount = NumberFormat.getNumberInstance(Locale.US).format(
                    Integer.parseInt(video.get(VideoListFragmentActivity.KEY_VIEW_COUNT)));

            view_count.setText( commaSeparatedViewCount + " Views");

            duration.setText(timeConvertor(video.get(VideoListFragmentActivity.KEY_DURATION)));

            Picasso.with(activity.getApplicationContext()).load(video.get(VideoListFragmentActivity.KEY_THUMB)).into(thumb_image);

            SharedPreferences prefs  = vi.getContext().getSharedPreferences(CollectionActivity.PREFS_NAME, vi.getContext().MODE_PRIVATE);

            JSONArray jsonArray = new JSONArray();

            try {
                jsonArray = new JSONArray(prefs.getString("favorites", "[]"));
            } catch (Exception e) {
                e.printStackTrace();
            }

            boolean favoriteExist = false;
            for (int i = 0; i < jsonArray.length(); i++){
                try {
                    if(jsonArray.getString(i).equals(video.get(VideoListFragmentActivity.KEY_ID))){
                        favoriteExist = true;
                        break;
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (favoriteExist){
                favoriteBT.setImageResource(R.drawable.ic_action_favorite);
            }




            favoriteBT.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {


                    SharedPreferences prefs  = vi.getContext().getSharedPreferences(CollectionActivity.PREFS_NAME, vi.getContext().MODE_PRIVATE);

                    JSONArray jsonArray = new JSONArray();

                    try {
                        jsonArray = new JSONArray(prefs.getString("favorites", "[]"));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    boolean favoriteExist = false;
                    int favoriteExistIndex = 0;
                    for (int i = 0; i < jsonArray.length(); i++){
                        try {
                            if(jsonArray.getString(i).equals(video.get(VideoListFragmentActivity.KEY_ID))){
                                favoriteExist = true;
                                favoriteExistIndex = i;
                                break;
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }

                    if (favoriteExist){
                        favoriteBT.setImageResource(R.drawable.ic_action_favorite_holo);
                        //remove the id from the favorite

                       jsonArray =  RemoveJSONArray(jsonArray, favoriteExistIndex);
                    }else{
                        favoriteBT.setImageResource(R.drawable.ic_action_favorite);
                        jsonArray.put(video.get(VideoListFragmentActivity.KEY_ID));
                    }

                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("favorites", jsonArray.toString());
                    editor.commit();



                }
            });

            return vi;
    }


    public static JSONArray RemoveJSONArray( JSONArray jarray,int pos) {

        JSONArray Njarray=new JSONArray();
        try{
            for(int i=0;i<jarray.length();i++){
                if(i!=pos)
                    Njarray.put(jarray.get(i));
            }
        }catch (Exception e){e.printStackTrace();}
        return Njarray;

    }



    private String timeConvertor (String interval){

        String hour;
        String minute;
        String second;
        DecimalFormat formatter = new DecimalFormat("#00");
        int intervalInt = Integer.parseInt(interval);
        hour = String.valueOf(intervalInt/3600);
        minute = String.valueOf(formatter.format((intervalInt-(Integer.parseInt(hour) * 3600))/60));
        second = String.valueOf(formatter.format(intervalInt-(Integer.parseInt(hour) * 3600)-(Integer.parseInt(minute) * 60)));

        String timeString="";
        if(!hour.equals("0")){
            timeString = hour + ":";
        }

        return timeString + minute + ":"+ second;

    }

}


