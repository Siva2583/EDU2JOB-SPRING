package com.edu2job.repositories;

import com.edu2job.models.UserPredictionHistory;
import com.edu2job.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserPredictionHistoryRepository extends JpaRepository<UserPredictionHistory, Long> {
    Optional<UserPredictionHistory> findFirstByUserOrderByCreatedAtDesc(User user);
}