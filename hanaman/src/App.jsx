import { useEffect, useState } from "react";
import {
  acknowledgeCycleSummary,
  createUser,
  fetchCycleSummary,
  fetchExercises,
  fetchRhythm,
  fetchTodayMission,
  startNextExercise,
  submitCheckIn,
} from "./api";

const WEEKS_PER_CYCLE = 4;
const RHYTHM_DAYS = 7;
const USER_ID_KEY = "hanaman_user_id";

// "미달/실패" 대신 과정 중심 표현 — 완전히 못 채운 날도 "그래도 채운 날"로 표현한다.
const RHYTHM_LABEL = {
  NONE: "기록 없음",
  BELOW: "조금 채웠어요",
  EXACT: "목표 달성",
  EXCEEDED: "목표 초과 달성",
};

const RHYTHM_SYMBOL = {
  NONE: "",
  BELOW: "•",
  EXACT: "●",
  EXCEEDED: "★",
};

// 지난주 대비 이번주 목표가 왜 이 숫자인지 설명하는 문구.
// previousWeekTarget이 없으면(1주차, 새 사이클 시작) 아무것도 보여주지 않는다.
function buildTargetNote(mission, unitLabel) {
  if (mission.previousWeekTarget == null) return null;

  const diff = mission.targetValue - mission.previousWeekTarget;
  const successDays = mission.previousWeekSuccessDays;

  if (diff > 0) {
    return `지난주 ${successDays}/7일 채운 덕분에 목표가 ${mission.previousWeekTarget}${unitLabel} → ${mission.targetValue}${unitLabel}로 늘었어요`;
  }
  if (diff < 0) {
    return `지난주는 ${successDays}/7일이었어요. 목표를 ${mission.previousWeekTarget}${unitLabel} → ${mission.targetValue}${unitLabel}로 낮췄어요 — 무리하지 않아도 돼요`;
  }
  return `지난주(${successDays}/7일)와 같은 목표예요. 이 리듬 그대로 가면 돼요`;
}

function GrowthRing({ progressRatio, count, target, unitLabel }) {
  const radius = 110;
  const circumference = 2 * Math.PI * radius;
  const clamped = Math.min(progressRatio, 1);
  const offset = circumference * (1 - clamped);

  return (
    <div className="ring-wrap">
      <svg className="ring-svg" viewBox="0 0 260 260">
        <circle className="ring-track" cx="130" cy="130" r={radius} />
        <circle
          className="ring-progress"
          cx="130"
          cy="130"
          r={radius}
          strokeDasharray={circumference}
          strokeDashoffset={offset}
        />
      </svg>
      <div className="ring-center">
        <span className="ring-count">{count}</span>
        <span className="ring-target">/ {target}{unitLabel} 목표</span>
      </div>
    </div>
  );
}

