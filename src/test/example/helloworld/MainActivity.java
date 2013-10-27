package test.example.helloworld;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.text.format.Time;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.DialogInterface.OnClickListener;
import android.content.SharedPreferences.Editor;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.TextView;
import android.bluetooth.*;

public class MainActivity extends Activity {
    public final static String EXTRA_MESSAGE = "test.example.helloworld.MESSAGE";
	protected BluetoothAdapter mBluetoothAdapter;
	protected obdService mobdService = null;
	protected ArrayAdapter<String> cmdPrompt;
	
	ArrayList<String> mpgDataList = new ArrayList<String>();
	TextView mainText;
	
	public static final int WRITE_SCREEN = 1;	        
	public static final int WRITE_PROMPT = 2;
	public static final int WRITE_FILE = 3;
	public static final int FINISH_IT = 4;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

        
        cmdPrompt = new ArrayAdapter<String>(this, android.R.layout.list_content);
        mainText = (TextView) findViewById(R.id.mainDisplay);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			AlertBox("blue", "Bluetooth not supported");
			finish();
		}else{
			mobdService = new obdService(this, mHandler);
		}
    }

    //currently the handler isn't too helpful, and is used only to log the retrieved data
    Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {

    		switch (msg.what) {
    		
    		case WRITE_PROMPT:
    			
    			cmdPrompt.add(msg.getData().getString("commData"));
    			if(cmdPrompt.getCount() >= 128){
    				writeCommsToFile();
    			}
    			break;
    		case WRITE_FILE:
    			writeCommsToFile();
    			writeMpgData();
    			
    			break;
    		case WRITE_SCREEN:
    			Time now = new Time();
    			now.setToNow();
    			String curTime = Integer.toString(now.year)+"-"+Integer.toString(now.month+1)+"-"+Integer.toString(now.monthDay)
    					+ " " + Integer.toString(now.hour) + ":" + Integer.toString(now.minute) + ":" +Integer.toString(now.second);
    			
    			mpgDataList.add(curTime+ ": " + msg.getData().getString("mpgData") + "\r");
    			if(mpgDataList.size() >=128){
    				writeMpgData();
    			}
    			
    			mainText.setText(msg.getData().getString("mpgData"));
    			break;
    			
    		}
    	}	
    };
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_settings:
                // Display the fragment as the main content.
            	Intent intent = new Intent(this, SettingsActivity.class);
            	startActivityForResult(intent, 1);
                return true;
                
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

  
    	if (resultCode == Activity.RESULT_OK) {
    		
			SharedPreferences.Editor prefs = PreferenceManager.getDefaultSharedPreferences(this).edit();
	        String deviceData = data.getExtras()
	                .getString(DisplayMessageActivity.DEVICE_DATA);
	        prefs.putString("bt_device", deviceData).apply();
	        
		}
	    		
    		
    }

    private void connectDevice(String deviceData) {
        // Get the device MAC address

String address = deviceData.substring(deviceData.indexOf('\n')+1);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mobdService.connect(device);
        
    }
    
    public void findDevice(View view){
    	Intent intent = new Intent(this, DisplayMessageActivity.class);
        
		startActivityForResult(intent, 0);
    	
    }
    
    @Override
    public void onRestart(){

    	super.onRestart();
    }

    
    @Override
    public void onStop(){
    	//TODO:This needs to keep the connection between devices going, whether or not it should keep tracking is still up for debate

    	mobdService.stop();
    	if(mpgDataList.size()>0){
    		Message message = mHandler.obtainMessage(MainActivity.WRITE_FILE, -1, -1);
    		message.sendToTarget();
    	}

		super.onStop();
    }
    
	public void startService(View view){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	String deviceName = prefs.getString("bt_device", "None");
    	if(deviceName.equals("None")){
    
    	
    		Intent intent = new Intent(this, DisplayMessageActivity.class);
    
    		startActivityForResult(intent, 0); //My displayMessageActivity needs renamed, but this allows the user to select a BT device
    	}else{
    		connectDevice(deviceName);
    	}
    }
    
    public void writeCommsToFile(){
    	
    	File file = new File(Environment.getExternalStorageDirectory(), "ELM327comm_data.txt");
		
		String str = "";
		try {
			 BufferedWriter bW;
	
	         bW = new BufferedWriter(new FileWriter(file,true));
	    	
	    	for(int i =0; i< cmdPrompt.getCount(); ++i){
	    		str = str.concat(cmdPrompt.getItem(i));
	    	}
	    	cmdPrompt.clear();
	    	
			bW.write(str);
			bW.newLine();
            bW.flush();
            bW.close();
			
		}catch (IOException e) {}
		
    }

    public void writeMpgData(){
    	mobdService.stop();
    	File file = new File(Environment.getExternalStorageDirectory(), "mpg_data.txt");

		
		String str = "";
		try {
			 BufferedWriter bW;
	
	         bW = new BufferedWriter(new FileWriter(file,true));
	    	
	    	for(int i =0; i< mpgDataList.size(); ++i){
	    		str = str.concat(mpgDataList.get(i));
	    	}
	    	mpgDataList.clear();
	    	
			bW.write(str);
			bW.newLine();
            bW.flush();
            bW.close();
			
		}catch (IOException e) {
			
		}

    }
    
	public void AlertBox( String title, String message ){
	    new AlertDialog.Builder(this)
	    .setTitle( title )
	    .setMessage( message + "\n Please Press OK" )
	    .setPositiveButton("OK", new OnClickListener() {
	        public void onClick(DialogInterface arg0, int arg1) {
	   
	        }
	    }).show();
	  }
    
}
