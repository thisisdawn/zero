package com.atguigu.gmall.cart.exception;

import org.springframework.stereotype.Component;


public class CartException extends RuntimeException {
    public CartException() {
        super();
    }

    public CartException(String message) {
        super(message);
    }
}
