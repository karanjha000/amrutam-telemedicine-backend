package com.amrutam.api.controller;

import com.amrutam.api.dto.PrescriptionDto;
import com.amrutam.api.entity.Prescription;
import com.amrutam.api.repository.PrescriptionRepository;
import com.amrutam.common.exception.NotFoundException;
import com.amrutam.common.exception.RestResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Prescription endpoints.
 */
@RestController
@RequestMapping("/api/v1/consultations")
public class PrescriptionsController {

    private final PrescriptionRepository prescriptions;

    /**
     * @param prescriptions prescription repository
     */
    public PrescriptionsController(PrescriptionRepository prescriptions) {
        this.prescriptions = prescriptions;
    }

    /**
     * Fetches the prescription for a consultation.
     *
     * @param id consultation id
     * @return the prescription
     */
    @GetMapping("/{id}/prescription")
    @Transactional(readOnly = true)
    public ResponseEntity<RestResponse<PrescriptionDto>> forConsultation(@PathVariable UUID id) {
        Prescription p = prescriptions.findByConsultationId(id)
                .orElseThrow(() -> new NotFoundException("prescription", id.toString()));
        return ResponseEntity.ok(RestResponse.ok(toDto(p)));
    }

    private PrescriptionDto toDto(Prescription p) {
        return new PrescriptionDto(p.getId(), p.getConsultationId(), p.getPatientId(), p.getDoctorId(),
                p.getMedications(), p.getDosageNotes(), p.getCreatedAt());
    }
}
