package com.felinkotek.easysync;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.room.Delete;
import androidx.room.OnConflictStrategy;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.felinkotek.easysync.annotations.Map;
import com.felinkotek.easysync.annotations.Parser;
import com.felinkotek.easysync.networking.ApiCall;
import com.felinkotek.easysync.syncmodel.EasySyncError;
import com.felinkotek.easysync.syncmodel.EasySyncItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Create By Felix K. Acheampong
 * {@github}https://github.com/FelixKAcheampong/easy-sync
 */
@SuppressWarnings("DanglingJavadoc")
public class EasySync {
    private Context context ;
    private SyncListener syncListener ;
    private ProgressDialog progressDialog ;
    private SQLiteDatabase database ;
    private static final String DATE_FORMAT = "yyyy-MM-dd" ;
    private static final String TIME_STATMP_FORMAT = "yyyy-MM-dd'T'HH:mm" ;
    private String loadingMessage = null ;
    private boolean showProgress = true ;
    private boolean setProgressCancelable = false ;
    private List<EasySyncItem> syncItems ;
    private HashMap<String,String> headers ;
    private ApiCall apiCall ;
    private boolean showInsertLog = false;
    private List<EasySyncError> syncErrors = new ArrayList<>() ;
    private SupportSQLiteDatabase roomDatabase ;
    private String postBaseUrl ;
    private boolean clearDataBeforeSync = false ;

    /**
     * Constructor accepts Context
     * @param context
     */
    public EasySync(Context context){
        /** Initialize context */
        this.context = context ;
        /** Initialize progressDialog */
        this.progressDialog = new ProgressDialog(context) ;
        /** Set to cancelable */
        this.progressDialog.setCancelable(isSetProgressCancelable());
        apiCall = new ApiCall() ;
    }

    public ProgressDialog getProgressDialog() {
        return progressDialog;
    }

    /**
     * Start syncing progress after setting and initializing all Objects
     */
    public void startEasySync(){
        /** Set dialog message */
        if(getLoadingMessage()!=null) this.progressDialog.setMessage(getLoadingMessage());
        else this.progressDialog.setMessage("Loading...");

        if(getDatabase()==null && getRoomDatabase()==null){
            syncListener.onFatalError("No db found");
            return ;
        }
        /** Check if user has set class Annotated with {@LiveSync} */
        if(syncItems==null){
            /** Dismiss progress dialog */
            this.progressDialog.dismiss();
            /** Notify user of the situation */
            if(syncListener!=null) syncListener.onFatalError("No classes set");
            /** Stop code execution */
            return ;
        }

        /** Show progress dialog*/
        if(isShowProgress())  this.progressDialog.show();

        /** Initialize ApiCall Object from {@datasyncer} library */
        startDownload(0);
    }

