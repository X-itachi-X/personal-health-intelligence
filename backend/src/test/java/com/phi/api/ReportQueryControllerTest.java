package com.phi.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.phi.auth.JwtService;
import com.phi.domain.Account;
import com.phi.domain.AccountRepository;
import com.phi.domain.DocumentType;
import com.phi.domain.LabReport;
import com.phi.domain.LabReportRepository;
import com.phi.domain.Person;
import com.phi.domain.PersonRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PersonRepository personRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private LabReportRepository labReportRepository;

    @Autowired
    private JwtService jwtService;

    @Test
    void listReportsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/reports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listReportsIncludesDocumentType() throws Exception {
        Person person = personRepository.save(new Person("API Test"));
        Account account = accountRepository.save(new Account(person, "api-test@example.com", "hash", null));
        String token = jwtService.createToken(account.getId(), account.getEmail());

        LabReport prescription = new LabReport(
                person,
                null,
                account.getId(),
                "rx.pdf",
                "/tmp/rx.pdf",
                "hash-rx",
                DocumentType.PRESCRIPTION
        );
        prescription.markCompleted("rx text");
        labReportRepository.save(prescription);

        mockMvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentType").value("PRESCRIPTION"))
                .andExpect(jsonPath("$[0].filename").value("rx.pdf"));
    }
}
