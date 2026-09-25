package de.nidal.taskboard.api;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

/*
 * Nicht automatisiert getestet (reine Oberflächen-Schritte):
 * - Szenario 1: "wird er zur Login-Seite weitergeleitet" (Weiterleitung ist UI-Verhalten)
 * - Szenario 4: "wird er zur Login-Seite weitergeleitet" (Weiterleitung ist UI-Verhalten)
 * - Szenario 6: "Leeres Feld" - deaktivierter Button ist reines UI-Verhalten, nicht über die API prüfbar
 */
@Tag("TB-1")
class TB1ApiTest {

    private static final String BASE_URL = System.getProperty("baseUrl", "http://localhost:8080");

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = BASE_URL;
        given().when().post("/test/reset").then().statusCode(204);
    }

    private Response register(String email, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/register");
    }

    private Response login(String email, String password) {
        return given()
                .contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .when()
                .post("/api/auth/login");
    }

    @Test
    @DisplayName("Erfolgreiche Registrierung")
    void erfolgreicheRegistrierung() {
        String email = "nidal@test.de";
        String password = "geheim123";

        register(email, password)
                .then()
                .statusCode(201)
                .body("email", equalTo(email))
                .body("id", notNullValue())
                .body("password", equalTo(null));

        login(email, password)
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    @DisplayName("E-Mail bereits registriert")
    void emailBereitsRegistriert() {
        String email = "nidal@test.de";
        String originalPassword = "geheim123";
        String andererPassword = "anderes123";

        register(email, originalPassword)
                .then()
                .statusCode(201);

        register(email, andererPassword)
                .then()
                .statusCode(409)
                .body("message", equalTo("Diese E-Mail ist bereits registriert"));

        // Beweis: kein neues Konto angelegt - Login mit dem ursprünglichen Passwort funktioniert weiterhin
        login(email, originalPassword)
                .then()
                .statusCode(200)
                .body("token", notNullValue());

        // Beweis: Login mit dem zweiten Passwort schlägt fehl, da kein Konto damit angelegt wurde
        login(email, andererPassword)
                .then()
                .statusCode(401)
                .body("message", equalTo("E-Mail oder Passwort ist falsch"));
    }

    @Test
    @DisplayName("Ungültige E-Mail")
    void ungueltigeEmail() {
        String email = "nidal-test.de";
        String password = "geheim123";

        register(email, password)
                .then()
                .statusCode(400)
                .body("message", equalTo("Bitte gib eine gültige E-Mail-Adresse ein"));

        // Beweis: kein Konto angelegt - Login schlägt fehl
        login(email, password)
                .then()
                .statusCode(401)
                .body("message", equalTo("E-Mail oder Passwort ist falsch"));
    }

    @Test
    @DisplayName("Passwort mit der Mindestlänge")
    void passwortMitMindestlaenge() {
        String email = "mindestlaenge@test.de";
        String password = "12345678"; // genau 8 Zeichen

        register(email, password)
                .then()
                .statusCode(201)
                .body("email", equalTo(email))
                .body("id", notNullValue());

        login(email, password)
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    @DisplayName("Passwort zu kurz")
    void passwortZuKurz() {
        String email = "zukurz@test.de";
        String password = "1234567"; // 7 Zeichen

        register(email, password)
                .then()
                .statusCode(400)
                .body("message", equalTo("Passwort muss mindestens 8 Zeichen lang sein"));

        // Beweis: kein Konto angelegt - Login schlägt fehl
        login(email, password)
                .then()
                .statusCode(401)
                .body("message", equalTo("E-Mail oder Passwort ist falsch"));
    }
}