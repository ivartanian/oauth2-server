package uk.co.caeldev.oauthserver.features.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.caeldev.spring.mvc.ResponseEntityBuilder;
import uk.co.caeldev.springsecuritymongo.domain.User;

import java.security.Principal;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;

@RestController
public class UserController {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserController.class);

    private final UserDetailsManager userDetailsManager;
    private final UserResourceAssembler userResourceAssembler;

    @Autowired
    public UserController(@Qualifier("mongoUserDetailsManager") final UserDetailsManager userDetailsManager,
                          final UserResourceAssembler userResourceAssembler) {
        this.userDetailsManager = userDetailsManager;
        this.userResourceAssembler = userResourceAssembler;
    }

    @RequestMapping("/user")
    public Principal user(Principal user) {
        return user;
    }

    @RequestMapping(value = "/users",
            method = POST,
            consumes = {APPLICATION_JSON_VALUE},
            produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<UserResource> createUser(@RequestBody final UserResource userResource) {
        LOGGER.info("Creating user");

        final User user = userResourceAssembler.toDomain(userResource, new User());

        userDetailsManager.createUser(user);

        final UserDetails userCreated = userDetailsManager.loadUserByUsername(userResource.getUsername());

        return ResponseEntityBuilder.
                <UserResource>responseEntityBuilder()
                .statusCode(CREATED)
                .entity(userResourceAssembler.toResource((User)userCreated))
                .build();
    }
}
