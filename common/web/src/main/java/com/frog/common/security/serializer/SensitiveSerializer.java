//package com.frog.common.security.serializer;
//
//import com.fasterxml.jackson.core.JsonGenerator;
//import com.fasterxml.jackson.databind.BeanProperty;
//import com.fasterxml.jackson.databind.JsonMappingException;
//import com.fasterxml.jackson.databind.JsonSerializer;
//import com.fasterxml.jackson.databind.SerializerProvider;
//import com.fasterxml.jackson.databind.ser.ContextualSerializer;
//
//import java.io.IOException;
//
///**
// *敏感数据序列化器
// *
// * @author Deng
// * createData 2025/10/24 15:57
// * @version 1.0
// */
//public class SensitiveSerializer extends JsonSerializer<String> implements ContextualSerializer {
//
//    private Sensitive sensitive;
//
//    @Override
//    public void serialize(String value, JsonGenerator gen, SerializerProvider serializers)
//            throws IOException {
//        if (value != null && sensitive != null) {
//            gen.writeString(sensitive.type().desensitize(value));
//        } else {
//            gen.writeString(value);
//        }
//    }
//
//    @Override
//    public JsonSerializer<?> createContextual(SerializerProvider prov, BeanProperty property)
//            throws JsonMappingException {
//        if (property != null) {
//            Sensitive annotation = property.getAnnotation(Sensitive.class);
//            if (annotation != null) {
//                SensitiveSerializer serializer = new SensitiveSerializer();
//                serializer.sensitive = annotation;
//                return serializer;
//            }
//        }
//        return prov.findNullValueSerializer(null);
//    }
//}
