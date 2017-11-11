package com.csipsimple.ui.account;

import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragment;
import com.csipsimple.api.SipProfile;

public class AccountEditFragment extends SherlockFragment {

	public static AccountEditFragment newInstance(long profileId) {
		AccountEditFragment f = new AccountEditFragment();

		// Supply index input as an argument.
		Bundle args = new Bundle();
		args.putLong(SipProfile.FIELD_ID, profileId);
		f.setArguments(args);

		return f;
	}
	
	/*
	private OnQuitListener onQuitListener;
	public void setOnQuitListener(OnQuitListener l) {
		onQuitListener = l;
	}
	public interface OnQuitListener {
		public void onQuit();
		public void onShowProfile(long profileId);
	}
	*/
}
