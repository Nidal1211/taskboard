package de.nidal.taskboard.ui;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.microsoft.playwright.Locator;

import de.nidal.taskboard.ui.pages.TasksPage;

@Tag("TB-5")
class TB5UiTest extends UiTestBase {

    private void createTaskViaApi(String token, String taskTitle) {
        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", taskTitle))
                .post("/api/tasks")
                .then().statusCode(201);
    }

    private void changeStatusViaApi(String token, String taskTitle, String status) {
        int id = given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .get("/api/tasks")
                .then().statusCode(200)
                .extract().jsonPath().getInt("find { it.title == '" + taskTitle + "' }.id");

        given().contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("status", status))
                .patch("/api/tasks/" + id + "/status")
                .then().statusCode(200);
    }

    private String setUpBasicTasks() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        createTaskViaApi(token, "Einkaufen");
        createTaskViaApi(token, "Putzen");
        createTaskViaApi(token, "Einkaufsliste schreiben");
        changeStatusViaApi(token, "Putzen", "DONE");
        changeStatusViaApi(token, "Einkaufsliste schreiben", "IN_PROGRESS");
        return token;
    }

    @Test
    @DisplayName("Alle Aufgaben ohne Filter")
    void alleAufgabenOhneFilter() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.filterStatusSelect()).hasValue("ALL");
        assertThat(tasksPage.taskItems()).hasCount(3);
    }

    @Test
    @DisplayName("Nach Status filtern")
    void nachStatusFiltern() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.filterByStatus("DONE");

        assertThat(tasksPage.taskItems()).hasCount(1);
        assertThat(tasksPage.taskItemByTitle("Putzen")).isVisible();
    }

    @Test
    @DisplayName("Suche nach einem Teil des Titels")
    void sucheNachEinemTeilDesTitels() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.searchFor("Einkauf");

        assertThat(tasksPage.taskItems()).hasCount(2);
        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItemByTitle("Einkaufsliste schreiben")).isVisible();
        assertThat(tasksPage.taskItemByTitle("Putzen")).not().isVisible();
    }

    @Test
    @DisplayName("Suche ohne Beachtung der Groß- und Kleinschreibung")
    void sucheOhneBeachtungDerGrossUndKleinschreibung() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.searchFor("einkauf");

        assertThat(tasksPage.taskItems()).hasCount(2);
        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItemByTitle("Einkaufsliste schreiben")).isVisible();
    }

    @Test
    @DisplayName("Filter und Suche kombiniert")
    void filterUndSucheKombiniert() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.filterByStatus("IN_PROGRESS");
        tasksPage.searchFor("Einkauf");

        assertThat(tasksPage.taskItems()).hasCount(1);
        assertThat(tasksPage.taskItemByTitle("Einkaufsliste schreiben")).isVisible();
    }

    @Test
    @DisplayName("Keine Treffer")
    void keineTreffer() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.searchFor("Urlaub");

        assertThat(tasksPage.emptyMessage()).hasText("Keine Aufgaben gefunden");
        assertThat(tasksPage.taskItems()).hasCount(0);
    }

    @Test
    @DisplayName("Suche zurücksetzen")
    void sucheZuruecksetzen() {
        String token = setUpBasicTasks();
        signInWithToken(token);

        TasksPage tasksPage = new TasksPage(page).open();

        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItems()).hasCount(3);

        tasksPage.searchFor("Urlaub");
        assertThat(tasksPage.emptyMessage()).hasText("Keine Aufgaben gefunden");
        assertThat(tasksPage.taskItems()).hasCount(0);

        tasksPage.searchFor("");

        assertThat(tasksPage.taskItems()).hasCount(3);
        assertThat(tasksPage.taskItemByTitle("Einkaufen")).isVisible();
        assertThat(tasksPage.taskItemByTitle("Putzen")).isVisible();
        assertThat(tasksPage.taskItemByTitle("Einkaufsliste schreiben")).isVisible();
    }
}
