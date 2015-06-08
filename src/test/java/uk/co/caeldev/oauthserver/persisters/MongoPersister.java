package uk.co.caeldev.oauthserver.persisters;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.co.caeldev.springsecuritymongo.domain.MongoClientDetails;
import uk.co.caeldev.springsecuritymongo.domain.User;
import uk.co.caeldev.springsecuritymongo.repositories.MongoClientDetailsRepository;
import uk.co.caeldev.springsecuritymongo.repositories.UserRepository;

@Component
public class MongoPersister implements Persister {

    private final UserRepository userRepository;
    private final MongoClientDetailsRepository mongoClientDetailsRepository;

    @Autowired
    public MongoPersister(final UserRepository userRepository,
                          final MongoClientDetailsRepository mongoClientDetailsRepository) {
        this.userRepository = userRepository;
        this.mongoClientDetailsRepository = mongoClientDetailsRepository;
    }

    @Override
    public User persist(User user) {
        return userRepository.save(user);
    }

    @Override
    public MongoClientDetails persist(MongoClientDetails mongoClientDetails) {
        return mongoClientDetailsRepository.save(mongoClientDetails);
    }
}
