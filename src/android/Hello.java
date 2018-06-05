package com.example.plugin;

import org.apache.cordova.*;
import org.json.JSONArray;
import org.json.JSONException;
import android.util.Base64;
import android.util.Log;
import android.os.Environment;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.RandomAccessFile;
import android.content.Context;
import org.apache.cordova.*;
import java.io.File;
import android.content.pm.ActivityInfo;
import android.app.Activity;
import java.util.LinkedList;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;
import android.telephony.TelephonyManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.provider.Settings;
import android.content.Intent;
import android.os.Build;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.content.pm.ApplicationInfo;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import android.view.Window;
import android.view.WindowManager;
import android.net.Uri;
import org.apache.cordova.CallbackContext;

import android.content.ContentUris;
import android.content.CursorLoader;
import android.database.Cursor;
import android.os.Build;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import org.apache.cordova.PluginResult;

import android.app.Activity;

import org.apache.cordova.CordovaPlugin;

import java.net.URISyntaxException;


public class Hello extends CordovaPlugin {
    int PICK_REQUEST_CODE = 0;
    private Imei imei = null;
    private Directory directory = null;
    private MergeFileUtils mergeFileUtil = null;
    private OrientationUtil orientationUtil = null;
    private Util util = null;
    private fileSizeUtil filesizeutil = null;
    private final int BUFFER_SIZE = 8192; // 8KB
    private final int HEADER_SIZE =8;
    public static String DIR_NAME = "/Pictures/";
    public static String DOWNLOADS_DIR_NAME = "/Downloads/";
    private CryptoUtils mCryptoUtils = null;
    Context mContext;
    Activity activity;
    CallbackContext callback;
    @Override
    public boolean execute(final String action, final JSONArray data, final CallbackContext callbackContext) throws JSONException {
	    if(action.equals("fileChooser")) {
            this.cordova.getThreadPool().execute(new Runnable() {
                @Override
                public void run() {
                    fileChooser(callbackContext);
                }
            });
            return true;
        }
	else if(action.equals("fileSize")){
	   activity = this.cordova.getActivity();
            if(filesizeutil == null){
                filesizeutil = new fileSizeUtil();
            }
            String filesize = data.getString(0);
            Log.d("Vivikta_Vlearn","filesize: "+filesize);

            int fileSizeMb = filesizeutil.fileSize(filesize);
	    if(fileSizeMb > 0){
	         callbackContext.success(fileSizeMb);
            }
            else {
		callbackContext.error("unable to get file size");
	    }
            return true;
	}
        return false;
    }
    
    public void fileChooser(CallbackContext callbackContext) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
        Intent chooser = Intent.createChooser(intent, "Select File");

        cordova.startActivityForResult(this,chooser, PICK_REQUEST_CODE);
        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }
        
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (requestCode == PICK_REQUEST_CODE && callback != null) {
            if (resultCode == Activity.RESULT_OK) {

                Uri uri = null;
                if (intent != null) {
                    uri = intent.getData();
                    String path = getFilePath(this.cordova.getActivity().getApplicationContext(), uri);
		    if( path != null){
			 callback.success(path);                   	
		    }
		    else{
			callback.error("Unable to get path");
		    }
                }
            }
        }
        else if (resultCode == Activity.RESULT_CANCELED) {
            PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
            callback.sendPluginResult(pluginResult);

        } else {

            callback.error(resultCode);
        }
    }

   public static String getFilePath(Context context, Uri fileUri) {
        String realPath;
        // SDK < API11
        if (Build.VERSION.SDK_INT < 11) {
            realPath = getRealPathFromURI_BelowAPI11(context, fileUri);
        }
        // SDK >= 11 && SDK < 19
        else if (Build.VERSION.SDK_INT < 19) {
            realPath = getRealPathFromURI_API11to18(context, fileUri);
        }
        // SDK > 19 (Android 4.4) and up
        else {
            realPath = getRealPathFromURI_API19(context, fileUri);
        }
        return realPath;
    }



    public static String getRealPathFromURI_API11to18(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        String result = null;

        CursorLoader cursorLoader = new CursorLoader(context, contentUri, proj, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();

        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
        }
        return result;
    }

    public static String getRealPathFromURI_BelowAPI11(Context context, Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = context.getContentResolver().query(contentUri, proj, null, null, null);
        int column_index = 0;
        String result = "";
        if (cursor != null) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            result = cursor.getString(column_index);
            cursor.close();
            return result;
        }
        return result;
    }

    public static String getRealPathFromURI_API19(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                return null;
            }
        }
        //contacts
        else if(isContacts(uri)){
            return null;

        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

    public static boolean isContacts(Uri uri){
        return "com.android.contacts".equals(uri.getAuthority());
    }

    public class fileSizeUtil{
        public int fileSize(String filePath){
           Log.d("Vivikta_Vlearn","Trying to get the filesize...");
           try{
                File file = new File(filePath);
                long length = file.length();
                int size = (int) length;
		return size;
            }catch(Exception e){
               return 0;
            }
        }
    }   
}

