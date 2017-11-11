package com.csipsimple.utils;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.chatme.R;
import com.csipsimple.service.DeviceStateReceiver;
import com.csipsimple.service.Downloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class NightlyUpdater {

	private static final String THIS_FILE = "NightlyUpdater";
	
	public static final String LAST_NIGHTLY_CHECK = "nightly_check_date";
	public static final String IGNORE_NIGHTLY_CHECK = "nightly_check_ignore";
	private static final String DOWNLOADED_VERSION = "dl_version";

	private Context context;
	private SharedPreferences prefs;
	private PackageInfo pinfo;
    private String channel;

	public NightlyUpdater(Context ctxt) {
		prefs = PreferenceManager.getDefaultSharedPreferences(ctxt);
		context = ctxt;
		pinfo = PreferencesProviderWrapper.getCurrentPackageInfos(context);
        channel = getChannelFolder(context);
	}
	
	private final static String BASE_UPDATE_VERSION = CustomDistribution.getNightlyUpdaterURL();
	
	private final static String META_TYPE = "app_type";
	private final static String META_CHANNEL = "app_channel";
	
	private final static String NIGHTLY_TYPE = "nightly";
	//private final static String RELEASE_TYPE = "release";
	
	
	private static Bundle getApplicationMetaData(Context ctxt) throws NameNotFoundException {
	    ApplicationInfo apInfo = ctxt.getPackageManager().getApplicationInfo(ctxt.getPackageName(), PackageManager.GET_META_DATA);
	    return apInfo.metaData;
	}
	
	public static boolean isNightlyBuild(Context ctxt) {
	    try {
	        Bundle metaData = getApplicationMetaData(ctxt);
	        if(metaData != null) {
                String appType = metaData.getString(META_TYPE);
        	    if(!TextUtils.isEmpty(appType)) {
        	    		if (!TextUtils.isEmpty(BASE_UPDATE_VERSION)) {
        	    			if (NIGHTLY_TYPE.equalsIgnoreCase(appType)) {
        	    					return true;
        	    			}
        	    		}
        	    }
	        }
	    } catch (NameNotFoundException e) {
            Log.e(THIS_FILE, "Not able to get self app info", e);
        }
	    return false;
	}
	
	public static String getChannelFolder(Context ctxt) {
        try {
            Bundle metaData = getApplicationMetaData(ctxt);
            if(metaData != null) {
                String appChannel = metaData.getString(META_CHANNEL);
                if(!TextUtils.isEmpty(appChannel)) {
                    return appChannel;
                }
            }
        } catch (NameNotFoundException e) {
            Log.e(THIS_FILE, "Not able to get self app info", e);
        }
        return "trunk";
	}
	
	
	private int getLastOnlineVersion() {
		try {
			URL url = new URL(BASE_UPDATE_VERSION + channel + "/CSipSimple-latest-" + channel + ".version");
			Log.d(THIS_FILE, "Url : "+url);
			InputStream content = (InputStream) url.getContent();
			if(content != null) {
				BufferedReader r = new BufferedReader(new InputStreamReader(content));
				String versionString = r.readLine();
				return Integer.parseInt(versionString);
			}
		} catch (MalformedURLException e) {
			Log.e(THIS_FILE, "Invalid nightly build url", e);
		} catch (IOException e) {
			Log.e(THIS_FILE, "Can't get nightly latest value", e);
		} catch (NumberFormatException e) {
			Log.e(THIS_FILE, "Invalid number format", e);
		}
		return 0;
	}
	
	public boolean ignoreCheckByUser() {
		return prefs.getBoolean(IGNORE_NIGHTLY_CHECK, false);
	}
	
	public long lastCheck() {
		return prefs.getLong(LAST_NIGHTLY_CHECK, (long) 0);
	}
	
	public UpdaterPopupLauncher getUpdaterPopup(boolean fallbackAlert) {
		UpdaterPopupLauncher popLauncher = null;
		Editor edt = prefs.edit();
		
		int onlineVersion = getLastOnlineVersion();
		//change by chw 20150915 将获取最新的软件版本更改为直接赋值
//		int onlineVersion = 2;
		//end change by chw 20150915 将获取最新的软件版本更改为直接赋值
		
		// Reset ignore check value
		edt.putBoolean(IGNORE_NIGHTLY_CHECK, false);
		if(pinfo != null && pinfo.versionCode < onlineVersion) {
			popLauncher = new UpdaterPopupLauncher(context, onlineVersion);
		}else {
			// Set last check to now :)
			edt.putLong(LAST_NIGHTLY_CHECK, System.currentTimeMillis());
			// And delete latest nightly from cache
			File cachedFile = getCachedFile();
			if(cachedFile.exists()) {
				cachedFile.delete();
			}
			if(fallbackAlert) {
				popLauncher = new UpdaterPopupLauncher(context, 0);
			}
		}
		edt.commit();
		
		return popLauncher;
	}
	
	private File getCachedFile() {
		return new File(context.getCacheDir(), "CSipSimple-latest-trunk.apk");
	}
	
	public class UpdaterPopupLauncher implements Runnable {
		
		private Context context;
		private int version = 0;
		
		public UpdaterPopupLauncher(Context ctxt, int onlineVersion) {
			context = ctxt;
			version = onlineVersion;
		}
		
		@Override
		public void run() {
			Builder ab = new AlertDialog.Builder(context);
			
			
			ab.setIcon(R.drawable.ic_launcher_nightly)
				.setTitle(context.getString(R.string.update_software));
			
			if(version > 0) {
				ab.setMessage(context.getString(R.string.update_software_info))
					.setPositiveButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							Intent it = new Intent();
//							it.setData(Uri.parse(BASE_UPDATE_VERSION + channel + "/CSipSimple-r"+version+"-" + channel + ".apk"));
							//change by chw 20160131  对下载最新软件的URI地址进行更改
							it.setData(Uri.parse("http://p.gdown.baidu.com/0b0874af2ae364b4030daf0a67dbdc78d965f480cb88a1e83556e0b0eab436d6210ab60e702b88e1f8c7e86e669efa799fa9e8e704e3a803156edff9bb2fdf26091fc8859821ccbe159c928bc84684aba82be5de6649d1ca5d73e94b1d4fc83cb8af3e5447d9858bcbda2fc031afdc9715e8291a1a50542bf3e4aa1cec3a7b1326b87579c63b6f19d90d83525ed462fe2f36e3f77aaed4af1e5531fdfa62710eb807958b4ca853473da10af212aceaa889a8d01da9066a281165855734b97045141cf0cf4368df615870ba338b39e043b36c2802229366b90c9d5dff9e5aa455ac0873c4d4a812e7007d519ef4c7b459a91356b9f163095ba1c51e8fc9a54c190e540bf3c178f757c681165204e60ec5"));
//							it.setData(Uri.parse("http://gdown.baidu.com/data/wisegame/a48dc42adf5374bf/baiduxinwen_5300.apk"));
							//end change by chw 20160131  对下载最新软件的URI地址进行更改
							it.setClass(context, Downloader.class);
							it.putExtra(Downloader.EXTRA_ICON, R.drawable.ic_launcher_nightly);
//							it.putExtra(Downloader.EXTRA_TITLE, "CSipSimple nightly build");
							it.putExtra(Downloader.EXTRA_TITLE, "ChatMe_" + version);
							it.putExtra(Downloader.EXTRA_CHECK_MD5, true);
							it.putExtra(Downloader.EXTRA_OUTPATH, getCachedFile().getAbsolutePath());
							
							Intent resultIntent = new Intent(context, DeviceStateReceiver.class);
							resultIntent.setAction(DeviceStateReceiver.APPLY_NIGHTLY_UPLOAD);
							resultIntent.putExtra(DOWNLOADED_VERSION, version);
							PendingIntent pi = PendingIntent.getBroadcast(context, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
							it.putExtra(Downloader.EXTRA_PENDING_FINISH_INTENT, pi);
							context.startService(it);
						}
					})
					.setNegativeButton(R.string.cancel, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Editor edt = prefs.edit();
							edt.putBoolean(IGNORE_NIGHTLY_CHECK, true);
							edt.commit();
							dialog.dismiss();
						}
					});
			}else {
				ab.setMessage("No update available")
					.setPositiveButton(R.string.ok, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							dialog.dismiss();
						}
					});
			}

			ab.create().show();
			
		}
	}

	public void applyUpdate(Intent i) {
		File f = getCachedFile();
		
		Intent intent = new Intent(Intent.ACTION_VIEW);
    	intent.setDataAndType(Uri.fromFile(f.getAbsoluteFile()), "application/vnd.android.package-archive");
    	intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	context.startActivity(intent);
	}
}
