package com.amrutam.api.config;

import com.amrutam.api.entity.User;
import com.amrutam.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Dev-only bootstrap: seeds users with real BCrypt password hashes at boot.
 *
 * <p>{@code data.sql} ships placeholder hashes (a checked-in DDL file must
 * stay generic) and, on re-run under {@code spring.sql.init.mode=always},
 * re-applies the same {@code now()}-relative availability windows. This
 * runner — active only under the {@code dev} profile — upgrades every
 * {@code $2a$10$PLACEHOLDER} row to a real hash so logins work in dev,
 * and inserts any seed user that is not yet present. Prod never activates
 * this bean; prod users are created through the API.</p>
 */
@Component
@Profile("dev")
public class DevAdminSeedRunner implements ApplicationRunner {

    /** A seed user: username + the password its placeholder hash maps to. */
    public record SeedUser(String username, String password) {
    }

    /** The seeded users (passwords documented in {@code data.sql}). */
    public static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin", "admin123"),
            new SeedUser("doc.ramesh", "doctor123"),
            new SeedUser("doc.aisha", "doctor123"),
            new SeedUser("doc.iyer", "doctor123"),
            new SeedUser("patient.arjun", "patient123"));

    /** Prefix of the placeholder hash written by {@code data.sql}. */
    public static final String PLACEHOLDER_PREFIX = "$2a$10$PLACEHOLDER";

    private static final Logger log = LoggerFactory.getLogger(DevAdminSeedRunner.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;

    /**
     * @param users user repository
     * @param encoder password encoder
     */
    public DevAdminSeedRunner(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int upgraded = 0;
        int created = 0;
        for (SeedUser su : SEED_USERS) {
            Optional<User> existing = users.findByUsername(su.username());
            if (existing.isPresent()) {
                User u = existing.get();
                if (u.getPasswordHash() != null && u.getPasswordHash().startsWith(PLACEHOLDER_PREFIX)) {
                    u.setPasswordHash(encoder.encode(su.password()));
                    u.setUpdatedAt(Instant.now());
                    users.save(u);
                    upgraded++;
                }
            } else {
                Instant now = Instant.now();
                User user = new User();
                user.setUsername(su.username());
                user.setPasswordHash(encoder.encode(su.password()));
                user.setRole(User.RoleString.PATIENT);
                user.setMfaEnabled(false);
                user.setActive(true);
                user.setCreatedAt(now);
                user.setUpdatedAt(now);
                users.save(user);
                created++;
            }
        }
        if (upgraded > 0 || created > 0) {
            log.info("Dev seed complete: {} placeholder row(s) upgraded to real BCrypt hashes, "
                    + "{} user(s) created (dev profile only)", upgraded, created);
        }
    }
}
