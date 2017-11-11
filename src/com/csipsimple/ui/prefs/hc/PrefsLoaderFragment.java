package com.csipsimple.ui.prefs.hc;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.text.TextUtils;

import com.chatme.R;
import com.csipsimple.ui.prefs.IPreferenceHelper;
import com.csipsimple.ui.prefs.PrefsLogic;
import com.csipsimple.utils.Log;

@TargetApi(11)
public class PrefsLoaderFragment extends PreferenceFragment implements IPreferenceHelper {
    
    public PrefsLoaderFragment() {
        
    }

    private int getPreferenceType() {
        return getArguments().getInt(PrefsLogic.EXTRA_PREFERENCE_TYPE);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int type = getPreferenceType();
        addPreferencesFromResource(PrefsLogic.getXmlResourceForType(type));
        PrefsLogic.afterBuildPrefsForType(getActivity(), this, getPreferenceType());
        
    }

    @Override
    public void hidePreference(String parent, String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        PreferenceGroup parentPref = pfs;
        if (parent != null) {
            parentPref = (PreferenceGroup) pfs.findPreference(parent);
        }

        Preference toRemovePref = pfs.findPreference(fieldName);

        if (toRemovePref != null && parentPref != null) {
            boolean rem = parentPref.removePreference(toRemovePref);
            Log.d("Generic prefs", "Has removed it : " + rem);
        } else {
            Log.d("Generic prefs", "Not able to find" + parent + " " + fieldName);
        }
    }

    // Utilities for update Descriptions
    /**
     * Get field summary if nothing set. By default it will try to add _summary
     * to name of the current field
     * 
     * @param field_name Name of the current field
     * @return Translated summary for this field
     */
    protected String getDefaultFieldSummary(String field_name) {
        try {
            String keyid = R.string.class.getField(field_name + "_summary").get(null).toString();
            return getString(Integer.parseInt(keyid));
        } catch (SecurityException e) {
            // Nothing to do : desc is null
        } catch (NoSuchFieldException e) {
            // Nothing to do : desc is null
        } catch (IllegalArgumentException e) {
            // Nothing to do : desc is null
        } catch (IllegalAccessException e) {
            // Nothing to do : desc is null
        }

        return "";
    }
    
    @Override
    public void setStringFieldSummary(String fieldName) {
        PreferenceScreen pfs = getPreferenceScreen();
        SharedPreferences sp = pfs.getSharedPreferences();
        Preference pref = pfs.findPreference(fieldName);

        String val = sp.getString(fieldName, null);
        if (TextUtils.isEmpty(val)) {
            val = getDefaultFieldSummary(fieldName);
        }
        setPreferenceSummary(pref, val);
    }
    

    /**
     * Safe setSummary on a Preference object that make sure that the preference
     * exists before doing anything
     * 
     * @param pref the preference to change summary of
     * @param val the string to set as preference summary
     */
    protected void setPreferenceSummary(Preference pref, CharSequence val) {
        if (pref != null) {
            pref.setSummary(val);
        }
    }


    @Override
    public void setPreferenceScreenType(String key, int type) {
        setPreferenceScreenType(getClass(), key, type);
    }
    

    @Override
    public void setPreferenceScreenSub(String key, Class<?> activityClass, Class<?> fragmentClass, int type) {
        setPreferenceScreenType(fragmentClass, key, type);
    }
    
    private void setPreferenceScreenType(Class<?> classObj, String key, int type) {
        Preference pf = findPreference(key);
        pf.setFragment(classObj.getCanonicalName());
        Bundle b = pf.getExtras();
        b.putInt(PrefsLogic.EXTRA_PREFERENCE_TYPE, type);
    }
    
    
}
