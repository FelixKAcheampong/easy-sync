package com.felinkotek.easysync.syncmodel;

import com.felinkotek.easysync.DataExistsStrategy;

import org.json.JSONObject;

import java.util.HashMap;

public class EasySyncItem {
    private Class<?> syncClass ;
    private String url ;
    private HashMap<String,Object> body ;
    private JSONObject bodyJson ;
    private String masterType ;
    private Integer dataExistsStrategy = DataExistsStrategy.IGNORE;

    public EasySyncItem(Class<?> syncClass, String url) {
        this.syncClass = syncClass;
        this.url = url;
    }

    public EasySyncItem(Class<?> syncClass, String url, HashMap<String,Object> body){
        this.syncClass = syncClass;
        this.url = url;
        this.body = body ;
    }

    public EasySyncItem(Class<?> syncClass, HashMap<String,Object> body){
        this.syncClass = syncClass;
        this.body = body ;
    }

    public EasySyncItem(Class<?> syncClass, String url,int dataExistsStrategy) {
        this.syncClass = syncClass;
        this.url = url;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    public EasySyncItem(Class<?> syncClass, String url, HashMap<String,Object> body,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.url = url;
        this.body = body ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    public EasySyncItem(Class<?> syncClass, HashMap<String,Object> body,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.body = body ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    public EasySyncItem(Class<?> syncClass, String url, JSONObject payload){
        this.syncClass = syncClass;
        this.url = url;
        this.bodyJson = payload ;
    }

    public EasySyncItem(Class<?> syncClass, JSONObject payload){
        this.syncClass = syncClass;
        this.bodyJson = payload ;
    }

    public EasySyncItem(Class<?> syncClass, String url, JSONObject payload,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.url = url;
        this.bodyJson = payload ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    public Class<?> getSyncClass() {
        return syncClass;
    }

    public String getUrl() {
        return url;
    }

    public HashMap<String,Object> getBody() {
        return body;
    }

    public String getMasterType() {
        return masterType;
    }

    public void setMasterType(String masterType) {
        this.masterType = masterType;
    }

    public Integer getDataExistsStrategy() {
        return dataExistsStrategy;
    }

    public void setDataExistsStrategy(int dataExistsStrategy) {
        this.dataExistsStrategy = dataExistsStrategy;
    }

    public JSONObject getBodyJson() {
        return bodyJson;
    }

    public void setBodyJson(JSONObject bodyJson) {
        this.bodyJson = bodyJson;
    }
}