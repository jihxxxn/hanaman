import { useEffect, useState } from "react";
import { createUser, fetchExercises, fetchRhythm, fetchTodayMission, submitCheckIn } from "./api";

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

export default function App() {
  const [userId, setUserId] = useState(() => localStorage.getItem(USER_ID_KEY));
  const [mission, setMission] = useState(null);
  const [rhythm, setRhythm] = useState([]);
  const [completed, setCompleted] = useState(0);
  const [submitted, setSubmitted] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  function loadToday(id) {
    setLoading(true);
    setError(null);
    return Promise.all([fetchTodayMission(id), fetchRhythm(id)])
      .then(([mission, rhythmDays]) => {
        setMission(mission);
        setRhythm(rhythmDays);
        setCompleted(mission.completedValueToday);
        setSubmitted(mission.achievedToday);
      })
      .catch((err) => {
        if (err.status === 404) {
          // 저장된 계정이 서버에 더 이상 없음 (예: 개발 중 DB 초기화) — 온보딩으로 되돌아감
          localStorage.removeItem(USER_ID_KEY);
          setUserId(null);
          return;
        }
        setError(err.message);
      })
      .finally(() => setLoading(false));
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
        localStorage.removeItem(USER_ID_KEY);
        setUserId(null);
        return;
      }
      setError(err.message);
    }
  }

  return (
    <div className="app-shell">
      <div className="mission-card">
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
