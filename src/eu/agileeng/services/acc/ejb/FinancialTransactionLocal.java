package eu.agileeng.services.acc.ejb;

import javax.ejb.Local;

import eu.agileeng.services.acc.FinancialTransactionService;

@Local
public interface FinancialTransactionLocal extends FinancialTransactionService {
	
}
