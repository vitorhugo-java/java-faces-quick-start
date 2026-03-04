package com.example.facesapp.unit;

import com.example.facesapp.config.EnvConfig;
import com.example.facesapp.model.User;
import com.example.facesapp.model.VerificationToken;
import com.example.facesapp.repository.UserRepository;
import com.example.facesapp.service.AuthException;
import com.example.facesapp.service.EmailService;
import com.example.facesapp.service.UserServiceImpl;
import com.example.facesapp.util.PasswordUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserServiceImpl}.
 * Uses Mockito to mock all external dependencies.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository  userRepo;
    @Mock private EmailService    emailService;
    @Mock private EnvConfig       env;

    @InjectMocks
    private UserServiceImpl userService;

    // ── register ──────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("register()")
    class RegisterTests {

        @Test
        @DisplayName("creates user and sends verification e-mail when email-verification is required")
        void register_emailVerificationRequired_sendsEmail() {
            when(env.isEmailVerificationRequired()).thenReturn(true);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.empty());
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
            when(userRepo.saveVerificationToken(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.register("Alice", "alice@example.com", "pass123");

            assertThat(result.getEmail()).isEqualTo("alice@example.com");
            assertThat(result.isEmailVerified()).isFalse();
            verify(emailService).sendVerificationEmail(eq(result), anyString());
        }

        @Test
        @DisplayName("marks user as verified immediately when email-verification is disabled")
        void register_emailVerificationDisabled_noEmailSent() {
            when(env.isEmailVerificationRequired()).thenReturn(false);
            when(userRepo.findByEmail("bob@example.com")).thenReturn(Optional.empty());
            when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.register("Bob", "bob@example.com", "pass123");

            assertThat(result.isEmailVerified()).isTrue();
            verifyNoInteractions(emailService);
        }

        @Test
        @DisplayName("throws IllegalArgumentException for duplicate e-mail")
        void register_duplicateEmail_throwsIllegalArgument() {
            User existing = new User("Alice", "alice@example.com", "hash");
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(existing));

            assertThatThrownBy(() -> userService.register("Alice2", "alice@example.com", "pass"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("already registered");
        }
    }

    // ── login ─────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("login()")
    class LoginTests {

        private User verifiedUser;
        private User unverifiedUser;

        @BeforeEach
        void setUp() {
            String hash = PasswordUtil.hash("secret");
            verifiedUser   = new User("Alice", "alice@example.com", hash);
            verifiedUser.setEmailVerified(true);
            unverifiedUser = new User("Bob",   "bob@example.com",   hash);
            // unverifiedUser.emailVerified defaults to false
        }

        @Test
        @DisplayName("returns user on valid credentials when verification not required")
        void login_validCredentials_noVerificationRequired() {
            when(env.isEmailVerificationRequired()).thenReturn(false);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(verifiedUser));

            User result = userService.login("alice@example.com", "secret");

            assertThat(result).isEqualTo(verifiedUser);
        }

        @Test
        @DisplayName("returns verified user on valid credentials when verification required")
        void login_verifiedUser_verificationRequired() {
            when(env.isEmailVerificationRequired()).thenReturn(true);
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(verifiedUser));

            User result = userService.login("alice@example.com", "secret");

            assertThat(result).isEqualTo(verifiedUser);
        }

        @Test
        @DisplayName("throws AuthException for unverified user when verification required")
        void login_unverifiedUser_verificationRequired_throwsAuthException() {
            when(env.isEmailVerificationRequired()).thenReturn(true);
            when(userRepo.findByEmail("bob@example.com")).thenReturn(Optional.of(unverifiedUser));

            assertThatThrownBy(() -> userService.login("bob@example.com", "secret"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("verify your e-mail");
        }

        @Test
        @DisplayName("allows unverified user to log in when verification not required")
        void login_unverifiedUser_verificationNotRequired_succeeds() {
            when(env.isEmailVerificationRequired()).thenReturn(false);
            when(userRepo.findByEmail("bob@example.com")).thenReturn(Optional.of(unverifiedUser));

            User result = userService.login("bob@example.com", "secret");
            assertThat(result).isEqualTo(unverifiedUser);
        }

        @Test
        @DisplayName("throws AuthException for wrong password")
        void login_wrongPassword_throwsAuthException() {
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(verifiedUser));

            assertThatThrownBy(() -> userService.login("alice@example.com", "WRONG"))
                    .isInstanceOf(AuthException.class)
                    .hasMessageContaining("Invalid");
        }

        @Test
        @DisplayName("throws AuthException for unknown e-mail")
        void login_unknownEmail_throwsAuthException() {
            when(userRepo.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.login("unknown@example.com", "any"))
                    .isInstanceOf(AuthException.class);
        }
    }

    // ── verifyEmail ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("verifyEmail()")
    class VerifyEmailTests {

        @Test
        @DisplayName("marks user as verified for a valid token")
        void verifyEmail_validToken_marksVerified() {
            User user = new User("Alice", "alice@example.com", "hash");
            VerificationToken token = new VerificationToken(user);

            when(userRepo.findVerificationToken(token.getToken())).thenReturn(Optional.of(token));
            when(userRepo.save(user)).thenReturn(user);
            doNothing().when(userRepo).updateVerificationToken(token);

            userService.verifyEmail(token.getToken());

            assertThat(user.isEmailVerified()).isTrue();
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("throws for an invalid token")
        void verifyEmail_invalidToken_throwsIllegalArgument() {
            when(userRepo.findVerificationToken("bad-token")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.verifyEmail("bad-token"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── requestPasswordReset ──────────────────────────────────────────────────

    @Nested
    @DisplayName("requestPasswordReset()")
    class PasswordResetRequestTests {

        @Test
        @DisplayName("sends reset e-mail when user exists")
        void requestReset_existingUser_sendsEmail() {
            User user = new User("Alice", "alice@example.com", "hash");
            when(userRepo.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            when(userRepo.savePasswordResetToken(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.requestPasswordReset("alice@example.com");

            verify(emailService).sendPasswordResetEmail(eq(user), anyString());
        }

        @Test
        @DisplayName("does nothing (no exception) when e-mail is not registered")
        void requestReset_unknownEmail_noException() {
            when(userRepo.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatCode(() -> userService.requestPasswordReset("ghost@example.com"))
                    .doesNotThrowAnyException();
            verifyNoInteractions(emailService);
        }
    }
}
