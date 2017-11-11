package com.csipsimple.ui.help;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.chatme.R;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.utils.CollectLogs;
import com.csipsimple.utils.CustomDistribution;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.NightlyUpdater;
import com.csipsimple.utils.NightlyUpdater.UpdaterPopupLauncher;
import com.csipsimple.utils.PreferencesProviderWrapper;

import java.util.ArrayList;
import java.util.List;

public class Help extends SherlockDialogFragment implements OnItemClickListener {
	
	
	private static final String THIS_FILE = "Help";
	private PreferencesProviderWrapper prefsWrapper;
	
	public static Help newInstance() {
        Help instance = new Help();
        Bundle args = new Bundle();
        args.putBoolean(ARG_KILL_LOADING, false);
        instance.setArguments(args);
        return instance;
    }
	
	private static final int REQUEST_SEND_LOGS = 0;
	
	// Help choices
	private final static int FAQ = 0, OPEN_ISSUES = 1, SEND_LOGS = 2, START_LOGS = 3, LEGALS = 4, NIGHTLY = 5;
	
	private class HelpEntry {
		public int iconRes;
		public int textRes;
		public int choiceTag;
		public HelpEntry(int icon, int text, int choice) {
			iconRes = icon;
			textRes = text;
			choiceTag = choice;
		}
	}

    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
    	prefsWrapper = new PreferencesProviderWrapper(getActivity());
    	
        
    }
    

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_menu_help)
                .setTitle(R.string.help)
                .setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            dismiss();
                        }
                    }
                )
                .setView(getCustomView(getActivity().getLayoutInflater(), null, savedInstanceState))
                .create();
    }

    
    public View getCustomView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    	View v = inflater.inflate(R.layout.help, container, false);
        ListView lv = (ListView) v.findViewById(android.R.id.list);
        lv.setOnItemClickListener(this);
        
        
        ArrayList<HelpEntry> items = new ArrayList<HelpEntry>();
        
        // 合法信息
        items.add(new HelpEntry(android.R.drawable.ic_menu_gallery, R.string.legal_information, LEGALS));

        // 自动更新
        if(NightlyUpdater.isNightlyBuild(getActivity())){
        	//items.add(new HelpEntry(R.drawable.ic_launcher_nightly, R.string.update_nightly_build, NIGHTLY));
        	//change by chw 20150914 更改自动更新的图标
        	items.add(new HelpEntry(android.R.drawable.ic_menu_compass, R.string.update_nightly_build, NIGHTLY));
        	//end change by chw 20150914 更改自动更新的图标
        }
		
        // 常见问题
		if(!TextUtils.isEmpty(CustomDistribution.getFaqLink())) {
			items.add(new HelpEntry(android.R.drawable.ic_menu_info_details, R.string.faq, FAQ));
		}
        
		// 通过网络查找问题 
		if(CustomDistribution.showIssueList()) {
			items.add(new HelpEntry(android.R.drawable.ic_menu_view, R.string.view_existing_issues, OPEN_ISSUES));
		}
		
		// 记录当前日志并发送到研发人员报告错误
		if(!TextUtils.isEmpty(CustomDistribution.getSupportEmail()) ) {
			if(isRecording()) {
		        items.add(new HelpEntry( android.R.drawable.ic_menu_send , R.string.send_logs, SEND_LOGS));
			}else {
		        items.add(new HelpEntry( android.R.drawable.ic_menu_save , R.string.record_logs, START_LOGS));
			}
		}

        lv.setAdapter(new HelpArrayAdapter(getActivity(), items));
        
        TextView tv = (TextView) v.findViewById(android.R.id.text1);
        //设置 帮助 横线下面的数据  Based on GPL application ChatMe version: 1.01.00 r2269
        tv.setText(CollectLogs.getApplicationInfo(getActivity()));
        
        return v;
    }
    
    private class HelpArrayAdapter extends ArrayAdapter<HelpEntry> {
    	public HelpArrayAdapter(Context ctxt, List<HelpEntry> items) {
			super(ctxt, R.layout.help_list_row, android.R.id.text1, items);
		}
    	
    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		View v = super.getView(position, convertView, parent);
    		bindView(v, getItem(position));
    		return v;
    	}
    	
    	/**
    	 * Bind the fiew to the help entry content
    	 * @param v the view to bind info to
    	 * @param he the help entry to display info of
    	 */
    	private void bindView(View v, HelpEntry he) {
    		TextView tv = (TextView) v;
    		tv.setText(he.textRes);
    		tv.setCompoundDrawablesWithIntrinsicBounds(he.iconRes, 0, 0, 0);
    	}
    }
    
    

	private boolean isRecording() {
		return (prefsWrapper.getLogLevel() >= 3);
	}

	@Override
	public void onItemClick(AdapterView<?> av, View v, int position, long id) {
        Log.i(THIS_FILE, "Item clicked : " + id +" " + position);
		HelpArrayAdapter haa = (HelpArrayAdapter) av.getAdapter();
		HelpEntry he = haa.getItem(position);
		
		SherlockDialogFragment newFragment;
		switch (he.choiceTag) {
		case FAQ:
			newFragment = Faq.newInstance();
	        newFragment.show(getFragmentManager(), "faq");
			break;
		case LEGALS:
			newFragment = Legal.newInstance();
	        newFragment.show(getFragmentManager(), "issues");
			break;
		case OPEN_ISSUES:		//通过网络查找问题
			Intent it = new Intent(Intent.ACTION_VIEW);
			it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//			it.setData(Uri.parse("http://code.google.com/p/csipsimple/issues"));
			//change by chw 20160131 将查找问路的连接更改为百度
			it.setData(Uri.parse("http://www.91caihongwang.com"));
			//change by chw 20160131 将查找问路的连接更改为百度
			startActivity(it);
			break;
		case NIGHTLY:		//自动更新
			// We have to check for an update
			final NightlyUpdater nu = new NightlyUpdater(getActivity());
			Thread t = new Thread() {
				public void run() {
					UpdaterPopupLauncher ru = nu.getUpdaterPopup(true);
					if (ru != null) {
						getActivity().runOnUiThread(ru);
					}
				};
			};
			//change by chw 20150914 将下载最新软件的线程关闭
			t.start();
			//end change by chw 20150914 将下载最新软件的线程关闭
			break;
		case SEND_LOGS:
			prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "1");
			try {
				startActivityForResult(CollectLogs.getLogReportIntent("<<<PLEASE ADD THE BUG DESCRIPTION HERE>>>", getActivity()), REQUEST_SEND_LOGS);
			}catch(Exception e) {
				Log.e(THIS_FILE, "Impossible to send logs...", e);
			}
			Log.setLogLevel(1);
			break;
		case START_LOGS:
			prefsWrapper.setPreferenceStringValue(SipConfigManager.LOG_LEVEL, "4");
			Log.setLogLevel(4);
			Intent intent = new Intent(SipManager.ACTION_SIP_REQUEST_RESTART);
			getActivity().sendBroadcast(intent);
			dismiss();
			break;
		default:
			break;
		}
	}
	
	
	private final static String ARG_KILL_LOADING = "kill_loading";

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_SEND_LOGS) {
			try {
				dismiss();
			} catch (IllegalStateException ex) {
				getArguments().putBoolean(ARG_KILL_LOADING, true);
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onResume() {
		super.onResume();
		final boolean kill = getArguments().getBoolean(ARG_KILL_LOADING, false);
		if (kill) {
			dismiss();
		}
	}

}
