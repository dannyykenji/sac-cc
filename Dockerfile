# SAC-CC — artefato único (ADR-002/#T4): backend serve a SPA buildada.
# Multi-stage: build (JDK com Maven+Node) → runtime (JRE enxuto).
# Compatível com Docker e Podman (rootless).

# ---------- estágio 1: frontend ----------
FROM docker.io/library/node:20-slim AS frontend
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json* ./
RUN npm ci --no-fund --no-audit || npm install --no-fund --no-audit
COPY frontend/ ./
RUN npm run build

# ---------- estágio 2: backend ----------
FROM docker.io/library/maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build
COPY backend/pom.xml .
RUN ./mvnw -q dependency:go-offline || mvn -q dependency:go-offline || true
COPY backend/ ./
# build do jar EMPACOTANDO o frontend (pom copia de ../frontend/dist via perfil)
COPY --from=frontend /app/dist ./frontend-dist
RUN mvn -q package -DskipTests -Pwith-frontend

# ---------- estágio 3: runtime ----------
FROM docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S saccc && adduser -S saccc -G saccc
USER saccc
COPY --from=backend /build/target/*.jar app.jar
ENV TZ=America/Sao_Paulo \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health | grep -q UP || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
