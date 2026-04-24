package com.panto.wms.common.exception;

import com.panto.wms.common.api.Result;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一处理控制器层抛出的异常，并转换为标准响应结构。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理可预期的业务异常。
     *
     * @param ex 业务异常
     * @return 标准失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(Result.failure(ex.getErrorCode().getCode(), ex.getMessage()));
    }

    /**
     * 处理请求参数校验异常。
     *
     * @param ex 参数校验异常
     * @return 包含字段错误信息的失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Map<String, String>>> handleValidationException(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();

        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new Result<>(
                ErrorCode.VALIDATION_ERROR.getCode(),
                ErrorCode.VALIDATION_ERROR.getDefaultMessage(),
                fieldErrors
            ));
    }

    /**
     * 处理未预期的系统异常。
     *
     * @param ex 未知异常
     * @return 标准失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnexpectedException(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Result.failure(
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage()
            ));
    }
}
