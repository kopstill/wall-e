package com.kopever.robot.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;
import java.io.StringWriter;

/**
 * Created by Lullaby on 2017/1/19.
 */
public class Jackson {

    public static String toJson(Object object) {
        if (object != null) {
            StringWriter writer = new StringWriter();
            ObjectMapper mapper = JacksonMapperInstance.INSTANCE.getInstance();
            try {
                mapper.writeValue(writer, object);
                return writer.toString();
            } catch (IOException e) {
                throw new RuntimeException("Jackson write value exception");
            }
        }

        return null;
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json != null) {
            ObjectMapper mapper = JacksonMapperInstance.INSTANCE.getInstance();
            try {
                return mapper.readValue(json, clazz);
            } catch (IOException e) {
                throw new RuntimeException("Jackson read value exception");
            }
        }

        return null;
    }

    public static <T> T fromObject(Object object, Class<T> clazz) {
        if (object != null) {
            ObjectMapper mapper = JacksonMapperInstance.INSTANCE.getInstance();
            return mapper.convertValue(object, clazz);
        }

        return null;
    }

}

enum JacksonMapperInstance {

    INSTANCE;

    private ObjectMapper mapper;

    JacksonMapperInstance() {
        mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public ObjectMapper getInstance() {
        return mapper;
    }

}
