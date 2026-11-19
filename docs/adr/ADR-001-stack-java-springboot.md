# ADR-001 — Stack Java + Spring Boot 3.5, build com Maven Wrapper

**Status:** ACEITA (escolha do usuário em 2026-10-10)
**Data:** 2026-10-10

## Contexto

- O documento de requisitos exige sistema web acessado por navegador, com regras de negócio relevantes (concorrência de agendamento, estatísticas, auditoria) e RNFs de manutenibilidade (RNF34–43: camadas, testes, versionamento).
- O usuário definiu Java + Spring Boot como tecnologia do projeto.
- Máquina local: OpenJDK 25.0.4.1; sem Maven, sem Docker.
- Linhas vigentes do Spring Boot: 4.1.x (atual) e 3.5.x (aintra mantida, patches em 2026). O usuário escolheu **3.5.x** por compatibilidade com tutoriais e material de aula.
- A documentação oficial declara que o Spring Boot 3.5 exige Java 17+ e é compatível até o Java 25 — o JDK local 25 atende.

## Decisão

1. **Spring Boot 3.5.x**, sempre na patch mais recente da linha (3.5.16 na data deste ADR), sobre Jakarta EE 10 / Servlet 6.0.
2. Runtime local: **JDK 25**; compilação com `maven.compiler.release=17` por portabilidade institucional (decisão confirmada pelo usuário — #T2, 2026-10-10).
3. Build com **Maven Wrapper** (`./mvnw`) incluído no repositório — nenhum desenvolvedor precisa instalar Maven.
4. Starters iniciais: `web`, `data-jpa`, `security`, `validation`, `actuator`, `flyway-core` + `flyway-database-postgresql`, `springdoc-openapi-starter-webmvc-ui`; testes: `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ) + `h2`.

## Consequências

**Positivas**
- Alinhamento com o material didático e a curva de aprendizado do time.
- Stack madura, com caminho de migração para o Spring Boot 4 documentado pela VMware (mesmo baseline Java 17+).
- Maven Wrapper elimina a dependência de instalação global.

**Negativas**
- Linha 3.5 receberá suporte por menos tempo que a 4.x → plano de upgrade futuro (novo ADR quando necessário).
- `--release 17` impede uso de recursos de linguagem pós-17 no código (deliberado, revertível via #T2).
