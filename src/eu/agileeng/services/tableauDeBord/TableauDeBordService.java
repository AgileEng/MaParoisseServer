package eu.agileeng.services.tableauDeBord;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

public interface TableauDeBordService extends AEService {

	public AEResponse loadChauffage(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadQuetesOrdinaires(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadQuetesParticuleres(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadSyntheseCharges(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadSyntheseRecettes(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	
}
