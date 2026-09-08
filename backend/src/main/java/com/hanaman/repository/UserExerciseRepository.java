package com.hanaman.repository;

import com.hanaman.domain.User;
import com.hanaman.domain.UserExercise;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserExerciseRepository extends JpaRepository<UserExercise, UUID> {

    Optional<UserExercise> findByUserAndStatus(User user, UserExercise.Status status);

    List<UserExercise> findAllByUserOrderByOrderIndexAsc(User user);
}
