package uk.co.caeldev.oauthserver.features.user;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.ResponseEntity;
import org.springframework.security.provisioning.UserDetailsManager;
import uk.co.caeldev.springsecuritymongo.domain.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.springframework.http.HttpStatus.CREATED;
import static uk.co.caeldev.oauthserver.builders.UserBuilder.userBuilder;

@RunWith(MockitoJUnitRunner.class)
public class UserControllerTest {

    @Mock
    private UserResourceAssembler userResourceAssembler;

    @Mock
    private UserDetailsManager userDetailsManager;

    private UserController userController;

    @Before
    public void testee() throws Exception {
        userController = new UserController(userDetailsManager, userResourceAssembler);
    }

    @Test
    public void shouldCreateUser() throws Exception {
        //Given
        final UserResource userResource = UserResourceBuilder.userResourceBuilder().build();

        //And
        final User expectedUser = userBuilder()
                .fromUserResource(userResource)
                .build();
        given(userResourceAssembler.toDomain(eq(userResource), any(User.class))).willReturn(expectedUser);

        //And
        doNothing().when(userDetailsManager).createUser(expectedUser);

        //And
        given(userDetailsManager.loadUserByUsername(userResource.getUsername())).willReturn(expectedUser);

        //And
        given(userResourceAssembler.toResource(expectedUser)).willReturn(userResource);

        //When
        final ResponseEntity<UserResource> result = userController.createUser(userResource);

        //Then
        assertThat(result.getStatusCode()).isEqualTo(CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody()).isEqualTo(userResource);
    }
}