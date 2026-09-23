package com.teo1.clinicadental.exception;

import com.teo1.clinicadental.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> manejarResponseStatus(ResponseStatusException exception, HttpServletRequest request) {
        return construir(exception.getStatusCode(), exception.getReason(), request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> manejarValidacion(MethodArgumentNotValidException exception, HttpServletRequest request) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }

        String mensaje = errores.isEmpty()
                ? "Datos invalidos"
                : errores.entrySet().iterator().next().getKey() + ": " + errores.values().iterator().next();

        return construir(HttpStatus.BAD_REQUEST, mensaje, request, errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> manejarCuerpoInvalido(HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "El cuerpo de la peticion tiene un formato invalido", request, null);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> manejarParametroInvalido(MethodArgumentTypeMismatchException exception, HttpServletRequest request) {
        return construir(HttpStatus.BAD_REQUEST, "Valor invalido para " + exception.getName(), request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException exception, HttpServletRequest request) {
        return construir(HttpStatus.METHOD_NOT_ALLOWED, "Metodo " + exception.getMethod() + " no permitido en esta ruta", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> manejarIntegridad(HttpServletRequest request) {
        return construir(HttpStatus.CONFLICT, "La operacion viola una restriccion de la base de datos", request, null);
    }

    private ResponseEntity<ErrorResponse> construir(
            HttpStatusCode status,
            String mensaje,
            HttpServletRequest request,
            Map<String, String> errores
    ) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        ErrorResponse body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                httpStatus != null ? httpStatus.getReasonPhrase() : null,
                mensaje,
                request.getRequestURI(),
                errores
        );
        return ResponseEntity.status(status).body(body);
    }
}
