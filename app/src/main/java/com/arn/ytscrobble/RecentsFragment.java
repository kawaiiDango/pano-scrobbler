package com.arn.ytscrobble;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
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
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        final ListView recentsList = (ListView) getActivity().findViewById(R.id.recents_list);
        Spinner spinner = new Spinner(getActivity());
        recentsList.setEmptyView(spinner);
        adapter = new RecentsAdapter(getActivity(), R.layout.list_item);
        recentsList.setAdapter(adapter);

        LayoutInflater inflater = getActivity().getLayoutInflater();
        ViewGroup header = (ViewGroup)inflater.inflate(R.layout.recents_header, recentsList, false);
        recentsList.addHeaderView(header, null, false);

        SwipeRefreshLayout refresh = (SwipeRefreshLayout) getActivity().findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                adapter.loadURL();
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        // Inflate the menu; this adds items to the action bar if it is present.
        inflater.inflate(R.menu.menu_main, menu);
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        ((AppCompatActivity)getActivity()).getSupportActionBar().setDisplayHomeAsUpEnabled(hidden);
        if (!hidden) {
            ((CollapsingToolbarLayout) getActivity().findViewById(R.id.toolbar_layout))
                    .setTitle(getString(R.string.app_name));
        }

    }
}