    private void startDownload(int index){
        new Thread(new Runnable() {
            @Override
            public void run() {
                if(syncListener!=null){
                    if(index<syncItems.size()-1) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                syncListener.onQueueChanged(index,syncItems.get(index)) ;
                            }
                        }) ;
                    }
                }
                /**
                 * Start downloading data
                 */
                com.felinkotek.easysync.annotations.EasySync easySync = syncItems.get(index).getSyncClass().getAnnotation(com.felinkotek.easysync.annotations.EasySync.class) ;
                if(easySync==null){
                    if(index>=syncItems.size()-1) {
                        /** Dismiss progress dialog */
                        progressDialog.dismiss();
                        syncErrors.add(new EasySyncError(syncItems.get(index).getSyncClass(),"Class not annotated with @EasySync")) ;
                        if (syncListener != null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    syncListener.onComplete(syncErrors.size() >0, syncErrors);
                                }
                            }) ;
                        }
                    }else startDownload(index+1);
                    return ;
                }
                String tableName = (easySync.tableName().trim().equals("")) ? syncItems.get(index).getSyncClass().getSimpleName() : easySync.tableName();

                /** Check if item url is null and postbase url not nulll */
                if(syncItems.get(index).getUrl()==null && getPostBaseUrl()!=null){
                    /** Set url using main postBaseUrl */
                    syncItems.get(index).setUrl(getPostBaseUrl()) ;
                }
                apiCall.downloadData(context, syncItems.get(index).getUrl(), new ApiCall.DownloadFinish() {
                    @Override
                    public void onSuccess(JSONArray payloadResponse) {
                        /** User url is {@ Very super } and returned 200 http status code */
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    /** Save data */
                                    saveData(tableName,payloadResponse,syncItems.get(index),index);
                                }catch (JSONException e){
                                    e.printStackTrace();
                                    /** Dismiss progress dialog */
                                    progressDialog.dismiss();
                                    /** Notify user of if any json key provided cannot be found in the response payload */
                                    if(index>=syncItems.size()-1) {
                                        syncErrors.add(new EasySyncError(syncItems.get(index).getSyncClass(),e.toString())) ;
                                        if (syncListener != null) {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    syncListener.onComplete(syncErrors.size() >0, syncErrors);
                                                }
                                            });
                                        }
                                    }else startDownload(index+1);
                                }
                            }
                        }).start();
                    }

                    @Override
                    public void onFailure(EasySyncError error) {
                        /** Notify user of error returned by the server is {@syncListener} is set*/
                        if(index>=syncItems.size()-1) {
                            /** Dismiss progress dialog */
                            progressDialog.dismiss();
                            syncErrors.add(error) ;
                            if (syncListener != null) {
                                new Handler(Looper.getMainLooper()).post(new Runnable() {
                                    @Override
                                    public void run() {
                                        syncListener.onComplete(syncErrors.size() >0, syncErrors);
                                    }
                                }) ;
                            }
                        }else startDownload(index+1);
                    }
                },getHeaders(),syncItems.get(index));
            }
        }).start() ;
    }

    public void dismissDialog(){
        if(progressDialog!=null) progressDialog.dismiss();
    }

    /**
     * Save data from response payload
     * @param jsonArray response payload
     * @throws JSONException catch Exception
     */
    private void saveData(String tableName,JSONArray jsonArray,EasySyncItem easySyncItem,int index) throws JSONException {
        /** Get all class properties {@Fields} */
        Class<?> syncClass = easySyncItem.getSyncClass() ;
        Field[] fields = syncClass.getDeclaredFields() ;
        /** Get Annotation of type @EasySync */
        com.felinkotek.easysync.annotations.EasySync liveSync = syncClass.getAnnotation(com.felinkotek.easysync.annotations.EasySync.class) ;
        /** Check if class has been Annotated with @EasySync */
        if(liveSync!=null) {
            /** Loop through JSONArray */
            if(isClearDataBeforeSync()){
                String deleteQuery = "DELETE FROM "+tableName ;
                if(getDatabase()!=null) getDatabase().execSQL(deleteQuery);
                else getRoomDatabase().execSQL(deleteQuery);
            }
            int counter = 0 ;
            for(int i=0;i<jsonArray.length();i++) {
                JSONObject obj = jsonArray.getJSONObject(i) ;
                ContentValues contentValues = new ContentValues() ;
                /** Loop through each an every field in the class */
                for (Field field : fields) {
                    /** Get Annotation of type {@ @Live}*/
                    Map map = field.getAnnotation(Map.class);
                    /** Check if map is not null or if field has been Annotated with {@ @Live} */
                    if (map != null) {
                        /** Form column name query builder */
                        String fieldName ;
                        // check if column name is different from variable name
                        if(map.to().isEmpty()) fieldName = field.getName() ;
                        else fieldName = map.to() ;
                        /** Get data based on the JSON key specified by the user */
                        Object value = null;
                        if(map.key().contains("/")){
                            String[] keys = map.key().split("/") ;
                            JSONObject object = obj ;
                            int multiCounter=0 ;
                            do{
                                if(multiCounter==keys.length-1){
                                    value = getJsonValue(object, field.getType(), map);
                                    break;
                                }else{
                                    try {
                                        object = getMulti(object, keys[multiCounter]);
                                    }catch (JSONException e){
                                        break;
                                    }
                                }
                                multiCounter++ ;
                            }while (true) ;
                        }else value = getJsonValue(obj, field.getType(), map);
                        try {
                            if (field.getType().equals(Integer.class) || field.getType() == int.class) {
                                contentValues.put(fieldName, Integer.parseInt(value+""));
                            } else if ((field.getType().equals(Long.class) || field.getType() == long.class)) {
                                contentValues.put(fieldName, Long.parseLong(value+""));
                            } else if ((field.getType().equals(Double.class) || field.getType() == double.class)) {
                                contentValues.put(fieldName, Double.parseDouble(value+""));
                            } else if ((field.getType().equals(Float.class) || field.getType() == float.class)) {
                                contentValues.put(fieldName, Float.parseFloat(value+""));
                            } else if(field.getType().equals(Boolean.class) || field.getType() ==  boolean.class) {
                                contentValues.put(fieldName,Boolean.parseBoolean(value+""));
                            }else{
                                contentValues.put(fieldName, ((String) value).replace("'","`"));
                            }
                        }catch (NullPointerException e){e.printStackTrace();}
                        catch (ClassCastException e){e.printStackTrace();}
                    }
                }
                try{
                    /** Insert data into the table with the automatic query generation */
                    final int ct = counter;
                    if(getDatabase()!=null) {
                        long insert= easySyncItem.getDataExistsStrategy()!=null ? getDatabase().insertWithOnConflict(tableName,null,contentValues,easySyncItem.getDataExistsStrategy()) :
                                getDatabase().insert(tableName,null,contentValues) ;

                        if(insert>-1){
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(true,easySyncItem, jsonArray.length(), c);
                                }
                            });
                        }else{
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(false,easySyncItem, jsonArray.length(), c);
                                }
                            });
                        }
                    }else{
                        easySyncItem.setDataExistsStrategy(easySyncItem.getDataExistsStrategy()==null ? DataExistsStrategy.REPLACE : easySyncItem.getDataExistsStrategy()) ;
                        long insert= getRoomDatabase().insert(tableName,easySyncItem.getDataExistsStrategy(),contentValues) ;
                        if(insert>-1){
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(true,easySyncItem, jsonArray.length(), c);
                                }
                            });
                        }else{
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(false,easySyncItem, jsonArray.length(), c);
                                }
                            });
                        }
                    }

                }catch (SQLException e){
                    e.printStackTrace();
                    final int ct = counter;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        final int c = ct + 1;
                        if (syncListener != null) {
                            syncListener.onInsert(false,easySyncItem, jsonArray.length(), c);
                        }
                    });
                }
                counter++ ;
            }
            /** Dismiss loader */
            if(index>=syncItems.size()-1) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    this.progressDialog.dismiss();
                    if (syncListener != null)
                        syncListener.onComplete(syncErrors.size() >0, syncErrors);
                }) ;
            }else startDownload(index+1);

        }else{
            /** Notify user if class with Annotation {@SyncLive} is not set */
            if(index>=syncItems.size()-1) {
                /** Dismiss loader */
                new Handler(Looper.getMainLooper()).post(() -> {
                    this.progressDialog.dismiss();
                    if (syncListener != null)
                        syncListener.onComplete(syncErrors.size() >0, syncErrors);
                }) ;
            }else startDownload(index+1);
        }
    }

    private JSONObject getMulti(JSONObject data,String key) throws JSONException{
        return data.getJSONObject(key) ;
    }

    private Object invokeUserDefinedMethod(Class<?> c,String methodName,Object value){
        Object result = "" ;
        try {
            Method main = c.getDeclaredMethod(methodName, Object.class);
            main.isAccessible() ;
            result = (Object) main.invoke(c.newInstance(),value);
        } catch (NoSuchMethodException x) {
            x.printStackTrace();
        } catch (IllegalAccessException x) {
            x.printStackTrace();
        } catch (InvocationTargetException x) {
            x.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }

        return result ;
    }

    /**
     * Set class that has been Annotated with @EasySync to be used for syncing
     * @param syncItems @EasySync Annotated class
     * @return this class
     */
    public EasySync setSyncItems(List<EasySyncItem> syncItems){
        this.syncItems = syncItems ;

        return this ;
    }

    /**
     * Set listener for callbacks
     * @param syncListener  listener
     * @return this class
     */
    public EasySync setSyncListener(SyncListener syncListener){
        this.syncListener = syncListener ;

        return this ;
    }

    public EasySync setDatabase(SQLiteDatabase database) {
        this.database = database;

        return this ;
    }

    public SQLiteDatabase getDatabase(){
        return database ;
    }

    public String getLoadingMessage() {
        return loadingMessage;
    }

    public EasySync setLoadingMessage(String loadingMessage) {
        this.loadingMessage = loadingMessage;

        return this ;
    }

    public boolean isShowProgress() {
        return showProgress;
    }

    public EasySync showProgress(boolean showProgress) {
        this.showProgress = showProgress;

        return this ;
    }

    public boolean isSetProgressCancelable() {
        return setProgressCancelable;
    }

    public EasySync setProgressCancelable(boolean setProgressCancelable) {
        this.setProgressCancelable = setProgressCancelable;

        return this ;
    }

    public EasySync setHeaders(HashMap<String, String> headers) {
        this.headers = headers;

        return this ;
    }

    public HashMap<String,String> getHeaders(){
        return headers ;
    }

    public List<EasySyncItem> getSyncItems() {
        return syncItems;
    }

    public boolean getShowInsertLog() {
        return showInsertLog;
    }

    public EasySync showInsertLog(boolean insertError) {
        this.showInsertLog = insertError;
        return this ;
    }

    public SupportSQLiteDatabase getRoomDatabase() {
        return roomDatabase;
    }

    public EasySync setRoomDatabase(SupportSQLiteDatabase roomDatabase) {
        this.roomDatabase = roomDatabase;
        return this ;
    }

    public String getPostBaseUrl() {
        return postBaseUrl;
    }

    public EasySync setPostBaseUrl(String postBaseUrl) {
        this.postBaseUrl = postBaseUrl;

        return this ;
    }

    public boolean isClearDataBeforeSync() {
        return clearDataBeforeSync;
    }

    public EasySync setClearDataBeforeSync(boolean clearDataBeforeSync) {
        this.clearDataBeforeSync = clearDataBeforeSync;

        return this ;
    }

    /**
     * Interface for callbacks
     */
    public interface SyncListener{
        void onComplete(boolean onErrorFound,List<EasySyncError> easySyncErrors) ; // When sync is complete with no error
        void onFatalError(String errorMessage) ;
        default void onQueueChanged(int index, EasySyncItem syncItem){}
        default void onInsert(boolean isSuccess,EasySyncItem easySyncItem,int numberOfItems,int inserted) {} ;
    }

    @Nullable
    public Date getDate(String  date, String format)
    {
        SimpleDateFormat sdf = new SimpleDateFormat(format);

        try
        {
            if(date != null)
            {
                return sdf.parse(date);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * Get data from JSON based key
     * @param jsonObject JSON OBject
     * @param type Field Type
     * @param map LiveAnnotation
     * @return Object
     */
    private Object getJsonValue(JSONObject jsonObject, Class<?> type, @Nullable Map map) {
        /** Check if Map is not Null */
        if (map != null) {
            /** Get JSON key specified by user */
            String key = map.key();
            if(key.contains("/")){
                String[] split = key.split("/") ;
                key = split[split.length-1] ;
            }
            /** Initialize Object to null */
            Object fieldData = null;
            try {
                /** Check if field type is an Integer */
                if (type.equals(Integer.class) || type == int.class) {
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = String.valueOf(jsonObject.getInt(key));
                        else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                    } /** Get value as Integer */
                    else fieldData = Integer.parseInt(map.defaultValue());
                } else if (type.equals(Boolean.class) || type == boolean.class) { /** Check if field type is Float */
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = String.valueOf(jsonObject.getBoolean(key));
                        else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                    } /** Get value as Float */
                    else fieldData = Float.parseFloat(map.defaultValue());
                } else if (type.equals(Float.class) || type == float.class) { /** Check if field type is Float */
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = String.valueOf((float) jsonObject.getDouble(key));
                        else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                    } /** Get value as Float */
                    else fieldData = Float.parseFloat(map.defaultValue());
                } else if (type.equals(String.class) || type.equals(Date.class)) { /** Check if value is String */
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = jsonObject.getString(key);
                        else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                    } /** Get value as String */
                    else fieldData = map.defaultValue();
                } else if (type.equals(Double.class) || type == double.class) {
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = jsonObject.optDouble(key);
                        else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                    } else fieldData = Double.parseDouble(map.defaultValue());
                } else if (type.equals(Long.class) || type == long.class) { /** Check if value is Long */
                    if (jsonObject.has(key) && !jsonObject.isNull(key)) { /** Check if value is not Null */
                        /** Check if user want to convert long to Date */
                        if (map.toDate()) {
                            Date date = getDate(jsonObject.getString(key), DATE_FORMAT);
                            if (date != null) {
                                fieldData = date.getTime();
                            } else {
                                fieldData = "";
                            }
                        } else if (map.toDateTime()) { /** Else if to TimeStamp */
                            Date date = getDate(jsonObject.getString(key), TIME_STATMP_FORMAT);
                            if (date != null) {
                                fieldData = date.getTime();
                            } else {
                                fieldData = "";
                            }
                        } else {
                            /** Then get the value as raw long */
                            fieldData = String.valueOf(jsonObject.getLong(key));
                        }
                    }else if(!map.defaultValue().trim().equals("")) fieldData = map.defaultValue() ;
                }
            } catch (JSONException e) { /** Catch JSON Exception */
                e.printStackTrace();
            } catch (NullPointerException e) { /** Catch Any Null Pointers to avoid system crash */
                e.printStackTrace();
            }

            Parser parser = map.parser();
            if (parser.aClass() != Map.class && !parser.methodName().isEmpty()) {
                fieldData = invokeUserDefinedMethod(parser.aClass(), parser.methodName(), fieldData);
            }
            /** Do not save any null value */
            if (fieldData == null || fieldData.equals("null")) {
                fieldData = null;
            }
            return fieldData; /** Return result */
        } else {
            return null; /** Else return empty string */
        }
    }

    public static HashMap<String,Object> getBody(String... strings){
        HashMap<String,Object> map = new HashMap<>() ;
        for(String string:strings){
            String[] explode = string.split("\\=") ;
            map.put(explode[0],explode[1]) ;
        }

        return map ;
    }
}
