# SAC-CC — Guia de Execução e Testes

Guia operacional para rodar o sistema e validá-lo de ponta a ponta. Complementa o
`README.md` (instalação) e o `CRITERIOS_DE_ACEITE.md` (ensaio formal dos 12 critérios).

---

## 1. Requisitos por modo

| Modo | Precisa de |
|---|---|
| **A — Containers** (perto da produção) | Docker (ou Podman). Nada mais. |
| **B — Desenvolvimento** (com hot-reload) | JDK 25, PostgreSQL 16+, Node 20+, Maven não (usa `./mvnw`) |

Senhas de teste (seeds da migration V001 — **trocar em produção**):

| E-mail | Senha | Perfil |
|---|---|---|
| `admin@saccc.br` | `Admin@1234` | ADMIN |
| `coordenacao@saccc.br` | `Coord@1234` | COORDENADOR |
| `aluno@saccc.br` | `Aluno@1234` | ALUNO |

---

## 2. Opção A — Containers

> Em qualquer máquina com Docker: `docker compose up -d --build`.
> Com Podman sem plugin de compose, siga os comandos abaixo (equivalentes, testados).

```bash
cd ~/Área\ de\ trabalho/eng-de-soft

# 1. Build da imagem única (SPA + API na mesma origem; ADR-009) — só na 1ª vez/ao mudar código
podman build -t saccc:latest .

# 2. Rede + banco + aplicação
export SACCC_DB_PASSWORD='escolha-uma-senha'        # não comitar este valor
podman network create saccc-net
podman run -d --name saccc-db --network saccc-net \
  -e POSTGRES_DB=saccc -e POSTGRES_USER=saccc -e POSTGRES_PASSWORD=$SACCC_DB_PASSWORD \
  -v pgdata:/var/lib/postgresql/data \
  docker.io/library/postgres:18-alpine
podman run -d --name saccc-app --network saccc-net -p 8080:8080 \
  -e SACCC_DB_URL=jdbc:postgresql://saccc-db:5432/saccc \
  -e SACCC_DB_USER=saccc -e SACCC_DB_PASSWORD=$SACCC_DB_PASSWORD \
  -e TZ=America/Sao_Paulo saccc:latest

# 3. Conferir
curl localhost:8080/actuator/health     # → {"status":"UP"}
```

- Acesso: **http://localhost:8080** (SPA + API + WebSocket na mesma porta; refresh em
  `/app/...` funciona — fallback da SPA, ADR-009).
- Parar e limpar: `podman rm -f saccc-app saccc-db && podman network rm saccc-net`
  (com `-v pgdata` acima, os dados persistem entre subidas).

## 3. Opção B — Desenvolvimento (dois terminais)

```bash
# Terminal 1 — backend
cd backend
export SACCC_DB_PASSWORD="$(grep -o '[^=]*$' ~/.saccc-env)"   # dev local
./mvnw spring-boot:run                                        # :8080

# Terminal 2 — frontend
cd frontend
npm install            # só na primeira vez
npm run dev            # :5173 (proxy /api e /ws → 8080)
```

- Acesso: **http://localhost:5173**
- Alternativa sem Node: com o perfil `with-frontend`, o próprio jar serve a SPA
  (`./mvnw package -DskipTests -Pwith-frontend` + `java -jar target/backend-*.jar`).

---

## 4. Testes automatizados

```bash
cd backend  && ./mvnw verify      # 78 testes — unidade + integração (H2 em memória)
cd frontend && npx tsc --noEmit   # tipos, modo estrito
cd frontend && npm run build      # build de produção
```

Cobertura relevante dos 78: autenticação (bloqueio pós-5 falhas, CSRF), agendamento
(anti-duplicação, RN03 sobreposição, antecedência #N5, limite futuro #N4, **corrida do
mesmo aluno** com lock `FOR UPDATE` — prova real no PostgreSQL), cancelamento (prazo
aluno × coordenação), atendimento (RN07–14), estatísticas, auditoria, chat (janela,
append-only, isolamento 404).

