package com.app.bank_acquiring.service;

public class IdValidationException extends RuntimeException{
    public IdValidationException(String msg){
        super(msg);
    }
}
