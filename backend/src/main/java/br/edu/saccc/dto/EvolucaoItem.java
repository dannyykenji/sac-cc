package br.edu.saccc.dto;

/** RF52/RF53: série temporal (realizados × resolvidos por mês). */
public record EvolucaoItem(int ano, int mes, long realizados, long resolvidos) {
}
