package com.github.sszuev.ontapi;

import ru.avicomp.ontapi.OntFormat;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by @szuev on 11.01.2018.
 */
public class Formats {

    public static OntFormat find(String key) {
        Objects.requireNonNull(key, "Null search key");
        return OntFormat.formats().filter(f -> aliases(f).contains(key.toLowerCase())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Can't find format '" + key + "'"));
    }

    public static List<String> aliases(OntFormat f) {
        return Stream.of(String.valueOf(f.ordinal()), f.name(), f.getID(), f.getExt())
                .map(String::toLowerCase)
                .distinct().collect(Collectors.toList());
    }
}
