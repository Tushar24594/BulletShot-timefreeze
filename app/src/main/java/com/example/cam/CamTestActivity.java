package com.example.cam;

/**
 * @author Jose Davis Nidhin
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;



import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import cz.msebera.android.httpclient.Header;

@SuppressWarnings("deprecation")
public class CamTestActivity extends Activity implements  SensorListener {
	private static final String TAG = "CamTestActivity";
	Preview preview;
	Button buttonClick;
	Camera camera;
	Activity act;
	Context ctx;
	 /**
     * Socket.io client socket for getting command from server
     */
    private Socket mSocket;

    /**
     * Server address to connect with, this is stored in shared preference and can be chaged by setting activity
     */
    String serverAddress;

    /**
     * Device name will be identifier for this device, scoker.io server will shoot this device name result to that picture will be clicked default name is camera1
     */
    String deviceName = "";

    /**
     * Async http client for uploading image via HTTP POST
     */
    AsyncHttpClient client;

    /**
     * One more identifier from server
     */
    String key = "";
    
    /**
     * Shot timestamp this will be received from server at this time image will be captured so all camera can be in sync
     */
    String shotTimeStamp;
    
    /**
     * Server time stamp received form buuletshot server
     */
    String serverTime;
    
    /**
     * Boradcast receiver for power state change
     */
    private BroadcastReceiver mReceiver;
    
    /**
     * Sensor manager for getting accelerometer values
     */
    private SensorManager mSensorManager;
    private Sensor mSensor;
    
    /**
     * Text view for displaying accelerometer values
     */
    TextView mTextViewGyro;
    
    float[] gravity = new float[3];
    float[] linear_acceleration = new float[3];
    DecimalFormat df = new DecimalFormat("#.0");
    //get code for position sensor android documentation
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];

    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];
	private AudioManager mAudioManager;
	private ComponentName mReceiverComponent;
    static CamTestActivity camTestActivity=null;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if(camTestActivity==null){
			camTestActivity = this;
		}
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		
		setContentView(R.layout.main);
		
		mTextViewGyro = (TextView)findViewById(R.id.textViewGyroValues);
		
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		preview = new Preview(this, (SurfaceView)findViewById(R.id.surfaceView));
		preview.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		((FrameLayout) findViewById(R.id.layout)).addView(preview);
		preview.setKeepScreenOn(true);

		preview.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View view, MotionEvent motionEvent) {
				preview.focusOnTouch(motionEvent);
				//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
				return false;
			}
		});

		Toast.makeText(ctx, getString(R.string.take_photo_help), Toast.LENGTH_LONG).show();
		
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
		
		serverAddress = SP.getString("serveraddress", "10.0.0.13:3000");
        deviceName = SP.getString("devicename", "1");
        if (serverAddress.isEmpty() || deviceName.isEmpty()) {
//            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            showToast("Server hostname/ip or device name is empty");
//            startActivity(intent);
            finish();
        }
        try {
            mSocket = IO.socket("http://" + serverAddress);
            Log.e(this.getClass().getCanonicalName(), "trying to connect with server " + serverAddress + "");
            if (!mSocket.connected())
                mSocket.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        mSocket.on("takeShot", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                	Log.e("takeShot Aargument", args[0]+"");
                	showToast("Got data from server on " + args[0]);
                	StringTokenizer strToken = new StringTokenizer((String) args[0], ":");
                	if(strToken.countTokens()>=2){
                		key = strToken.nextToken();
                		if(strToken.hasMoreTokens()){
                			shotTimeStamp = strToken.nextToken();
                			serverTime = strToken.nextToken();
                			showToast("Got trigger on " + deviceName);
                			long curTime = System.currentTimeMillis();
                			long travelTime = Long.parseLong(serverTime) - curTime;
                			long timeDifference = Long.parseLong(shotTimeStamp) - curTime - travelTime;
                			Log.d(TAG, "time difference between server and client"+timeDifference);
                			showToast("time difference between server and client"+timeDifference);
                            new Timer().schedule(new TimerTask() {
								
								@Override
								public void run() {
									// TODO Auto-generated method stub
									if(camera!=null) {
		                            	//camera.takePicture(shutterCallback, rawCallback, jpegCallback);
		                            }
								}
							}, timeDifference);
                		}
                	}
                    
                }
                
            }
        });
        mSocket.on("upload", new Emitter.Listener() {

            @Override
            public void call(Object... args) {
                if (args.length > 0) {
                    key = (String) args[0];
                    Log.e("upload key", key);
                }
                runOnUiThread(new Runnable() {
					
					@Override
					public void run() {
						 	File file;
							final StringBuilder fileNameSB = new StringBuilder();
							File sdCard = Environment.getExternalStorageDirectory();
							File dir = new File (sdCard.getAbsolutePath() + "/autocam");
							if(key.isEmpty()) {
								return;
				            }else{
				            	fileNameSB.append(key+"_"+deviceName+".jpg");
				            }
							
							file = new File(dir, fileNameSB.toString());
							if(!file.exists())
								return;
			                RequestParams params = new RequestParams();	
			                try {
			                    params.put("camerano",deviceName);
			                    params.put("key", key);
			                    params.put("image", file);
			                } catch (FileNotFoundException e1) {
			                    // TODO Auto-generated catch block
			                    e1.printStackTrace();
			                }
						// TODO Auto-generated method stub
						client.post("http://" + serverAddress + "/upload?timestamp"+System.currentTimeMillis(), params, new TextHttpResponseHandler() {

		                    @Override
		                    public void onSuccess(int arg0, Header[] arg1, String arg2) {
		                        // TODO Auto-generated method stub
		                        if (arg0 == 201) {
		                            showToast("image uploaded sucessfully " + fileNameSB.toString());
		                        }

		                    }

		                    @Override
		                    public void onFailure(int arg0, Header[] arg1, String arg2, Throwable arg3) {
		                        // TODO Auto-generated method stub

		                    }

		                });

					}
				});
            }
        });

	}

	@Override
	protected void onResume() {
		super.onResume();
		int numCams = Camera.getNumberOfCameras();
		if(numCams > 0){
			try{
				camera = Camera.open(1);
				camera.startPreview();
				preview.setCamera(camera);
			} catch (RuntimeException ex){
				Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
			}
		}
		if (client == null) {
            client = new AsyncHttpClient();
            client.setResponseTimeout(60000);
            client.setConnectTimeout(60000);
            client.setTimeout(60000);
            client.setMaxConnections(10);
        }
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("android.intent.action.ACTION_POWER_DISCONNECTED");
		mReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
				boolean isCharging = (status == BatteryManager.BATTERY_STATUS_CHARGING);
				
				int chargePlug = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
				boolean usbCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_USB;
				boolean acCharge = chargePlug == BatteryManager.BATTERY_PLUGGED_AC;
				Toast.makeText(CamTestActivity.this, "charge disconnect connect broadcast receiver", Toast.LENGTH_LONG).show();
				if(!isCharging){
					if(camera!=null) {
                    	camera.takePicture(shutterCallback, rawCallback, jpegCallback);
                    }
				}
				
			}
		};
		// registering our receiver
		this.registerReceiver(mReceiver, intentFilter);
		// Get updates from the accelerometer and magnetometer at a constant rate.
	    // To make batch operations more efficient and reduce power consumption,
	    // provide support for delaying updates to the application.
	    //
	    // In this example, the sensor reporting delay is small enough such that
	    // the application receives an update before the system checks the sensor
	    // readings again.
	    mSensorManager.registerListener(this, Sensor.TYPE_ACCELEROMETER,
	      SensorManager.SENSOR_DELAY_NORMAL);
	    mSensorManager.registerListener(this, Sensor.TYPE_MAGNETIC_FIELD,
	      SensorManager.SENSOR_DELAY_NORMAL);
		mAudioManager =  (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mReceiverComponent = new ComponentName(this,YourBroadcastReceiver.class);
		mAudioManager.registerMediaButtonEventReceiver(mReceiverComponent);
	}

	@Override
	protected void onPause() {
		if(camera != null) {
			camera.stopPreview();
			preview.setCamera(null);
			camera.release();
			camera = null;
		}
//		if (mSocket.connected()) {
//            mSocket.disconnect();
//        }
		super.onPause();
		//unregister our receiver
		this.unregisterReceiver(this.mReceiver);
		mAudioManager.unregisterMediaButtonEventReceiver(mReceiverComponent);
	}

	private void resetCam() {
		camera.startPreview();
		preview.setCamera(camera);
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
						 Log.d(TAG, "onShutter'd");
		}
	};
	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
						 Log.d(TAG, "onPictureTaken - raw");
		}
	};
	PictureCallback jpegCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			new SaveImageTask().execute(data);
			resetCam();
			Log.d(TAG, "onPictureTaken - jpeg");
		}
	};
	
	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File outFile;
				String fileName;
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File (sdCard.getAbsolutePath() + "/autocam");
				if(key.isEmpty()) {
					long timeStamp = new Date().getTime()/1000;
					fileName = "pic_"+deviceName+"_"+timeStamp+".jpg";
	            }else{
	            	fileName =  key+"_"+deviceName+".jpg";
	            }
				dir.mkdirs();				
				
				outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

				refreshGallery(outFile);
				mSocket.emit("shotCompleted", key);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}
	
	 /**
     * Shows a {@link Toast} on the UI thread.
     *
     * @param text The message to show
     */
    private void showToast(final String text) {
        final Activity activity = this;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * onSensorChanged function called when sensor value changes
     */
    public void onSensorChanged(SensorEvent event){
		if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)) {
			System.arraycopy(event.values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
		} else if (event.sensor == mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)) {
			System.arraycopy(event.values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
		}
		computeOrientation();
	}

	@Override
	public void onAccuracyChanged(int sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}
	private void computeOrientation() {
	    if ( SensorManager.getRotationMatrix(mRotationMatrix, null,
	    	      mAccelerometerReading, mMagnetometerReading)) {
	    	// "mRotationMatrix" now has up-to-date information.

	        SensorManager.getOrientation(mRotationMatrix, mOrientationAngles);

	        // "mOrientationAngles" now has up-to-date information.

	        /* 1 radian = 57.2957795 degrees */
	        /* [0] : yaw, rotation around z axis
	         * [1] : pitch, rotation around x axis
	         * [2] : roll, rotation around y axis */
	        float yaw = mOrientationAngles[0] * 57.2957795f;
	        float pitch = mOrientationAngles[1] * 57.2957795f;
	        float roll = mOrientationAngles[2] * 57.2957795f;
			String htmlText = "";
			htmlText += "X Axis - "+df.format(pitch)+"<br/>";
			htmlText += "Y Axis - "+df.format(roll)+"<br/>";
			htmlText += "Z Axis - "+df.format(yaw)+"<br/>";
	        mTextViewGyro.setText(Html.fromHtml(htmlText));  
	    }
	}

	@Override
	public void onSensorChanged(int sensor, float[] values) {
		// TODO Auto-generated method stub
		if (sensor == Sensor.TYPE_ACCELEROMETER) {
			System.arraycopy(values, 0, mAccelerometerReading, 0, mAccelerometerReading.length);
		} else if (sensor ==Sensor.TYPE_MAGNETIC_FIELD) {
			System.arraycopy(values, 0, mMagnetometerReading, 0, mMagnetometerReading.length);
		}
		computeOrientation();
		
	}

	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
		
	}

	public static class YourBroadcastReceiver extends BroadcastReceiver{

		// Constructor is mandatory
		public YourBroadcastReceiver ()
		{
			super ();
		}
		@Override
		public void onReceive(Context context, Intent intent) {
			String intentAction = intent.getAction();
			Log.i (TAG, intentAction.toString() + " happended");
			if (!Intent.ACTION_MEDIA_BUTTON.equals(intentAction)) {
				Log.i (TAG, "no media button information");
				return;
			}
			KeyEvent event = (KeyEvent)intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
			if (event == null) {
				Log.i (TAG, "no keypress");
				return;
			}
			// other stuff you want to do
			if(event.getAction()==KeyEvent.ACTION_UP){
				camTestActivity.camera.takePicture(camTestActivity.shutterCallback,camTestActivity.rawCallback,camTestActivity.jpegCallback);
			}
		}
	}
}


