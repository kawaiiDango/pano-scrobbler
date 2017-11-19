package org.codechimp.apprater;

import android.content.Context;
import android.net.Uri;

public abstract class Market {

    protected static String packageName;

    protected abstract Uri getMarketURI(Context context);

    public void overridePackageName(String packageName) {
        Market.packageName = packageName;
    }

    protected static String getPackageName(Context context) {
        if (Market.packageName != null) {
            return Market.packageName;
        }
        return context.getPackageName();
    }
}
