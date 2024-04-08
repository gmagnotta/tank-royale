FROM quay.io/gmagnotta/openjdk-jmods-x86
ADD --chown=java . /tmp/src/
WORKDIR /tmp/src/

RUN ./gradlew assemble
