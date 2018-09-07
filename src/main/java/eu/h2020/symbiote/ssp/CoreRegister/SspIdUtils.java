package eu.h2020.symbiote.ssp.CoreRegister;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import eu.h2020.symbiote.ssp.resources.db.ResourceInfo;
import eu.h2020.symbiote.ssp.resources.db.ResourcesRepository;
import eu.h2020.symbiote.ssp.resources.db.SessionInfo;
import eu.h2020.symbiote.ssp.resources.db.SessionsRepository;

public class SspIdUtils {

	private Object repository;
	public SspIdUtils(Object repository){
		this.repository=repository;
	}
	public String createSspId() {
		// TODO Auto-generated method stub
		String mySspId = null;
		if (repository instanceof SessionsRepository) {

			List<SessionInfo> ss= ((SessionsRepository)(repository)).findAll();
			List<String> sessionIdList = new ArrayList<String>();

			for (SessionInfo s : ss)
				sessionIdList.add(s.getSspId() );
			int i=0;
			mySspId = Integer.toString(i);
			while (true) {
				mySspId = Integer.toString(i);
				if (!sessionIdList.contains(mySspId)) {
					break;
				}
				i++;
			}

		}
		if (repository instanceof ResourcesRepository) {

			List<ResourceInfo> ss= ((ResourcesRepository)(repository)).findAll();
			List<String> resourceIdList = new ArrayList<String>();

			for (ResourceInfo s : ss) {
				resourceIdList.add(s.getSspIdResource() );
			}
			
			int i=0;
			mySspId = Integer.toString(i);
			while (true) {
				mySspId = Integer.toString(i);
				if (!resourceIdList.contains(mySspId)) {
					break;
				}
				i++;
			}

		}
		return mySspId;


	}


}
