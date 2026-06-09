# inplay — convenience commands. All builds run inside podman so host JDK
# version (Java 25 in dev) does not interfere with the pinned JDK 21.

.PHONY: help build test test-headless clean shell tasks compile mongo-up mongo-down mongo-logs

GRADLE_IMAGE := docker.io/gradle:8.10-jdk21
# ADR-009 headless 라이브 테스트 전용 — Chromium 포함. Playwright Java 버전(1.49.0)과 태그 일치 필요.
PLAYWRIGHT_IMAGE := mcr.microsoft.com/playwright/java:v1.49.0-noble
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
	@echo "  make test-headless — run KBO live headless test (Playwright Chromium image, real fetch)"
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

# ADR-009 실 KBO fetch 검증. Chromium 포함 이미지에서 KBO_LIVE=1 로 KboHeadlessLiveTest 만 실행.
# GRADLE_USER_HOME 으로 캐시 볼륨 공유. 폴링 1회, 수집 데이터 미커밋.
test-headless:
	podman run --rm \
	  -v $(PWD):/app \
	  -v $(GRADLE_CACHE):/home/gradle/.gradle \
	  -e GRADLE_USER_HOME=/home/gradle/.gradle \
	  -e KBO_LIVE=1 \
	  -w /app $(PLAYWRIGHT_IMAGE) \
	  ./gradlew :modules:collector:test --tests 'com.inplay.collector.kbo.KboHeadlessLiveTest'

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
