FROM jenkins/jenkins:lts-jdk17

USER root
ENV PLAYWRIGHT_BROWSERS_PATH=/ms-playwright
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-requests python3-dotenv curl ca-certificates \
    && curl -fsSL https://deb.nodesource.com/setup_24.x | bash - \
    && apt-get install -y --no-install-recommends nodejs \
    && npx -y playwright@1.62.0 install --with-deps chromium \
    && chown -R jenkins:jenkins /ms-playwright \
    && rm -rf /var/lib/apt/lists/*
USER jenkins