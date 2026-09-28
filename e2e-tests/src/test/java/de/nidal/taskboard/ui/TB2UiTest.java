package de.nidal.taskboard.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.regex.Pattern;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import de.nidal.taskboard.ui.pages.LoginPage;

@Tag("TB-2")
class TB2UiTest extends UiTestBase {

    @Test
    @DisplayName("Erfolgreicher Login")
    void erfolgreicherLogin() {
        register("nidal@test.de", "geheim123");

        new LoginPage(page).open().loginAs("nidal@test.de", "geheim123");

        assertThat(page).hasURL(Pattern.compile(".*/tasks$"));
        assertThat(page.getByTestId("tasks-title")).isVisible();
    }

    @Test
    @DisplayName("Falsches Passwort")
    void falschesPasswort() {
        register("nidal@test.de", "geheim123");
        LoginPage loginPage = new LoginPage(page).open();

        loginPage.loginAs("nidal@test.de", "falschesPasswort");

        assertThat(loginPage.errorMessage()).hasText("E-Mail oder Passwort ist falsch");
        assertThat(page).hasURL(Pattern.compile(".*/login$"));
        assertNull(page.evaluate("localStorage.getItem('token')"),
                "Nach fehlgeschlagenem Login darf kein Token gespeichert sein");
    }

    @Test
    @DisplayName("Leeres Feld")
    void leeresFeld() {
        LoginPage loginPage = new LoginPage(page).open();

        assertThat(loginPage.submitButton()).isDisabled();

        loginPage.emailInput().fill("nidal@test.de");
        assertThat(loginPage.submitButton()).isDisabled();

        loginPage.passwordInput().fill("geheim123");
        assertThat(loginPage.submitButton()).isEnabled();
    }

    @Test
    @DisplayName("Zugriff ohne Anmeldung")
    void zugriffOhneAnmeldung() {
        page.navigate("/tasks");

        assertThat(page).hasURL(Pattern.compile(".*/login$"));
    }
}