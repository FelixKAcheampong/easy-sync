package com.felinkotek.easysync;

public interface DataExistsStrategy{
        int IGNORE = 1 ;
        int REPLACE = 2 ;
        int ROLLBACK = 3 ;
        int ABORT = 4 ;
        int FAIL = 5 ;
}