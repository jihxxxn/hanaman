package com.hanaman.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * "이번 주 리듬" (최근 7일 달성 여부)을 Redis에 캐싱해서
 * 매번 CheckIn 테이블을 집계 쿼리하지 않고 O(1)에 가깝게 응답하기 위한 서비스.
 *
 * key: hanaman:rolling:{userId}:{yyyy-MM-dd}  value: "1" (달성) / "0" (미달성)
 * TTL 8일 — 자연스럽게 오래된 날짜는 만료되어 별도 정리 배치가 필요 없음.
 */
@Service
@RequiredArgsConstructor
public class RollingStreakService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Duration TTL = Duration.ofDays(8);

    private final StringRedisTemplate redisTemplate;

    public void markDay(UUID userId, LocalDate date, boolean achieved) {
        String key = buildKey(userId, date);
        redisTemplate.opsForValue().set(key, achieved ? "1" : "0", TTL);
    }

    /** 최근 7일(오늘 포함) 중 달성한 일수를 반환. 캐시 미스(만료/미기록)는 미달성으로 간주. */
    public int getRollingSuccessCount(UUID userId, LocalDate today) {
        List<String> keys = java.util.stream.IntStream.range(0, 7)
                .mapToObj(i -> buildKey(userId, today.minusDays(i)))
                .collect(Collectors.toList());

        List<String> values = redisTemplate.opsForValue().multiGet(keys);
        if (values == null) {
            return 0;
        }
        return (int) values.stream().filter(v -> "1".equals(v)).count();
    }

    private String buildKey(UUID userId, LocalDate date) {
        return "hanaman:rolling:" + userId + ":" + date.format(DATE_FMT);
    }
}