> Detalhe: os testes de Fase3 usam slot fixo amanhã 10:00 (`slotBreveMenosDe24h`) —
> imunes ao horário em que a suíte roda.

---

## 5. Roteiro de teste manual (pontas a ponta, por perfil)

### 5.1 Preparação (como COORDENAÇÃO)
1. Login → **Gerenciar Disponibilidade** → criar bloco: data hoje+3, 14:00–16:00, duração 30min, modalidade PRESENCIAL.
2. Criar um segundo bloco **ONLINE** começando em **~5 minutos** (para o chat: janela abre 10min antes e fecha 15min depois — parâmetros em `parametro_sistema`).

### 5.2 Aluno agenda (como ALUNO)
3. Login → **Agendar Atendimento** → wizard: modalidade → slot → confirmar.
4. Conferir **protocolo** gerado e o card em **Meus Agendamentos** (aba Próximos).
5. Cancelamento: agende outro e cancele com mais de 12h de antecedência (motivo obrigatório — validação visível).
6. Limite: tente agendar um 2º futuro — deve receber 409 "limite de 1 agendamento" (#N4).

### 5.3 Chat em tempo real (ALUNO + COORDENAÇÃO, duas janelas/navegadores)
7. Dentro da janela do slot online: ALUNO entra por **Meus Agendamentos → Entrar na conversa**; COORDENAÇÃO por **Agenda de Atendimentos → Conversa**.
8. Mensagem de um lado deve aparecer no outro **sem recarregar** (WebSocket/STOMP).
9. Fora da janela: o acesso deve ser negado com mensagem amigável.

### 5.4 Atendimento e indicadores (como COORDENAÇÃO)
10. **Agenda de Atendimentos** → no agendamento realizado → **Registrar**: status final (Resolvido / Não resolvido / Em tratativa), categorização e notas.
11. **Estatísticas**: resumo, evolução mensal e distribuição refletindo os registros acima.

### 5.5 Administração (como ADMIN)
12. Deslogado → **Criar cadastro** (auto-cadastro de aluno) → fica PENDENTE.
13. Admin → **Cadastros Pendentes** → aprovar → o aluno consegue logar.
14. **Usuários**: ativar/desativar um usuário; **Auditoria**: conferir registros das ações acima.

### 5.6 Checklist rápido de sanidade
- [ ] Logo Afya (versão branca) no login e na sidebar; favicons na aba
- [ ] Sidebar com rótulos limpos (sem emojis); sino de notificações com badge
- [ ] Mobile 390px: sem overflow horizontal em todas as telas; zoom 200% ok
- [ ] (Container) refresh em `/app/agendar` não dá 404
- [ ] Logout volta ao login; sessão expira ao fechar o navegador

---

## 6. Problemas comuns

| Sintoma | Causa provável | Solução |
|---|---|---|
| Backend não sobe: `Connection refused` | PostgreSQL parado | `sudo systemctl start postgresql` |
| `password authentication failed` | senha errada na env | conferir `~/.saccc-env` (dev) ou `SACCC_DB_PASSWORD` |
| Front não carrega dados no :5173 | backend fora do ar | `curl localhost:8080/actuator/health` |
| Porta 8080 ocupada | instância antiga | `pgrep -af spring-boot` → `kill <pid>` |
| Chat diz "fora da janela" | slot não está na janela ±10/15min | criar slot começando em ~5min |
| 429 no login | 5 falhas seguidas | aguardar alguns minutos (Retry-After) |
| 1º acesso do container lento | Flyway + Hibernate inicializando | normal no primeiro boot |
| Refresh em `/app/...` dá 403/404 | jar sem a SPA ou sem o fallback | rebuild com `-Pwith-frontend` (ADR-009) |

---

*Última atualização: 2026-11-18 — correspondente ao commit pós-dockerização (ADR-009).*
