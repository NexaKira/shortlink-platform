package com.sc.shortlinkcore.common;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintViolationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public Result<?> handle(RuntimeException e) {
        return Result.error(500, e.getMessage());
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public Result<?> handle(ConstraintViolationException e) {
        return Result.error(400, "参数校验失败：" + e.getMessage());
    }

    @ExceptionHandler(BusinessException.class)
    public Result<?> handle(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

}
