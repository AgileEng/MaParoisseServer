/**
 * 
 */
package eu.agileeng.services.bank.ejb;

import java.io.File;

import javax.ejb.Local;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.bank.BankRecognitionRulesList;
import eu.agileeng.domain.business.bank.BankTransactionsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEResponse;
import eu.agileeng.services.bank.BankService;

/**
 * Agile Engineering Ltd
 * 
 * @author Vesko Vatov
 * @date 15.11.2013 11:58:45
 */
@Local
public interface BankLocal extends BankService {
	public BankRecognitionRulesList saveRecognitionRules(BankRecognitionRulesList rules, AEConnection localConnection) throws AEException;
	public AEResponse importETEBAC(File fileETEBAC, AEInvocationContext invContext) throws AEException;
	public void recognizeBankTransactionsList(BankTransactionsList bankTransList, BankRecognitionRulesList rulesList) throws AEException;
}