function OnboardingForm({ onCreated }) {
  const [exercises, setExercises] = useState([]);
  const [nickname, setNickname] = useState("");
  const [triggerHabit, setTriggerHabit] = useState("");
  const [firstExerciseName, setFirstExerciseName] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchExercises()
      .then((list) => {
        setExercises(list);
        if (list.length > 0) setFirstExerciseName(list[0].name);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!nickname.trim() || !firstExerciseName) return;
    setSubmitting(true);
    setError(null);
    try {
      const userId = await createUser({
        nickname: nickname.trim(),
        triggerHabit: triggerHabit.trim(),
        firstExerciseName,
      });
      localStorage.setItem(USER_ID_KEY, userId);
      onCreated(userId);
    } catch (err) {
      setError(err.message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="app-shell">
      <form className="mission-card onboarding-card" onSubmit={handleSubmit}>
        <h1 className="onboarding-title">하나만, 시작해볼까요</h1>
        <p className="onboarding-sub">
          매일 채울 동작 하나를 고르고, 시작 트리거를 정해주세요.
        </p>

        <label className="field-label">
          닉네임
          <input
            className="field-input"
            value={nickname}
            onChange={(e) => setNickname(e.target.value)}
            placeholder="예: 지현"
            required
          />
        </label>

        <label className="field-label">
          시작 트리거 (선택)
          <input
            className="field-input"
            value={triggerHabit}
            onChange={(e) => setTriggerHabit(e.target.value)}
            placeholder="예: 양치하고 나서"
          />
        </label>

        <label className="field-label">
          첫 동작
          {loading ? (
            <p className="onboarding-sub">동작 목록을 불러오는 중…</p>
          ) : (
            <select
              className="field-input"
              value={firstExerciseName}
              onChange={(e) => setFirstExerciseName(e.target.value)}
              required
            >
              {exercises.map((ex) => (
                <option key={ex.id} value={ex.name}>
                  {ex.name}
                </option>
              ))}
            </select>
          )}
        </label>

        {error && <p className="error-text">{error}</p>}

        <button className="commit-btn" type="submit" disabled={submitting || loading}>
          {submitting ? "시작하는 중…" : "하나만 시작하기"}
        </button>
      </form>
    </div>
  );
}

// 4주 사이클을 "마스터"한 직후 한 번 보여주는 요약 화면.
// 마스터는 다음 동작을 골라야 하는 진짜 결정이 있어서 블로킹 화면으로 유지한다.
// (연장은 결정할 게 없어서 오늘의 미션 화면 위 배너로만 보여줌 — extensionNotice 참고)
// "실패" 대신 이번 사이클을 어떻게 마무리했는지만 담백하게 알려주고,
// 목표 조정 안내(target-note)와 같은 톤을 쓰기 위해 기존 클래스를 그대로 재사용한다.
function CycleSummaryScreen({ summary, onConfirm, confirming, error }) {
  return (
    <div className="app-shell">
      <div className="mission-card onboarding-card">
        <h1 className="onboarding-title">
          {`4주 중 ${summary.successWeeks}주 성공해서 ${summary.exerciseName}을 마스터했어요`}
        </h1>
        <p className="onboarding-sub">다음 동작을 고르면 새로운 4주가 시작돼요</p>

        <div className="rhythm-row" aria-label={`4주 중 ${summary.successWeeks}주 성공`}>
          {Array.from({ length: summary.totalWeeks }).map((_, i) => (
            <span
              key={i}
              className={`rhythm-day ${i < summary.successWeeks ? "exact" : "below"}`}
            />
          ))}
        </div>

        {error && <p className="error-text">{error}</p>}

        <button className="commit-btn" onClick={onConfirm} disabled={confirming}>
          {confirming ? "확인하는 중…" : "다음 동작 고르러 가기"}
        </button>
      </div>
    </div>
  );
}

// 마스터 후 다음 동작을 고르는 화면 — 온보딩의 동작 선택 단계와 같은 형태를 재사용한다.
function ExercisePicker({ onPicked }) {
  const [exercises, setExercises] = useState([]);
  const [selectedId, setSelectedId] = useState("");
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  useEffect(() => {
    fetchExercises()
      .then((list) => {
        setExercises(list);
        if (list.length > 0) setSelectedId(list[0].id);
      })
      .catch((err) => setError(err.message))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(e) {
    e.preventDefault();
    if (!selectedId) return;
    setSubmitting(true);
    setError(null);
    try {
      await onPicked(selectedId);
    } catch (err) {
      setError(err.message);
      setSubmitting(false);
    }
  }

  return (
    <div className="app-shell">
      <form className="mission-card onboarding-card" onSubmit={handleSubmit}>
        <h1 className="onboarding-title">다음 동작을 골라볼까요</h1>
        <p className="onboarding-sub">새로운 4주가 이 동작으로 시작돼요.</p>

        <label className="field-label">
          다음 동작
          {loading ? (
            <p className="onboarding-sub">동작 목록을 불러오는 중…</p>
          ) : (
            <select
              className="field-input"
              value={selectedId}
              onChange={(e) => setSelectedId(e.target.value)}
              required
            >
              {exercises.map((ex) => (
                <option key={ex.id} value={ex.id}>
                  {ex.name}
                </option>
              ))}
            </select>
          )}
        </label>

        {error && <p className="error-text">{error}</p>}

        <button className="commit-btn" type="submit" disabled={submitting || loading}>
          {submitting ? "시작하는 중…" : "이 동작으로 계속하기"}
        </button>
      </form>
    </div>
  );
}

export default function App() {
  const [userId, setUserId] = useState(() => localStorage.getItem(USER_ID_KEY));
  const [mission, setMission] = useState(null);
  const [rhythm, setRhythm] = useState([]);
  const [cycleSummary, setCycleSummary] = useState(null);
  const [needsExercisePick, setNeedsExercisePick] = useState(false);
  const [confirming, setConfirming] = useState(false);
  const [completed, setCompleted] = useState(0);
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function handleUserGone() {
    // 저장된 계정이 서버에 더 이상 없음 (예: 개발 중 DB 초기화) — 온보딩으로 되돌아감
    localStorage.removeItem(USER_ID_KEY);
    setUserId(null);
  }

  function loadToday(id) {
    setLoading(true);
    setError(null);
    // 앱에 들어올 때 가장 먼저 "확인 안 한 마스터 요약"이 있는지부터 본다 (다음 동작 선택이
    // 필요한 진짜 결정이라 블로킹). 연장 요약은 여기 안 걸리고, 아래 today 응답에 배너로 딸려온다.
    return fetchCycleSummary(id)
      .then((summary) => {
        if (summary) {
          setCycleSummary(summary);
          return null;
        }
        setCycleSummary(null);
        return Promise.all([fetchTodayMission(id), fetchRhythm(id)]).then(([m, rhythmDays]) => {
          setMission(m);
          setRhythm(rhythmDays);
          setCompleted(m.completedValueToday);
          setSubmitted(m.achievedToday);
        });
      })
      .catch((err) => {
        if (err.status === 404) {
          handleUserGone();
          return;
        }
        setError(err.message);
      })
      .finally(() => setLoading(false));
  }

  async function handleAckCycleSummary() {
    setConfirming(true);
    try {
      await acknowledgeCycleSummary(userId, cycleSummary.id);
      setCycleSummary(null);
      setNeedsExercisePick(true);
    } catch (err) {
      if (err.status === 404) {
        handleUserGone();
        return;
      }
      setError(err.message);
    } finally {
      setConfirming(false);
    }
  }

  async function handleExercisePicked(exerciseId) {
    await startNextExercise(userId, exerciseId);
    setNeedsExercisePick(false);
    await loadToday(userId);
  }

  useEffect(() => {
    if (userId) loadToday(userId);
  }, [userId]);

  if (!userId) {
    return (
      <OnboardingForm
        onCreated={(id) => {
          setUserId(id);
        }}
      />
    );
  }

  if (cycleSummary) {
    return (
      <CycleSummaryScreen
        summary={cycleSummary}
        onConfirm={handleAckCycleSummary}
        confirming={confirming}
        error={error}
      />
    );
  }

  if (needsExercisePick) {
    return <ExercisePicker onPicked={handleExercisePicked} />;
  }

  if (loading && !mission) {
    return (
      <div className="app-shell">
        <p className="onboarding-sub">오늘의 미션을 불러오는 중…</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="app-shell">
        <div className="mission-card">
          <p className="error-text">{error}</p>
          <button className="commit-btn" onClick={() => loadToday(userId)}>
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  if (!mission) return null;

  const unitLabel = mission.unit === "SECONDS" ? "초" : "회";
  const achieved = completed >= mission.targetValue;
  const progressRatio = mission.targetValue === 0 ? 0 : completed / mission.targetValue;
  const rollingCount = Math.min(mission.rollingSuccessCount, RHYTHM_DAYS);
  const targetNote = buildTargetNote(mission, unitLabel);

  function handleAdjust(delta) {
    setCompleted((prev) => Math.max(0, prev + delta));
    setSubmitted(false);
  }

  async function handleCommit() {
    try {
      await submitCheckIn(userId, completed);
      await loadToday(userId);
    } catch (err) {
      if (err.status === 404) {
        handleUserGone();
        return;
      }
      setError(err.message);
    }
  }

  return (
    <div className="app-shell">
      <div className="mission-card">
        {mission.extensionNotice && (
          <div className="banner">
            {`4주 중 ${mission.extensionNotice.successWeeks}주 성공, 조금 더 다져볼게요 — ${mission.extensionNotice.exerciseName}으로 계속 이어가요`}
          </div>
        )}

        {mission.notificationMessage && (
          <div className="banner">{mission.notificationMessage}</div>
        )}

        {mission.rollingStreakRecordMessage && (
          <div className="banner">{mission.rollingStreakRecordMessage}</div>
        )}

        <div className="cycle-row">
          <span className="exercise-name">오늘은, {mission.exerciseName}</span>
          <div
            className="week-leaves"
            title="4주 동안 이 동작을 꾸준히 채우면 다음 동작이 열려요"
          >
            {Array.from({ length: WEEKS_PER_CYCLE }).map((_, i) => (
              <span
                key={i}
                className={`leaf ${i < mission.currentWeek ? "filled" : ""}`}
              />
            ))}
            <span className="week-label">{mission.currentWeek}/4주차</span>
          </div>
        </div>

        {targetNote && <p className="target-note">{targetNote}</p>}

        <GrowthRing
          progressRatio={progressRatio}
          count={completed}
          target={mission.targetValue}
          unitLabel={unitLabel}
        />

        <div className="stepper-row">
          <button className="stepper-btn" onClick={() => handleAdjust(-1)} aria-label="1개 빼기">
            −
          </button>
          <button className="stepper-btn" onClick={() => handleAdjust(1)} aria-label="1개 더하기">
            +
          </button>
        </div>

        <button
          className={`commit-btn ${submitted ? "done" : ""}`}
          onClick={handleCommit}
          disabled={completed === 0}
        >
          {submitted ? "오늘 기록 완료" : achieved ? "목표 채우고 기록하기" : "여기까지 기록하기"}
        </button>

        <div
          className="rhythm-section"
          title="★ 목표 초과 달성 · ● 목표 달성 · • 조금 채웠어요 — 5일 이상이면 이번 주는 그걸로 충분해요"
        >
          <p className="rhythm-caption">이번 주 리듬 · 7일 중 {rollingCount}일 달성</p>
          <div className="rhythm-row" aria-label={`최근 ${RHYTHM_DAYS}일 중 ${rollingCount}일 달성`}>
            {rhythm.map((day) => (
              <span
                key={day.date}
                className={`rhythm-day ${day.status.toLowerCase()}`}
                title={RHYTHM_LABEL[day.status]}
              >
                {RHYTHM_SYMBOL[day.status]}
              </span>
            ))}
          </div>
          <div className="rhythm-legend">
            <span><i className="legend-dot exceeded" />초과</span>
            <span><i className="legend-dot exact" />달성</span>
            <span><i className="legend-dot below" />조금 채움</span>
          </div>
          <p className="rhythm-reassure">
            하루를 놓쳐도 습관은 끊기지 않아요 — 목표는 계속 진행 중이에요
          </p>
        </div>

        {rollingCount <= 2 && (
          <div className="banner">
            요즘 며칠 못 채웠어도 괜찮아요. 하나만 다시 채우면, 리듬은 바로 돌아와요.
          </div>
        )}
      </div>
    </div>
  );
}
