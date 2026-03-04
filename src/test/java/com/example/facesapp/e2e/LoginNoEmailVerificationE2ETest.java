package com.example.facesapp.e2e;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests for the login flow when
 * {@code EMAIL_VERIFICATION_REQUIRED=false}.
 *
 * <p>Pre-conditions:
 * <ul>
 *   <li>Application deployed with {@code EMAIL_VERIFICATION_REQUIRED=false}</li>
 *   <li>A (technically unverified) test user: email={@code e2e-noverify@example.com},
 *       password={@code E2ePass1!}</li>
 * </ul>
 *
 * <p>Because verification is disabled the user was persisted with
 * {@code email_verified=true} by the registration logic, so this test suite
 * validates that users registered in that mode can log in immediately without
 * any e-mail confirmation step.
 *
 * <p>Run: {@code mvn verify -Pe2e -Dapp.email.verification=false}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginNoEmailVerificationE2ETest {

    private static final String BASE_URL = System.getProperty(
            "app.base.url",
            System.getenv("APP_BASE_URL") != null ? System.getenv("APP_BASE_URL") : "http://localhost:8080/facesapp"
    );

    private static WebDriver  driver;
    private static WebDriverWait wait;

    @BeforeAll
    static void setUpDriver() {
        WebDriverManager.chromedriver().setup();
        ChromeOptions opts = new ChromeOptions();
        opts.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage",
                          "--disable-gpu", "--window-size=1280,800");
        driver = new ChromeDriver(opts);
        wait   = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterAll
    static void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }

    @BeforeEach
    void goToLogin() {
        driver.get(BASE_URL + "/login.xhtml");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void fillLogin(String email, String password) {
        WebElement emailInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(By.id("loginForm:email")));
        emailInput.clear();
        emailInput.sendKeys(email);

        WebElement passwordInput = driver.findElement(By.id("loginForm:password_input"));
        passwordInput.clear();
        passwordInput.sendKeys(password);

        driver.findElement(By.id("loginForm:loginBtn")).click();
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    @Order(1)
    @DisplayName("User registered without e-mail verification can log in immediately")
    void login_userRegisteredWithoutVerification_succeedsImmediately() {
        fillLogin("e2e-noverify@example.com", "E2ePass1!");

        wait.until(ExpectedConditions.urlContains("/dashboard/"));
        assertThat(driver.getCurrentUrl()).contains("/dashboard/");
    }

    @Test
    @Order(2)
    @DisplayName("Registration form allows creating a new account without verification step")
    void register_noEmailVerification_userCanLoginRightAway() {
        String uniqueEmail = "e2e-new-" + System.currentTimeMillis() + "@example.com";

        // Navigate to register
        driver.get(BASE_URL + "/register.xhtml");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("registerForm:name")));

        driver.findElement(By.id("registerForm:name")).sendKeys("New User");
        driver.findElement(By.id("registerForm:email")).sendKeys(uniqueEmail);
        driver.findElement(By.id("registerForm:password_input")).sendKeys("E2ePass1!");
        driver.findElement(By.id("registerForm:confirmPassword_input")).sendKeys("E2ePass1!");
        driver.findElement(By.id("registerForm:registerBtn")).click();

        // Should redirect to login page
        wait.until(ExpectedConditions.urlContains("login"));

        // Now log in with the new account – should succeed without e-mail verification
        fillLogin(uniqueEmail, "E2ePass1!");

        wait.until(ExpectedConditions.urlContains("/dashboard/"));
        assertThat(driver.getCurrentUrl()).contains("/dashboard/");
    }

    @Test
    @Order(3)
    @DisplayName("Login still fails with wrong password even without e-mail verification")
    void login_wrongPassword_failsEvenWithNoVerification() {
        fillLogin("e2e-noverify@example.com", "wrong-password");

        WebElement errorMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ui-messages-error-summary, .ui-message-error-detail")));
        assertThat(errorMsg.getText()).containsIgnoringCase("Invalid");
    }

    @Test
    @Order(4)
    @DisplayName("Dashboard redirects to login when session is not set")
    void dashboard_unauthenticatedAccess_redirectsToLogin() {
        driver.manage().deleteAllCookies(); // clear session
        driver.get(BASE_URL + "/dashboard/index.xhtml");

        wait.until(ExpectedConditions.urlContains("login"));
        assertThat(driver.getCurrentUrl()).contains("login");
    }
}
