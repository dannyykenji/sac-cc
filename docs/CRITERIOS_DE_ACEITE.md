# ENSAIO DE CRITÉRIOS DE ACEITE (seção 9 do documento de requisitos)

Data do ensaio: 2025-11-05 (Fase 6). Método: cada critério mapeado à evidência real — teste automatizado (nome do teste, suíte) ou verificação executada contra o PostgreSQL de desenvolvimento.

| # | Critério (seção 9) | Status | Evidência |
|---|---|---|---|
| 1 | Aluno autenticado consulta horários disponíveis | ✅ | `RF15` — `Fase2DisponibilidadeTest.bloqueadoNaoApareceParaAluno*`; E2E navegador (print 04) |
| 2 | Aluno realiza agendamento válido | ✅ | `Fase3AgendamentoTest$Regras.agendarGeraProtocoloEReservaOSlot`; E2E (prints 05–06) |
| 3 | Sistema impede dois agendamentos no mesmo horário | ✅ | `Fase3AgendamentoTest.doisAlunosConcorrendoPeloMesmoSlotExatamenteUmVence` (2 threads → 1 vence) + índice UNIQUE parcial V004 no PG (ADR-005) |
| 4 | Coordenação visualiza a agenda | ✅ | `Fase4AtendimentoTest$Isolamento.agendaAdminFiltraPorStatusEPagina` (filtros + paginação) |
| 5 | Coordenação registra a realização do atendimento | ✅ | `Fase4AtendimentoTest$CamposCondicionais.resolvidoComSolucaoCriaAtendimentoHistoricoENotificacao` |
| 6 | Coordenação classifica resolvido/não resolvido/em tratativa | ✅ | RN07/08/09 nos mesmos testes + `Fase4AtendimentoTest$Transicoes.*` |
| 7 | Sistema apresenta os 4 indicadores principais corretamente | ✅ | `Fase5EstatisticasTest.resumoBateComAContaDeMesa` (massa conhecida: 10/6/2/2) |
| 8 | Indicadores atualizam após alterações nos registros | ✅ | `Fase5EstatisticasTest.indicadorRefleteMudancaDeStatusSemReiniciar` (RNF61: 60%→70%) |
| 9 | Aluno não acessa dados de outros alunos | ✅ | `Fase3AgendamentoTest$Consulta.alunoVeSomenteSeusAgendamentos` (404 neutro) + `Fase4AtendimentoTest$Isolamento` (403) |
| 10 | Alterações administrativas relevantes registradas | ✅ | Auditoria em toda operação (LOGIN, CRIAR, CANCELAR, APROVAR, REGISTRAR…) + `Fase6EntregaTest.adminConsultaTrilhaDeAuditoria` |
| 11 | Sistema funciona em computador e dispositivo móvel | ✅/⚠️ | Layout responsivo (sidebar→menu horizontal no mobile) nas telas atuais; **revalidar no frontend novo** |
| 12 | Principais fluxos com testes automatizados | ✅ | 66 testes em 7 suítes (`./mvnw verify` verde) |

**Pendências que dependem de terceiros** (não bloqueiam aceite funcional): #N1–N9 (validação da coordenação — defaults parametrizados e testados), #T5 SMTP (notificações ficam registradas no sistema), #T4 deploy/subdomínio.
