import { useMemo, useState } from "react";

// 데모용 로컬 상태 — 실제로는 GET /api/users/{id}/today, POST /api/users/{id}/checkins 로 대체됩니다.
const INITIAL_MISSION = {
  exerciseName: "스쿼트",
  unit: "REPS",
  target: 12,
  currentWeek: 2,
  triggerHabit: "양치하고 나서",
  // 최근 7일 리듬 (오늘 제외 6일 + 오늘) — true = 목표 달성
  rhythm: [true, true, false, true, true, false, null],
};

const WEEKS_PER_CYCLE = 4;

function GrowthRing({ progressRatio, count, target }) {
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
        <span className="ring-target">/ {target}회 목표</span>
      </div>
    </div>
  );
}

export default function App() {
  const [mission] = useState(INITIAL_MISSION);
  const [completed, setCompleted] = useState(0);
  const [submitted, setSubmitted] = useState(false);

  const achieved = completed >= mission.target;
  const progressRatio = mission.target === 0 ? 0 : completed / mission.target;

  const todayRhythm = useMemo(() => {
    const copy = [...mission.rhythm];
    copy[copy.length - 1] = achieved;
    return copy;
  }, [mission.rhythm, achieved]);

  const rollingCount = todayRhythm.filter(Boolean).length;

  function handleAdjust(delta) {
    setCompleted((prev) => Math.max(0, prev + delta));
    setSubmitted(false);
  }

  function handleCommit() {
    // 실제 구현: POST /api/users/{id}/checkins { completedValue: completed }
    setSubmitted(true);
  }

  return (
    <div className="app-shell">
      <div className="mission-card">
        <div className="cycle-row">
          <span className="exercise-name">오늘은, {mission.exerciseName}</span>
          <div className="week-leaves" aria-label={`4주 중 ${mission.currentWeek}주차`}>
            {Array.from({ length: WEEKS_PER_CYCLE }).map((_, i) => (
              <span
                key={i}
                className={`leaf ${i < mission.currentWeek ? "filled" : ""}`}
              />
            ))}
          </div>
        </div>

        <GrowthRing progressRatio={progressRatio} count={completed} target={mission.target} />

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

        {mission.triggerHabit && (
          <p className="trigger-note">"{mission.triggerHabit}" 하나만 하면 끝이에요</p>
        )}

        <div className="rhythm-row" aria-label="최근 7일 리듬">
          {todayRhythm.map((hit, i) => (
            <span key={i} className={`rhythm-day ${hit ? "hit" : ""}`}>
              {hit ? "●" : ""}
            </span>
          ))}
        </div>

        {rollingCount <= 2 && (
          <div className="banner">
            이번 주는 조금 느슨해도 괜찮아요. 하나만 채우면, 오늘은 그걸로 충분해요.
          </div>
        )}
      </div>
    </div>
  );
}
