package com.qa.core.basePage;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * This Custom Annotation is created so that some WebElement controls from page
 * can be marked as core and same will be used during page verification.
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface Core {

}
