package com.csipsimple.ui.prefs.hc;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.chatme.R;
import com.csipsimple.api.SipManager;
import com.csipsimple.ui.prefs.PrefsFilters;
import com.csipsimple.ui.prefs.PrefsLogic;
import com.csipsimple.utils.PreferencesWrapper;

import java.util.List;

@TargetApi(11)
public class MainPrefs extends SherlockPreferenceActivity {
    private PreferencesWrapper prefsWrapper;
    private List<Header> mFragments;

    @Override
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.prefs_headers, target);
        for(Header header : target) {
            // Well not the cleanest way to do that...
            if(header.iconRes == R.drawable.ic_prefs_fast) {
                header.intent = new Intent(SipManager.ACTION_UI_PREFS_FAST);
            }else if(header.iconRes == R.drawable.ic_prefs_filter) {
                header.intent = new Intent(this, PrefsFilters.class);
            }
        }
        mFragments = target;
    }
    
    @Override
    public Header onGetInitialHeader() {
        for(Header h : mFragments) {
            if(!TextUtils.isEmpty(h.fragment)) {
                return h;
            }
        }
        return super.onGetInitialHeader();
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefsWrapper = new PreferencesWrapper(this);
        // TODO -- enable display home as up
        //getActionBar().setDisplayHomeAsUpEnabled(true);
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_prefs, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        PrefsLogic.onMainActivityPrepareOptionMenu(menu, this, prefsWrapper);
        return super.onPrepareOptionsMenu(menu);
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(PrefsLogic.onMainActivityOptionsItemSelected(item, this, prefsWrapper)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
}
