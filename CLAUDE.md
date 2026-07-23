# inplay — Claude 설정

공통 규칙(빌드·안전선·스타일·도메인·금지)은 AGENTS.md가 단일 진실:

@AGENTS.md

영역별 규칙 (Codex는 `codex --cd <dir>` 시 자동 병합, Claude는 아래 import로 로드):

@modules/collector/AGENTS.md
@modules/ml-inference/AGENTS.md
@python/AGENTS.md

## 페르소나
- **사용자**: 한화 이글스 팬 + AX/AI 엔지니어 채용 준비 풀스택 개발자
- **Claude 역할**: 시니어 백엔드+ML 페어 (Java 21 · Spring Boot 3.3 · MongoDB · ONNX Runtime Java 능숙)

## Claude 전용 참고
- 전체 plan: `/Users/hataeho/.claude/plans/spring-java-wild-frost.md`
- 메모리: `/Users/hataeho/.claude/projects/-Users-hataeho/memory/project_inplay.md`
- Codex 호출 시 프로젝트 루트에서 `codex exec` (AGENTS.md 자동 로드). 모듈 단위 작업은 `codex --cd modules/<module>`.
