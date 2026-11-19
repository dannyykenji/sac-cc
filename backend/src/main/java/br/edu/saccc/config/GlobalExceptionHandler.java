package br.edu.saccc.config;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Erros de API em ProblemDetail (RFC 7807) pt-BR — RNF04, CONVENCOES.md. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(br.edu.saccc.exception.ValidacaoException.class)
    ProblemDetail validacaoDeNegocio(br.edu.saccc.exception.ValidacaoException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        pd.setTitle("Dados inválidos");
        return pd;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    ProblemDetail naoEncontrado(EntityNotFoundException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
        pd.setTitle("Registro não encontrado");
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail requisicaoInvalida(IllegalArgumentException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("Não foi possível concluir a operação");
        return pd;
    }

    @ExceptionHandler(IllegalStateException.class)
    ProblemDetail conflitoDeEstado(IllegalStateException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        pd.setTitle("Operação não permitida");
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Verifique os campos informados.");
        pd.setTitle("Dados inválidos");
        var erros = new java.util.LinkedHashMap<String, String>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> erros.put(err.getField(), err.getDefaultMessage()));
        pd.setProperty("campos", erros);
        return pd;
    }
}
