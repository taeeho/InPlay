# ADR-002: ONNX 경유 (Python 학습 → JVM 추론 분리)

- Status: Accepted
- Date: 2026-05-12

## Context
LightGBM/LSTM 4모델 ensemble. 학습은 Python 생태계가 압도적, 서빙은 JVM 안정성 필요.

## Decision
Python(PyTorch + LightGBM)에서 학습 → ONNX export → Java ONNX Runtime 추론. 학습 코드 Java 포팅 금지.

## Consequences
모델 swap PR은 Python vs Java 1e-4 parity test 통과 필수. ONNX 미지원 op 발생 시 모델 단순화.
