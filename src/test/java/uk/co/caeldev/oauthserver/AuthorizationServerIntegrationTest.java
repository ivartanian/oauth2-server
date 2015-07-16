package uk.co.caeldev.oauthserver;

import com.google.common.base.Splitter;
import com.jayway.restassured.filter.session.SessionFilter;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import uk.co.caeldev.oauthserver.persisters.Persister;

import java.util.Map;

import static com.jayway.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static uk.co.caeldev.oauthserver.builders.SSM.givenMongoClientDetail;
import static uk.co.caeldev.oauthserver.builders.SSM.givenUser;
import static uk.org.fyodor.generators.RDG.integer;
import static uk.org.fyodor.generators.RDG.string;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@ActiveProfiles("test")
@IntegrationTest("server.port:0")
public class AuthorizationServerIntegrationTest {

    public static final String OAUTH2_RESOURCE = "oauth2-resource";

    @Value("${local.server.port}")
    private int port;

    @Value("${server.contextPath}")
    private String basePath;

    @Autowired
    private Persister persister;

    @Test
    public void shouldNotAuthorizeWhenUrlIsProtected() {
        given().port(port).basePath(basePath).redirects().follow(false)
                .when()
                .get("/")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value());
    }

    @Test
    public void shouldNotAuthorizeForUserEndpointWithoutValidToken() {
        given().port(port).basePath(basePath).redirects().follow(false)
                .when()
                .get("/user")
                .then()
                .assertThat()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    @Test
    public void shouldRedirectAuthorizationWhenUrlIsProtectedAndNotLogin() {
        given().port(port).basePath(basePath).redirects().follow(false)
                .when()
                .get("/oauth/authorize")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value())
                .and()
                .header("Location", containsString("login"));
    }

    @Test
    public void shouldAuthenticateSuccessfullyAndRedirectToHomePage() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        given().port(port).basePath(basePath).redirects().follow(false)
                .formParam("username", username)
                .formParam("password", password)
                .when()
                .post("/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value())
                .and()
                .header("Location", not(containsString("login")));
    }

    @Test
    public void shouldFailedAuthenticationWhenPasswordIsWrong() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        given().port(port).basePath(basePath).redirects().follow(false)
                .formParam("username", username)
                .formParam("password", "wrong")
                .when()
                .post("/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value())
                .and()
                .header("Location", containsString("login"));
    }

    @Test
    public void shouldRedirectToApprovalPageWhenTryToGetAuthorizationCode() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        String clientId = integer().next() + "test" + integer().next();
        givenMongoClientDetail().clientId(clientId)
                .clientSecret(string().next())
                .scopes("read")
                .authorizedGrantTypes("authorization_code")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect("http://localhost:9000/sso/")
                .persist(persister);

        SessionFilter sessionFilter = new SessionFilter();

        //And
        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .auth().form(username, password)
                .filter(sessionFilter)
                .when()
                .log().all()
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_url", "/")
                .queryParam("scope", "read")
                .get("/oauth/authorize")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("html.body.h1", containsString("OAuth Approval"));
    }

    @Test
    public void shouldApproveAndGetAuthorizationCode() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        String clientId = integer().next() + "test" + integer().next();
        givenMongoClientDetail().clientId(clientId)
                .clientSecret(string().next())
                .scopes("read")
                .authorizedGrantTypes("authorization_code")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect("http://localhost:9000/sso/")
                .persist(persister);

        SessionFilter sessionFilter = new SessionFilter();

        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .auth().form(username, password)
                .filter(sessionFilter)
                .when()
                .log().all()
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_url", "/")
                .queryParam("scope", "read")
                .get("/oauth/authorize")
                .then()
                .statusCode(HttpStatus.OK.value());

        //And
        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .filter(sessionFilter)
                .when()
                .log().all()
                .formParam("scope.read", true)
                .formParam("user_oauth_approval", true)
                .post("/oauth/authorize")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value())
                .and()
                .header("Location", containsString("code="));
    }

    @Test
    public void shouldGetUserDetailsFromValidAccessToken() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        String clientId = integer().next() + "test" + integer().next();
        final String clientSecret = string().next();
        givenMongoClientDetail().clientId(clientId)
                .clientSecret(clientSecret)
                .scopes("read")
                .authorizedGrantTypes("password")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect("http://localhost:9000/sso/")
                .persist(persister);

        final String accessToken = given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .auth().preemptive().basic(clientId, clientSecret)
                .when()
                .log().all()
                .param("client_id", clientId)
                .param("grant_type", "password")
                .param("username", username)
                .param("password", password)
                .post("/oauth/token")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().getString("access_token");

        //When
        final String expectedUsername = given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .when()
                .log().all()
                .queryParam("access_token", accessToken)
                .get("/user")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .extract().body().jsonPath().getString("userAuthentication.details.username");

        assertThat(expectedUsername).isEqualTo(username);
    }

    @Test
    public void shouldGetAccessTokenFromAuthorizationCode() {
        //Given
        String password = string().next();
        String username = string().next();
        givenUser().username(username)
                .password(password)
                .grantedAuthorities("ROLE_USER")
                .persist(persister);

        //And
        final String clientId = integer().next() + "test" + integer().next();
        final String clientSecret = string().next();
        final String redirectUrl = "/";
        givenMongoClientDetail().clientId(clientId)
                .clientSecret(clientSecret)
                .scopes("read")
                .authorizedGrantTypes("authorization_code")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect(redirectUrl)
                .persist(persister);

        SessionFilter sessionFilter = new SessionFilter();

        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .auth().form(username, password)
                .filter(sessionFilter)
                .when()
                .log().all()
                .queryParam("response_type", "code")
                .queryParam("client_id", clientId)
                .queryParam("redirect_url", "/")
                .queryParam("scope", "read")
                .get("/oauth/authorize")
                .then()
                .statusCode(HttpStatus.OK.value());

        //And
        final String finalUrl = given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .filter(sessionFilter)
                .when()
                .log().all()
                .formParam("scope.read", true)
                .formParam("user_oauth_approval", true)
                .post("/oauth/authorize")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.FOUND.value())
                .and()
                .header("Location", containsString("code="))
                .and()
                .extract().header("Location");

        final String code = getQueryStringParam(finalUrl, "code");

        //And
        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .auth().preemptive().basic(clientId, clientSecret)
                .when()
                .log().all()
                .param("code", code)
                .param("client_id", clientId)
                .param("client_secret", clientSecret)
                .param("redirect_uri", redirectUrl)
                .param("grant_type", "authorization_code")
                .post("/oauth/token")
                .then()
                .log().all()
                .assertThat()
                .statusCode(HttpStatus.OK.value())
                .and()
                .body("access_token", anything());
    }

    private String getQueryStringParam(String url, String key) {
        String query = url.split("\\?")[1];
        final Map<String, String> map = Splitter.on('&').trimResults().withKeyValueSeparator("=").split(query);
        return map.get(key);
    }


}
