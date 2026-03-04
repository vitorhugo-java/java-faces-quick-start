package com.example.facesapp.controller;

import com.example.facesapp.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * JSF backing bean for the e-mail verification page.
 */
@Named("verifyEmailController")
@RequestScoped
public class VerifyEmailController implements Serializable {

    @Inject
    private UserService userService;

    private String token;
    private boolean verified = false;
    private String message;

    // ── lifecycle ─────────────────────────────────────────────────────────────

    public void verify() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (token == null || token.isBlank()) {
            message = "No verification token provided.";
            return;
        }
        try {
            userService.verifyEmail(token);
            verified = true;
            message  = "Your e-mail has been verified! You can now log in.";
        } catch (Exception ex) {
            message = ex.getMessage();
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
        }
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public boolean isVerified() { return verified; }
    public String getMessage() { return message; }
}
