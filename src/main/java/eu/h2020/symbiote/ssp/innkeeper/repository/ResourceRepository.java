package eu.h2020.symbiote.ssp.innkeeper.repository;

import eu.h2020.symbiote.ssp.innkeeper.model.InnkeeperResource;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

/**
 * Created by vasgl on 8/24/2017.
 */
@RepositoryRestResource(collectionResourceRel = "resource", path = "resource")
public interface ResourceRepository extends MongoRepository<InnkeeperResource, String> {
}