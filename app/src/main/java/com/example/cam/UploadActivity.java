package com.example.cam;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.github.nkzawa.emitter.Emitter.Listener;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.FileAsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;

public class UploadActivity extends Activity {
	
	@BindView(R.id.editTextRfid)
	EditText mEditTextRfid;
	
	@BindView(R.id.buttonUpload)
	Button mButtonUpload;

	@BindView(R.id.buttonPost)
	Button mButtonPost;
	
	@BindView(R.id.buttonEmailGif)
	Button mButtonPostTwitter;
	
	@BindView(R.id.editTextStatus)
	EditText mEditTextStatus;
	
	@BindView(R.id.progressBar)
	ProgressBar mProgressBar;

	@BindView(R.id.imageButtonDownload)
	ImageButton mImageButtonDownload;
	
	/**
	 * Socket.io client socket for getting command from server
	 */
	private Socket mSocket;
	
	/**
	 * Server address to connect with, this is stored in shared preference and
	 * can be chaged by setting activity
	 */
	String serverAddress;
	
	/**
	 * Device name will be identifier for this device, scoker.io server will
	 * shoot this device name result to that picture will be clicked default
	 * name is camera1
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
	
	AmazonS3 s3;
	TransferUtility transferUtility;
	
	File sdCard,dir;
	
	String TAG = UploadActivity.class.getSimpleName();
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload);
		ButterKnife.bind(this);
		
		mProgressBar.setMax(100);
		
		SharedPreferences SP = PreferenceManager.getDefaultSharedPreferences(this);
		
		serverAddress = SP.getString("serveraddress", "");
        deviceName = SP.getString("devicename", "1");
        if (serverAddress.isEmpty() || deviceName.isEmpty()) {
        	mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"server address or device name is empty");
            Intent intent = new Intent(this, SettingsActivity.class);
            showToast("Server hostname/ip or device name is empty");
            startActivity(intent);
            finish();
        }
        
         try {
			sdCard = Environment.getExternalStorageDirectory();
			 dir = new File (sdCard.getAbsolutePath() + "/autocam");
			if(!dir.exists()){
				dir.mkdirs();
			}
		} catch (Exception e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
        try {
        	mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"trying to connect with server : http://" + serverAddress);
            mSocket = IO.socket("http://" + serverAddress);
            Log.e(this.getClass().getCanonicalName(), "trying to connect with server " + serverAddress + "");
            if (!mSocket.connected()){
                mSocket.connect();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
     // Initializes TransferUtility, always do this before using it.
     	transferUtility = Util.getTransferUtility(this);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (client == null) {
            client = new AsyncHttpClient();
            client.setResponseTimeout(60000);
            client.setConnectTimeout(60000);
            client.setTimeout(60000);
            client.setMaxConnections(10);
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
    
    @OnClick(R.id.buttonUpload)
    public void generateGifNUpload(){
    	key = mEditTextRfid.getText().toString();
    	if(key.isEmpty()){
    		mEditTextRfid.setError("Enter RFID");
    		return;
    	}else{
    		mEditTextRfid.setError(null);
    	}
		if (mSocket.connected()) {
			mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"Sending genereate gif command to server");
			mSocket.emit("generateGif", key);
			mSocket.once("gifReadey", new Listener() {
				
				@Override
				public void call(Object... arg0) {
					final String fileName = (String) arg0[0];
					if (fileName.isEmpty()) {
						return;
					} else {
						
						final String url = "http://"+serverAddress+"/"+fileName;
						// TODO Auto-generated method stub
						final Activity activity = UploadActivity.this;
						if (activity != null) {
							activity.runOnUiThread(new Runnable() {
								@Override
								public void run() {
									mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"file name got from server is "+fileName);
									mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"downloading gif from server: "+url);
									File file= new File(dir, fileName);
									client.get(url,new FileAsyncHttpResponseHandler(file) {
										@Override
										public void onSuccess(int arg0, Header[] arg1, File arg2) {
											// TODO Auto-generated method stub
											//beginUpload(arg2.getAbsolutePath());
											mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"gif downloaded from server: "+arg2.getAbsolutePath());
											mProgressBar.setProgress(0);
										}
										
										@Override
										public void onProgress(long bytesWritten, long totalSize) {
											// TODO Auto-generated method stub
											super.onProgress(bytesWritten, totalSize);
											mProgressBar.setProgress((int) ((bytesWritten*100)/totalSize));
										}

										@Override
										public void onFailure(int arg0, Header[] arg1, Throwable arg2, File arg3) {
											// TODO Auto-generated method stub
											mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"unable to downloaded gif from server: ");
											
										}
									});
								}
							});
						}
					}
				}
			});
		}
    }
    
    @OnClick(R.id.buttonPost)
    public void uploadNPostToSocial(){
    	key = mEditTextRfid.getText().toString();
    	if(key.isEmpty()){
    		mEditTextRfid.setError("Enter RFID");
    		return;
    	}else{
    		mEditTextRfid.setError(null);
    	}
    	File file= new File(dir, "out_"+key+".mp4");
    	if(file.exists()){
    		beginUpload(file.getAbsolutePath());
    	}
    	
    }
    
    @OnClick(R.id.buttonEmailGif)
    public void sendEmail(){
    	key = mEditTextRfid.getText().toString();
    	if(key.isEmpty()){
    		mEditTextRfid.setError("Enter RFID");
    		return;
    	}else{
    		mEditTextRfid.setError(null);
    	}
    	final File file= new File(dir, "boomerang_"+key+".mp4");
    	mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+" check user email in case of non social account ");
    	if(file.exists()){
    		
    		Uri path = Uri.fromFile(file); 
			Intent emailIntent = new Intent(Intent.ACTION_SEND);
			// set the type to 'email'
			emailIntent .setType("vnd.android.cursor.dir/email");
			String to[] = {""};
			emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
			// the attachment
			emailIntent .putExtra(Intent.EXTRA_STREAM, path);
			// the mail subject
			emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Celebrating Our Partnership");
			emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,"Hello,"
								+"\n\nCapturing memories of a remarkable evening where we are celebrating our partnership."
                                +"\n\nWe welcome you all to GM Modular family."
                                );
			startActivity(Intent.createChooser(emailIntent , "Send email..."));	
    		/*client.get(Constants.FIREBASE_USER_URL+key+".json", new JsonHttpResponseHandler(){

				@Override
				public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
					// TODO Auto-generated method stub
					super.onFailure(statusCode, headers, responseString, throwable);
					if(statusCode==404){
						mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+" this RFID is not registered with us, please check ");
					}
				}

				@Override
				public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
					// TODO Auto-generated method stub
					super.onSuccess(statusCode, headers, response);
					if(response==null){
						mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+" this RFID is not registered with us, please check ");
					}else if( response.has(Constants.FIREBASE_USER_EMAIL_KEY)){
						try {
							String email = response.getString(Constants.FIREBASE_USER_EMAIL_KEY);
							mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+" this RFID is registered with us email id="+email);
							mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"open email client");
							Uri path = Uri.fromFile(file); 
							Intent emailIntent = new Intent(Intent.ACTION_SEND);
							// set the type to 'email'
							emailIntent .setType("vnd.android.cursor.dir/email");
							String to[] = {email};
							emailIntent .putExtra(Intent.EXTRA_EMAIL, to);
							// the attachment
							emailIntent .putExtra(Intent.EXTRA_STREAM, path);
							// the mail subject
							emailIntent .putExtra(Intent.EXTRA_SUBJECT, "Precious Diwali with Ferrero Rocher");
							emailIntent.putExtra(android.content.Intent.EXTRA_TEXT,"Hi,"
												+"\n\nKindly find attached your Time Freeze moment.");
							startActivity(Intent.createChooser(emailIntent , "Send email..."));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
							mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+" this RFID is not registered with us, please check ");
						}
					}
				}
    			
    		});
    	*/}
    	
    }

    @OnClick(R.id.imageButtonDownload)
	public void downloadImage(){
		key = mEditTextRfid.getText().toString();
		if(key.isEmpty()){
			mEditTextRfid.setError("Enter RFID");
			return;
		}else{
			mEditTextRfid.setError(null);
		}
		final String fileName = "adidas_"+key+".mp4";
		if (fileName.isEmpty()) {
			return;
		} else {

			final String url = "http://"+serverAddress+"/"+fileName;
			// TODO Auto-generated method stub
			final Activity activity = UploadActivity.this;
			if (activity != null) {
				activity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"file name got from server is "+fileName);
						mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"downloading mp4 from server: "+url);
						File file= new File(dir, fileName);
						client.get(url,new FileAsyncHttpResponseHandler(file) {
							@Override
							public void onSuccess(int arg0, Header[] arg1, File arg2) {
								// TODO Auto-generated method stub
								//beginUpload(arg2.getAbsolutePath());
								mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"mp4 downloaded from server: "+arg2.getAbsolutePath());
								mProgressBar.setProgress(0);
							}

							@Override
							public void onProgress(long bytesWritten, long totalSize) {
								// TODO Auto-generated method stub
								super.onProgress(bytesWritten, totalSize);
								mProgressBar.setProgress((int) ((bytesWritten*100)/totalSize));
							}

							@Override
							public void onFailure(int arg0, Header[] arg1, Throwable arg2, File arg3) {
								// TODO Auto-generated method stub
								mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"unable to downloaded mp4 from server: ");

							}
						});
					}
				});
			}
		}
	}
    /*
	 * Begins to upload the file specified by the file path.
	 */
	private void beginUpload(String filePath) {
		if (filePath == null) {
			Toast.makeText(this, "Could not find the filepath of the selected file", Toast.LENGTH_LONG).show();
			mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"Could not find the file");
			return;
		}
		File file = new File(filePath);
		String fileName = Constants.SLUG_OF_CAMPAIGN+"/"+key + "_" + System.currentTimeMillis()/1000 + "." +this.getFileExtension(file);
		TransferObserver observer = transferUtility.upload(Constants.BUCKET_NAME,
				fileName, file);
		observer.setTransferListener(new UploadListener(fileName));
		mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"Start uploading mp4 to Amazon s3 ");
	}
	
	private String getFileExtension(File file) {
		String name = file.getName();
		try {
			return name.substring(name.lastIndexOf(".")+1);

		} catch (Exception e) {
			return "";
		}

	}
	/*
	 * A TransferListener class that can listen to a upload task and be notified
	 * when the status changes.
	 */
	private class UploadListener implements TransferListener {

		String path;

		public UploadListener(String path) {
			super();
			this.path = path;
		}

		// Simply updates the UI list when notified.
		@Override
		public void onError(int id, Exception e) {
			Log.e(TAG, "Error during upload: " + id, e);
			mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"Error during upload mp4 to Amazon s3 ");
		}

		@Override
		public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
			long percentage = bytesCurrent*100/bytesTotal;
			Log.e(TAG, "Amazon upload progress written "+bytesCurrent+" from "+bytesTotal+" percentage"+ percentage);
			mProgressBar.setProgress((int) percentage);
		}

		@Override
		public void onStateChanged(int id, TransferState newState) {
			if (newState == TransferState.COMPLETED) {
				mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"upload to amazon s3 completed ");
				new SetMimeTypeAsyncTask().execute(this.path);
			}
		}
	}
	
	class SetMimeTypeAsyncTask extends AsyncTask<String, Boolean, String>{

		
		
		@Override
		protected void onPreExecute() {
			// TODO Auto-generated method stub
			super.onPreExecute();
			mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"setting metadata for amazon");
		}

		@Override
		protected String doInBackground(String... params) {
			AmazonS3Client amazonS3Client = Util.getS3Client(UploadActivity.this);
			ObjectMetadata oldObjectMetadata= amazonS3Client.getObjectMetadata(Constants.BUCKET_NAME, params[0]);
			oldObjectMetadata.setContentType("video/"+(params[0].substring(params[0].lastIndexOf(".")+1)));
			final CopyObjectRequest request = new CopyObjectRequest(Constants.BUCKET_NAME, params[0], Constants.BUCKET_NAME, params[0])
	        .withSourceBucketName( Constants.BUCKET_NAME )
	        .withSourceKey(params[0])
	        .withNewObjectMetadata(oldObjectMetadata);

			amazonS3Client.copyObject(request);
			return params[0];
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			super.onPostExecute(result);
			mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"setting metadata for amazon completed");
			postStatus(result,key,true);
		}
	}
	
	
	
	public void postStatus(String fileName,String rfid,boolean fromAmazon) {
		// String url="http://getsocial.tagglabs.in/action?platform=old";
//		String rfid = fileName.replace(Constants.SLUG_OF_CAMPAIGN+"/", "");
		//StringTokenizer tokens = new StringTokenizer(rfid, "_");
		//rfid = tokens.nextToken();// this will contain "Fruit"
		String pictureUrl="";
		if(fromAmazon){
			pictureUrl = Constants.AWS_SHARE_VIDEO_URL + fileName;
		}else{
			 pictureUrl = Constants.TAGGLABS_ACTION_URL+"/uploads/"+ Constants.SLUG_OF_CAMPAIGN + "/out_" + rfid + ".mp4";
		}
		
		RequestParams params = new RequestParams();
		params.put("reader_id", "android-bulletshot-gif-"+Constants.SLUG_OF_CAMPAIGN+"-"+android.text.format.DateFormat.format("ddMMyyyy", new java.util.Date()));
		params.put("identity", key);
		params.put("id", Constants.SLUG_OF_CAMPAIGN);
		params.put("passcode", Constants.CAMPAING_PASSCODE);
		params.put("request_id", new Date().getTime() + "");
		params.put("time", android.text.format.DateFormat.format("yyyy-MM-dd hh:mm:ss", new java.util.Date()));
		params.put("reader_action", Constants.POST_CUSTOM_STATUS_READER_ID);
		params.put("reader_type", Constants.POST_CUSTOM_STATUS_READER_TYPE);
//		params.put("vars",
//				"{\"name\":\"" + "Test Name" + "\",\"message\":\"" + "Test Message" + "\",\"picture\":\"" + Constants.LOGO_URL
//						+ "\",\"description\":\"I selected door no "
//						+ "\",\"caption\":\"Tagglabs BulletShot\",\"link\":\"" + String.format("%s%s", Constants.AWS_VIDEO_URL, fileName) + "\"}");
		params.put("vars",
				"{\"message\":\"" + "#GetSetFreeze @RMFIndia" + "\",\"caption\":\"" + "" + "\",\"video\":\"" + pictureUrl
						+ "\",\"tweet\":\"#GetSetFreeze @RMF_India "
						+ "\"}");
//		File file= new File(dir, "out_"+key+".mp4");
//    	if(file.exists()){
//			
//			try {
//				params.put("photo", file);
//			} catch (FileNotFoundException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//    	}
    	mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"posting to getsocial api for RFID "+rfid);
		client.post(Constants.UPLOAD_IMAGE_URL, params, new JsonHttpResponseHandler() {

			@Override
			public void onStart() {
				// TODO Auto-generated method stub
				super.onStart();
			}

			
			@Override
			public void onProgress(long bytesWritten, long totalSize) {
				// TODO Auto-generated method stub
				super.onProgress(bytesWritten, totalSize);
				Log.e(TAG, (bytesWritten*100)/totalSize+"");
				Log.e(TAG, "GetSocial upload progress written "+bytesWritten/8+" from "+totalSize/8+" percentage"+ (bytesWritten*100)/totalSize);
				mProgressBar.setProgress((int) ((bytesWritten*100)/totalSize));
			}



			@Override
			public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONArray errorResponse) {
				// TODO Auto-generated method stub
				super.onFailure(statusCode, headers, throwable, errorResponse);
				System.out.println(errorResponse.toString());
			}

			@Override
			public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
				// TODO Auto-generated method stub
				super.onFailure(statusCode, headers, throwable, errorResponse);
				if (errorResponse != null)
					System.out.println(errorResponse.toString());
				
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
				// If the response is JSONObject instead of expected
				// JSONArray
				System.out.println(response.toString());
				mEditTextStatus.setText(mEditTextStatus.getText()+"\n"+"Sucessfully posted on social account \n"+response.toString());
			}

			@Override
			public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
				// Pull out the first event on the public timeline

			}
		});

	}
}
