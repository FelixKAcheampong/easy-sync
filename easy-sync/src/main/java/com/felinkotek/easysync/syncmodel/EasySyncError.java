package com.felinkotek.easysync.syncmodel;

import androidx.annotation.NonNull;

public class EasySyncError {
    @NonNull private Class<?> aClass ;
    @NonNull private String message ;

    public EasySyncError(Class<?> aClass, String message) {
        this.aClass = aClass;
        this.message = message;
    }

    public Class<?> getEntity() {
        return aClass;
    }

    public String getMessage() {
        return message;
    }
}
