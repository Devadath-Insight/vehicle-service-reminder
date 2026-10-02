package com.vehicleservice.security;

import com.vehicleservice.TestData;
import com.vehicleservice.model.ExpenseType;
import com.vehicleservice.model.ReminderType;
import com.vehicleservice.model.ServiceRecord;
import com.vehicleservice.model.ServiceType;
import com.vehicleservice.model.User;
import com.vehicleservice.model.Vehicle;
import com.vehicleservice.repository.UserRepository;
import com.vehicleservice.repository.VehicleRepository;
import com.vehicleservice.service.ExpenseService;
import com.vehicleservice.service.ReminderService;
import com.vehicleservice.service.ServiceRecordService;
import com.vehicleservice.service.UserService;
import com.vehicleservice.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * End-to-end web tests with MockMvc: login/registration flow, protected pages, CSRF,
 * user data isolation through URLs, and a smoke test that renders every Thymeleaf page.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class SecurityIntegrationTest {

    private static final String ALICE = "alice.web@example.com";
    private static final String BOB = "bob.web@example.com";

    @Autowired private MockMvc mockMvc;
    @Autowired private UserService userService;
    @Autowired private UserRepository userRepository;
    @Autowired private VehicleService vehicleService;
    @Autowired private VehicleRepository vehicleRepository;
    @Autowired private ServiceRecordService serviceRecordService;
    @Autowired private ReminderService reminderService;
    @Autowired private ExpenseService expenseService;
    @Autowired private PasswordEncoder passwordEncoder;

    private User alice;
    private Vehicle alicesCar;
    private ServiceRecord alicesService;

    @BeforeEach
    void setUp() {
        alice = userService.register(TestData.registration(ALICE));
        userService.register(TestData.registration(BOB));
        alicesCar = vehicleService.create(alice, TestData.vehicle("KL01AB1234", 42_000));
        alicesService = serviceRecordService.create(alice.getId(),
                TestData.service(alicesCar.getId(), ServiceType.GENERAL_SERVICE, LocalDate.now().minusDays(5), 42_000, "5850"));
        reminderService.create(alice.getId(),
                TestData.reminder(alicesCar.getId(), ReminderType.INSURANCE, LocalDate.now().plusDays(10), null));
        reminderService.create(alice.getId(),
                TestData.reminder(alicesCar.getId(), ReminderType.POLLUTION_CERTIFICATE, LocalDate.now().minusDays(1), null));
        expenseService.create(alice.getId(),
                TestData.expense(alicesCar.getId(), ExpenseType.FUEL, "3200", LocalDate.now()));
    }

    // ---------- public vs protected pages ----------

    @Test
    void loginAndRegisterPagesArePublic() throws Exception {
        mockMvc.perform(get("/login")).andExpect(status().isOk()).andExpect(view().name("login"));
        mockMvc.perform(get("/register")).andExpect(status().isOk()).andExpect(view().name("register"));
        mockMvc.perform(get("/css/style.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/main.js")).andExpect(status().isOk());
    }

    @Test
    void protectedPagesRedirectToLoginWhenNotLoggedIn() throws Exception {
        for (String url : new String[]{"/dashboard", "/vehicles", "/services", "/reminders", "/expenses", "/reports", "/profile"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }
    }

    // ---------- registration ----------

    @Test
    void registrationCreatesAccountWithHashedPassword() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "New Person")
                        .param("email", "new.person@example.com")
                        .param("phone", "9876543210")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        User saved = userRepository.findByEmail("new.person@example.com").orElseThrow();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", saved.getPassword())).isTrue();
    }

    @Test
    void registrationWithInvalidDataShowsErrors() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "")
                        .param("email", "not-an-email")
                        .param("phone", "123")
                        .param("password", "short")
                        .param("confirmPassword", "different"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "fullName", "email", "phone", "password"));
    }

    @Test
    void registrationWithExistingEmailIsRejected() throws Exception {
        mockMvc.perform(post("/register").with(csrf())
                        .param("fullName", "Copy Cat")
                        .param("email", ALICE)
                        .param("phone", "9876543210")
                        .param("password", "password123")
                        .param("confirmPassword", "password123"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeHasFieldErrors("registrationRequest", "email"));
    }

    // ---------- login / logout ----------

    @Test
    void loginWithCorrectPasswordGoesToDashboard() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", ALICE).password("password", TestData.PASSWORD))
                .andExpect(authenticated().withUsername(ALICE))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void loginWithWrongPasswordFails() throws Exception {
        mockMvc.perform(formLogin("/login").user("email", ALICE).password("password", "wrong-password"))
                .andExpect(unauthenticated())
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void logoutRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/logout").with(user(ALICE)).with(csrf()))
                .andExpect(redirectedUrl("/login?logout"));
    }

    // ---------- CSRF ----------

    @Test
    void postWithoutCsrfTokenIsForbidden() throws Exception {
        mockMvc.perform(post("/vehicles/{id}/delete", alicesCar.getId()).with(user(ALICE)))
                .andExpect(status().isForbidden());
        assertThat(vehicleRepository.findById(alicesCar.getId())).isPresent();
    }

    // ---------- user data isolation ----------

    @Test
    void anotherUserCannotViewEditOrDeleteByChangingTheId() throws Exception {
        Long carId = alicesCar.getId();

        mockMvc.perform(get("/vehicles/{id}", carId).with(user(BOB)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
        mockMvc.perform(get("/vehicles/{id}/edit", carId).with(user(BOB)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/vehicles/{id}/delete", carId).with(user(BOB)).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/services/{id}/edit", alicesService.getId()).with(user(BOB)))
                .andExpect(status().isNotFound());

        assertThat(vehicleRepository.findById(carId)).isPresent();
    }

    @Test
    void ownerCanDeleteOwnVehicle() throws Exception {
        mockMvc.perform(post("/vehicles/{id}/delete", alicesCar.getId()).with(user(ALICE)).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vehicles"));
        assertThat(vehicleRepository.findById(alicesCar.getId())).isEmpty();
    }

    @Test
    void invalidIdShowsFriendlyErrorPage() throws Exception {
        mockMvc.perform(get("/vehicles/not-a-number").with(user(ALICE)))
                .andExpect(status().isBadRequest())
                .andExpect(view().name("error"));
        mockMvc.perform(get("/vehicles/999999").with(user(ALICE)))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"));
    }

    // ---------- every page renders (catches template / model mistakes) ----------

    @Test
    void allPagesRenderForLoggedInUser() throws Exception {
        String[][] pages = {
                {"/dashboard", "dashboard"},
                {"/vehicles", "vehicles"},
                {"/vehicles?q=kl01", "vehicles"},
                {"/vehicles/new", "vehicle-form"},
                {"/vehicles/" + alicesCar.getId(), "vehicle-details"},
                {"/vehicles/" + alicesCar.getId() + "?serviceType=GENERAL_SERVICE", "vehicle-details"},
                {"/vehicles/" + alicesCar.getId() + "/edit", "vehicle-form"},
                {"/services", "service-records"},
                {"/services?sort=cost_desc&serviceType=GENERAL_SERVICE", "service-records"},
                {"/services/new", "service-form"},
                {"/services/new?vehicleId=" + alicesCar.getId(), "service-form"},
                {"/services/" + alicesService.getId() + "/edit", "service-form"},
                {"/reminders", "reminders"},
                {"/reminders?status=OVERDUE", "reminders"},
                {"/reminders/new", "reminder-form"},
                {"/expenses", "expenses"},
                {"/expenses/new", "expense-form"},
                {"/reports", "reports"},
                {"/profile", "profile"}
        };
        for (String[] page : pages) {
            mockMvc.perform(get(page[0]).with(user(ALICE)))
                    .andExpect(status().isOk())
                    .andExpect(view().name(page[1]));
        }
    }

    @Test
    void pagesRenderForBrandNewUserWithNoData() throws Exception {
        for (String url : new String[]{"/dashboard", "/vehicles", "/services", "/reminders", "/expenses", "/reports", "/profile"}) {
            mockMvc.perform(get(url).with(user(BOB))).andExpect(status().isOk());
        }
        // forms that need a vehicle send a new user to "Add vehicle" first
        mockMvc.perform(get("/services/new").with(user(BOB)))
                .andExpect(redirectedUrl("/vehicles/new"));
    }

    @Test
    void addVehicleThroughTheForm() throws Exception {
        mockMvc.perform(post("/vehicles").with(user(ALICE)).with(csrf())
                        .param("registrationNumber", "kl 07 cd 4521")
                        .param("make", "Honda")
                        .param("model", "Activa 6G")
                        .param("year", "2021")
                        .param("fuelType", "PETROL")
                        .param("currentMileage", "18200"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/vehicles/*"));

        assertThat(vehicleService.isRegistrationTaken(alice.getId(), "KL 07 CD 4521", null)).isTrue();
    }

    @Test
    void duplicateRegistrationShowsFormError() throws Exception {
        mockMvc.perform(post("/vehicles").with(user(ALICE)).with(csrf())
                        .param("registrationNumber", "KL01AB1234")
                        .param("make", "Maruti Suzuki")
                        .param("model", "Swift")
                        .param("year", "2020")
                        .param("fuelType", "PETROL")
                        .param("currentMileage", "1000"))
                .andExpect(status().isOk())
                .andExpect(view().name("vehicle-form"))
                .andExpect(model().attributeHasFieldErrors("vehicleForm", "registrationNumber"));
    }
}
