package de.nidal.taskboard.api;

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

// Oberflaechen-Teile, die nicht per API getestet werden:
// - Szenario 1: "Wenn er die Aufgabenliste oeffnet" und "Dann ist der Filter „Alle“ ausgewaehlt"
//   sind reine Oberflaechenzustaende (Anzeige des ausgewaehlten Filters).
// - Szenario 6: Die Meldung "Keine Aufgaben gefunden" wird nur in der Oberflaeche angezeigt.
//   Die API liefert laut Vertrag bei keinem Treffer lediglich eine leere Liste [].
// - Szenario 7: "er hat Urlaub in das Suchfeld eingegeben" und "er das Suchfeld leert"
//   sind Oberflaechenaktionen (Eingabe/Leeren eines Suchfelds).

@Tag("TB-5")
public class TB5ApiTest {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = BASE_URL;
        given().when().post("/test/reset").then().statusCode(204);
    }

    private String register(String email, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(201)
                .extract().asString();
    }

    private String login(String email, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(200)
                .extract().path("token");
    }

    private int createTask(String token, String title) {
        return given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", title))
                .when()
                .post("/api/tasks")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    private void setStatus(String token, int id, String status) {
        given()
                .contentType("application/json")
                .header("Authorization", "Bearer " + token)
                .body(Map.of("status", status))
                .when()
                .patch("/api/tasks/{id}/status", id)
                .then()
                .statusCode(200);
    }

    /**
     * Legt die drei Grund-Aufgaben aus der Story an:
     * "Einkaufen" -> OPEN
     * "Putzen" -> DONE
     * "Einkaufsliste schreiben" -> IN_PROGRESS
     */
    private String setupThreeTasks() {
        register("nidal@test.de", "geheim123");
        String token = login("nidal@test.de", "geheim123");

        createTask(token, "Einkaufen");

        int putzenId = createTask(token, "Putzen");
        setStatus(token, putzenId, "DONE");

        int einkaufslisteId = createTask(token, "Einkaufsliste schreiben");
        setStatus(token, einkaufslisteId, "IN_PROGRESS");

        return token;
    }

    private Response getTasks(String token, Map<String, ?> params) {
        var request = given()
                .header("Authorization", "Bearer " + token);
        if (params != null) {
            request = request.queryParams(params);
        }
        return request.when().get("/api/tasks");
    }

    @Test
    @DisplayName("Alle Aufgaben ohne Filter")
    void alleAufgabenOhneFilter() {
        String token = setupThreeTasks();

        getTasks(token, null)
                .then()
                .statusCode(200)
                .body("title", hasItems("Einkaufen", "Putzen", "Einkaufsliste schreiben"))
                .body("$", hasSize(3));
    }

    @Test
    @DisplayName("Nach Status filtern")
    void nachStatusFiltern() {
        String token = setupThreeTasks();

        getTasks(token, Map.of("status", "DONE"))
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Putzen"))
                .body("[0].status", equalTo("DONE"));
    }

    @Test
    @DisplayName("Suche nach einem Teil des Titels")
    void percheNachTeilDesTitels() {
        String token = setupThreeTasks();

        getTasks(token, Map.of("search", "Einkauf"))
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("title", hasItems("Einkaufen", "Einkaufsliste schreiben"))
                .body("title", not(hasItem("Putzen")));
    }

    @Test
    @DisplayName("Suche ohne Beachtung der Groß- und Kleinschreibung")
    void percheOhneGrossKleinschreibung() {
        String token = setupThreeTasks();

        getTasks(token, Map.of("search", "einkauf"))
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("title", hasItems("Einkaufen", "Einkaufsliste schreiben"));
    }

    @Test
    @DisplayName("Filter und Suche kombiniert")
    void filterUndSucheKombiniert() {
        String token = setupThreeTasks();

        getTasks(token, Map.of("status", "IN_PROGRESS", "search", "Einkauf"))
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].title", equalTo("Einkaufsliste schreiben"));
    }

    @Test
    @DisplayName("Keine Treffer")
    void keineTreffer() {
        String token = setupThreeTasks();

        // Die Meldung "Keine Aufgaben gefunden" ist laut Vertrag kein Teil der
        // API-Antwort,
        // sie wird nur in der Oberflaeche angezeigt. Die API liefert eine leere Liste.
        getTasks(token, Map.of("search", "Urlaub"))
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    @Test
    @DisplayName("Suche zurücksetzen")
    void percheZuruecksetzen() {
        String token = setupThreeTasks();

        // Gegeben: Suche nach "Urlaub" liefert kein Ergebnis
        getTasks(token, Map.of("search", "Urlaub"))
                .then()
                .statusCode(200)
                .body("$", hasSize(0));

        // Wenn das Suchfeld geleert wird, entspricht das einer Anfrage ohne
        // Suchparameter
        getTasks(token, null)
                .then()
                .statusCode(200)
                .body("$", hasSize(3))
                .body("title", hasItems("Einkaufen", "Putzen", "Einkaufsliste schreiben"));
    }
}