# inplay/python/trainer

Python 학습 코드. **운영 코드 X — 학습 전용.** 산출물(`*.onnx`)만 Java 모듈에 commit.

## 디렉토리 구조 (예정)

```
trainer/
├── win_prob/          # W2: 승률 예측 (LightGBM)
│   ├── train_lgbm.py
│   ├── export_onnx.py
│   └── data/          # raw·processed (gitignored)
├── critical_moment/   # W4: 결정적 순간 감지 (LightGBM)
├── pitcher_limit/     # W5: 투수 한계 (LSTM)
├── parity/            # ONNX parity test
└── requirements.txt   # W2에서 추가
```

## 산출물 위치

학습 후 ONNX 파일은 다음으로 commit:
```
modules/ml-inference/src/main/resources/models/v{n}/{model_name}.onnx
```

함께 commit:
- `model_card.md` (메트릭·학습 데이터·하이퍼파라미터)
- `parity_test_input.json` (Java parity test용 input/output 샘플)

## 환경 (W2 시점에 셋업)

```bash
cd python/trainer
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
```

## Parity Test

각 모델 export 후 반드시 parity test 통과:
- Python 추론 vs Java ONNX 추론 결과 1e-4 이내 일치
- Java 측 parity test: `./gradlew :modules:ml-inference:test --tests "*Parity*"`

## 주의

- 학습 코드 Java 포팅 X (ONNX 경계 유지)
- `data/raw`·`data/processed`·`runs`·`mlruns` 모두 gitignore
- 의존성은 venv에 격리 (시스템 Python 오염 X)
- 데이터 수집은 `modules/collector` (Java)에서. Python은 학습만.
