package test.example.helloworld;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;

public class SettingsActivity extends PreferenceActivity {
	
	SharedPreferences.OnSharedPreferenceChangeListener listener;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Display the fragment as the main content.
       listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        	  public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        	        if (key.equals("bt_device")) {
        	        	
        	            Preference pref = (Preference) findPreference(key);
        		        String summary =pref.getSharedPreferences().getString("bt_device", "None");
        		        pref.setSummary(summary);
        	        }
        	  }
        	};
    	SharedPreferences prefs = getPreferences(MODE_PRIVATE);
    	prefs.registerOnSharedPreferenceChangeListener(listener);
        
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
    	super.onActivityResult(requestCode, resultCode, data);
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
}
