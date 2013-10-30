package test.example.helloworld;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import com.devSyte.InfinityMPG.R;

public class SettingsFragment extends PreferenceFragment {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource

        addPreferencesFromResource(R.xml.preferences);
        

        
        Preference pref = (Preference) findPreference("units_pref");
        String summary =pref.getSharedPreferences().getString("units_pref", getString(R.string.mpg));
        pref.setSummary(summary);
        
        Preference btpref = (Preference) findPreference("bt_device");
        summary = btpref.getSharedPreferences().getString("bt_device", getString(R.string.default_none));
        btpref.setSummary(summary);
        
        btpref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
          @Override
          public boolean onPreferenceClick(Preference preference) {


        	startActivity(preference.getIntent());
            return true;
          }
        });
    }
   /* @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode == Activity.RESULT_OK) {
	        Preference pref = (Preference) findPreference("bt_device");
			SharedPreferences.Editor prefs = pref.getSharedPreferences().edit();
	        String deviceData = data.getExtras()
	                .getString(DisplayMessageActivity.DEVICE_DATA);
	        prefs.putString("bt_device", deviceData).apply();
	        String summary =deviceData;
	        pref.setSummary(summary);
	        
		}
    }
    */
    
    /*public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
    	
    	Preference pref = (Preference) findPreference(key);
        if (key.equals("bt_device")) {
            
	        String summary =pref.getSharedPreferences().getString(key, getString(R.string.default_none));
	        pref.setSummary(summary);
        }else if(key.equals("units_pref")){
        	String summary =pref.getSharedPreferences().getString(key, getString(R.string.mpg));
	        pref.setSummary(summary);
        }
    }*/
    

    
}
