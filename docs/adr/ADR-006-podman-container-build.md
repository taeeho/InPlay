# ADR-006: Podman 컨테이너 빌드 (host JDK 무관)

- Status: Accepted
- Date: 2026-05-12

## Context
개발 host는 JDK 25, 프로젝트는 JDK 21 LTS 고정. 호스트 gradle/JDK 변동으로 빌드가 깨지는 사례 빈번.

## Decision
모든 gradle 실행은 `docker.io/gradle:8.10-jdk21` 컨테이너 안에서. `make compile|test|build` 또는 `scripts/podman-gradle.sh <task>`. host `./gradlew` 직접 호출 금지. CI도 동일 이미지.

## Consequences
첫 빌드 느림(이미지 pull). 캐시는 named volume `inplay-gradle-cache`로 재사용.
