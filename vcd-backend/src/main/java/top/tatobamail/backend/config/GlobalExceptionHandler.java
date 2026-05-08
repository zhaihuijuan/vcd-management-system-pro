package top.tatobamail.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import top.tatobamail.backend.common.ApiResponse;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ApiResponse<Object> handleGeneralException(Exception e) {
        log.error("Unhandled exception: ", e);
        return ApiResponse.error(500, "Internal Server Error: " + e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Object> handleRuntimeException(RuntimeException e) {
        log.warn("Runtime exception: {}", e.getMessage());
        return ApiResponse.error(400, e.getMessage());
    }
}
