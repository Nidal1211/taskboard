package de.nidal.taskboard.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.nidal.taskboard.ui.pages.TasksPage;

// Szenario 6 (Fremde Aufgabe ändern oder löschen) wird nicht als UI-Test umgesetzt,
// da es ausschließlich das Verhalten der API prüft (Ablehnung der Änderung/Löschung
// einer fremden Aufgabe über PUT/PATCH/DELETE) und keine Oberfläche beteiligt ist.

@Tag("TB-4")
class TB4UiTest extends UiTestBase {

    private void createTaskViaApi(String token, String taskTitle) {
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", taskTitle))
                .post("/api/tasks")
                .then().statusCode(201);
    }

    @Test
    @DisplayName("Titel erfolgreich ändern")
    void titelErfolgreichAendern() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();

        tasksPage.editTitle(task, "Wocheneinkauf");

        Locator updatedTask = tasksPage.taskItemByTitle("Wocheneinkauf");
        assertThat(updatedTask).isVisible();
        assertThat(tasksPage.taskStatusOf(updatedTask)).hasValue("OPEN");
    }

    @Test
    @DisplayName("Titel beim Bearbeiten geleert")
    void titelBeimBearbeitenGeleert() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();

        tasksPage.taskEditButtonOf(task).click();
        tasksPage.taskEditInput().fill("");
        tasksPage.taskSaveButton().click();

        assertThat(tasksPage.taskErrorMessage()).hasText("Titel darf nicht leer sein");

        page.reload();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);
    }

    @Test
    @DisplayName("Status ändern")
    void statusAendern() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();
        assertThat(tasksPage.taskStatusOf(task)).hasValue("OPEN");

        tasksPage.changeStatus(task, "IN_PROGRESS");

        assertThat(tasksPage.taskStatusOf(task)).hasValue("IN_PROGRESS");

        page.reload();

        Locator reloadedTask = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(reloadedTask).isVisible();
        assertThat(tasksPage.taskStatusOf(reloadedTask)).hasValue("IN_PROGRESS");
    }

    @Test
    @DisplayName("Aufgabe löschen")
    void aufgabeLoeschen() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);

        tasksPage.taskDeleteButtonOf(task).click();
        assertThat(tasksPage.deleteDialog()).isVisible();
        tasksPage.deleteConfirmButton().click();

        assertThat(tasksPage.deleteDialog()).not().isVisible();
        assertThat(tasksPage.taskItems()).hasCount(0);
    }

    @Test
    @DisplayName("Löschen abbrechen")
    void loeschenAbbrechen() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        Locator task = tasksPage.taskItemByTitle("Einkaufen");
        assertThat(task).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);

        tasksPage.taskDeleteButtonOf(task).click();
        assertThat(tasksPage.deleteDialog()).isVisible();
        tasksPage.deleteCancelButton().click();

        assertThat(tasksPage.deleteDialog()).not().isVisible();
        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(1);
    }
}
