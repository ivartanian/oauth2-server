package uk.co.caeldev.oauthserver.builders;

import com.google.common.collect.Maps;

public class SSM {

    public static UserBuilder givenUser() {
        return UserBuilder.userBuilder()
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .enabled(true);
    }

    public static MongoClientDetailsBuilder givenMongoClientDetail() {
        return MongoClientDetailsBuilder.mongoClientDetailsBuilder()
                .accessTokenValiditySeconds(30000)
                .refreshTokenValiditySeconds(30000)
                .additionalInformation(Maps.<String, Object>newHashMap())
                .autoApproveScopes("");
    }
}
