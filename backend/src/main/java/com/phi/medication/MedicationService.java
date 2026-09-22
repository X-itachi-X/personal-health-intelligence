package com.phi.medication;

import com.phi.access.AccessControlService;
import com.phi.auth.AuthenticatedAccount;
import com.phi.domain.MedicationCourseType;
import com.phi.domain.MedicationEvent;
import com.phi.domain.MedicationEventRepository;
import com.phi.domain.MedicationSource;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MedicationService {

    private final MedicationEventRepository medicationEventRepository;
    private final PersonRepository personRepository;
    private final AccessControlService accessControl;

    public MedicationService(
            MedicationEventRepository medicationEventRepository,
            PersonRepository personRepository,
            AccessControlService accessControl
    ) {
        this.medicationEventRepository = medicationEventRepository;
        this.personRepository = personRepository;
        this.accessControl = accessControl;
    }

    @Transactional(readOnly = true)
    public List<MedicationDtos.MedicationView> list(AuthenticatedAccount account, Long personId) {
        requireCanManage(account, personId);
        return medicationEventRepository.findByPersonId(personId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional
    public MedicationDtos.MedicationView create(
            AuthenticatedAccount account,
            Long personId,
            MedicationDtos.CreateMedicationRequest request
    ) {
        requireCanManage(account, personId);
        validateRequest(request);

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Person not found"));

        String familyId = accessControl.resolveFamilyId(account, null);
        MedicationCourseType courseType = parseCourseType(request.courseType());
        LocalDate expectedEndOn = resolveExpectedEndOn(request, courseType);
        MedicationEvent event = new MedicationEvent(
                person,
                familyId,
                request.medicationName().trim(),
                normalize(request.dosage()),
                request.startedOn(),
                request.endedOn(),
                normalize(request.notes()),
                courseType,
                normalize(request.scheduleText()),
                request.durationDays(),
                expectedEndOn,
                MedicationSource.MANUAL,
                null,
                account.accountId()
        );
        return toView(medicationEventRepository.save(event));
    }

    @Transactional
    public MedicationDtos.MedicationView endMedication(
            AuthenticatedAccount account,
            Long personId,
            String medicationId,
            MedicationDtos.EndMedicationRequest request
    ) {
        requireCanManage(account, personId);
        MedicationEvent event = medicationEventRepository.findById(medicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found"));
        if (!event.getPerson().getId().equals(personId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found");
        }

        LocalDate endedOn = request.endedOn() != null ? request.endedOn() : LocalDate.now();
        event.endMedication(endedOn);
        return toView(medicationEventRepository.save(event));
    }

    @Transactional
    public void delete(AuthenticatedAccount account, Long personId, String medicationId) {
        requireCanManage(account, personId);
        MedicationEvent event = medicationEventRepository.findById(medicationId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found"));
        if (!event.getPerson().getId().equals(personId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Medication not found");
        }
        medicationEventRepository.delete(event);
    }

    private void requireCanManage(AuthenticatedAccount account, Long personId) {
        accessControl.requireAuthenticated(account);
        accessControl.requireCanUploadFor(account, accessControl.resolveFamilyId(account, null), personId);
    }

    private static void validateRequest(MedicationDtos.CreateMedicationRequest request) {
        if (request.medicationName() == null || request.medicationName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "medicationName is required");
        }
        if (request.startedOn() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "startedOn is required");
        }
        if (request.endedOn() != null && request.endedOn().isBefore(request.startedOn())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "endedOn cannot be before startedOn");
        }
    }

    private MedicationDtos.MedicationView toView(MedicationEvent event) {
        return new MedicationDtos.MedicationView(
                event.getId(),
                event.getPerson().getId(),
                event.getMedicationName(),
                event.getDosage(),
                event.getStartedOn() != null ? event.getStartedOn().toString() : null,
                event.getEndedOn() != null ? event.getEndedOn().toString() : null,
                event.getNotes(),
                event.getCourseType().name(),
                event.getScheduleText(),
                event.getDurationDays(),
                event.getExpectedEndOn() != null ? event.getExpectedEndOn().toString() : null,
                event.getSource().name(),
                event.getSourceReport() != null ? event.getSourceReport().getId() : null,
                event.getEndedOn() == null
        );
    }

    private static MedicationCourseType parseCourseType(String raw) {
        if (raw == null || raw.isBlank()) {
            return MedicationCourseType.UNKNOWN;
        }
        try {
            return MedicationCourseType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid courseType: " + raw);
        }
    }

    private static LocalDate resolveExpectedEndOn(
            MedicationDtos.CreateMedicationRequest request,
            MedicationCourseType courseType
    ) {
        if (request.expectedEndOn() != null) {
            return request.expectedEndOn();
        }
        if (courseType == MedicationCourseType.ACUTE
                && request.startedOn() != null
                && request.durationDays() != null
                && request.durationDays() > 0) {
            return request.startedOn().plusDays(request.durationDays() - 1L);
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
