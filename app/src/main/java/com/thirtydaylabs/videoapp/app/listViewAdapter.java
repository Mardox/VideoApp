package com.thirtydaylabs.videoapp.app;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;
import com.thirtydaylabs.pksongs.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

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

        View vi = inflater.inflate(R.layout.video_card_row, null);
        assert vi != null;
        TextView title = (TextView)vi.findViewById(R.id.title); // title
        ImageView thumb_image=(ImageView)vi.findViewById(R.id.list_image); // thumb image
        TextView sponsored = (TextView)vi.findViewById(R.id.sponsored); // title
        TextView duration = (TextView)vi.findViewById(R.id.duration); // title

        HashMap<String, String> video;
        video = data.get(position);

        // Setting all values in listview
        //  sponsored.setVisibility(sponsored.GONE);
        //  if(video.get(VideoListFragmentActivity.ITEM_TYPE)=="ad"){
        //          sponsored.setVisibility(sponsored.VISIBLE);
        //  }
        title.setText(video.get(VideoListFragmentActivity.KEY_TITLE));

//            duration.setText(timeConvertor(video.get(VideoListFragmentActivity.KEY_DURATION)));

        Picasso.with(activity.getApplicationContext()).load(video.get(VideoListFragmentActivity.KEY_THUMB)).into(thumb_image);
        return vi;
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


