# Container image for the Student Attendance Management Portal.
#
# Single stage on purpose. A multi-stage build that ran Maven inside Docker would
# re-resolve every dependency through the build daemon, which both duplicates work
# the pipeline has already done and needs network access the build context may not
# have. The pipeline builds the WAR once and this image packages that exact
# artefact, so the thing tested is the thing shipped.
#
#   docker build --build-arg APP_VERSION=1.0.0 -t samp-attendance:1.0.0 .
FROM eclipse-temurin:17-jre

ARG APP_VERSION=dev
ARG BUILD_NUMBER=local
ARG GIT_COMMIT=unknown

LABEL org.opencontainers.image.title="Student Attendance Management Portal" \
      org.opencontainers.image.description="Attendance capture with a role-based approval workflow" \
      org.opencontainers.image.version="${APP_VERSION}" \
      org.opencontainers.image.revision="${GIT_COMMIT}" \
      org.opencontainers.image.source="https://github.com/Swaroop192005/CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal" \
      samp.build.number="${BUILD_NUMBER}"

# Run as an unprivileged user: a servlet container has no need for root, and a
# container breakout costs far less when the process owns nothing.
RUN groupadd --system --gid 1001 samp \
 && useradd  --system --uid 1001 --gid samp --home-dir /app --shell /usr/sbin/nologin samp \
 && mkdir -p /app/data \
 && chown -R samp:samp /app

WORKDIR /app

# The WAR is executable: Spring Boot's launcher runs it with embedded Tomcat,
# while the same file still deploys into a standalone Tomcat (Task 8).
COPY --chown=samp:samp target/attendance-portal.war /app/attendance-portal.war

USER samp

# Defaults; every one is overridable at run time with -e (Task 3 configuration).
ENV SERVER_PORT=8080 \
    DB_URL=jdbc:h2:file:/app/data/sampdb \
    ATTENDANCE_THRESHOLD=75 \
    JAVA_OPTS="-XX:MaxRAMPercentage=75"

EXPOSE 8080

# The health check is the same endpoint the pipeline and Ansible probe, so
# "healthy" means the same thing everywhere rather than three different things.
HEALTHCHECK --interval=15s --timeout=4s --start-period=45s --retries=4 \
  CMD curl -fsS "http://localhost:${SERVER_PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/attendance-portal.war"]
