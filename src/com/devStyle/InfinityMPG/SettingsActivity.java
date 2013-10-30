package com.devStyle.InfinityMPG;

import com.devStyle.InfinityMPG.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;

public class SettingsActivity extends PreferenceActivity {
	
	SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
       listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
      	  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
      		  String summary = "";
      		  Preference pref = (Preference) findPreference(key);
      	        if (key.equals("bt_device")) {
      	        	
      	            
      		        summary =pref.getSharedPreferences().getString(key, getString(R.string.default_none));
      		       
      	        }else if(key.equals("units_pref")){
      	        	
      	        	summary =pref.getSharedPreferences().getString(key, getString(R.string.mpg));
      		        
      	        }
      	        pref.setSummary(summary);
      	  };
     };
     
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
    	prefs.registerOnSharedPreferenceChangeListener(listener);
        
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    

    @Override
    public void onResume() {
        super.onResume();
        getPreferences(MODE_PRIVATE)
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferences(MODE_PRIVATE)
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode == Activity.RESULT_OK) {
	        Preference pref = (Preference) findPreference("bt_device");
			SharedPreferences.Editor prefs = pref.getSharedPreferences().edit();
	        String deviceData = data.getExtras()
	                .getString(DisplayMessageActivity.DEVICE_DATA);
	        prefs.putString("bt_device", deviceData).apply();
	        //String summary =deviceData;
	        //pref.setSummary(summary);
	        
		}
    }
}
