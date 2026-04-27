package com.carddemo.billpay.web;

import com.carddemo.billpay.exception.AccountNotFoundException;
import com.carddemo.billpay.exception.DuplicateTransactionException;
import com.carddemo.billpay.exception.TransactionNotFoundException;
import com.carddemo.billpay.exception.ValidationException;
import com.carddemo.billpay.exception.VsamIoException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Maps COBIL00C error paths onto HTTP status codes while preserving the exact
 * user-facing message strings, so that any downstream presentation layer
 * (3270 emulator, Angular UI, etc.) can show the same copy as the mainframe.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<BillPaymentErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new BillPaymentErrorResponse("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotFoundException.class)
    public ResponseEntity<BillPaymentErrorResponse> handleAccountNotFound(AccountNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BillPaymentErrorResponse("ACCOUNT_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(TransactionNotFoundException.class)
    public ResponseEntity<BillPaymentErrorResponse> handleTranNotFound(TransactionNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new BillPaymentErrorResponse("TRANSACTION_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<BillPaymentErrorResponse> handleDuplicate(DuplicateTransactionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new BillPaymentErrorResponse("DUPLICATE_TRANSACTION", ex.getMessage()));
    }

    @ExceptionHandler(VsamIoException.class)
    public ResponseEntity<BillPaymentErrorResponse> handleIo(VsamIoException ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new BillPaymentErrorResponse("IO_ERROR", ex.getMessage()));
    }
}
