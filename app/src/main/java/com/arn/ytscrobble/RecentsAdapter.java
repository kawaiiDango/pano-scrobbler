package com.arn.ytscrobble;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.widget.SwipeRefreshLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;

import de.umass.lastfm.ImageSize;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

/**
 * Created by arn on 10/07/2017.
 */

public class RecentsAdapter extends ArrayAdapter<Track> {

        private int layoutResourceId;
        private static final Integer FILLED = 5;
//        private ArrayList<Track> tracks;

        public RecentsAdapter(Context c, int layoutResourceId) {
            super(c, layoutResourceId, new ArrayList<Track>());
            this.layoutResourceId = layoutResourceId;
            loadURL();
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
                LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }

            // object item based on the position
            Track t = getItem(position);

            // get the TextView and then set the text (item name) and tag (item ID) values
            TextView title = (TextView) convertView.findViewById(R.id.recents_title),
                    subtitle = (TextView) convertView.findViewById(R.id.recents_subtitle);
            String np = t.isNowPlaying() ? "▶️" : "";
            title.setText(np + t.getName());
            subtitle.setText(t.getArtist());

            final ImageButton love = (ImageButton) convertView.findViewById(R.id.recents_love);
            love.setOnClickListener(loveToggle);

            if (t.isLoved()) {
                love.setImageResource(R.drawable.ic_line_heart_enabled);
                love.setTag(R.id.recents_love, FILLED);
            } else {
                love.setImageResource(R.drawable.ic_line_heart_disabled);
                love.setTag(R.id.recents_love, 0);
            }

            ImageView albumArt =  (ImageView)convertView.findViewById(R.id.recents_album_art);
            String imgUrl = t.getImageURL(ImageSize.LARGE);

            if (imgUrl != null && !imgUrl.equals("")) {
                albumArt.clearColorFilter();
                Picasso.with(getContext())
                        .load(imgUrl)
                        .fit()
                        .centerInside()
                        .placeholder(R.drawable.ic_lastfm)
                        .error(R.drawable.ic_placeholder_music)
                        .into(albumArt);
            } else {
                albumArt.setImageResource(R.drawable.ic_placeholder_music);
                albumArt.setColorFilter(Stuff.getMatColor(getContext(),"500"));
            }
            return convertView;
        }

        void loadURL(){
            new Scrobbler(getContext()).execute(Stuff.GET_RECENTS);
        }

        void populate(PaginatedResult<Track> res){
            SwipeRefreshLayout refresh = (SwipeRefreshLayout)((Activity) getContext()).findViewById(R.id.swiperefresh);
            refresh.setRefreshing(false);
            clear();
            for (Track t : res) {
                if (t != null) {
                    add(t);
                }
            }
            notifyDataSetChanged();
        }

        void markLoved(PaginatedResult<Track> res){
            ArrayList<Track> loved = new ArrayList<>(10);
            for (Track t : res) {
                if (t != null)
                    loved.add(t);
            }
            for (int i=0; i<loved.size(); i++) {
                for (int j=0; j < getCount(); j++)
                if (loved.get(i).getName().equals(getItem(j).getName()) &&
                        loved.get(i).getArtist().equals(getItem(j).getArtist())){
                    getItem(j).setLoved(true);
                }
            }
            notifyDataSetChanged();
        }

        ImageButton.OnClickListener loveToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ImageButton ib = (ImageButton)v;
                View parentRow = (View) v.getParent();
                ListView listView = (ListView) parentRow.getParent();
                final int pos = listView.getPositionForView(parentRow);


                if (v.getTag(R.id.recents_love) == FILLED){
                    new Scrobbler(getContext()).execute(Stuff.UNLOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    ib.setImageResource(R.drawable.ic_line_heart_disabled);
                    ib.setTag(R.id.recents_love, 0);
                } else {
                    new Scrobbler(getContext()).execute(Stuff.LOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    ib.setImageResource(R.drawable.ic_line_heart_enabled);
                    ib.setTag(R.id.recents_love, FILLED);
                }

            }
        };
    }