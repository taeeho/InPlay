# inplay — convenience commands. All builds run inside podman so host JDK
# version (Java 25 in dev) does not interfere with the pinned JDK 21.

.PHONY: help build test clean shell tasks compile mongo-up mongo-down mongo-logs

GRADLE_IMAGE := docker.io/gradle:8.10-jdk21
GRADLE_CACHE := inplay-gradle-cache
GRADLE_RUN   := podman run --rm \
                  -v $(PWD):/app \
                  -v $(GRADLE_CACHE):/home/gradle/.gradle \
                  -w /app $(GRADLE_IMAGE)
COMPOSE_FILE := infra/compose/podman-compose.yml

help:
	@echo "Available targets:"
	@echo "  make build        — gradle build (compile + test all modules)"
	@echo "  make compile      — compile all modules only"
	@echo "  make test         — run all tests"
	@echo "  make tasks        — list gradle tasks"
	@echo "  make clean        — gradle clean"
	@echo "  make shell        — interactive bash shell inside the gradle container"
	@echo "  make mongo-up     — start MongoDB + replica set (podman compose)"
	@echo "  make mongo-down   — stop MongoDB"
	@echo "  make mongo-logs   — tail MongoDB logs"

build:
	$(GRADLE_RUN) ./gradlew build

compile:
	$(GRADLE_RUN) ./gradlew compileJava

test:
	$(GRADLE_RUN) ./gradlew test

tasks:
	$(GRADLE_RUN) ./gradlew tasks

clean:
	$(GRADLE_RUN) ./gradlew clean

shell:
	podman run --rm -it \
	  -v $(PWD):/app \
	  -v $(GRADLE_CACHE):/home/gradle/.gradle \
	  -w /app $(GRADLE_IMAGE) bash

mongo-up:
	podman compose -f $(COMPOSE_FILE) up -d

mongo-down:
	podman compose -f $(COMPOSE_FILE) down

mongo-logs:
	podman compose -f $(COMPOSE_FILE) logs -f mongo
