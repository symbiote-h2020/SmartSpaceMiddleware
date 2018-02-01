package eu.h2020.symbiote.ssp;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.AbstractMongoConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.util.Arrays;
import java.util.Collection;

@Configuration
@EnableMongoRepositories
class AppConfig extends AbstractMongoConfiguration {

    private static Log log = LogFactory.getLog(AppConfig.class);
    @Value("${rap.mongo.dbname}")
    private String databaseName;
    
    @Value("${rap.mongo.host}")
    private String mongoHost;
    
    @Override
    protected String getDatabaseName() {
        return databaseName;
    }

    @Override
    public Mongo mongo() throws Exception {
        return new MongoClient();
    }

    @Override
    protected Collection<String> getMappingBasePackages() { return Arrays.asList("com.oreilly.springdata.mongodb"); }
    
    @Bean
    @Override
    public MongoTemplate mongoTemplate() throws Exception {
        return new MongoTemplate(new MongoClient(mongoHost), databaseName);
    }

}