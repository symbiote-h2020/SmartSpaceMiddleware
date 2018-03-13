/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.h2020.symbiote.ssp.resources.db;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@Repository
public interface RegistrationInfoODataRepository extends MongoRepository<RegistrationInfoOData, String> {
    
    public List<RegistrationInfoOData> findByClassName(String className);
    
    public List<RegistrationInfoOData> findAll();
    
    default public RegistrationInfoOData insertNew(RegistrationInfoOData registrationInfoOData){
        List<RegistrationInfoOData> rio = findByClassName(registrationInfoOData.getClassName());
        if(rio != null && !rio.isEmpty()){
            for(RegistrationInfoOData rioOfClass: rio){
                if((rioOfClass.getSuperClass()== null && registrationInfoOData.getSuperClass()== null)
                        || rioOfClass.getSuperClass().equals(registrationInfoOData.getSuperClass())){
                    Set<ParameterInfo> piList = rioOfClass.getParameters();
                    registrationInfoOData.getParameters().addAll(piList);
                    registrationInfoOData.setId(rioOfClass.getId());
                }
            }
            try{
                registrationInfoOData = save(registrationInfoOData);
            }
            catch(DuplicateKeyException ex){
                delete(registrationInfoOData);
                registrationInfoOData = save(registrationInfoOData);
            }
        }
        else{
            registrationInfoOData = insert(registrationInfoOData);
        }
        return registrationInfoOData;
    }
}
