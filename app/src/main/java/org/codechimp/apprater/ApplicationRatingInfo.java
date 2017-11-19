 package org.codechimp.apprater;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

public final class ApplicationRatingInfo {
	private String applicationName;

	public String getApplicationName() {
		return applicationName;
	}

	public int getApplicationVersionCode() {
		return applicationVersionCode;
	}

	public String getApplicationVersionName() {
		return applicationVersionName;
	}

	private int applicationVersionCode;
	private String applicationVersionName;

	private ApplicationRatingInfo() {
	}

	public static ApplicationRatingInfo createApplicationInfo(Context context) {
		PackageManager packageManager = context.getPackageManager();
		ApplicationInfo applicationInfo = null;
		PackageInfo packageInfo = null;
		try {
			applicationInfo = packageManager.getApplicationInfo(
					context.getApplicationInfo().packageName, 0);
			packageInfo = packageManager.getPackageInfo(
					context.getApplicationInfo().packageName, 0);
		} catch (final PackageManager.NameNotFoundException e) {
		}
		ApplicationRatingInfo resultInfo = new ApplicationRatingInfo();
		resultInfo.applicationName = packageManager.getApplicationLabel(
				applicationInfo).toString();
		resultInfo.applicationVersionCode = packageInfo.versionCode;
		resultInfo.applicationVersionName = packageInfo.versionName;
		return resultInfo;
	}
}