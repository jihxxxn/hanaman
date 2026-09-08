const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    const err = new Error(`${path} 요청 실패 (${res.status}) ${body}`);
    err.status = res.status;
    throw err;
  }

  const text = await res.text();
  return text ? JSON.parse(text) : null;
}

export function fetchExercises() {
  return request("/api/exercises");
}

// 응답이 생성된 사용자 UUID 문자열이다.
export function createUser({ nickname, triggerHabit, firstExerciseName }) {
  return request("/api/users", {
    method: "POST",
    body: JSON.stringify({ nickname, triggerHabit, firstExerciseName }),
  });
}

export function fetchTodayMission(userId) {
  return request(`/api/users/${userId}/today`);
}

// 최근 7일치 기록 상태 배열: [{ date, status: "NONE"|"BELOW"|"EXACT"|"EXCEEDED" }, ...] (오래된 날짜 순)
export function fetchRhythm(userId) {
  return request(`/api/users/${userId}/rhythm`);
}

export function submitCheckIn(userId, completedValue) {
  return request(`/api/users/${userId}/checkins`, {
    method: "POST",
    body: JSON.stringify({ completedValue }),
  });
}

// 확인 안 한 사이클 요약이 있으면 객체, 없으면 null (백엔드가 204를 내려줌)
export function fetchCycleSummary(userId) {
  return request(`/api/users/${userId}/cycle-summary`);
}

export function acknowledgeCycleSummary(userId, summaryId) {
  return request(`/api/users/${userId}/cycle-summary/${summaryId}/ack`, {
    method: "POST",
  });
}

// 마스터 후 다음 동작 선택 — 응답은 새로 생성된 UserExercise의 UUID 문자열
export function startNextExercise(userId, exerciseId) {
  return request(`/api/users/${userId}/exercises`, {
    method: "POST",
    body: JSON.stringify({ exerciseId }),
  });
}
