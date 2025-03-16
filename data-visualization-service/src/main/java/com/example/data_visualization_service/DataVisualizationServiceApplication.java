package com.example.data_visualization_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@EnableKafka
@SpringBootApplication
public class DataVisualizationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataVisualizationServiceApplication.class, args);
	}

}
