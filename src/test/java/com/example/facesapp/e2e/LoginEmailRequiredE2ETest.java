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
 * {@code EMAIL_VERIFICATION_REQUIRED=true} (default).
 *
 * <p>Pre-conditions (handled by CI and documented in README):
 * <ul>
 *   <li>Application deployed at {@code APP_BASE_URL} (default: http://localhost:8080/facesapp)</li>
 *   <li>A verified test user exists: email={@code e2e-verified@example.com}, password={@code E2ePass1!}</li>
 *   <li>An unverified test user exists: email={@code e2e-unverified@example.com}, password={@code E2ePass1!}</li>
 * </ul>
 *
 * <p>Run these tests with: {@code mvn verify -Pe2e}
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class LoginEmailRequiredE2ETest {

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
    @DisplayName("Login page renders the sign-in form")
    void loginPage_renders() {
        assertThat(driver.getTitle()).contains("Sign In");
        assertThat(driver.findElement(By.id("loginForm:email"))).isNotNull();
        assertThat(driver.findElement(By.id("loginForm:password_input"))).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("Login with valid credentials and verified e-mail succeeds")
    void login_validVerifiedUser_redirectsToDashboard() {
        fillLogin("e2e-verified@example.com", "E2ePass1!");

        wait.until(ExpectedConditions.urlContains("/dashboard/"));
        assertThat(driver.getCurrentUrl()).contains("/dashboard/");
    }

    @Test
    @Order(3)
    @DisplayName("Login with unverified account (EMAIL_VERIFICATION_REQUIRED=true) shows error")
    void login_unverifiedUser_showsVerificationError() {
        fillLogin("e2e-unverified@example.com", "E2ePass1!");

        // Expect an error message about e-mail verification
        WebElement errorMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ui-messages-error-summary, .ui-message-error-detail")));
        assertThat(errorMsg.getText()).containsIgnoringCase("verify");
    }

    @Test
    @Order(4)
    @DisplayName("Login with wrong password shows error")
    void login_wrongPassword_showsError() {
        fillLogin("e2e-verified@example.com", "wrong-password");

        WebElement errorMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ui-messages-error-summary, .ui-message-error-detail")));
        assertThat(errorMsg.getText()).containsIgnoringCase("Invalid");
    }

    @Test
    @Order(5)
    @DisplayName("Login with unknown e-mail shows generic error")
    void login_unknownEmail_showsError() {
        fillLogin("no-such-user@example.com", "any-password");

        WebElement errorMsg = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ui-messages-error-summary, .ui-message-error-detail")));
        assertThat(errorMsg.getText()).isNotBlank();
    }

    @Test
    @Order(6)
    @DisplayName("Empty form submission shows required-field messages")
    void login_emptyForm_showsValidationErrors() {
        driver.findElement(By.id("loginForm:loginBtn")).click();

        // JSF validation messages should appear
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                By.cssSelector(".ui-message")));
        assertThat(driver.findElements(By.cssSelector(".ui-message"))).isNotEmpty();
    }

    @Test
    @Order(7)
    @DisplayName("'Forgot password?' link is visible and navigates to forgot-password page")
    void forgotPasswordLink_navigatesCorrectly() {
        driver.findElement(By.linkText("Forgot password?")).click();
        wait.until(ExpectedConditions.urlContains("forgot-password"));
        assertThat(driver.getTitle()).contains("Forgot Password");
    }

    @Test
    @Order(8)
    @DisplayName("'Create account' link navigates to register page")
    void createAccountLink_navigatesCorrectly() {
        driver.findElement(By.linkText("Create account")).click();
        wait.until(ExpectedConditions.urlContains("register"));
        assertThat(driver.getTitle()).contains("Create Account");
    }
}
