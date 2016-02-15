package uk.co.caeldev.oauthserver.features.user;

import com.jayway.restassured.http.ContentType;
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
import uk.co.caeldev.oauthserver.Application;
import uk.co.caeldev.oauthserver.persisters.Persister;

import static com.jayway.restassured.RestAssured.given;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static uk.co.caeldev.oauthserver.builders.SSM.givenMongoClientDetail;
import static uk.co.caeldev.oauthserver.builders.SSM.givenUser;
import static uk.co.caeldev.oauthserver.features.user.UserResourceBuilder.userResourceBuilder;
import static uk.org.fyodor.generators.RDG.integer;
import static uk.org.fyodor.generators.RDG.string;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration
@ActiveProfiles("test")
@IntegrationTest("server.port:0")
public class UserControllerIntegrationTest {

    public static final String OAUTH2_RESOURCE = "oauth2-resource";

    @Value("${local.server.port}")
    private int port;

    @Value("${server.contextPath}")
    private String basePath;

    @Autowired
    private Persister persister;

    @Test
    public void shouldCreateUser() {
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
                .scopes("read", "write")
                .authorizedGrantTypes("password")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect(redirectUrl)
                .persist(persister);

        //And
        UserResource userResource = userResourceBuilder()
                .noUUID()
                .build();

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

        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .when()
                .log().all()
                .contentType(ContentType.JSON)
                .body(userResource)
                .queryParam("access_token", accessToken)
                .post("/users")
                .then()
                .assertThat()
                .statusCode(CREATED.value());
    }

    @Test
    public void shouldNotCreateUserWhenDoesNotHaveWriteScope() {
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
                .authorizedGrantTypes("password")
                .authorities("ROLE_CLIENT")
                .resourceIds(OAUTH2_RESOURCE)
                .redirect(redirectUrl)
                .persist(persister);

        //And
        UserResource userResource = userResourceBuilder()
                .noUUID()
                .build();

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

        given().port(port).basePath(basePath).redirects().follow(true).log().all()
                .when()
                .log().all()
                .contentType(ContentType.JSON)
                .body(userResource)
                .queryParam("access_token", accessToken)
                .post("/users")
                .then()
                .assertThat()
                .statusCode(FORBIDDEN.value());
    }
}
