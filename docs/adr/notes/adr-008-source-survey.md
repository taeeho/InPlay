# ADR-008 source survey notes (사람 확인)

- Date: 2026-05-19
- UA used: `inplay/0.1 (+contact: ai@ccfm.co.kr)`
- 사람 확인자: 사용자(@taeeho) + Claude Code(자동 fetch). 결과 사용자 승인.

## Statiz (`www.statiz.co.kr`)
robots.txt (excerpt):
```
# 그 외 모든 봇 차단
User-agent: *
Disallow: /

# Anthropic (Claude) 봇 차단
User-agent: anthropic-ai
Disallow: /
User-agent: Claude-Web
Disallow: /
```
Googlebot · Yeti · Bingbot만 allow (Crawl-delay 5). inplay UA는 `*` 매칭 → 전체 disallow.

ToS (footer, `https://www.statiz.co.kr/policy/?m=terms`):
> "스포키 기록실 상품의 저작권은 스포키 및 스탯티즈에 있으며 이를 무단 이용하는 경우 저작권법 등에 따라 법적책임을 질 수 있습니다."

→ **Verdict: 폐기**.

## Naver Sports (`m.sports.naver.com`, `sports.news.naver.com`)
robots.txt:
```
User-agent: *
Disallow: /
Allow: /$
Allow: /index

User-agent: Yeti
Allow: /
```
일정 페이지(`/schedule/...`)는 inplay UA 기준 disallow.

→ **Verdict: 폐기**.

## Daum Sports (`sports.daum.net`)
robots.txt:
```
User-agent: *
Disallow: /media-api/
Disallow: /prx/
```
일정 페이지 path는 allow. 그러나 `/schedule/kbo` raw HTML은 `<tbody id="scheduleList"></tbody>` + "업데이트 준비중" 안내 (JS 렌더링). 데이터 출처 안내:
> "다음스포츠에서 제공하는 일정, 결과, 순위, 기록 데이터는 '제공처'의 사정에 따라 데이터 갱신이 지연 될 수 있습니다."
→ 제3자 재배포. KBO 공식 1차 source 우선.

→ **Verdict: 폐기 (1차 source 아님)**.

## KBO 공식 (`www.koreabaseball.com`)
robots.txt:
```
User-agent: *
Disallow: /Common/
Disallow: /Help/
Disallow: /Member/
Disallow: /ws/
```
`/Schedule/Schedule.aspx`는 allow. raw HTML에 `<table id="tblScheduleList">` 빈 골격만 존재. 데이터는 `/ws/Schedule.asmx/GetScheduleList` POST AJAX로 채워짐. `/ws/`는 robots disallow.

→ **Verdict: 채택 (조건부)**. headless 렌더링으로 SSR된 DOM만 파싱, `/ws/` 직접 호출 X. ADR-009 운영 조건 적용.

## 합법 자동 수집 가능 source 부재 결론
4개 후보 모두 ADR-005 보수 원칙(robots 자동 봇으로 직접 fetch)으로는 일정 데이터 수집 불가. KBO 공식은 헤드리스 회색지대(ADR-009)로 W1~W4 진행. 사용자가 KBO 공식 측 베타 사용 문의는 선택적 후속.
