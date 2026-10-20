package com.amrutam.api.repository;

import com.amrutam.api.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

/**
 * JPA repository for {@link User}.
 */
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by exact username.
     *
     * @param username the username
     * @return the user, if present
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user by exact email.
     *
     * @param email the email
     * @return the user, if present
     */
    Optional<User> findByEmail(String email);

    /**
     * Case-insensitive search over display name, email and username.
     *
     * @param query the search text
     * @param pageable paging
     * @return matching page
     */
    @Query("select u from User u where lower(u.displayName) like %lower(:query) "
            + "or lower(u.email) like %lower(:query) "
            + "or lower(u.username) like %lower(:query)")
    Page<User> searchUsers(String query, Pageable pageable);

    /**
     * @param username username
     * @return true if a user with this username exists
     */
    boolean existsByUsername(String username);
}
