package com.felinkotek.easysync;

import androidx.room.OnConflictStrategy;

public interface DataExistsStrategy{
        int IGNORE = OnConflictStrategy.IGNORE;
        int REPLACE = OnConflictStrategy.REPLACE ;
        int ROLLBACK = OnConflictStrategy.ROLLBACK ;
        int ABORT = OnConflictStrategy.ABORT ;
        int FAIL = OnConflictStrategy.FAIL ;
}