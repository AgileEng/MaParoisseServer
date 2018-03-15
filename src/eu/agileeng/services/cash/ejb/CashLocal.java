/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 15.11.2009 11:58:45
 */
package eu.agileeng.services.cash.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCData;
import eu.agileeng.domain.inventory.InventoryStatus;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.cash.CashService;


/**
 *
 */
@Local
public interface CashLocal extends CashService {
	public CFCData loadCFCData(CFC cfc, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadMandatData(CFC mandat, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public AEResponse loadUnpaidBankDocument(AERequest aeRequest, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public AEResponse createCashJournalItems(AERequest aeRequest, AEConnection aeConnection) throws AEException;
	
	public CFCData loadPAMData(CFC mandat, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadIDEData(CFC ide, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadInventorySupplyData(CFC invSupply, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadInventoryStatusData(InventoryStatus invStatus, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadInventoryStatusDataAnnually(InventoryStatus invStatus, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
	
	public CFCData loadMandatDataExt(CFC mandatExt, CFC Mandat, AccPeriod accPeriod, AEInvocationContext invContext, AEConnection aeConnection) throws AEException;
}
