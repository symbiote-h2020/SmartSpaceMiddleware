package eu.h2020.symbiote.ssp.utils;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;

public class InternalIdUtils {

	private Object repository;
	public InternalIdUtils(Object repository){
		this.repository=repository;
	}
	public String createInternalId() {
		// TODO Auto-generated method stub
		String myInternalId = null;
		if (repository instanceof SessionsRepository) {

			List<SessionInfo> ss= ((SessionsRepository)(repository)).findAll();
			List<String> sessionIdList = new ArrayList<String>();

			for (SessionInfo s : ss)
				sessionIdList.add(s.getInternalId() );
			int i=0;
			myInternalId = Integer.toString(i);
			while (true) {
				myInternalId = Integer.toString(i);
				if (!sessionIdList.contains(myInternalId)) {
					break;
				}
				i++;
			}

		}
		if (repository instanceof ResourcesRepository) {

			List<ResourceInfo> ss= ((ResourcesRepository)(repository)).findAll();
			List<String> sessionIdList = new ArrayList<String>();

			for (ResourceInfo s : ss)
				sessionIdList.add(s.getInternalId() );
			int i=0;
			myInternalId = Integer.toString(i);
			while (true) {
				myInternalId = Integer.toString(i);
				if (!sessionIdList.contains(myInternalId)) {
					break;
				}
				i++;
			}

		}
		return myInternalId;


	}


}
