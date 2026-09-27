FROM jenkins/jenkins:lts-jdk17

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-requests python3-dotenv \
    && rm -rf /var/lib/apt/lists/*
USER jenkins