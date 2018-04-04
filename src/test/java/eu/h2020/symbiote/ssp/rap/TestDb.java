package eu.h2020.symbiote.ssp.rap;

import eu.h2020.symbiote.security.accesspolicies.common.AccessPolicyType;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicy;
import eu.h2020.symbiote.ssp.resources.db.AccessPolicyRepository;
import eu.h2020.symbiote.ssp.resources.db.ParameterInfo;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoOData;
import eu.h2020.symbiote.ssp.resources.db.RegistrationInfoODataRepository;
import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.security.accesspolicies.IAccessPolicy;
import eu.h2020.symbiote.security.accesspolicies.common.SingleTokenAccessPolicyFactory;
import eu.h2020.symbiote.security.accesspolicies.common.singletoken.SingleTokenAccessPolicySpecifier;
import eu.h2020.symbiote.security.commons.exceptions.custom.InvalidArgumentsException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author Luca Tomaselli <l.tomaselli@nextworks.it>
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TestConfiguration 
@ActiveProfiles("test")
public class TestDb {
    private static final Logger log = LoggerFactory.getLogger(TestDb.class);
    
    @Autowired
    private ResourcesRepository resourcesRepository;

    @Autowired
    private AccessPolicyRepository accessPolicyRepository;
    
    @Autowired
    private RegistrationInfoODataRepository registrationInfoODataRepository;
    
    
    @Test
    public void testResourceInfo() throws Exception{
        //insert
        String resourceId = "1";
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "plugin_1";
        ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
        assert(resourceInfoResult != null);
        //search
        Optional<ResourceInfo> resInfoOptional = resourcesRepository.findById(resourceId);
        assert(resInfoOptional.isPresent() == true);
        resInfoOptional = resourcesRepository.findById(resourceId+"2");
        assert(resInfoOptional.isPresent() == false);        
        List<ResourceInfo> resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId+"2");
        assert(resourceInfoList == null || resourceInfoList.isEmpty());
        resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId);
        assert(resourceInfoList != null);
        assert(!resourceInfoList.isEmpty());
        //delete
        resourcesRepository.delete(resourceId);
        resourceInfoList = resourcesRepository.findByInternalIdResource(platformResourceId);
        assert(resourceInfoList == null || resourceInfoList.isEmpty());
    }
    
    private ResourceInfo addResource(String resourceId, String platformResourceId, List<String> obsProperties, String pluginId) {
        ResourceInfo resourceInfo = new ResourceInfo(resourceId, "", platformResourceId,"", "");
        if(obsProperties != null)
            resourceInfo.setObservedProperties(obsProperties);
        if(pluginId != null && pluginId.length()>0)
            resourceInfo.setPluginUrl(pluginId);
        
        ResourceInfo resourceInfoResult = resourcesRepository.save(resourceInfo);
        return resourceInfoResult;
    }

    @Test 
    public void testAccessPolicy() throws Exception{
        //insert resource
        String resourceId = "1";
        String platformResourceId = "pl_1";
        List<String> obsProperties = null;
        String pluginId = "plugin_1";
        ResourceInfo resourceInfoResult = addResource(resourceId, platformResourceId, obsProperties, pluginId);
        assert(resourceInfoResult != null);
        //insert
        AccessPolicyType policyType = AccessPolicyType.PUBLIC;
        Map<String, String> requiredClaims = new HashMap<>();
        SingleTokenAccessPolicySpecifier accPolicy = new SingleTokenAccessPolicySpecifier(policyType,requiredClaims);
        AccessPolicy accessPolicy = addPolicy(resourceId,platformResourceId,accPolicy);
        assert(accessPolicy != null);
        //search
        Optional<AccessPolicy> accessPolicyOptional = accessPolicyRepository.findById(resourceId);
        assert(accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findByInternalId(platformResourceId);
        assert(accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findById(platformResourceId);
        assert(!accessPolicyOptional.isPresent());
        accessPolicyOptional = accessPolicyRepository.findByInternalId(resourceId);
        assert(!accessPolicyOptional.isPresent());
        //delete
        accessPolicyRepository.delete(resourceId);
        accessPolicyOptional = accessPolicyRepository.findById(resourceId);
        assert(!accessPolicyOptional.isPresent());
        //delete resource
        //delete
        resourcesRepository.delete(resourceId);
        Optional<ResourceInfo> resourceInfoOptional = resourcesRepository.findById(resourceId);
        assert(!resourceInfoOptional.isPresent());
    }
    
    private AccessPolicy addPolicy(String resourceId, String internalId, SingleTokenAccessPolicySpecifier accPolicy) throws InvalidArgumentsException {
        IAccessPolicy policy = SingleTokenAccessPolicyFactory.getSingleTokenAccessPolicy(accPolicy);
        AccessPolicy ap = new AccessPolicy(resourceId, internalId, policy);
        AccessPolicy apNew = accessPolicyRepository.save(ap);
        return apNew;
    } 
    
    @Test
    public void testRegistrationInfoOData() throws Exception{
        //insert
        String symbioteId = "1";
        String className = "className";
        String superClass = "superClass";
        Set<ParameterInfo> parameters = new HashSet<ParameterInfo>();
        ParameterInfo parameterInfo = new ParameterInfo("type","name");
        parameters.add(parameterInfo);
        RegistrationInfoOData rio = new RegistrationInfoOData(symbioteId, className, superClass, parameters);
        registrationInfoODataRepository.insertNew(rio);
        //search
        List<RegistrationInfoOData> registrationInfoODataList = registrationInfoODataRepository.findByClassName(className);
        assert(registrationInfoODataList != null);
        assert(!registrationInfoODataList.isEmpty());
        registrationInfoODataList = registrationInfoODataRepository.findByClassName(className+"2");
        assert(registrationInfoODataList == null || registrationInfoODataList.isEmpty());
        //delete
        registrationInfoODataRepository.delete(rio.getId());
        registrationInfoODataList = registrationInfoODataRepository.findByClassName(className);
        assert(registrationInfoODataList == null || registrationInfoODataList.isEmpty());
    }
}
