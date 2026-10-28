package com.vetclinic.config;

import com.vetclinic.common.EnumSupport;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.converter.ConverterFactory;
import org.springframework.format.FormatterRegistry;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Makes query parameters as forgiving as the JSON bodies: {@code ?status=completed} and
 * {@code ?status=COMPLETED} both work, and an unknown value returns a readable error.
 */
@Configuration
public class RelaxedEnumConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverterFactory(new LenientEnumConverterFactory());
    }

    private static final class LenientEnumConverterFactory implements ConverterFactory<String, Enum<?>> {

        @Override
        @SuppressWarnings({"unchecked", "rawtypes"})
        public <T extends Enum<?>> @NonNull Converter<String, T> getConverter(Class<T> targetType) {
            return source -> (T) EnumSupport.parse((Class) targetType, source);
        }
    }
}
