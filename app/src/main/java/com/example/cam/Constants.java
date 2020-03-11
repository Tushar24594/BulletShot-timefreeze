package com.example.cam;

public class Constants {

	
	public static final int REQUEST_TAKE_PHOTO = 1;
	public static final int REQUEST_SHARE_PHOTO = 2;
	
	public static final String PATH_EXTRA = "photopath";
	public static final String RFID_EXTRA = "rdif";
	
	public static final String TAGGLABS_ACTION_URL="http://getsocial.tagglabs.in";
	public static final String TAGGLABS_GETSOCIAL_API_URL="http://socialapi.tagglabs.in";
	
	public static final String SLUG_OF_CAMPAIGN="fitbit-sc";
	public static final String CAMPAING_PASSCODE="f1tb1tp3hn0khudbh4gj40";
	public static final String POST_SELFIE_READER_ID="NBA_photobooth-1";
	public static final String POST_SELFIE_READER_TYPE="hf";
	public static final String POST_CUSTOM_STATUS_READER_ID="video";
	public static final String POST_CUSTOM_STATUS_READER_TYPE="hf";
	
	public static final String UPLOAD_IMAGE_ENDPOINT="action?platform=old";
	public static final String GET_READER_ENDPOINT="getReader?platform=old";
	
	public static final String UPLOAD_IMAGE_URL=String.format("%s/%s", TAGGLABS_ACTION_URL,UPLOAD_IMAGE_ENDPOINT);
	public static final String GET_READER_URL=String.format("%s/%s/%s&h=%s", TAGGLABS_GETSOCIAL_API_URL,SLUG_OF_CAMPAIGN,GET_READER_ENDPOINT,CAMPAING_PASSCODE);
	
	public static final String PARSE_APP_ID="RqVglgQPtj4exyVMKchWsFB2es7bXV4M54FHq6Mm";
	public static final String PARSE_CLIENT_KEY="ebj9VPJ8YXUGAx3M8vNRqj7SyoivngkyCbzuHDw9";
	
	public static final String SHARED_PREF_NAME="reader";
	public static final String READER_RESPONSE_CACHE_SHARED_PREF_KEY="readers";
	public static final String READER_ID_SHARED_PREF_KEY="readerId";
	public static final String READER_NAME_SHARED_PREF_KEY="readerName";
	public static final String READER_TYPE_SHARED_PREF_KEY="readerType";
	public static final String READER_SELECTED_POSITION_SHARED_PREF_KEY="readerselectedposition";
	
	public static final String PREFILLED_DATA_FILE_NAME="prefilleddata.csv";
	public static final String PHOTO_DIRECTORY_NAME="CheerStation";
	public static final String LOGO_URL = "";
	
	public static final String FIREBASE_USER_URL = "https://ik-test-project.firebaseio.com/Ferrero/Users/";
	public static final String FIREBASE_USER_EMAIL_KEY = "email";
	 /*
     * AWS access key to be provided by your administrator
     */
    public static final String AWS_ACCESS_KEY = "AKIAI6G5ZOVNWG2XLYGA";
    
    /*
     * AWS Secret key to be provided by your administrator
     */
    public static final String AWS_SECRET_KEY = "d5xSOEqqFFxaMjqt/Usr7c7UTKWg7gSeKTH//ooH";
    
    public static final String BUCKET_NAME = "videos.getsocial.tagglabs.in";
    
    public static final String AWS_SERVER_ADDRESS = "https://s3-ap-southeast-1.amazonaws.com/";
    
    public static final String AWS_VIDEO_URL = String.format("%s%s/", AWS_SERVER_ADDRESS,BUCKET_NAME);
    
    public static final String AWS_SHARE_VIDEO_URL = String.format("http://%s/",BUCKET_NAME);
	
	
}