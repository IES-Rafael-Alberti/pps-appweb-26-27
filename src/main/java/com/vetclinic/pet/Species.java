package com.vetclinic.pet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;

public enum Species {
    DOG,
    CAT,
    BIRD,
    RABBIT,
    REPTILE,
    OTHER;

    @JsonCreator
    public static Species from(String value) {
        return EnumSupport.parse(Species.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
