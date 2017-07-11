package com.arn.ytscrobble;

import android.app.Activity;
import android.content.Context;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import android.os.Handler;

import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;

import de.umass.lastfm.ImageSize;
import de.umass.lastfm.PaginatedResult;
import de.umass.lastfm.Track;

/**
 * Created by arn on 10/07/2017.
 */

public class RecentsAdapter extends ArrayAdapter<Track> {

        private Context c;
        private int layoutResourceId;
        private static final Integer FILLED = 5;
//        private ArrayList<Track> tracks;

        public RecentsAdapter(Context c, int layoutResourceId) {
            super(c, layoutResourceId, new ArrayList<Track>());
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
                love.setBackgroundResource(R.drawable.btn_heart_enabled);
                love.setTag(R.id.recents_love, FILLED);
            } else {
                love.setBackgroundResource(R.drawable.btn_heart_disabled);
                love.setTag(R.id.recents_love, 0);
            }

            String imgUrl = t.getImageURL(ImageSize.LARGE);

            if (imgUrl != null && !imgUrl.equals(""))
                Picasso.with(getContext())
                        .load(imgUrl)
                        .fit()
                        .centerInside()
                        .placeholder(R.drawable.ic_lastfm)
                        .error(R.drawable.ic_placeholder_music)
                        .into((ImageView) convertView.findViewById(R.id.recents_album_art));
            return convertView;
        }

        void populate(PaginatedResult<Track> res){
            clear();
            for (Track t : res) {
                if (t != null) {
                    add(t);
                }
            }
//            new Scrobbler(c).execute(Stuff.GET_LOVED);
            notifyDataSetChanged();
        }

        void markLoved(PaginatedResult<Track> res){
            ArrayList<Track> loved = new ArrayList<>(10);
            for (Track t : res) {
                if (t != null)
                    loved.add(t);
            }
            for (int i=0, j=0; i<loved.size() && j < getCount(); ) {
                if (loved.get(i).getPlayedWhen().before(getItem(j).getPlayedWhen()))
                    j++;
                else if (loved.get(i).getPlayedWhen().after(getItem(j).getPlayedWhen()))
                    i++;
                else {
                    getItem(j).setLoved(true);
                    i++; j++;
                }
            }
            notifyDataSetChanged();
        }

        ImageButton.OnClickListener loveToggle = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View parentRow = (View) v.getParent();
                ListView listView = (ListView) parentRow.getParent();
                final int pos = listView.getPositionForView(parentRow);

                if (v.getTag(R.id.recents_love) == FILLED){
                    Stuff.log(getContext(), "FILLED");
                    new Scrobbler(c).execute(Stuff.UNLOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    v.setBackgroundResource(R.drawable.btn_heart_disabled);
                    v.setTag(R.id.recents_love, 0);
                } else {
                    Stuff.log(getContext(), "0");
                    new Scrobbler(c).execute(Stuff.LOVE,
                            getItem(pos).getArtist(), getItem(pos).getName());
                    v.setBackgroundResource(R.drawable.btn_heart_enabled);
                    v.setTag(R.id.recents_love, FILLED);
                }
            }
        };
    }