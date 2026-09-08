const API_BASE = import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080";

async function request(path, options = {}) {
  const res = await fetch(`${API_BASE}${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });

  if (!res.ok) {
    const body = await res.text().catch(() => "");
    throw new Error(`${path} 요청 실패 (${res.status}) ${body}`);
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

export function submitCheckIn(userId, completedValue) {
  return request(`/api/users/${userId}/checkins`, {
    method: "POST",
    body: JSON.stringify({ completedValue }),
  });
}
