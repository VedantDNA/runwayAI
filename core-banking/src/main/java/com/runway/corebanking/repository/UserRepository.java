package com.runway.corebanking.repository;

import com.runway.corebanking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data repository for User.
 *
 * WHY JpaRepository: it provides CRUD + pagination out of the box.
 * Spring Data generates the implementation at startup — zero boilerplate SQL.
 * Method names like findByEmail translate to "SELECT * FROM users WHERE email = ?".
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
