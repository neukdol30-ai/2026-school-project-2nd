package com.siyan1234.itproject2nd.board.exception;

public class BoardRateLimitException extends RuntimeException {

    public BoardRateLimitException(String message) {
        super(message);
    }
}
