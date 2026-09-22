package com.phi.api;

import com.phi.auth.AuthenticatedAccount;
import com.phi.medication.MedicationDtos;
import com.phi.medication.MedicationService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/persons/{personId}/medications")
public class MedicationController {

    private final MedicationService medicationService;

    public MedicationController(MedicationService medicationService) {
        this.medicationService = medicationService;
    }

    @GetMapping
    public List<MedicationDtos.MedicationView> list(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId
    ) {
        return medicationService.list(account, personId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationDtos.MedicationView create(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @RequestBody MedicationDtos.CreateMedicationRequest request
    ) {
        return medicationService.create(account, personId, request);
    }

    @PostMapping("/{medicationId}/end")
    public MedicationDtos.MedicationView end(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @PathVariable String medicationId,
            @RequestBody(required = false) MedicationDtos.EndMedicationRequest request
    ) {
        MedicationDtos.EndMedicationRequest body = request != null
                ? request
                : new MedicationDtos.EndMedicationRequest(null);
        return medicationService.endMedication(account, personId, medicationId, body);
    }

    @DeleteMapping("/{medicationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AuthenticatedAccount account,
            @PathVariable Long personId,
            @PathVariable String medicationId
    ) {
        medicationService.delete(account, personId, medicationId);
    }
}
