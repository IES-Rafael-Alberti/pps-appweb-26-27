package com.vetclinic.pet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

public enum PetSex {
    MALE,
    FEMALE,
    UNKNOWN;

    @JsonCreator
    public static PetSex from(String value) {
        return EnumSupport.parse(PetSex.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
