package com.infomaximum.cluster.graphql.anotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by kris on 29.12.16.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GraphQLTypeMethod {

    String value() default "";
    TypeAuthControl auth() default TypeAuthControl.AUTH;
    String deprecated() default "";

}
