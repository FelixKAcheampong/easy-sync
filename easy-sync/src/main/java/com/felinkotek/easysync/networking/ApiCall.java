package com.felinkotek.easysync.networking;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.felinkotek.easysync.annotations.EasySync;
import com.felinkotek.easysync.syncmodel.EasySyncError;
import com.felinkotek.easysync.syncmodel.SyncItem;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.HashMap;
import java.util.Map;

public class ApiCall {
    private static final int MAX_RETRIES = 3000;

    public void downloadData(Context context, String url, DownloadFinish downloadFinish,
                             HashMap<String,String> headers, SyncItem syncItem) {
        EasySync easySync = syncItem.getSyncClass().getAnnotation(EasySync.class) ;
        Class<?> aClass = syncItem.getSyncClass() ;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        if(!easySync.arrayKey().isEmpty()) {
            JsonObjectRequest objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
                try {
                    downloadFinish.onSuccess(response.getJSONArray(easySync.arrayKey()));
                } catch (JSONException e) {
                    e.printStackTrace();
                    downloadFinish.onFailure(new EasySyncError(aClass, e.toString()));
                }
            }, error -> downloadFinish.onFailure(new EasySyncError(aClass, error.toString()))) {
                @Override
                public Map<String, String> getHeaders() {
                    if (headers != null) return headers;
                    else {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/x-www-form-urlencoded");

                        return headers;
                    }
                }
            };
            objectRequest.setRetryPolicy(new DefaultRetryPolicy(MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(objectRequest);
        }else{
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
                downloadFinish.onSuccess(response);
            }, error -> {
                downloadFinish.onFailure(new EasySyncError(aClass,error.toString())) ;
            }){
                @Override
                public Map<String, String> getHeaders() {
                    if (headers != null) return headers;
                    else {
                        HashMap<String, String> headers = new HashMap<>();
                        headers.put("Content-Type", "application/x-www-form-urlencoded");

                        return headers;
                    }
                }
            } ;

            jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(jsonArrayRequest);
        }
    }

    public interface DownloadFinish {
        void onSuccess(JSONArray payloadResponse);

        void onFailure(EasySyncError easySyncError);
    }
}
