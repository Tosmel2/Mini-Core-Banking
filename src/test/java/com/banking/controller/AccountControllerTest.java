package com.banking.controller;

import com.banking.dto.*;
import com.banking.service.AccountService;
import com.banking.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.banking.entity.Account;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AccountController.class)
@AutoConfigureMockMvc(addFilters = false)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Mock security filter to avoid loading full security context in web slice tests
    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // Mock JPA mapping context to avoid JPA/Auditing initialization in @WebMvcTest slice
    @MockBean(name = "jpaMappingContext")
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @MockBean
    private AccountService accountService;

    @Test
    @DisplayName("GET /api/v1/accounts/current returns 204 when no current account")
    void getCurrentAccount_NoContent() throws Exception {
        Mockito.when(accountService.getCurrentAccount()).thenReturn(null);

        mockMvc.perform(get("/accounts/current"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/accounts/{id} returns 200 with account body")
    void getAccount_Ok() throws Exception {
        AccountDto dto = AccountDto.builder()
                .id(1L)
                .accountNumber("1234567890")
                .accountType(Account.AccountType.SAVINGS)
                .balance(new BigDecimal("100.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(accountService.getAccount(eq(1L))).thenReturn(dto);

        mockMvc.perform(get("/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountNumber").value("1234567890"))
                .andExpect(jsonPath("$.currency").value("USD"));
    }

    @Test
    @DisplayName("POST /api/v1/accounts creates an account and returns 201")
    void createAccount_Created() throws Exception {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setAccountType(Account.AccountType.CURRENT);
        req.setInitialDeposit(new BigDecimal("50.00"));

        AccountDto dto = AccountDto.builder()
                .id(5L)
                .accountNumber("5555555555")
                .accountType(Account.AccountType.CURRENT)
                .balance(new BigDecimal("50.00"))
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();

        Mockito.when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(dto);

        String body = "{\n" +
                "  \"accountType\": \"CURRENT\",\n" +
                "  \"initialDeposit\": 50.00\n" +
                "}";

        mockMvc.perform(post("/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Content-Type", org.hamcrest.Matchers.containsString("application/json")))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.accountNumber").value("5555555555"));
    }

    @Test
    @DisplayName("GET /api/v1/accounts/lookup/{acct} returns beneficiary details")
    void lookupBeneficiary_Ok() throws Exception {
        BeneficiaryLookupResponse resp = BeneficiaryLookupResponse.builder()
                .accountNumber("9999999999")
                .accountType(Account.AccountType.SAVINGS)
                .currency("USD")
                .status(Account.AccountStatus.ACTIVE)
                .holderDisplayName("John D.")
                .build();
        Mockito.when(accountService.lookupBeneficiary(eq("9999999999"))).thenReturn(resp);

        mockMvc.perform(get("/accounts/lookup/9999999999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("9999999999"))
                .andExpect(jsonPath("$.holderDisplayName").value("John D."));
    }
}
