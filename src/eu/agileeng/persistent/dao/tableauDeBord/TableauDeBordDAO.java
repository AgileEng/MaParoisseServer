package eu.agileeng.persistent.dao.tableauDeBord;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AETimePeriod;

public class TableauDeBordDAO extends AbstractDAO {

	public TableauDeBordDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	private static String selectSQLDefault = 
			"select sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from GeneralJournal gJournal "
			+ "	inner join Account acc on gJournal.ACCOUNT_ID = acc.ID "
			+ "	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
			+ "where "
			+ "	gJournal.OWNER_ID = ? and gJournal.ENTRY_DATE between ? and ? "
			+ "	and coa.OWNER_ID = ? ";
	
	private static String selectSQLDefaultOpeningBalance = 
		"select sum(DEBIT_AMOUNT) as debit_amount, sum(CREDIT_AMOUNT) as credit_amount from AccountBalance accBalance "
		+ "	inner join Account acc on accBalance.ACCOUNT_ID = acc.ID "
		+ "	inner join ChartOfAccounts coa on acc.COA_ID = coa.ID "
		+ "where "
		+ "	accBalance.OWNER_ID = ? and accBalance.YEAR = ? and coa.OWNER_ID = ? ";
	
	public AccAccountBalance loadChauffage(AEDescriptor tenantDescr, AETimePeriod period, List<String> accCodesList) throws AEException {
		AccAccountBalance ab = new AccAccountBalance();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDefault + createAnd(accCodesList));
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				// create from rs
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
			}
			return ab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccAccountBalance loadQuetesOrdinaires(AEDescriptor tenantDescr, AETimePeriod period, List<String> accCodesList) throws AEException {
		AccAccountBalance ab = new AccAccountBalance();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
//			ps = getAEConnection().prepareStatement(selectSQLDefaultOpeningBalance + createAnd(accCodesList));
//			ps.setLong(1, tenantDescr.getID());
//			ps.setLong(2, AEDateUtil.getYear(period.getStartDate()));
//			ps.setLong(3, tenantDescr.getID());
//			rs = ps.executeQuery();
//			while(rs.next()) {
//				// create from rs
//				double dt = rs.getDouble("debit_amount");
//				double ct = rs.getDouble("credit_amount");
//				ab.setOpeningBalance(dt - ct);
//			}	
//			AEConnection.close(rs);
//			AEConnection.close(ps);
			
			ps = getAEConnection().prepareStatement(selectSQLDefault + createAnd(accCodesList));
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				// create from rs
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
			}
			return ab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccAccountBalance loadQuetesParticuleres(AEDescriptor tenantDescr, AETimePeriod period, List<String> accCodesList) throws AEException {
		AccAccountBalance ab = new AccAccountBalance();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDefault + createAnd(accCodesList));
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				// create from rs
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
			}
			return ab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccAccountBalance loadSyntheseCharges(AEDescriptor tenantDescr, AETimePeriod period, List<String> accCodesList) throws AEException {
		AccAccountBalance ab = new AccAccountBalance();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDefault + createAnd(accCodesList));
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				// create from rs
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
			}
			return ab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AccAccountBalance loadSyntheseRecettes(AEDescriptor tenantDescr, AETimePeriod period, List<String> accCodesList) throws AEException {
		AccAccountBalance ab = new AccAccountBalance();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDefault + createAnd(accCodesList));
			ps.setLong(1, tenantDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getStartDate())));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDateTime(period.getEndDate())));
			ps.setLong(4, tenantDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				// create from rs
				ab.setDebitTurnover(rs.getDouble("debit_amount"));
				ab.setCreditTurnover(rs.getDouble("credit_amount"));

				// calculate
				ab.calculateFinalBalance();
				
				// set view
				ab.setView();
			}
			return ab;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private String createAnd(List<String> accCodesList) throws AEException {
		try {
			StringBuilder sb = new StringBuilder();
			for (String accCode : accCodesList) {
				if(sb.length() == 0) {
					sb.append(" and (");
				} else {
					sb.append(" or ");
				};
				sb.append(" acc.code like '").append(accCode).append("' ");
			}
			sb.append(")");
			return sb.toString();
		} catch (Exception e) {
			throw AEError.System.INVALID_REQUEST.toException();
		}
	}
}
