package de.nidal.taskboard.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.nidal.taskboard.ui.pages.TasksPage;


@Tag("TB-3")
class TB3UiTest extends UiTestBase {

    private void createTaskViaApi(String token, String taskTitle) {
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", taskTitle))
                .post("/api/tasks")
                .then().statusCode(201);
    }

    @Test
    @DisplayName("Aufgabe erfolgreich anlegen")
    void aufgabeErfolgreichAnlegen() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        tasksPage.addTask("Einkaufen");

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();
        assertThat(tasksPage.taskStatusOf(task)).hasValue("OPEN");
    }

    @Test
    @DisplayName("Leerer Titel")
    void leererTitel() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Vorhandene Aufgabe");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Vorhandene Aufgabe")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);

        tasksPage.addTask("");

        assertThat(tasksPage.taskErrorMessage()).hasText("Titel darf nicht leer sein");
        assertThat(tasksPage.taskItems()).hasCount(1);
    }

    @Test
    @DisplayName("Titel nur aus Leerzeichen")
    void titelNurAusLeerzeichen() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Vorhandene Aufgabe");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Vorhandene Aufgabe")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);

        tasksPage.addTask("   ");

        assertThat(tasksPage.taskErrorMessage()).hasText("Titel darf nicht leer sein");
        assertThat(tasksPage.taskItems()).hasCount(1);
    }

    @Test
    @DisplayName("Titel mit der maximalen Länge")
    void titelMitDerMaximalenLaenge() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        String maxTitle = "A".repeat(100);
        tasksPage.addTask(maxTitle);

        Locator task = tasksPage.taskItemByTitle(maxTitle);
        assertThat(task).isVisible();
        assertThat(tasksPage.taskTitleOf(task)).hasText(maxTitle);
    }

    @Test
    @DisplayName("Titel zu lang")
    void titelZuLang() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Vorhandene Aufgabe");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Vorhandene Aufgabe")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);

        String tooLongTitle = "A".repeat(101);
        tasksPage.addTask(tooLongTitle);

        assertThat(tasksPage.taskErrorMessage()).hasText("Titel darf höchstens 100 Zeichen lang sein");
        assertThat(tasksPage.taskItems()).hasCount(1);
    }
}
