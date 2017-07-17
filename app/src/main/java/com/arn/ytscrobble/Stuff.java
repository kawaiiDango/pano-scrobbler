package com.arn.ytscrobble;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by arn on 13-03-2017.
 */

class Stuff {
    static final String NOW_PLAYING = "np", SCROBBLE = "scrobble", CHECKAUTH = "auth", GET_RECENTS = "recents";
    static final String LAST_KEY = Tokens.LAST_KEY, LAST_SECRET = Tokens.LAST_SECRET;
    static final String TAG = "ytscrobble";
    public static final String STATE_SCROBBLING = "now scrobbling...",
            STATE_PARSE_ERR = "did not scrobble",
            STATE_SCROBBLED = "scrobble submitted...",
            STATE_NETWORK_ERR = "network err while scrobbling";
    static final String LOVE = "loved", UNLOVE = "hated", GET_LOVED = "getloved", TRACK_HERO = "heroinfo";
    static final String seperators[] = { // in priority order
            "—",
            " ‎– ",
            "–",
            " - ",
            "-",
            "「",
            "『",
            "ー",

            "【",
            "〖",
            ":",
            " \"",
            " /"
    },
    unwantedSeperators[] = {
            "』",
            "」",
            "\"",
            "'",
            "】",
            "〗"
    };

    static void log(Context c, String s){
        Log.i(TAG,s);
//        toast(c, s);
    }

    static void toast(Context c, String s){
        try {
            Toast.makeText(c, s, Toast.LENGTH_SHORT).show();
        }catch (Exception e){
//            Log.i(TAG,"toastErr: "+e.getMessage());
        }
    }

    static void dumpBundle(Context c, Bundle bundle){
        if (bundle == null)
            Log.i(TAG, "Bundle: null");
        else {
            for (String key : bundle.keySet()) {
                Object value = bundle.get(key);
                Log.i(TAG, String.format("%s %s (%s)", key,
                        value.toString(), value.getClass().getName()));
            }
        }
    }

    static String[] sanitizeTitle(String titleContentOriginal){
        //New detection of trackinformation
        //remove (*) and/or [*] to remove unimportant data
        String titleContent = titleContentOriginal.replaceAll(" *\\([^)]*\\) *", " ")
                .replaceAll(" *\\[[^)]*\\] *", " ")

                //remove HD info
                .replaceAll("\\W* HD( \\W*)?", " ")
                .replaceAll("\\W* HQ( \\W*)?", " ");

        Pattern r = Pattern.compile("\\([^)]*(?:remix|mix|cover|version|edit|booty?leg)\\)");
        //get remix info
        Matcher remixInfo = r.matcher(titleContentOriginal);

        String musicInfo[] = null;
        for (String s:seperators){
            musicInfo = titleContent.split(s);
//            log(null ,"********"+s + "|"+musicInfo[0]);
            if (musicInfo.length > 1) {
                for (int j=0; j< seperators.length-2; j++){
                    String splits[] = musicInfo[1].split(seperators[j]);
                    musicInfo[1] = splits[0];
//                    log(null, splits[0] + "|" + splits.length);
                    if (splits.length > 1)
                        break;
                }
                break;
            }
        }


        if (musicInfo == null || musicInfo.length == 1 || musicInfo[0] == null ||  musicInfo[1] == null ) {
            return new String[] {titleContent, ""};
//            feedback = "notFound";
        }

        //remove ", ', 」, 』 from musicInfo
        for (int i=0;i<musicInfo.length;i++) {
            for (String s:unwantedSeperators)
                musicInfo[i] = musicInfo[i].replaceAll("^\\s*"+ s +"|"+ s +"\\s*$", " ");
        }

        musicInfo[1] = musicInfo[1].replace("\\.(avi|wmv|mp4|mpeg4|mov|3gpp|flv|webm)$", " ")
                .replaceAll("Full Album", "");
        //Full Album Video

        //move feat. info from artist to
        musicInfo[0] =  musicInfo[0].replaceAll(" (ft\\.?) ", " feat. ");
        if (musicInfo[0].matches(" feat.* .*")) {
            r = Pattern.compile(" feat.* .*");
            Matcher m = r.matcher(musicInfo[0]);
            musicInfo[1] = musicInfo[1] + m.group();
            musicInfo[0] = musicInfo[0].replaceAll(" feat.* .*", "");
        }

        //add remix info
        if(remixInfo.find()){
            musicInfo[1] += " " + remixInfo.group(0);
        }

        //delete spaces
        musicInfo[0] = musicInfo[0].replace("^\\s\\s*", "").replace("\\s\\s*$", "");
        musicInfo[1] = musicInfo[1].replace("^\\s\\s*", "").replace("\\s\\s*$", "");

        return musicInfo;
    }

    static int getMatColor(Context c, String typeColor)
    {
        int returnColor = Color.BLACK;
        int arrayId = c.getResources().getIdentifier("mdcolor_" + typeColor, "array", c.getPackageName());

        if (arrayId != 0)
        {
            TypedArray colors = c.getResources().obtainTypedArray(arrayId);
            int index = (int) (Math.random() * colors.length());
            returnColor = colors.getColor(index, Color.BLACK);
            colors.recycle();
        }
        return returnColor;
    }
}
