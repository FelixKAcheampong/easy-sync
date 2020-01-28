package com.felinkotek.easysync.syncmodel;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONObject;

import java.util.HashMap;

/**
 * This library was created by Felix K. Acheampong
 * The aim is to let developers sync table entities with few lines of code
 * without making their own http request to their api
 * and also mapping data and inserting data to their table entities
 * in the database
 *
 */
public class EasySyncItem {
    @NonNull
    private Class<?> syncClass ;
    private String url ;
    private HashMap<String,Object> body ;
    private JSONObject bodyJson ;
    private String masterType ;
    private Integer dataExistsStrategy ;

    /**
     * Get request with specific url
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url) {
        this.syncClass = syncClass;
        this.url = url;
    }

    /**
     * POST request with specific url and body
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull HashMap<String,Object> payload){
        this.syncClass = syncClass;
        this.url = url;
        this.body = payload ;
    }

    /**
     * GET request with specific url and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,int dataExistsStrategy) {
        this.syncClass = syncClass;
        this.url = url;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    /**
     * POST request with specific url, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull HashMap<String,Object> payload,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.url = url;
        this.body = payload ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull HashMap<String,Object> payload,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.body = payload ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull JSONObject payload,int dataExistsStrategy){
        this.syncClass = syncClass;
        this.bodyJson = payload ;
        this.dataExistsStrategy = dataExistsStrategy ;
    }

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull HashMap<String,Object> payload){
        this.syncClass = syncClass;
        this.body = payload ;
    }

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull JSONObject payload){
        this.syncClass = syncClass;
        this.bodyJson = payload ;
    }

    /**
     * POST request with specific url and request payload
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as JSONOBJECT
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull JSONObject payload){
        this.syncClass = syncClass;
        this.url = url;
        this.bodyJson = payload ;
    }

    /**
     * POST requwst with specific url, request payload as JSONOBJECT and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as JSONOBJECT
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull JSONObject payload,int dataExistsStrategy){
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

    public void setUrl(String url){
        this.url = url ;
    }
}