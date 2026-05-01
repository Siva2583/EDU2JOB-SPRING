package com.edu2job.repositories;

import com.edu2job.models.PredHistory;
import com.edu2job.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PredHistoryRepository extends JpaRepository<PredHistory, Long> {
    List<PredHistory> findByUserOrderByCreatedAtDesc(User user);
}