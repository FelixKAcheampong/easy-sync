package com.felinkotek.easysync.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Map {
    String defaultValue() default "" ;
    String key() default "" ;
    String to() default "" ;
    Parser parser() default @Parser(aClass = Map.class,methodName = "");
    boolean toDate() default false ;
    boolean toDatetTime() default false ;
}
