//package com.frog.common.security.config;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.module.SimpleModule;
//import com.frog.common.security.serializer.SensitiveSerializer;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
///**
// *
// *
// * @author Deng
// * createData 2025/10/24 15:59
// * @version 1.0
// */
//@Configuration
//public class JacksonConfig {
//
//    @Bean
//    public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
//        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
//        ObjectMapper objectMapper = converter.getObjectMapper();
//
//        // 注册脱敏序列化器
//        SimpleModule module = new SimpleModule();
//        module.addSerializer(String.class, new SensitiveSerializer());
//        objectMapper.registerModule(module);
//
//        return converter;
//    }
//}
