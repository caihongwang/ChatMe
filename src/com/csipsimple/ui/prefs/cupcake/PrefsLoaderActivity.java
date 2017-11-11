package com.csipsimple.ui.prefs.cupcake;

import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.MenuItem;
import com.csipsimple.ui.prefs.GenericPrefs;
import com.csipsimple.ui.prefs.PrefsLogic;
import com.csipsimple.utils.Compatibility;

public class PrefsLoaderActivity extends GenericPrefs {
    
    private int getPreferenceType() {
        return getIntent().getIntExtra(PrefsLogic.EXTRA_PREFERENCE_TYPE, 0);
    }

    @Override
    protected int getXmlPreferences() {
        return PrefsLogic.getXmlResourceForType(getPreferenceType());
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(PrefsLogic.getTitleResourceForType(getPreferenceType()));
        ActionBar ab = getSupportActionBar();
        if(ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == Compatibility.getHomeMenuId()) {
            finish();
            return true;
        }
        return false;
    }

    

    @Override
    protected void afterBuildPrefs() {
        super.afterBuildPrefs();
        PrefsLogic.afterBuildPrefsForType(this, this, getPreferenceType());
        
    }

    @Override
    protected void updateDescriptions() {
        PrefsLogic.updateDescriptionForType(this, this, getPreferenceType());
    }


    
}
