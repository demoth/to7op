package org.demoth.nogaem.common;

/**
 * @author demoth
 */
public class Util {
    public static String trimFirstWord(String str) {
        return str.substring(str.indexOf(' '), str.length());
    }
}
