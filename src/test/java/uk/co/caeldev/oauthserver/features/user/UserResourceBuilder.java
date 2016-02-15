package uk.co.caeldev.oauthserver.features.user;

import uk.org.fyodor.generators.Generator;

import java.util.Set;
import java.util.UUID;

import static uk.org.fyodor.generators.RDG.*;

public class UserResourceBuilder {

    private String username = string().next();
    private String password = string().next();
    private String userUUID = UUID.randomUUID().toString();
    private Set<String> authorities = set(ofAuthorities()).next();
    private Boolean accountNonExpired = bool().next();
    private Boolean accountNonLocked = bool().next();
    private Boolean credentialsNonExpired = bool().next();
    private Boolean enabled = bool().next();

    private UserResourceBuilder() {
    }

    public static UserResourceBuilder userResourceBuilder() {
        return new UserResourceBuilder();
    }

    public UserResource build() {
        return new UserResource(username, password, userUUID, authorities, accountNonExpired, accountNonLocked, credentialsNonExpired, enabled);
    }

    private Generator<String> ofAuthorities() {
        return new Generator<String>() {
            @Override
            public String next() {
                return string().next();
            }
        };
    }

    public UserResourceBuilder noUUID() {
        this.userUUID = null;
        return this;
    }
}
