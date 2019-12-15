package com.felinkotek.easysync.syncmodel;

public class SyncItem {
    private Class<?> syncClass ;
    private String url ;

    public SyncItem(Class<?> syncClass, String url) {
        this.syncClass = syncClass;
        this.url = url;
    }

    public Class<?> getSyncClass() {
        return syncClass;
    }

    public String getUrl() {
        return url;
    }
}
