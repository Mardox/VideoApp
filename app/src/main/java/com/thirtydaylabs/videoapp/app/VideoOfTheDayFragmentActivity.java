package com.thirtydaylabs.videoapp.app;

import android.annotation.TargetApi;
import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.thirtydaylabs.electronicstutorials.R;


/**
 * Created by HooMan on 6/03/14.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class VideoOfTheDayFragmentActivity extends Fragment {

    public static final String ARG_OBJECT = "object";
    public static final String QUERY_OBJECT = "query";

    View rootView;

    int current_page;
    String search_query;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.video_of_the_day_fragment, container, false);
        Bundle args = getArguments();
        current_page = args.getInt(ARG_OBJECT);
        search_query = args.getString(QUERY_OBJECT);
        //Action bar
        setHasOptionsMenu(true);
        return rootView;
    }


}
