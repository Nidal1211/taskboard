package de.nidal.taskboard.ui;

import static io.restassured.RestAssured.given;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import org.junit.jupiter.api.Tag;
import io.restassured.RestAssured;
@Tag("ui")
public abstract class UiTestBase {

    protected static final String FRONTEND_URL = System.getProperty("frontendUrl", "http://localhost:4200");
    protected static final String API_URL = System.getProperty("baseUrl", "http://localhost:8080");

    private static Playwright playwright;
    private static Browser browser;

    protected BrowserContext context;
    protected Page page;

    @BeforeAll
    static void startBrowser() {
        playwright = Playwright.create(new Playwright.CreateOptions()
                .setEnv(Map.of("PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD", "1")));
        boolean headless = Boolean.parseBoolean(System.getProperty("headless", "true"));
        browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(headless));
    }

    @AfterAll
    static void stopBrowser() {
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    @BeforeEach
    void setUp() {
        RestAssured.baseURI = API_URL;
        given().post("/test/reset").then().statusCode(204);
        context = browser.newContext(new Browser.NewContextOptions().setBaseURL(FRONTEND_URL));
        page = context.newPage();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    protected void register(String email, String password) {
        given().contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .post("/api/auth/register")
                .then().statusCode(201);
    }

    protected String login(String email, String password) {
        return given().contentType("application/json")
                .body(Map.of("email", email, "password", password))
                .post("/api/auth/login")
                .then().statusCode(200)
                .extract().path("token");
    }

    protected void signInWithToken(String token) {
        context.addInitScript("window.localStorage.setItem('token', '" + token + "')");
    }
}