package de.nidal.taskboard.ui.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class LoginPage {

    private final Page page;

    public LoginPage(Page page) {
        this.page = page;
    }

    public LoginPage open() {
        page.navigate("/login");
        return this;
    }

    public Locator emailInput() {
        return page.getByTestId("login-email");
    }

    public Locator passwordInput() {
        return page.getByTestId("login-password");
    }

    public Locator submitButton() {
        return page.getByTestId("login-submit");
    }

    public Locator errorMessage() {
        return page.getByTestId("login-error");
    }

    public Locator infoMessage() {
        return page.getByTestId("login-info");
    }

    public void loginAs(String email, String password) {
        emailInput().fill(email);
        passwordInput().fill(password);
        submitButton().click();
    }
}