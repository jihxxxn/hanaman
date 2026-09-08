package com.hanaman;

import com.hanaman.domain.Exercise;
import com.hanaman.repository.ExerciseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** 개발/데모용 초기 동작 마스터 데이터 시드. 운영 환경에서는 마이그레이션 스크립트로 대체. */
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final ExerciseRepository exerciseRepository;

    @Override
    public void run(String... args) {
        if (exerciseRepository.count() > 0) {
            return;
        }

        exerciseRepository.save(Exercise.builder()
                .name("스쿼트").unit(Exercise.Unit.REPS).defaultStartValue(5).minValue(3).build());
        exerciseRepository.save(Exercise.builder()
                .name("런지").unit(Exercise.Unit.REPS).defaultStartValue(6).minValue(4).build());
        exerciseRepository.save(Exercise.builder()
                .name("플랭크").unit(Exercise.Unit.SECONDS).defaultStartValue(20).minValue(10).build());
        exerciseRepository.save(Exercise.builder()
                .name("푸시업").unit(Exercise.Unit.REPS).defaultStartValue(3).minValue(2).build());
    }
}
