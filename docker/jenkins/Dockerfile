FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-requests python3-dotenv curl ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && npx -y playwright@1.49.0 install-deps chromium \
    && rm -rf /var/lib/apt/lists/*

USER jenkins
RUN npx -y playwright@1.49.0 install chromium