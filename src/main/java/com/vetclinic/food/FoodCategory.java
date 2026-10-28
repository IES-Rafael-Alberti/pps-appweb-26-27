package com.vetclinic.food;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

public enum FoodCategory {
    DOG_FOOD,
    CAT_FOOD,
    PUPPY_FOOD,
    KITTEN_FOOD,
    BIRD_FOOD,
    RABBIT_FOOD,
    TREATS,
    SUPPLEMENTS,
    MEDICINE;

    @JsonCreator
    public static FoodCategory from(String value) {
        return EnumSupport.parse(FoodCategory.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
