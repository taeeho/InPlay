#!/usr/bin/env bash
# Run gradle commands inside a podman container with JDK 21 pinned.
# Host JDK version is irrelevant — everything runs in the container.
#
# Usage:
#   ./scripts/podman-gradle.sh tasks
#   ./scripts/podman-gradle.sh :modules:core:test
#   ./scripts/podman-gradle.sh build
#
# A named volume `inplay-gradle-cache` keeps the gradle dependency cache
# warm between runs (much faster after the first build).

set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
IMAGE="docker.io/gradle:8.10-jdk21"
CACHE_VOLUME="inplay-gradle-cache"

# Add -it only when both stdin and stdout are TTYs (interactive).
TTY_FLAGS=""
if [ -t 0 ] && [ -t 1 ]; then
    TTY_FLAGS="-it"
fi

exec podman run --rm $TTY_FLAGS \
    -v "$ROOT:/app" \
    -v "$CACHE_VOLUME:/home/gradle/.gradle" \
    -w /app \
    "$IMAGE" \
    ./gradlew "$@"
