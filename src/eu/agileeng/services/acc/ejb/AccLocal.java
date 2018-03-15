/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 09.12.2009 21:02:33
 */
package eu.agileeng.services.acc.ejb;

import java.util.Date;

import javax.ejb.Local;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccJournalItemsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.acc.AccService;

/**
 *
 */
@Local
public interface AccLocal extends AccService {
	public void saveCOA(JSONObject jsonCOA, AEConnection aeConnection) throws AEException;
	public JSONObject loadCOA(long orgId, AEConnection aeConnection) throws AEException;
	public JSONArray loadAccounts(long orgId, AEConnection aeConnection, String whereSql) throws AEException;
	public void deleteCOA(long coaId, AEConnection aeConnection) throws AEException;
	public void saveGOA(JSONObject jsonGOA, long ownerId, AEConnection aeConnection) throws AEException;
	public JSONArray loadCashAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException;
	public AccJournalItemsList loadGeneralJournal(long ownerId, String journalCode, Date entryDate, AEConnection aeConnection) throws AEException;
	public JSONArray loadSupplyAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException;
	public JSONArray loadSaleAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException;
	public JSONArray loadBankAccountsByOwner(long ownerId, AEConnection aeConnection) throws AEException;
	
	public AEResponse loadInitialBalance(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveInitialBalance(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadDonations(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveDonations(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadBudget(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveBudget(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadBudgetReal(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadBordereauParoisse(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse saveBordereauParoisse(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse loadAccPeriods(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse closeAccPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public AEResponse openAccPeriod(AERequest aeRequest, AEInvocationContext invContext) throws AEException;
	
	public JSONObject syncDonationsJson(AEDescriptor compDescr, int year, AEConnection aeConnection) throws AEException;
	
	public JSONObject syncDonationsJava(AEDescriptor compDescr, int year, AEConnection aeConnection) throws AEException;
}
