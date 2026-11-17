#!/bin/sh
# build na Vertra Cloud: o container só tem Java (sem Maven) e a pasta .mvn não é importada,
# então baixamos o Maven aqui mesmo e geramos o jar em target/
set -e

MAVEN_VERSION=3.9.9
MAVEN_URL="https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$MAVEN_VERSION/apache-maven-$MAVEN_VERSION-bin.tar.gz"

if [ ! -x "apache-maven-$MAVEN_VERSION/bin/mvn" ]; then
  if command -v wget >/dev/null 2>&1; then
    wget -qO- "$MAVEN_URL" | tar xz
  else
    curl -fsSL "$MAVEN_URL" | tar xz
  fi
fi

# os testes de integração precisam do banco; os unitários rodam no CI/local
"./apache-maven-$MAVEN_VERSION/bin/mvn" -B package -DskipTests
