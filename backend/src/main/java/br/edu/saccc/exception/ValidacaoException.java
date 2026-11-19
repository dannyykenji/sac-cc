package br.edu.saccc.exception;

/**
 * Violação de regra de entrada/estado que configura requisição inválida (400),
 * distinta de conflito com outro registro (409, IllegalStateException).
 */
public class ValidacaoException extends RuntimeException {

    public ValidacaoException(String mensagem) {
        super(mensagem);
    }
}
