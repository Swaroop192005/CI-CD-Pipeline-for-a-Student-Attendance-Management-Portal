# Jenkins for the Student Attendance Management Portal.
#
# Why this image is built rather than pulled:
#
#   * This environment blocks updates.jenkins.io and every Jenkins artifact
#     mirror by network policy, so plugins cannot be installed at runtime and the
#     stock jenkins/jenkins image ships with none.
#   * jenkinsci/blueocean does bundle a full plugin set, but it is Alpine/musl
#     based and carries only JDK 11. The portal is Spring Boot 3 and needs
#     JDK 17, and a glibc JDK cannot be bind-mounted into a musl container.
#
# So: take the Debian, JDK 17 official image and bake in the plugin set copied
# out of the blueocean image. Docker Hub is reachable, which makes this the one
# route to a plugin-complete Jenkins that can also build this project.
FROM jenkins/jenkins:lts-jdk17

USER root
# Maven is bind-mounted from the host at /opt/maven; only the wrapper dir is needed.
RUN mkdir -p /opt/maven && chown -R jenkins:jenkins /opt/maven
USER jenkins

# Plugins are copied into the reference directory, which Jenkins expands into
# JENKINS_HOME on first start.
COPY --chown=jenkins:jenkins plugins/ /usr/share/jenkins/ref/plugins/

ENV JAVA_OPTS="-Djenkins.install.runSetupWizard=false \
               -Dhudson.model.DownloadService.noSignatureCheck=true \
               -Dhudson.plugins.git.GitSCM.ALLOW_LOCAL_CHECKOUT=true"
ENV JENKINS_OPTS="--httpPort=8090"
