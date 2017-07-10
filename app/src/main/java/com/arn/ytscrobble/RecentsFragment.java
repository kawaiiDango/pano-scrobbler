package com.arn.ytscrobble;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.Spinner;

/**
 * Created by arn on 09/07/2017.
 */

public class RecentsFragment extends Fragment {

//    final String dummy[] = {"ayy", "lmao", "wtf","ayy", "lmao", "wtf","ayy", "lmao", "wtf","ayy", "lmao", "wtf","ayy", "lmao", "wtf"};
    static RecentsAdapter adapter = null;
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        return inflater.inflate(R.layout.content_main, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView recentsList = (ListView) getActivity(). findViewById(R.id.content_main);
        Spinner spinner = new Spinner(getContext());
        adapter = new RecentsAdapter(getContext(), R.layout.list_item, null);
        recentsList.setEmptyView(spinner);
        recentsList.setAdapter(adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }
}
