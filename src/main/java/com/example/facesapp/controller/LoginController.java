package com.example.facesapp.controller;

import com.example.facesapp.model.User;
import com.example.facesapp.service.AuthException;
import com.example.facesapp.service.UserService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * JSF backing bean for the login page.
 */
@Named("loginController")
@RequestScoped
public class LoginController implements Serializable {

    @Inject
    private UserService userService;

    private String email;
    private String password;

    // ── action ────────────────────────────────────────────────────────────────

    public String login() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            User user = userService.login(email, password);

            // Store logged-in user in HTTP session
            HttpSession session = (HttpSession) ctx.getExternalContext().getSession(true);
            session.setAttribute("loggedInUser", user);

            return "/dashboard/index?faces-redirect=true";
        } catch (AuthException ex) {
            ctx.addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, ex.getMessage(), null));
            return null;
        }
    }

    public String logout() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        HttpSession session = (HttpSession) ctx.getExternalContext().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "/login?faces-redirect=true";
    }

    // ── getters / setters ─────────────────────────────────────────────────────

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
