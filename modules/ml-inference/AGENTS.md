# ml-inference 모듈 — 영역 규칙 (루트 AGENTS.md에 병합됨)

- ONNX 경계 유지: 학습 로직의 Java 포팅 금지. 이 모듈은 ONNX Runtime 추론만.
- 모델 파일 swap은 parity test(Python 학습 출력 ↔ Java 추론 출력 일치) 통과 없이는 금지.
- 모듈 단독 테스트: 루트에서 `make shell` 후 `./gradlew :modules:ml-inference:test`.
