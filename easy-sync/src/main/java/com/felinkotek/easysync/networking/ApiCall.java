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
import com.felinkotek.easysync.syncmodel.EasySyncItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class ApiCall {
    private static final int MAX_RETRIES = 3000;

    public void downloadData(Context context, String url, DownloadFinish downloadFinish,
                             HashMap<String,String> headers, EasySyncItem syncItem) {
        EasySync easySync = syncItem.getSyncClass().getAnnotation(EasySync.class) ;
        Class<?> aClass = syncItem.getSyncClass() ;
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        if(!easySync.arrayKey().isEmpty()) {
            JsonObjectRequest objectRequest ;
            if(syncItem.getBody()==null && syncItem.getBodyJson()==null) {
                url = url.replace(" ","%20") ;
                objectRequest = new JsonObjectRequest(Request.Method.GET, url, null, response -> {
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
            }else{
                JSONObject payload = (syncItem.getBodyJson()!=null) ? syncItem.getBodyJson() : convertToJsonObject(syncItem.getBody()) ;
                objectRequest = new JsonObjectRequest(Request.Method.POST, url, payload, response -> {
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
                            headers.put("Content-Type", "application/json; charset=utf-8");

                            return headers;
                        }
                    }
                };
            }
            objectRequest.setRetryPolicy(new DefaultRetryPolicy(MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(objectRequest);
        }else{
            JsonArrayRequest jsonArrayRequest ;
            if(syncItem.getBody()==null && syncItem.getBodyJson()==null){
                url = url.replace(" ","%20") ;
                jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, response -> {
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
            }else{
                jsonArrayRequest = new JsonArrayRequest(Request.Method.POST, url,null, response -> {
                    downloadFinish.onSuccess(response);
                }, error -> {
                    downloadFinish.onFailure(new EasySyncError(aClass,error.toString())) ;
                }){
                    @Override
                    public Map<String, String> getHeaders() {
                        if (headers != null) return headers;
                        else {
                            HashMap<String, String> headers = new HashMap<>();
                            headers.put("Content-Type", "application/json; charset=utf-8");

                            return headers;
                        }
                    }

                    @Override
                    public byte[] getBody() {
                        return ((syncItem.getBodyJson()!=null) ? syncItem.getBodyJson().toString() : convertToJsonObject(syncItem.getBody()).toString()).getBytes() ;
                    }
                } ;
            }

            jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(MAX_RETRIES, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
            requestQueue.add(jsonArrayRequest);
        }
    }

    private JSONObject convertToJsonObject(HashMap<String,Object> body){
        JSONObject jsonObject = new JSONObject() ;
        try {
            for (Map.Entry map : body.entrySet()) {
                jsonObject.put(map.getKey().toString(), map.getValue());
            }
        }catch (JSONException e){
            e.printStackTrace();
        }

        return jsonObject ;
    }

    public interface DownloadFinish {
        void onSuccess(JSONArray payloadResponse);

        void onFailure(EasySyncError easySyncError);
    }
}
