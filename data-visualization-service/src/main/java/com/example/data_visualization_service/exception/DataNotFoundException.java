package com.example.data_visualization_service.exception;

/**
 * 데이터가 없을 때 발생하는 예외
 */
public class DataNotFoundException extends RuntimeException {
    public DataNotFoundException(String message) {
        super(message);
    }
}
