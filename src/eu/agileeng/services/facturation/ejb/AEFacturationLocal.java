package eu.agileeng.services.facturation.ejb;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.facturation.AEFacturationService;


public interface AEFacturationLocal extends AEFacturationService {
	public void setNextNumber(AEDescriptive descr, AEConnection aeConnection, AEInvocationContext invContext) throws AEException;
}
