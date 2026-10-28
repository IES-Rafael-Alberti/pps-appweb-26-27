package com.vetclinic.sale;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.vetclinic.common.EnumSupport;
import java.util.Locale;

public enum PaymentMethod {
    CASH,
    CARD,
    TRANSFER;

    @JsonCreator
    public static PaymentMethod from(String value) {
        return EnumSupport.parse(PaymentMethod.class, value);
    }

    @JsonValue
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }
}
