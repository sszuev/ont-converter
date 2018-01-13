package com.github.sszuev;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Utility to work with exception.
 * <p>
 * Created by @szuev on 13.01.2018.
 */
public class Exceptions {

    public static Exception wrap(String message, Exception e) {
        return new Exception(message, e);
    }

    public static String flatSuppressedMessage(Exception e) { // first level suppressed exceptions only:
        return Arrays.stream(e.getSuppressed()).map(Throwable::getMessage).collect(Collectors.joining("\n"));
    }

    public static String shortMessage(Exception e, int limit) {
        String res = e.getMessage();
        if (res == null) return null;
        res = res.replace("\n", "");
        return res.length() > limit ? res.substring(0, limit) + "..." : res;
    }
}
