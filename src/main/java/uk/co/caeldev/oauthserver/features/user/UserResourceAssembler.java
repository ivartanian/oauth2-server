package uk.co.caeldev.oauthserver.features.user;

import com.google.common.base.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import uk.co.caeldev.spring.mvc.resources.DomainResourceAssemblerSupport;
import uk.co.caeldev.springsecuritymongo.domain.User;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

import static com.google.common.collect.FluentIterable.from;
import static com.google.common.collect.Sets.newHashSet;

@Component
public class UserResourceAssembler extends DomainResourceAssemblerSupport<User, UserResource> {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserResourceAssembler.class);

    public UserResourceAssembler() {
        super(UserController.class, UserResource.class);
    }

    @Override
    public User toDomain(UserResource userResource, User user) {
        LOGGER.debug("Transform UserResource to User");
        return new User(userResource.getPassword(), userResource.getUsername(), userResource.getUserUUID() == null? null: UUID.fromString(userResource.getUserUUID()), toGrantedAuthorities(userResource), userResource.isAccountNonExpired(), userResource.isAccountNonLocked(), userResource.isCredentialsNonExpired(), userResource.isEnabled());
    }

    @Override
    public UserResource toResource(User user) {
        LOGGER.debug("Transform User to UserResource");
        return new UserResource(user.getUsername(), user.getPassword(), user.getUserUUID() == null? null : user.getUserUUID().toString(), toSetOfStrings(user), user.isAccountNonExpired(), user.isAccountNonLocked(), user.isCredentialsNonExpired(), user.isEnabled());
    }

    private Set<String> toSetOfStrings(User user) {

        if (user.getAuthorities().isEmpty()) {
            return newHashSet();
        }

        return from(user.getAuthorities()).transform(new Function<GrantedAuthority, String>() {
            @Nullable
            @Override
            public String apply(@Nullable GrantedAuthority input) {
                return input.getAuthority();
            }
        }).toSet();
    }

    private Set<GrantedAuthority> toGrantedAuthorities(UserResource userResource) {

        if (userResource.getAuthorities().isEmpty()) {
            return newHashSet();
        }

        return from(userResource.getAuthorities()).transform(new Function<String, GrantedAuthority>() {
            @Nullable
            @Override
            public GrantedAuthority apply(@Nullable String input) {
                return new SimpleGrantedAuthority(input);
            }
        }).toSet();
    }
}
