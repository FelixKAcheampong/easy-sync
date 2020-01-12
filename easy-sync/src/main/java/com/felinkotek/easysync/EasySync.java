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

@SuppressWarnings("DanglingJavadoc")
public class EasySync {
    private Context context ;
    private SyncListener syncListener ;
    private ProgressDialog progressDialog ;
    private SQLiteDatabase database ;
    private static final String DATE_FORMAT = "yyyy-MM-dd" ;
    private static final String TIME_STATMP_FORMAT = "yyyy-MM-dd'T'HH:mm" ;
    private String loadingMessage ;
    private boolean showProgress = true ;
    private boolean setProgressCancelable = false ;
    private List<EasySyncItem> syncItems ;
    private HashMap<String,String> headers ;
    private ApiCall apiCall ;
    private boolean showInsertLog = false;
    private List<EasySyncError> syncErrors = new ArrayList<>() ;
    private SupportSQLiteDatabase roomDatabase ;

    /**
     * Constructor accepts Context
     * @param context
     */
    public EasySync(Context context){
        /** Initialize context */
        this.context = context ;
        /** Initialize progressDialog */
        this.progressDialog = new ProgressDialog(context) ;
        /** Set dialog message */
        if(getLoadingMessage()!=null) this.progressDialog.setMessage(getLoadingMessage());
        else this.progressDialog.setMessage("Loading...");
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
        this.progressDialog.show();

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
                                    syncListener.onComplete(syncErrors.size() < 1, syncErrors);
                                }
                            }) ;
                        }
                    }else startDownload(index+1);
                    return ;
                }
                String tableName = (easySync.tableName().trim().equals("")) ? syncItems.get(index).getSyncClass().getSimpleName() : easySync.tableName();

                apiCall.downloadData(context, syncItems.get(index).getUrl(), new ApiCall.DownloadFinish() {
                    @Override
                    public void onSuccess(JSONArray payloadResponse) {
                        /** User url is {@ Very super } and returned 200 http status code */
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    /** Save data */
                                    saveData(tableName,payloadResponse,syncItems.get(index).getSyncClass(),index);
                                }catch (JSONException e){
                                    /** Dismiss progress dialog */
                                    progressDialog.dismiss();
                                    /** Notify user of if any json key provided cannot be found in the response payload */
                                    if(index>=syncItems.size()-1) {
                                        syncErrors.add(new EasySyncError(syncItems.get(index).getSyncClass(),e.toString())) ;
                                        if (syncListener != null) {
                                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                                @Override
                                                public void run() {
                                                    syncListener.onComplete(syncErrors.size() < 1, syncErrors);
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
                                        syncListener.onComplete(syncErrors.size() < 1, syncErrors);
                                    }
                                }) ;
                            }
                        }else startDownload(index+1);
                    }
                },getHeaders(),syncItems.get(index));
            }
        }) ;
    }

    public void dismissDialog(){
        if(progressDialog!=null) progressDialog.dismiss();
    }

    /**
     * Save data from response payload
     * @param jsonArray response payload
     * @throws JSONException catch Exception
     */
    private void saveData(String tableName,JSONArray jsonArray,Class<?> syncClass,int index) throws JSONException {
        /** Get all class properties {@Fields} */
        Field[] fields = syncClass.getDeclaredFields() ;
        /** Get Annotation of type @EasySync */
        com.felinkotek.easysync.annotations.EasySync liveSync = syncClass.getAnnotation(com.felinkotek.easysync.annotations.EasySync.class) ;
        /** Check if class has been Annotated with @EasySync */
        if(liveSync!=null) {
            EasySyncItem easySyncItem = syncItems.get(index) ;
            /** Loop through JSONArray */
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
                        Object value = getJsonValue(obj, field.getType(), map);
                        try {
                            if (field.getType().equals(Integer.class) || field.getType() == int.class) {
                                contentValues.put(fieldName, (int) value);
                            } else if ((field.getType().equals(Long.class) || field.getType() == long.class)) {
                                contentValues.put(fieldName, (Long) value);
                            } else if ((field.getType().equals(Double.class) || field.getType() == double.class)) {
                                contentValues.put(fieldName, (Double) value);
                            } else if ((field.getType().equals(Float.class) || field.getType() == float.class)) {
                                contentValues.put(fieldName, (Float) value);
                            } else {
                                contentValues.put(fieldName, (String) value);
                            }
                        }catch (NullPointerException e){e.printStackTrace();}

                    }
                }
                try{
                    /** Insert data into the table with the automatic query generation */
                    if(getDatabase()!=null) {
                        long insert= getDatabase().insertWithOnConflict(tableName,null,contentValues,easySyncItem.getDataExistsStrategy()) ;

                        if(insert>-1){
                            final int ct = counter;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(tableName, jsonArray.length(), c);
                                }
                            });
                        }
                    }else{
                        long insert= getRoomDatabase().insert(tableName,easySyncItem.getDataExistsStrategy(),contentValues) ;
                        if(insert>-1){
                            final int ct = counter;
                            new Handler(Looper.getMainLooper()).post(() -> {
                                final int c = ct + 1;
                                if (syncListener != null) {
                                    syncListener.onInsert(tableName, jsonArray.length(), c);
                                }
                            });
                        }
                    }
                    if(getShowInsertLog()) Log.d("query_"+syncClass.getSimpleName(),"") ;
                }catch (SQLException e){
                    /** Notify user if authomatic query builder generation is wrong */
                    if(getShowInsertLog()) Log.d("error:"+syncClass.getSimpleName(),e.toString());
                    //Log.d("query:",queryBuilder.toString()) ;
                }
                if(syncListener!=null)syncListener.onInsert(tableName,jsonArray.length(),counter);
                counter++ ;
            }
            /** Dismiss loader */
            if(index>=syncItems.size()-1) {
                this.progressDialog.dismiss();
                if (syncListener != null) syncListener.onComplete(syncErrors.size()<1,syncErrors);
            }else startDownload(index+1);

        }else{
            /** Notify user if class with Annotation {@SyncLive} is not set */
            if(index>=syncItems.size()-1) {
                /** Dismiss loader */
                this.progressDialog.dismiss();
                if (syncListener != null)
                    syncListener.onComplete(syncErrors.size()<1,syncErrors);
            }else startDownload(index+1);
        }
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

    /**
     * Interface for callbacks
     */
    public interface SyncListener{
        void onComplete(boolean noErrorFound,List<EasySyncError> easySyncErrors) ; // When sync is complete with no error
        void onFatalError(String errorMessage) ;
        default void onQueueChanged(int index, EasySyncItem syncItem){}
        default void onInsert(String tableName,int numberOfItems,int in) {} ;
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
            /** Initialize Object to null */
            Object fieldData = null;
            try {
                /** Check if field type is an Integer */
                if (type.equals(Integer.class) || type == int.class) {
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = String.valueOf(jsonObject.getInt(key));
                    } /** Get value as Integer */
                    else fieldData = Integer.parseInt(map.defaultValue());
                } else if (type.equals(Float.class) || type == float.class) { /** Check if field type is Float */
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = String.valueOf((float) jsonObject.getDouble(key));
                    } /** Get value as Float */
                    else fieldData = Float.parseFloat(map.defaultValue());
                } else if (type.equals(String.class) || type.equals(Date.class)) { /** Check if value is String */
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = jsonObject.getString(key);
                    } /** Get value as String */
                    else fieldData = map.defaultValue();
                } else if (type.equals(Double.class) || type == double.class) {
                    if (map.defaultValue().equals("")) {
                        if (jsonObject.has(key) && !jsonObject.isNull(key))
                            fieldData = jsonObject.optDouble(key);
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
                        } else if (map.toDatetTime()) { /** Else if to TimeStamp */
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
                    }
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
