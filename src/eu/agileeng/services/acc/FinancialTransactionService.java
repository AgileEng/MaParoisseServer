package eu.agileeng.services.acc;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;

public interface FinancialTransactionService extends AEService{
	/**
	 * Loads application module template related to specified <code>application module</code> 
	 * and valid on specified <code>date</code>) 
	 * with FinancialTransactionTemplates (optional) related to the template.
	 * 
	 * @param aeRequest
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	public AEResponse loadFinancialTransactionTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	
	public AEResponse saveFinancialTransactionTemplates(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Creates a Financial Transaction for specified app module (given as parameter). 
	 * 
	 * @param aeRequest
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	public AEResponse createFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Saves a Financial Transaction for specified app module (given as parameter). 
	 * 
	 * @param aeRequest
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	public AEResponse saveFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAccJournals(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAccJournalItemsByFilter(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Loads a Financial Transaction identified by <code>accJournalItemId</code> or <code>financialTransactionId</code>. 
	 * 
	 * @param aeRequest
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	public AEResponse loadFinancialTransaction(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	/**
	 * Saves specified <code>accJournalItems</code> into specified <code>accJournal</code>. 
	 * 
	 * @param aeRequest
	 * @param invContext
	 * @return
	 * @throws AEException
	 */
	public AEResponse saveAccJournalItems(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse importContributors(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse deleteContributor(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse tallyAccJournalItem(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
}
