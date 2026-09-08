# 하나만 — 설계 문서

## 1. 컨셉
게으른 사람도 실패하지 않고 습관을 만들 수 있도록, **하루에 운동 동작 하나만** 목표치만큼 채우는 것에 집중하는 습관 형성 앱.
여러 동작을 동시에 시키지 않고, 하나의 동작을 일정 기간 꾸준히 채워야 다음 동작이 해금되는 구조.

## 2. 핵심 도메인 규칙

### 2.1 동작(Exercise) 진행 구조
- 사용자는 한 번에 **활성 동작(active exercise) 1개**만 가질 수 있다.
- 활성 동작은 4주(28일) 동안 진행되며, **주 단위 성공/실패**로 평가한다.
- 주 단위 성공 기준: 7일 중 **5일 이상** 목표치 달성 → 그 주는 "성공"으로 기록.
- 4주 중 **3주 이상 성공**하면 해당 동작을 "마스터"로 처리하고, 다음 동작을 새로 선택할 수 있게 해금.
- 마스터하지 못해도 페널티는 없다 — 그냥 같은 동작을 다음 4주 사이클로 자동 연장. (탈락/리셋 개념 없음)

### 2.2 스트릭(연속 기록)
- "스트릭 리셋"은 존재하지 않는다. 대신 **완화형 스트릭**:
  - 하루를 놓치면 스트릭 카운터는 유지하되 "느슨해진 날"로 표시.
  - 완화형 스트릭 값 = 최근 7일 중 달성한 일수 (rolling window), UI에는 이걸 "이번 주 리듬"으로 표시.
  - 전체 누적 스트릭(총 활동 일수)은 별도로 기록해 동기부여용으로 노출.

### 2.3 적응형 목표치 조정
- 매주 종료 시점에 지난 주 성공률을 계산:
  - 성공률 ≥ 80% → 다음 주 목표치 +15% (반올림)
  - 성공률 40~79% → 목표치 유지
  - 성공률 < 40% → 다음 주 목표치 -20% (최소 하한값 존재, 동작별 min_reps)
- 목표치 변경은 사용자에게 사전 고지 없이 자동 적용하되, 홈 화면에 "이번 주 목표가 조정됐어요" 배너로만 안내.

### 2.4 체크인(CheckIn)
- 하루 1회, 오늘의 동작에 대해 "완료한 개수"를 기록.
- 목표치 이상이면 성공(success), 미만이면 부분 기록(partial)으로 저장 — 0개도 기록은 남기되 "실패"라는 라벨은 쓰지 않음.

## 3. 도메인 모델 (ERD)

```
User (1) ──< (N) UserExercise
UserExercise (1) ──< (N) WeeklyGoal
UserExercise (1) ──< (N) CheckIn
Exercise (1) ──< (N) UserExercise   // 마스터 데이터: 스쿼트, 런지 등
```

### User
| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| nickname | String | |
| trigger_habit | String | "양치 후" 등 사용자가 지정한 트리거 |
| created_at | Timestamp | |

### Exercise (마스터 데이터)
| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| name | String | 스쿼트, 런지, 플랭크 등 |
| unit | Enum | REPS / SECONDS |
| default_start_value | Int | 초기 목표치 |
| min_value | Int | 적응형 하향 조정 시 하한 |

### UserExercise (사용자별 진행 중인 동작 사이클)
| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| user_id | UUID | FK |
| exercise_id | UUID | FK |
| status | Enum | ACTIVE / MASTERED / EXTENDED |
| cycle_start_date | Date | 4주 사이클 시작일 |
| current_week | Int | 1~4 |
| current_target | Int | 현재 주 목표치 |
| order_index | Int | 몇 번째로 해금된 동작인지 |

### WeeklyGoal (주차별 스냅샷 — 통계/적응형 조정 근거)
| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| user_exercise_id | UUID | FK |
| week_number | Int | 1~4 |
| target_value | Int | 그 주 목표치 |
| success_days | Int | 목표 달성한 일수 |
| result | Enum | SUCCESS / FAIL (5일 기준) |

### CheckIn
| 필드 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK |
| user_exercise_id | UUID | FK |
| date | Date | |
| completed_value | Int | 실제 완료 개수 |
| target_value_snapshot | Int | 당시 목표치 (이력 보존용) |
| achieved | Boolean | completed >= target |

## 4. API 스펙 (REST)

| Method | Endpoint | 설명 |
|---|---|---|
| POST | /api/users | 온보딩 — 닉네임/트리거 등록 |
| GET | /api/users/{id}/today | 오늘의 미션 조회 (동작명, 목표치, 체크 여부) |
| POST | /api/users/{id}/checkins | 오늘 체크인 기록 |
| GET | /api/users/{id}/exercises/active | 현재 활성 동작 진행 상태 조회 |
| POST | /api/users/{id}/exercises | 새 동작 선택 (해금 이후에만 가능) |
| GET | /api/users/{id}/stats | 전체 통계 (누적일수, 이번 주 리듬, 마스터한 동작 목록) |
| GET | /api/exercises | 선택 가능한 동작 마스터 목록 |

## 5. 백엔드 아키텍처 포인트
- **핵심 로직 (직접 설계)**: 주간 성공 판정, 적응형 목표치 조정, 4주 사이클 관리, 동작 해금 로직 → `WeeklyEvaluationService`, `AdaptiveGoalService`에 격리.
- **Redis 활용**: `user:{id}:rolling7d` 같은 키로 최근 7일 체크인 여부를 캐싱 → "이번 주 리듬" 조회를 매번 DB 집계하지 않고 O(1)에 가깝게 응답.
- **스케줄러**: 매일 자정 배치(Spring Scheduler)로 주차 전환/사이클 완료 여부를 평가 — 사용자가 앱을 안 열어도 다음날 정확한 상태로 진입.
- **AI 활용 구간 (README에 명시할 것)**: 프론트 컴포넌트 초안, 반복적인 DTO/매퍼 코드, 테스트 케이스 초안은 AI 활용. 위 핵심 도메인 서비스 3종은 직접 설계.

## 6. 프론트엔드 화면 구조
1. **온보딩**: 동작 선택 → 트리거 선택 → 시작
2. **홈 (오늘의 미션)**: 동작명, 목표치, 완료 개수 입력, 체크 버튼 — 딱 이것만
3. **진행 현황** (서브 화면): 이번 주 리듬(7칸), 현재 사이클 몇 주차, 마스터한 동작 리스트
4. **동작 해금 화면**: 4주 마스터 시에만 노출되는 다음 동작 선택 화면
