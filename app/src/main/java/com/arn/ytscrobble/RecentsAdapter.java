package com.arn.ytscrobble;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import android.os.Handler;

import java.util.Iterator;

import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

/**
 * Created by arn on 10/07/2017.
 */

public class RecentsAdapter extends ArrayAdapter<Track> {

        private Context c;
        private int layoutResourceId;
        private Track tracks[];

        public RecentsAdapter(Context c, int layoutResourceId, Track[] tracks) {
            super(c, layoutResourceId, tracks=new Track[0]);

            this.layoutResourceId = layoutResourceId;
            this.c = c;
            new Scrobbler(c).execute(Stuff.GET_RECENTS);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

        /*
         * The convertView argument is essentially a "ScrapView" as described is Lucas post
         * http://lucasr.org/2012/04/05/performance-tips-for-androids-listview/
         * It will have a non-null value when ListView is asking you recycle the row layout.
         * So, when convertView is not null, you should simply update its contents instead of inflating a new row layout.
         */
            if(convertView==null){
                // inflate the layout
                LayoutInflater inflater = ((Activity) c).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }

            // object item based on the position
            Track t = tracks[position];

            // get the TextView and then set the text (item name) and tag (item ID) values
            TextView title = (TextView) convertView.findViewById(R.id.recents_title),
                    subtitle = (TextView) convertView.findViewById(R.id.recents_subtitle);
            title.setText(t.getName());
            subtitle.setText(t.getArtist());
            return convertView;

        }

        public void populate(PaginatedResult<Track> res){
            Iterator<Track> it = res.iterator();
            int i = 0;
            while (it.hasNext()){
                Track t = it.next();
                tracks[i] = t;
                Stuff.log(getContext(), t.getName());
            }
        }
    }