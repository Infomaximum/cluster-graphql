package com.infomaximum.cluster.graphql.utils;

public class CaseFormat {

    public static String toLowerUnderscore(String value) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (Character.isUpperCase(ch)) {
                if (i > 0 && (value.charAt(i - 1) != '_')) {
                    result.append('_');
                }
                result.append(Character.toLowerCase(ch));
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
