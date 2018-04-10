package eu.h2020.symbiote.ssp.utils;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;

public class CheckCoreUtility {
	private Object repository;

	private Boolean isOnline;


	public CheckCoreUtility(Object repo, Boolean isOnline) {
		this.repository=repo;
		this.isOnline=isOnline;
	}
	
	public Boolean getCoreConnectivity() {
		return this.isOnline;
	}

	public String checkCoreSymbioteIdRegistration(String symId) {
		if (!this.isOnline)
			return "";

		try {
			boolean smyIdSessionsRepositoryExists=false;
			//Mock function

			//if I register an SDEV for first time and symId is null
			String symId_ret="";
			if (symId.equals("")) {
				int symIdMock = new Random().nextInt((1000 - 1) + 1) + 1;
				symId_ret="sym"+symIdMock;
				smyIdSessionsRepositoryExists=true;
			}else {
				//search in the sessionsRepository if  symId Exists											
				if (repository instanceof SessionsRepository) {
					smyIdSessionsRepositoryExists = ((SessionsRepository) (repository)).findBySymId(symId) != null;
				}

				if (repository instanceof ResourcesRepository) {
					smyIdSessionsRepositoryExists = ((ResourcesRepository) (repository)).findBySymIdParent(symId) != null;
				}

				symId_ret=symId;
			}
			//DEBUG: search if symId Exists in the Core ALWAYS TRUE FOR THE MOMENT
			boolean smyIdCoreExists=true;

			if (smyIdSessionsRepositoryExists && smyIdCoreExists) {
				return symId_ret;
			}else {
				return null;
			}
		}catch(Exception e) {			
			System.err.println(e);
			return null;
		}
	}


}
