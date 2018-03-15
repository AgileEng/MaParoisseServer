/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:01:36
 */
package eu.agileeng.services.acc;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

/**
 *
 */
public interface AccService extends AEService {
	public AEResponse loadVATItems(AERequest request) throws AEException;
	public AEResponse saveVATItems(AERequest request) throws AEException;
	public AEResponse loadCOAModels(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse saveCOAModels(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse importAccounts(AERequest request) throws AEException;
	public AEResponse loadCOA(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse saveCOA(AERequest request, AEInvocationContext invContext) throws AEException;
	public AEResponse deleteCOA(AERequest request) throws AEException;
	public AEResponse saveGOA(AERequest request) throws AEException;
	public AEResponse loadGOA(AERequest request) throws AEException;
	public AEResponse loadGeneralJournal(AERequest request) throws AEException;
	public AEResponse export(AERequest aeRequest,  AEInvocationContext invContext) throws AEException; 
	public AEResponse exportDaily(AERequest aeRequest,  AEInvocationContext invContext) throws AEException;
	public AEResponse loadAccPeriodByFilter(AERequest request) throws AEException;
	
	/**
	 * Loads final balance for specified account for specified up-to-date.
	 * 
	 * @param accBalance
	 * @param invContext
	 * @param aeConnection
	 * @return
	 * @throws AEException
	 */
	public void loadAccFinalBalance(AccAccountBalance accBalance, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public void loadAccFinalBalance(AccAccountBalance accBalance, AEDescriptive journal, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;	
}
