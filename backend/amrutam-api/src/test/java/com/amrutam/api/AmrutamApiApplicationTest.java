package com.amrutam.api;

import com.amrutam.api.repository.AuditLogRepository;
import com.amrutam.api.repository.ConsultationRepository;
import com.amrutam.api.repository.DoctorRepository;
import com.amrutam.api.repository.PaymentRepository;
import com.amrutam.api.repository.UserRepository;
import com.amrutam.api.service.BookingService;
import com.amrutam.api.service.DoctorService;
import com.amrutam.api.service.PaymentService;
import com.amrutam.api.service.UserService;
import com.amrutam.common.idempotency.IdempotencyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Full-context smoke test: boots the whole application context against H2
 * (schema-h2.sql) and asserts that every primary bean wires up — services,
 * the idempotency kernel, repositories and the security filter chain.
 */
@SpringBootTest
@ActiveProfiles("test")
class AmrutamApiApplicationTest {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private UserService userService;

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private IdempotencyService idempotencyService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ConsultationRepository consultationRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Test
    void contextLoadsAndAllPrimaryBeansWire() {
        assertThat(context).isNotNull();
        assertThat(userService).isNotNull();
        assertThat(doctorService).isNotNull();
        assertThat(paymentService).isNotNull();
        assertThat(bookingService).isNotNull();
        assertThat(idempotencyService).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(doctorRepository).isNotNull();
        assertThat(consultationRepository).isNotNull();
        assertThat(paymentRepository).isNotNull();
        assertThat(auditLogRepository).isNotNull();
    }

    @Test
    void h2SchemaWasApplied() {
        // A successful autowire of the repositories above already implies
        // the H2 datasource initialized; prove it by issuing a real query.
        // data-h2.sql is intentionally empty, so both counts are zero.
        assertThat(userRepository.count()).isZero();
        assertThat(doctorRepository.count()).isZero();
        assertThat(consultationRepository.count()).isZero();
    }
}
