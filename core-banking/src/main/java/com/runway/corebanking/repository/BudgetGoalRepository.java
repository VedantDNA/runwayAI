package com.runway.corebanking.repository;

import com.runway.corebanking.entity.BudgetGoal;
import com.runway.corebanking.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetGoalRepository extends JpaRepository<BudgetGoal, UUID> {

    List<BudgetGoal> findByUserId(UUID userId);

    Optional<BudgetGoal> findByUserIdAndCategory(UUID userId, Transaction.Category category);
}
