package com.example.facesapp.controller;

import com.example.facesapp.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * JSF backing bean for "Forgot Password" and "Reset Password" pages.
 */
@Named("passwordResetController")
@RequestScoped
public class PasswordResetController implements Serializable {

    @Inject
    private UserService userService;

    private String email;
    private String token;
    private String newPassword;
    private String confirmNewPassword;

    // ── forgot-password action ────────────────────────────────────────────────

    public String requestReset() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            userService.requestPasswordReset(email);
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "If that e-mail is registered you will receive a reset link shortly.", null));
        } catch (Exception ex) {
            // swallow – don't reveal whether e-mail exists
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "If that e-mail is registered you will receive a reset link shortly.", null));
        }
        return null;
    }

    // ── reset-password action ─────────────────────────────────────────────────

    public String resetPassword() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (!newPassword.equals(confirmNewPassword)) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match.", null));
            return null;
        }

        try {
            userService.resetPassword(token, newPassword);
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Your password has been updated. You may now log in.", null));
            return "/login?faces-redirect=true";
        } catch (Exception ex) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }

    public String getConfirmNewPassword() { return confirmNewPassword; }
    public void setConfirmNewPassword(String confirmNewPassword) { this.confirmNewPassword = confirmNewPassword; }
}
