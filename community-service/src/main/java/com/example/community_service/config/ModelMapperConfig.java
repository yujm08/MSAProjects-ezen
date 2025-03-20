package com.example.community_service.config;

import org.modelmapper.ModelMapper;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.UUID;

@Configuration
public class ModelMapperConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();
        
        // UUID to String
        modelMapper.addConverter(new Converter<UUID, String>() {
            public String convert(MappingContext<UUID, String> context) {
                return context.getSource() != null ? context.getSource().toString() : null;
            }
        });

        // String to UUID
        modelMapper.addConverter(new Converter<String, UUID>() {
            public UUID convert(MappingContext<String, UUID> context) {
                return context.getSource() != null ? UUID.fromString(context.getSource()) : null;
            }
        });

        return modelMapper;
    }
}

