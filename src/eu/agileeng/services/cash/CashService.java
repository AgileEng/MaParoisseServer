/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 15.11.2009 11:54:34
 */
package eu.agileeng.services.cash;

import java.io.File;

import eu.agileeng.domain.AEException;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.AEService;



/**
 *
 */
public interface CashService extends AEService{
	public AEResponse loadCashDeskTurnOver(AERequest aeRequest) throws AEException;
	
	public AEResponse saveCashJournalEntry(AERequest aeRequest) throws AEException;
	
	public AEResponse closingPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse loadCFC(AERequest aeRequest) throws AEException;
	
	public AEResponse saveCFC(AERequest aeRequest) throws AEException;
	
	public AEResponse saveCFCCell(AERequest aeRequest) throws AEException;
	
	public AEResponse closingCFCPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openCFCPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse saveBankAccount(AERequest aeRequest) throws AEException;
	
	public AEResponse loadBankAccounts(AERequest aeRequest) throws AEException;
	
	public AEResponse saveBankTransactions(AERequest aeRequest) throws AEException;
	
	public AEResponse loadBankTransactions(AERequest aeRequest) throws AEException;
	
	public AEResponse saveBankBalances(AERequest aeRequest, AEInvocationContext invocationContext) throws AEException;
	
	public AEResponse loadBankBalances(AERequest aeRequest, AEInvocationContext invocationContext) throws AEException;
	
	public AEResponse importETEBAC(File fileETEBAC, AEInvocationContext invContext) throws AEException;
	
	public AEResponse closingBankPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openBankPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse loadMandat(AERequest aeRequest) throws AEException;
	
	public AEResponse saveMandat(AERequest aeRequest) throws AEException;
	
	public AEResponse saveMandatCell(AERequest aeRequest) throws AEException;
	
	public AEResponse closingMandatPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openMandatPeriod(AERequest aeRequest) throws AEException;

	public AEResponse loadBankDocumentData(AERequest aeRequest) throws AEException;
	
	public AEResponse closingBankDocumentPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openBankDocumentPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse loadBankDocument(AERequest aeRequest) throws AEException;
	
	public AEResponse payBankDocuments(AERequest aeRequest) throws AEException;
	
	public AEResponse reconcileBankDocuments(AERequest aeRequest) throws AEException;
	
	public AEResponse recognizeBankTransactions(AERequest aeRequest) throws AEException;
	
	public AEResponse accountCashSubPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadPAM(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse savePAM(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse savePAMCell(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse closingPAMPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse openPAMPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadIDE(AERequest aeRequest) throws AEException;
	
	public AEResponse saveIDE(AERequest aeRequest) throws AEException;
	
	public AEResponse saveIDECell(AERequest aeRequest) throws AEException;
	
	public AEResponse closingIDEPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse openIDEPeriod(AERequest aeRequest) throws AEException;
	
	public AEResponse loadInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveInventoryStatusCell(AERequest aeRequest) throws AEException;
	
	public AEResponse closeInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse openInventoryStatus(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveInventoryStatusCellAnnually(AERequest aeRequest) throws AEException;
	
	public AEResponse loadMandatExt(AERequest aeRequest) throws AEException;
	
	public AEResponse saveMandatExt(AERequest aeRequest) throws AEException;
	
	public AEResponse saveMandatCellExt(AERequest aeRequest) throws AEException;
	
	public AEResponse closingMandatPeriodExt(AERequest aeRequest) throws AEException;
	
	public AEResponse openMandatPeriodExt(AERequest aeRequest) throws AEException;

	public AEResponse recognizeAndSaveBankTransactions(AERequest aeRequest) throws AEException;

}
