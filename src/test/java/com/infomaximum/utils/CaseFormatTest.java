package com.infomaximum.utils;


import com.infomaximum.cluster.graphql.utils.CaseFormat;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CaseFormatTest {

    @Test
    public void test1() {
        Assertions.assertEquals("get_name", CaseFormat.toLowerUnderscore("getName"));
        Assertions.assertEquals("get_name", CaseFormat.toLowerUnderscore("GetName"));
        Assertions.assertEquals("get_first_name", CaseFormat.toLowerUnderscore("getFirstName"));
        Assertions.assertEquals("get_first_name", CaseFormat.toLowerUnderscore("get_First_Name"));
        Assertions.assertEquals("get_afirst_name", CaseFormat.toLowerUnderscore("getAFirstName"));
        Assertions.assertEquals("gcomponent", CaseFormat.toLowerUnderscore("GComponent"));
    }
}
