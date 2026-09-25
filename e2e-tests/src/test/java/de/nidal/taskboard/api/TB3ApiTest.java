package de.nidal.taskboard.api;

// Hinweis zu reinen Oberflaechen-Schritten (nicht automatisiert getestet):
// - "auf 'Hinzufügen' klickt" (UI-Interaktion, wird durch direkten API-Aufruf ersetzt)
// - "erscheint die Aufgabe ... in seiner Liste" als UI-Darstellung (wird stattdessen ueber GET /api/tasks geprueft)

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@Tag("TB-3")
public class TB3ApiTest {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = BASE_URL;
        given()
            .when()
                .post("/test/reset")
            .then()
                .statusCode(204);
    }

    private String registerAndLogin(String email, String password) {
        given()
            .contentType("application/json")
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/auth/register")
        .then()
            .statusCode(201);

        Response loginResponse = given()
            .contentType("application/json")
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/auth/login");

        loginResponse.then().statusCode(200);

        return loginResponse.jsonPath().getString("token");
    }

    private Response createTask(String token, String title) {
        return given()
            .header("Authorization", "Bearer " + token)
            .contentType("application/json")
            .body(Map.of("title", title))
        .when()
            .post("/api/tasks");
    }

    private Response getTasks(String token) {
        return given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/tasks");
    }

    @Test
    @DisplayName("Aufgabe erfolgreich anlegen")
    void aufgabeErfolgreichAnlegen() {
        String token = registerAndLogin("nidal@test.de", "geheim123");

        createTask(token, "Einkaufen")
            .then()
                .statusCode(201)
                .body("title", equalTo("Einkaufen"))
                .body("status", equalTo("OPEN"));

        getTasks(token)
            .then()
                .statusCode(200)
                .body("title", hasItem("Einkaufen"));
    }

    @Test
    @DisplayName("Leerer Titel")
    void leererTitel() {
        String token = registerAndLogin("nidal@test.de", "geheim123");

        createTask(token, "")
            .then()
                .statusCode(400)
                .body("message", equalTo("Titel darf nicht leer sein"));

        getTasks(token)
            .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @DisplayName("Titel nur aus Leerzeichen")
    void titelNurAusLeerzeichen() {
        String token = registerAndLogin("nidal@test.de", "geheim123");

        createTask(token, "   ")
            .then()
                .statusCode(400)
                .body("message", equalTo("Titel darf nicht leer sein"));

        getTasks(token)
            .then()
                .statusCode(200)
                .body("", hasSize(0));
    }

    @Test
    @DisplayName("Titel mit der maximalen Länge")
    void titelMitDerMaximalenLaenge() {
        String token = registerAndLogin("nidal@test.de", "geheim123");
        String titel100 = "a".repeat(100);

        createTask(token, titel100)
            .then()
                .statusCode(201)
                .body("title", equalTo(titel100))
                .body("status", equalTo("OPEN"));

        getTasks(token)
            .then()
                .statusCode(200)
                .body("title", hasItem(titel100));
    }

    @Test
    @DisplayName("Titel zu lang")
    void titelZuLang() {
        String token = registerAndLogin("nidal@test.de", "geheim123");
        String titel101 = "a".repeat(101);

        createTask(token, titel101)
            .then()
                .statusCode(400)
                .body("message", equalTo("Titel darf höchstens 100 Zeichen lang sein"));

        getTasks(token)
            .then()
                .statusCode(200)
                .body("", hasSize(0));
    }
}