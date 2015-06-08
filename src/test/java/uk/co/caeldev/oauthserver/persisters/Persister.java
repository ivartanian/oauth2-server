package uk.co.caeldev.oauthserver.persisters;

import uk.co.caeldev.springsecuritymongo.domain.MongoClientDetails;
import uk.co.caeldev.springsecuritymongo.domain.User;

public interface Persister {

    User persist(User user);

    MongoClientDetails persist(MongoClientDetails mongoClientDetails);

}
