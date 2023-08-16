package com.qa.core.dataLib;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Retention(RUNTIME)
@Target(METHOD)
public @interface DataProviderArgs {
    String name() default "";
    String column() default "";
    String value() default "";
    String filters() default "";
}

