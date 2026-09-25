package de.nidal.taskboard.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNull;

/*
 * Nicht ueber die API getestete Oberflaechen-Schritte (laut Story):
 *
 * Szenario 1: Erfolgreicher Login
 *   - "wird er zu seiner Aufgabenliste weitergeleitet"
 *
 * Szenario 2: Falsches Passwort
 *   - "er bleibt auf der Login-Seite"
 *
 * Szenario 3: Unbekannte E-Mail
 *   - "er bleibt auf der Login-Seite"
 *
 * Szenario 4: Leeres Feld
 *   - "ist der Button „Anmelden“ deaktiviert" (komplett Oberflaechen-Test)
 *
 * Szenario 5: Zugriff ohne Anmeldung
 *   - "wird er zur Login-Seite weitergeleitet"
 */
@Tag("TB-2")
public class TB2ApiTest {

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

    private Response register(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/register");
    }

    private Response login(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/login");
    }

    private Response createTask(String token, String title) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token)
                .body(Map.of("title", title))
                .when()
                .post("/api/tasks");
    }

    @Test
    @DisplayName("Erfolgreicher Login")
    void erfolgreicherLogin() {
        register("nidal@test.de", "geheim123");

        Response loginResponse = login("nidal@test.de", "geheim123");
        loginResponse.then()
                .statusCode(200)
                .body("token", notNullValue());

        String token = loginResponse.jsonPath().getString("token");

        createTask(token, "Einkaufen").then().statusCode(201);

        register("other@test.de", "anderesPw1");
        String otherToken = login("other@test.de", "anderesPw1").jsonPath().getString("token");
        createTask(otherToken, "Fremde Aufgabe").then().statusCode(201);

        given()
                .header("Authorization", "Bearer " + token)
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(200)
                .body("title", hasItem("Einkaufen"))
                .body("title", not(hasItem("Fremde Aufgabe")));
    }

    @Test
    @DisplayName("Falsches Passwort")
    void falschesPasswort() {
        register("nidal@test.de", "geheim123");

        Response response = login("nidal@test.de", "falschesPasswort");
        response.then()
                .statusCode(401)
                .body("message", equalTo("E-Mail oder Passwort ist falsch"));

        assertNull(response.jsonPath().get("token"),
                "Bei falschem Passwort darf kein Token zurueckgegeben werden");
    }

    @Test
    @DisplayName("Unbekannte E-Mail")
    void unbekannteEmail() {
        Response response = login("unbekannt@test.de", "irgendeinPasswort");
        response.then()
                .statusCode(401)
                .body("message", equalTo("E-Mail oder Passwort ist falsch"));

        assertNull(response.jsonPath().get("token"),
                "Bei unbekannter E-Mail darf kein Token zurueckgegeben werden");
    }

    @Test
    @DisplayName("Zugriff ohne Anmeldung")
    void zugriffOhneAnmeldung() {
        given()
                .when()
                .get("/api/tasks")
                .then()
                .statusCode(401)
                .body("message", equalTo("Bitte melde dich an"));
    }
}