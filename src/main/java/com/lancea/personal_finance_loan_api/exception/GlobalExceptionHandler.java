package com.lancea.personal_finance_loan_api.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {


    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ){

        Map<String, String> fieldErrors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(fieldError -> {
            log.warn("Field: {} Rejected Value: {}, Message: {}", fieldError.getField(), fieldError.getRejectedValue(), fieldError.getDefaultMessage());
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        });

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Validation failed");
        problem.setTitle("Validation Error");
        problem.setProperty("errors", fieldErrors);

        return new ResponseEntity<>(problem, headers, status);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDuplicateEmail(Exception ex){
        log.error("Data integrity violation:", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Email already exist");
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericExceptions(Exception ex){
        log.error("Unhandled exception:" , ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }


}
