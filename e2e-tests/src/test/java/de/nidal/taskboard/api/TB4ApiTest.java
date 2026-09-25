package de.nidal.taskboard.api;

// Nicht ueber die API testbare bzw. reine Oberflaechen-Anteile der Szenarien:
// - Szenario 1: "und auf 'Speichern' klickt" (UI-Aktion, API-Teil wird getestet)
// - Szenario 2: "und auf 'Speichern' klickt" (UI-Aktion, API-Teil wird getestet)
// - Szenario 4: "Sicherheitsabfrage mit 'Ja, löschen' bestätigt" (UI-Dialog, API-Teil wird getestet)
// - Szenario 5: komplett UI-only ("auf 'Löschen' klickt", "Sicherheitsabfrage mit 'Abbrechen' schließt") -
//   es findet keine API-Anfrage statt, daher nicht automatisiert testbar

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@Tag("TB-4")
public class TB4ApiTest {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @BeforeEach
    void resetState() {
        RestAssured.baseURI = BASE_URL;
        given()
                .when()
                .post("/test/reset")
                .then()
                .statusCode(204);
    }

    private void register(String email, String password) {
        given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201);
    }

    private String login(String email, String password) {
        Response response = given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/login");
        response.then().statusCode(200);
        return response.jsonPath().getString("token");
    }

    private int createTask(String token, String title) {
        Response response = given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", title))
                .when()
                .post("/api/tasks");
        response.then().statusCode(201);
        return response.jsonPath().getInt("id");
    }

    private Response getTasks(String token) {
        return given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/tasks");
    }

    private Map<String, Object> findTaskById(String token, int id) {
        Response response = getTasks(token);
        response.then().statusCode(200);
        List<Map<String, Object>> tasks = response.jsonPath().getList("$");
        for (Map<String, Object> task : tasks) {
            if (((Number) task.get("id")).intValue() == id) {
                return task;
            }
        }
        return null;
    }

    @Test
    @DisplayName("Titel erfolgreich ändern")
    void titelErfolgreichAendern() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        int taskId = createTask(token, "Einkaufen");

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", "Wocheneinkauf"))
                .when()
                .put("/api/tasks/" + taskId)
                .then()
                .statusCode(200)
                .body("title", equalTo("Wocheneinkauf"))
                .body("status", equalTo("OPEN"));

        Map<String, Object> task = findTaskById(token, taskId);
        assertNotNull(task, "Aufgabe sollte weiterhin in der Liste vorhanden sein");
        assertEquals("Wocheneinkauf", task.get("title"));
        assertEquals("OPEN", task.get("status"));
    }

    @Test
    @DisplayName("Titel beim Bearbeiten geleert")
    void titelBeimBearbeitenGeleert() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        int taskId = createTask(token, "Einkaufen");

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", ""))
                .when()
                .put("/api/tasks/" + taskId)
                .then()
                .statusCode(400)
                .body("message", equalTo("Titel darf nicht leer sein"));

        Map<String, Object> task = findTaskById(token, taskId);
        assertNotNull(task, "Aufgabe sollte weiterhin existieren");
        assertEquals("Einkaufen", task.get("title"));
    }

    @Test
    @DisplayName("Status ändern")
    void statusAendern() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        int taskId = createTask(token, "Einkaufen");

        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("status", "IN_PROGRESS"))
                .when()
                .patch("/api/tasks/" + taskId + "/status")
                .then()
                .statusCode(200)
                .body("status", equalTo("IN_PROGRESS"));

        // Simuliert das Neuladen der Seite durch eine erneute Anfrage
        Map<String, Object> taskAfterReload = findTaskById(token, taskId);
        assertNotNull(taskAfterReload, "Aufgabe sollte weiterhin existieren");
        assertEquals("IN_PROGRESS", taskAfterReload.get("status"));
    }

    @Test
    @DisplayName("Aufgabe löschen")
    void aufgabeLoeschen() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");
        int taskId = createTask(token, "Einkaufen");

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .delete("/api/tasks/" + taskId)
                .then()
                .statusCode(204);

        Map<String, Object> task = findTaskById(token, taskId);
        assertNull(task, "Aufgabe sollte nicht mehr in der Liste sein");
    }

    @Test
    @DisplayName("Fremde Aufgabe ändern oder löschen")
    void fremdeAufgabeAendernOderLoeschen() {
        register("a@test.de", "geheim123");
        register("b@test.de", "geheim123");
        String tokenA = login("a@test.de", "geheim123");
        String tokenB = login("b@test.de", "geheim123");

        int taskId = createTask(tokenB, "Einkaufen");

        // Versuch: Titel aendern
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + tokenA)
                .body(Map.of("title", "Gehackt"))
                .when()
                .put("/api/tasks/" + taskId)
                .then()
                .statusCode(404)
                .body("message", equalTo("Aufgabe nicht gefunden"));

        // Versuch: Status aendern
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + tokenA)
                .body(Map.of("status", "DONE"))
                .when()
                .patch("/api/tasks/" + taskId + "/status")
                .then()
                .statusCode(404)
                .body("message", equalTo("Aufgabe nicht gefunden"));

        // Versuch: Loeschen
        given()
                .header("Authorization", "Bearer " + tokenA)
                .when()
                .delete("/api/tasks/" + taskId)
                .then()
                .statusCode(404)
                .body("message", equalTo("Aufgabe nicht gefunden"));

        // Aufgabe von Nutzer B bleibt unveraendert
        Map<String, Object> task = findTaskById(tokenB, taskId);
        assertNotNull(task, "Aufgabe von Nutzer B sollte weiterhin existieren");
        assertEquals("Einkaufen", task.get("title"));
        assertEquals("OPEN", task.get("status"));
    }
}