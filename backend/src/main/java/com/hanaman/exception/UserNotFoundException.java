package com.hanaman.exception;

/** 존재하지 않는 사용자 ID로 조회할 때 발생 — 프론트엔드가 404로 구분해 재온보딩할 수 있도록 별도 예외로 분리. */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String message) {
        super(message);
    }
}
