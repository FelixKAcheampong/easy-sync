EASY-SYNC
============================
***

Sync multiple SQLITE db entities from your API with ease
----------------------------
***VERSIONS***
``VERSIONS =>`` `0.01`,`0.02`,`0.03`,`0.04`

**Gradle**
```java
maven { url "https://jitpack.io" }

compileOptions {
    sourceCompatibility JavaVersion.VERSION_1_8
    targetCompatibility JavaVersion.VERSION_1_8
}
```


***INSTALLATION***
`implementation 'com.github.FelixKAcheampong:easy-sync:VERSION'`

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
    @Map(key = "name")
    private String name ;
    @Map(key = "email")
    private String email ;
    @Map(key = "mobile")
    private String mobile ;
}
```


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
`defaultValue` `String` use this to set a default value to a field.
`toDate` `boolean` This will convert a value `2020-01-21` to `long`
`toDateTime` `boolean` This will convert the value `2020-01-21T10:59:26` to `long` with date time
`example`long data = > `1579513269811`
***SQLIteOpenHelper database usage***
```java
    SQLiteDatabase db = yourSqliteOpenHelper.getWritableDatabase() ;
    List<EasySyncItem> easySyncItems = new ArrayList<>() ;
    easySyncItems.add(new EasySyncItem(User.class,"api_url")) ;
    easySyncItems.add(new EasySyncItem(Videos.class,"api_url")) ;
    new EasySync(this)
        .setSyncItems(easySyncItems)
        .setDatabase(db)
        .setSyncListener(this)
        .startEasySync();
```

***Room persistence database support***
```java
    roomDBManager = DBManager.getInstance(this) ;
    List<EasySyncItem> easySyncItems = new ArrayList<>() ;
    easySyncItems.add(new EasySyncItem(User.class,"api_url")) ;
    easySyncItems.add(new EasySyncItem(Videos.class,"api_url")) ;
   new EasySync(this)
        .setSyncItems(easySyncItems)
        .setRoomDatabase(roomDBManager.getOpenHelper().getWritableDatabase())
        .setSyncListener(this)
        .startEasySync();
```

*** SYNCLISTENER INTERFACE METHODS ***
```java
    @Override
    public void onComplete(boolean onErrorFound,List<EasySyncError> easySyncErrors) {
        if(!onErrorFound){
            Toast.makeText(this,"Completed with no error",Toast.LENGTH_LONG).show();
        }else{
            for(EasySyncError err: easySyncErrors){
                 Log.d("error","class:"+err.getEntity()+", error:"+err.getMessage()) ;
            }
        }
    }

    @Override
    public void onFatalError(String errorMessage) {
        // This only happens when database is not set
    }

    @Override
    public void onQueueChanged(int index, EasySyncItem syncItem) {
        // Use to track the current sync item index
        // Use this to set your items progess bar
    }
    
    @Override
    public void onInsert(boolean isSuccess, EasySyncItem easySyncItem, int numberOfItems, int inserted) {
           // isSuccess = true if insertion was successfull otherwise false,
           // easySyncItem = easySyncItem being inserted
           // numberOfItems = number of records from your api (`JSONArray records`)
           // inserted = total number of items inserted
           
           /*Use this to set your insertion progress bar*/
    }
```

***Sync Item Constructors***
============================================================
```java
/**
     * Get request with specific url
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url)

    /**
     * POST request with specific url and body
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull HashMap<String,Object> payload)

    /**
     * GET request with specific url and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,int dataExistsStrategy)

    /**
     * POST request with specific url, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull HashMap<String,Object> payload,int dataExistsStrategy)

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull HashMap<String,Object> payload,int dataExistsStrategy)

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     * @param dataExistsStrategy data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull JSONObject payload,int dataExistsStrategy)

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull HashMap<String,Object> payload)

    /**
     * POST request using main endpoint, request payload and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param payload Request payload as HashMap
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull JSONObject payload)

    /**
     * POST request with specific url and request payload
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as JSONOBJECT
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull JSONObject payload)

    /**
     * POST requwst with specific url, request payload as JSONOBJECT and data insertion strategy
     * @param syncClass Entity to store data from endpoint
     * @param url EndPoint url
     * @param payload Request payload as JSONOBJECT
     * @param dataExistsStrategy Data insertion strategy
     */
    public EasySyncItem(@NonNull Class<?> syncClass,@NonNull String url,@NonNull JSONObject payload,int dataExistsStrategy)
```

***Methods***
==========================================
***Setters***
`setSyncItems(List<EasySyncItem>)` Set items to be synced
`setDatabase(SQLiteDatabase)` set sqliteDatabase
`setPostBaseUrl` Set a base url if all your `POST` request uses the same base url, instead of passing same url to each EasySyncItem
`setClearDataBeforeSync` set to true if you want your records to be deleted before syncing
`showProgress(boolean)` This will show a progress dialog and dismiss authomatically when syncing is done. Defualt is `true`
`setSyncListener(EasySync.SyncListener)` get callbacks of success and errors
`setRoomDatabase(SupportSQLiteDatabase)` set this if you are using Room Persistence Database
`setLoadingMessage(String)` set message to be shown in the progress dialog. default is `Loading...`
`setHeaders(HashMap<String, String>)` overrite default Http headers. Send your `Authenticatoin Heaers` here
`startEasySync()` Finally, start the Easy Sync

***Watch Video Tutorial***
==================================
![alt text](https://img.youtube.com/vi/4JRd5J4m5Tk/0.jpg)


