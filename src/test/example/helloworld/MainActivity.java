package test.example.helloworld;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;




import android.media.MediaScannerConnection;
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
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
	TextView subText;
	
	public static final int WRITE_SCREEN = 1;	        
	public static final int WRITE_PROMPT = 2;
	public static final int WRITE_FILE = 3;
	public static final int FINISH_IT = 4;
	public static final int CONNECT_SUCCESS = 5;
	public static final int CONNECT_FAILURE = 6;
	
	private double currDisplayData = 0.0;
	private double currSubDispData = 0.0;
	private double currMPG = 0.0;
	private long numDataPts = 0L;
	
	private double runningMpgAvg = 0.0;
	

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
        SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        runningMpgAvg = prefs.getFloat("avgMpg", 0.0f);
        numDataPts = prefs.getLong("numPtsForAvg", 0l);
        
        
        cmdPrompt = new ArrayAdapter<String>(this, android.R.layout.list_content);
        mainText = (TextView) findViewById(R.id.mainDisplay);
        subText = (TextView) findViewById(R.id.subDisplay);
        
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null){
			AlertBox("blue", "Bluetooth not supported");
			finish();
		}else{
			mobdService = new obdService(this, mHandler);
		}
    }
    
    @Override
    protected void onStart(){
    	super.onStart();
    	Button findDev = (Button) findViewById(R.id.find_device);
    	Button startOrSave = (Button) findViewById(R.id.start_or_save);
    	if(!mobdService.isConnected()){
    		startOrSave.setText(R.string.start);
    		startOrSave.setOnClickListener(new View.OnClickListener() {
    			@Override
                public void onClick(View v) {
               
                	startService();

                }
            });
    		findDev.setVisibility(Button.VISIBLE);
    	}else{
    		startOrSave.setText(R.string.saveBut);
    		startOrSave.setOnClickListener(new View.OnClickListener() {
    			@Override
                public void onClick(View v) {
                	endAndSave();
                }
            });
    		findDev.setVisibility(Button.GONE);
    	}
    }

    //Here the preferences are implemented, the data conversions done, and the screen is written
    Handler mHandler = new Handler(){
    	@Override
    	public void handleMessage(Message msg) {
			Button startOrSave = (Button) findViewById(R.id.start_or_save);
    		Button findDev = (Button) findViewById(R.id.find_device);
    		
    		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
    		String unitOutput = ""; 
    		switch (msg.what) {
    		
    		case WRITE_PROMPT:
    			
    			cmdPrompt.add(msg.getData().getString("commData"));
    			if(cmdPrompt.getCount()>=128){
    				writeCommsToFile();
    			}
    			
    			
    			break;
    		case WRITE_FILE:
    			
    				
    				writeCommsToFile();
    				writeMpgData(true); //write_file message is only sent when the time should be appended (pass true)
    				//writeMpgData(msg.getData().getBoolean("writeTime"));
    			
    			
    			break;
    		case WRITE_SCREEN:
    			Time now = new Time();
    			now.setToNow();
    			String curTime = Integer.toString(now.hour) + ":" + Integer.toString(now.minute) + ":" +Integer.toString(now.second);
    			
    			DecimalFormat df = new DecimalFormat("#.##");
    			unitOutput = prefs.getString("units_pref", "Mpg");
    			
    			
    			currMPG = msg.getData().getDouble("mpgData");
    			++numDataPts;
    			currDisplayData = currMPG;
    			//currMPG is what is written to the file.  The file is ALWAYS stored as MPG, regardless of user preference
    			//When data analysis is available, it will be converted to user spec'd  units.

    			//runningavg will store the *total* number of data points for the file and the avg associated with them
    			//every iteration we just add the new point to the set
    	
    			//Case 0 is standard, engine running displaying mpg.  1 is engine idling, 2 engine is off (mpg = 0)

    			switch(msg.arg1){

	    			case 0:
	        			currMPG *= obdService.kmToMi/((obdService.stoichRatio*3600)/obdService.gramGasToGal);
	        			runningMpgAvg = ((((double)numDataPts-1) * runningMpgAvg) + currMPG)/((double)numDataPts);
	        			
	        			if(unitOutput.equals("Mpg")){
	        				//this will convert km/h to mi/h
	       				 	//this converts a gram of gas/second to gallon/hour
	        				currDisplayData = currMPG;
		        			currSubDispData = runningMpgAvg;

		       			}else if(unitOutput.equals("L/100km")){
		       				//this yields kmPerHour/gramsGasPerHour == km/gramGas
		       				currDisplayData *= 100.0/((obdService.stoichRatio*3600.0)/obdService.gramGasToLiter); // now 100km/literGas
		       				currDisplayData = 1.0/currDisplayData; //Liter/100km
		       				
		       				
		    				currSubDispData = runningMpgAvg * ((obdService.miToKm*100.0)/obdService.literGasToGal);
		        			currSubDispData = 1.0/currSubDispData;
		       			}else if(unitOutput.equals("Mpg(UK)")){
		       				currDisplayData *=(obdService.kmToMi)/ ((3600.0* obdService.stoichRatio)/obdService.gramGasToImpGal);
		       				
		       				currSubDispData = runningMpgAvg * (1.0/obdService.galGasToImpGal);
		       			}
	        			
	        			break;
	    			case 1:
	        			currMPG = 0.0;
	        			runningMpgAvg = ((((double)numDataPts-1) * runningMpgAvg) + currMPG)/((double)numDataPts);
	        			
	    				if(prefs.getBoolean("idle_stats_pref", true)){
		        			if(unitOutput.equals("Mpg")){
		        				
		       				 	//this converts a gram of air/second (the MAF stored in currMPG) to gallon/hour
		        				currDisplayData = (currDisplayData * ( 3600.0* obdService.stoichRatio))/obdService.gramGasToGal;
		        				unitOutput = "G/Hr";
		        				
			        			currSubDispData = runningMpgAvg;

			       			}else if(unitOutput.equals("L/100km")){
			       				currDisplayData = (currDisplayData *obdService.stoichRatio*3600.0)/obdService.gramGasToLiter;//liter/hour
			       				unitOutput = "L/Hr";
	
			    				currSubDispData = runningMpgAvg * ((obdService.miToKm*100.0)/obdService.literGasToGal);
			        			currSubDispData = 1.0/currSubDispData;
			       			}else if(unitOutput.equals("Mpg(UK)")){
			       				currDisplayData =(currDisplayData* 3600.0* obdService.stoichRatio)/obdService.gramGasToImpGal ;
			       				unitOutput = "G(UK)/Hr";
			       			}
	    				}else{
	    					currDisplayData = 0.0;
		       				currSubDispData = runningMpgAvg * (1.0/obdService.galGasToImpGal);

	    				}


	    				currMPG = 0.0;
	    				
	        			break;
	    			case 2:
	    				currMPG = msg.getData().getDouble("mpgData");
	    				
	    				currDisplayData = currMPG;
	        			currSubDispData = runningMpgAvg;

	    				unitOutput = "";
	        			break;

    			}
    			
       			
    			

    			SharedPreferences.Editor prefEdit = prefs.edit();
    			prefEdit.putLong("numPtsForAvg", numDataPts);
    			prefEdit.putFloat("avgMPG", (float)runningMpgAvg);
    			currMPG = Double.valueOf(df.format(currMPG));
    			currDisplayData = Double.valueOf(df.format(currDisplayData));
    			currSubDispData = Double.valueOf(df.format(currSubDispData));
    			
    			mpgDataList.add("\t" + curTime+ "> " + Double.toString(currMPG) + "\r");
    			if(mpgDataList.size() >=128){
    				writeMpgData(false);
    			}
				
            	
        		TextView unitText = (TextView) findViewById(R.id.unitDisplay);

            	
    			mainText.setText(Double.toString(currDisplayData) + unitOutput);
    			subText.setText("Avg for this trip: " + Double.toString(currSubDispData)+prefs.getString("units_pref", "Mpg"));
    			break;
    			
    		case CONNECT_SUCCESS:

        		startOrSave.setText(R.string.saveBut);
        		startOrSave.setOnClickListener(new View.OnClickListener() {
        			@Override
                    public void onClick(View v) {
                    	endAndSave();
                    }
                });
        		findDev.setVisibility(Button.GONE);
        		
        		

        		break;
        		
    		case CONNECT_FAILURE:

        		startOrSave.setText(R.string.start);
        		startOrSave.setOnClickListener(new View.OnClickListener() {
        			@Override
                    public void onClick(View v) {
                   
                    	startService();

                    }
                });
        		findDev.setVisibility(Button.VISIBLE);
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

    	endAndSave();

		super.onStop();
    }
    
	public void startService(){
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    	
    	String deviceName = prefs.getString("bt_device", "None");
    	if(deviceName.equals("None")){
    
    	
    		Intent intent = new Intent(this, DisplayMessageActivity.class);
    
    		startActivityForResult(intent, 0); //My displayMessageActivity needs renamed, but this allows the user to select a BT device
    	}else{
    		
    		connectDevice(deviceName);


    	}
    }
  
	public void endAndSave(){
		SharedPreferences prefs=PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		SharedPreferences.Editor prefEdit = prefs.edit();
		prefEdit.putLong("numPtsForAvg", numDataPts);
		prefEdit.putFloat("avgMPG", (float)runningMpgAvg);
		if(mobdService.isConnected()){
			mobdService.stop();
	
			writeCommsToFile();
			writeMpgData(true);
			File file = new File(Environment.getExternalStorageDirectory(), "mpg_data.txt");
			MediaScannerConnection.scanFile(this,  new String[] {file.toString()}, null, new MediaScannerConnection.OnScanCompletedListener() {
			      public void onScanCompleted(String path, Uri uri) {
	
			      }
			 });
		}
		Button startOrSave = (Button) findViewById(R.id.start_or_save);
		Button findDev = (Button) findViewById(R.id.find_device);
		startOrSave.setText(R.string.start);
		startOrSave.setOnClickListener(new View.OnClickListener() {
			@Override
            public void onClick(View v) {
           
            	startService();

            }
        });
		findDev.setVisibility(Button.VISIBLE);
		 
	}
	
    public void writeCommsToFile(){
    	
    	File file = new File(Environment.getExternalStorageDirectory(), "ELM327comm_data.txt");
		
		String str = "";
		try {
			 BufferedWriter bW;
	
	         bW = new BufferedWriter(new FileWriter(file,true));
	    	if(cmdPrompt.getCount() >0){
		    	for(int i =0; i< cmdPrompt.getCount(); ++i){
		    		str = str.concat(cmdPrompt.getItem(i));
		    	}
		    	cmdPrompt.clear();
		    	
				bW.write(str);
				bW.newLine();
	            bW.flush();
	            bW.close();
	    	}
			
		}catch (IOException e) {}
		
    }

    public void writeMpgData(boolean appendTime){
    	
    	File file = new File(Environment.getExternalStorageDirectory(), "mpg_data.txt");

		
		String str = "";
		try {
			 BufferedWriter bW;
	
	         bW = new BufferedWriter(new FileWriter(file,true));
	    	if(mpgDataList.size()>0){	
		    	for(int i =0; i< mpgDataList.size(); ++i){
		    		str = str.concat(mpgDataList.get(i));
		    	}
		    	mpgDataList.clear();
	    	}

	    	if(appendTime){
				Time now = new Time();
				now.setToNow();
				String date = Integer.toString(now.year)+"-"+Integer.toString(now.month+1)+"-"+Integer.toString(now.monthDay) +"\r";
		    
				str = str.concat(date);
	    	}
	    	
	    	
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
