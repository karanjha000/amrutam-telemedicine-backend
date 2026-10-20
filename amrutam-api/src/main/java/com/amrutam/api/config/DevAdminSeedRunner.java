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
 * Dev-only bootstrap: seeds the admin, doctor and patient users with
 * BCrypt-generated password hashes on first boot.
 *
 * <p>Passwords cannot live in {@code data.sql} (a checked-in DDL file must
 * stay generic), so this runner generates them at dev-bootstrap time via
 * {@link PasswordEncoder}. All seeded users are idempotent: the runner only
 * inserts rows whose usernames do not yet exist, so it is safe to re-run.
 * Only active under the {@code dev} profile.</p>
 */
@Component
@Profile("dev")
public class DevAdminSeedRunner implements ApplicationRunner {

    /**
     * Dev seed credentials — DO NOT use outside the dev profile. The
     * {@code prod} profile never activates this bean.
     */
    public record SeedUser(String username, String password, String displayName,
                           User.RoleString role) {
    }

    /** The seeded users, all with password {@code "admin123"} / {@code "secret123"}. */
    public static final List<SeedUser> SEED_USERS = List.of(
            new SeedUser("admin", "admin123", "Amrutam Admin", User.RoleString.ADMIN),
            new SeedUser("dr.smith", "secret123", "Dr. Smith", User.RoleString.DOCTOR),
            new SeedUser("dr.jones", "secret123", "Dr. Jones", User.RoleString.DOCTOR),
            new SeedUser("patient.john", "secret123", "John Doe", User.RoleString.PATIENT));

    /** Prefix of the placeholder hash data.sql writes; the runner upgrades it. */
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
        int seeded = 0;
        int upgraded = 0;
        for (SeedUser su : SEED_USERS) {
            Optional<User> existing = users.findByUsername(su.username());
            if (existing.isPresent()) {
                // data.sql seeds placeholder rows; upgrade to a real BCrypt
                // hash so logins work in dev.
                User u = existing.get();
                if (u.getPasswordHash() != null && u.getPasswordHash().startsWith(PLACEHOLDER_PREFIX)) {
                    u.setPasswordHash(encoder.encode(su.password()));
                    u.setUpdatedAt(Instant.now());
                    users.save(u);
                    upgraded++;
                }
                continue;
            }
            Instant now = Instant.now();
            User user = new User();
            user.setUsername(su.username());
            user.setPasswordHash(encoder.encode(su.password()));
            user.setDisplayName(su.displayName());
            user.setRole(su.role());
            user.setMfaEnabled(false);
            user.setActive(true);
            user.setCreatedAt(now);
            user.setUpdatedAt(now);
            users.save(user);
            seeded++;
        }
        if (seeded > 0 || upgraded > 0) {
            log.info("Dev seed complete: {} new user(s) created, {} placeholder row(s) upgraded "
                    + "(admin/admin123, dr.smith|dr.jones|patient.john: secret123 — dev profile only)",
                    seeded, upgraded);
        }
    }
}
