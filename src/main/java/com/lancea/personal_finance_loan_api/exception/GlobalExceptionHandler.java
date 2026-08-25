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
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex){
        log.error("Data integrity violation:", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The request could not be completed due to a conflict with existing data.");
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail handleBadRequest(Exception ex){
        log.error("Bad request:", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ProblemDetail handleDuplicateTransactions(DuplicateTransactionException ex){
        log.error("Duplicate transaction found", ex);

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.OK, ex.getMessage());
        problemDetail.setProperty("existingTransaction", ex.getExistingTransaction());

        return problemDetail;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFoundResources(Exception ex){
        log.error("Resource not found", ex);

        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericExceptions(Exception ex){
        log.error("Unhandled exception:" , ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred");
    }


}
