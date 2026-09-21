package io.artsy.Repository;

import io.artsy.Model.UserTbl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserTbl, Integer> {

    boolean existsByUsernameAndPassword(String username, String password);

    Optional<UserTbl> findByUsernameAndEmail(String username, String email);

    Optional<UserTbl> findByVerificationToken(String token);

    Optional<UserTbl> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

}