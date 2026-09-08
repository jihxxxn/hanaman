# 하나만

게으른 사람도 실패하지 않게, 하루에 운동 동작 하나만 목표치만큼 채우는 습관 형성 앱.
4주 동안 같은 동작을 꾸준히 채우면 다음 동작이 해금되는 구조로, 여러 동작을 한꺼번에 강요하지 않는다.

## 구성
- `docs/DESIGN.md` — 도메인 모델, ERD, API 스펙, 아키텍처 설계 근거
- `backend/` — Spring Boot 3 백엔드 (Java 17, JPA, Redis, H2/PostgreSQL)
- `frontend/` — React + Vite 프론트엔드 (오늘의 미션 화면 프로토타입)

## 핵심 설계 포인트
1. **주 단위 완화형 성공 기준**: 7일 중 5일 이상 목표 달성이면 그 주는 성공. 하루 놓쳐도 전체가 리셋되지 않음.
2. **적응형 목표치 조정**: 지난 주 성공률에 따라 다음 주 목표치를 자동으로 ±조정 (`AdaptiveGoalService`).
3. **4주 사이클 마스터 판정**: 4주 중 3주 이상 성공해야 다음 동작 해금, 실패해도 페널티 없이 동일 동작으로 연장 (`WeeklyEvaluationService`).
4. **Redis 롤링 캐시**: 최근 7일 달성 여부를 Redis에 캐싱해 "이번 주 리듬" 조회를 빠르게 응답 (`RollingStreakService`).
5. **매일 자정 스케줄러**: 사용자가 앱을 켜지 않아도 주차/사이클 전환이 정확히 처리됨 (`CycleTransitionScheduler`).

## AI 활용 구분 (포트폴리오 설명용)
- **직접 설계**: 도메인 규칙 전체 (주간 평가, 적응형 목표치, 4주 마스터 판정, 동작 해금 흐름), ERD, API 스펙
- **AI 활용**: 반복적인 DTO/Repository 보일러플레이트, 프론트엔드 컴포넌트 초안 작성 속도 향상

## 실행 방법

### 백엔드
```bash
cd backend
./mvnw spring-boot:run
# 기본 프로필: H2 인메모리 DB. Redis는 로컬 6379 포트 필요 (docker run -p 6379:6379 redis)
```

### 프론트엔드
```bash
cd frontend
npm install
npm run dev
```

## 다음 단계 제안
- 온보딩 플로우 화면 (동작 선택 → 트리거 설정) 프론트 구현
- 프론트엔드를 실제 백엔드 API와 연동 (`fetch`/axios로 목업 상태 대체)
- 통계/진행 현황 서브 화면 구현
- 배포: 백엔드는 Fly.io/Railway, 프론트는 Vercel
