package com.example.facesapp.controller;

import com.example.facesapp.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import java.io.Serializable;

/**
 * JSF backing bean for the user-registration page.
 */
@Named("registerController")
@RequestScoped
public class RegisterController implements Serializable {

    @Inject
    private UserService userService;

    private String name;
    private String email;
    private String password;
    private String confirmPassword;

    // ── action ────────────────────────────────────────────────────────────────

    public String register() {
        FacesContext ctx = FacesContext.getCurrentInstance();

        if (!password.equals(confirmPassword)) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match.", null));
            return null;
        }

        try {
            userService.register(name, email, password);
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_INFO,
                            "Registration successful! Please check your inbox to verify your e-mail.", null));
            return "/login?faces-redirect=true";
        } catch (IllegalArgumentException ex) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String confirmPassword) { this.confirmPassword = confirmPassword; }
}
