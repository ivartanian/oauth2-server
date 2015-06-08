package uk.co.caeldev.oauthserver.config;

import com.mongodb.MongoClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(MongoSettings.class)
@Profile("!test")
public class MongoClientConfiguration {

    @Autowired
    private MongoSettings mongoSettings;

    @Bean
    public MongoClient mongoClient() throws Exception {
        return new MongoClient(mongoSettings.getHost(), mongoSettings.getPort());
    }

}
