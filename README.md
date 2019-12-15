EASY-SYNC-ANDROID
============================
***

Sync multiple SQLITE db entities from your API with ease
----------------------------
***VERSIONS***
`0.01`
***INSTALLATION***
`implementation 'com.github.FelixKAcheampong:easy-sync-android:VERSION'`

***USAGE***
============================

**Create an entity representing your database table**

- `Entity class name should be the same as the DB table name`
- `The entity field names represent your table fields/colums`
>
    Example DATA for below setup
`[{
        "name":"Felix K. Acheampong",
        "email":"xxx.gmail.com",
        "mobile":"+233 54xxxxxxx"
}]`
`
```java
@EasySync()
public class User {
    @Map(key = "name",to = "username") 
    private String name ;
    @Map(key = "email")
    private String email ;
    @Map(key = "mobile")
    private String mobile ;
}
```

>
    Example DATA for below setup
`{
    "data":[
        {
            "name":"Felix K. Acheampong",
            "email":"xxx.gmail.com",
            "mobile":"+233 54xxxxxxx"
        }
]}`
`
```java
@EasySync(arrayKey = "data")
public class User {
    @Map(key = "name",to = "username") // Use if DB table field/column name is different from java field name
    private String name ;
    @Map(key = "email")
    private String email ;
    @Map(key = "mobile")
    private String mobile ;
}
```

***Advance fields configuration***
`parser = @Parser(aClass = MainActivity.class,methodName = "processUser")`
Used to process data before saving. Your method should be `public static Object(Object value)` and must return an object to saved in the field/column
`to = "username"` Use `to` to denote actual table field/column name if it's different from the java field name

***SQLIteOpenHelper database usage***
```java
    SQLiteDatabase db = yourSqliteOpenHelper.getWritableDatabase() ;
    List<SyncItem> syncItems = new ArrayList<>() ;
    syncItems.add(new SyncItem(User.class,"api_url")) ;
    syncItems.add(new SyncItem(Videos.class,"api_url")) ;
    new EasySync(this)
        .setSyncItems(syncItems)
        .setDatabase(db)
        .setSyncListener(this)
        .startEasySync();
```

***Room persistence database support***
```java
    roomDBManager = DBManager.getInstance(this) ;
    List<SyncItem> syncItems = new ArrayList<>() ;
    syncItems.add(new SyncItem(User.class,"api_url")) ;
    syncItems.add(new SyncItem(Videos.class,"api_url")) ;
   new EasySync(this)
        .setSyncItems(syncItems)
        .setRoomDatabase(roomDBManager.getOpenHelper().getWritableDatabase())
        .setSyncListener(this)
        .startEasySync();
```

*** SYNCLISTENER INTERFACE METHODS ***
```java
@Override
    public void onComplete(boolean noErrorFound,List<EasySyncError> easySyncErrors) {
        //easySyncErrors contains each Entity and it related error message
        if(noErrorFound){
            Toast.makeText(this,"Completed with no error",Toast.LENGTH_LONG).show();
        }else{
            Toast.makeText(this,"Error found",Toast.LENGTH_LONG).show();
            String i = easySyncErrors.get(0).getMessage() ;
            Log.d("error_message",i) ;
            // Loop to see all errors
        }
    }

    @Override
    public void onFatalError(String errorMessage) {
        // This happens when serious error occur which resulted in quiting the sync process
        // Example is when no db is set
    }

    @Override
    public void onQueueChanged(int index, SyncItem syncItem) {
        // Use to track the current sync item index
        int percentage = (index/syncItems.size())*100 ;
        spinner.setProgress(percentage) ;
    }
```

***METHODS***
==========================================
***SETTERS***
`setSyncItems(List<SyncItem>)` Set items to be synced
`setDatabase(SQLiteDatabase)` set sqliteDatabase
`showInsertLog()` This will log insert statment to LogCat ;
`showProgress(boolean)` This will show a progress dialog and dismiss authomatically when syncing is done. Defualt is `true`
`setSyncListener(EasySync.SyncListener)` get callbacks of success and errors
`setRoomDatabase(SupportSQLiteDatabase)` set this if you are using Room Persistence Database
`setLoadingMessage(String)` set message to be shown in the progress dialog. default is `Loading...`
`setHeaders(HashMap<String, String>)` overrite default Http headers. Send your `Auth` here
`startEasySync()` Finally, start the Easy Sync

