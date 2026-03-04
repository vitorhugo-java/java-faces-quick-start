package com.example.facesapp.controller;

import com.example.facesapp.model.User;
import jakarta.enterprise.context.SessionScoped;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpSession;

import java.io.Serializable;

/**
 * Session-scoped backing bean for the authenticated user's dashboard.
 */
@Named("dashboardController")
@SessionScoped
public class DashboardController implements Serializable {

    public User getLoggedInUser() {
        HttpSession session = (HttpSession)
                FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        if (session == null) {
            return null;
        }
        return (User) session.getAttribute("loggedInUser");
    }

    public boolean isLoggedIn() {
        return getLoggedInUser() != null;
    }

    public String logout() {
        HttpSession session = (HttpSession)
                FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "/login?faces-redirect=true";
    }
}
