package eu.agileeng.persistent.dao.facturation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.facturation.AEClient;
import eu.agileeng.domain.facturation.AEFacture;
import eu.agileeng.domain.facturation.AEFactureDescr;
import eu.agileeng.domain.facturation.AEFactureUtil;
import eu.agileeng.domain.facturation.AEPayment;
import eu.agileeng.domain.facturation.AEPaymentsFilter;
import eu.agileeng.domain.facturation.AEPaymentsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;

public class AEPaymentDAO extends AbstractDAO {
	private static String insertSQL = 
		"insert into Fact_Payment "
		+ "(CODE, NAME, DESCRIPTION, PROPERTIES, FACTURE_ID, PAYER_ID, "
		+ " PAYMENT_TERMS_ID, PAYABLE_TYPE_ID, PAYMENT_TYPE_ID, DATE, "
		+ " DUE_DATE, AMOUNT, PAID, PAID_DATE) "
		+ " values (?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, "
		+ " ?, ?, ?, ?)";
	
	private static String updateSQL = "update Fact_Payment set "
		+ "CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, FACTURE_ID = ?, PAYER_ID = ?, "
		+ " PAYMENT_TERMS_ID = ?, PAYABLE_TYPE_ID = ?, PAYMENT_TYPE_ID = ?, DATE = ?, "
		+ " DUE_DATE = ?, AMOUNT = ?, PAID = ?, PAID_DATE = ? "
		+ " where ID = ?";
	
	private static String updateSQLPayableType = "update Fact_Payment set "
		+ "PAYABLE_TYPE_ID = ? where ID = ?";
	
	private static String updateSQLToPaid = "update Fact_Payment set "
		+ "PAID = 1, PAID_DATE = ? where ID = ?";
	
	private static String updateSQLToUnpaid = "update Fact_Payment set "
		+ "PAID = 0, PAID_DATE = NULL where ID = ?";
	
	private static String selectSQL = 
		"select * from Fact_Payment where id = ?";
	
	private static String selectSQLFull = 
		"select p.*, f.OWNER_ID, f.CLIENT_CODE as PAYER_CODE, f.CLIENT_NAME as PAYER_NAME, f.DESCRIPTION as PAYER_DESCRIPTION, "
		+ " f.TYPE_ID as FACTURE_TYPE, f.SUB_TYPE_ID as FACTURE_SUB_TYPE, f.STATE as FACTURE_STATE, f.DOC_DATE as FACTURE_DATE, f.NUMBER_STRING as FACTURE_NUMBER "
		+ " from Fact_Payment p inner join Fact_Facture f on p.FACTURE_ID = f.ID "
		+ " where p.id = ?"; 
	
	private static String deleteSQL = 
		"delete from Fact_Payment where id = ?";
	
	private static String selectSQLToFacture = 
		"select * from Fact_Payment where FACTURE_ID = ? order by PAYMENT_TYPE_ID";
	
	private static String selectSQLAdvancePaymentToFacture = 
		"select * from Fact_Payment where FACTURE_ID = ? and PAYMENT_TYPE_ID = " + AEFactureUtil.PaymentType.ADVANCE.getTypeId();
	
	private static String selectSQLByFilter = 
		"select p.*, f.OWNER_ID, f.CLIENT_CODE as PAYER_CODE, f.CLIENT_NAME as PAYER_NAME, f.DESCRIPTION as PAYER_DESCRIPTION, "
		+ " f.TYPE_ID as FACTURE_TYPE, f.DOC_DATE as FACTURE_DATE, f.NUMBER_STRING as FACTURE_NUMBER "
		+ " from Fact_Payment p inner join Fact_Facture f on p.FACTURE_ID = f.ID "
		+ " where owner_id = ? and PAYABLE_TYPE_ID = " + AEFactureUtil.PayableType.REQUISITION.getTypeId(); 
	
	private static String selectSQLOverdueDebts = 
		"select p.*, f.OWNER_ID, f.CLIENT_CODE as PAYER_CODE, f.CLIENT_NAME as PAYER_NAME, f.DESCRIPTION as PAYER_DESCRIPTION, "
		+ " f.TYPE_ID as FACTURE_TYPE, f.DOC_DATE as FACTURE_DATE, f.NUMBER_STRING as FACTURE_NUMBER "
		+ " from Fact_Payment p inner join Fact_Facture f on p.FACTURE_ID = f.ID "
		+ " where owner_id = ? and PAYABLE_TYPE_ID = " + AEFactureUtil.PayableType.REQUISITION.getTypeId() 
		+ " and p.DUE_DATE < ?"; 
	
	private static String selectSQLOverdueDebtsSum = 
		"   select SUM(payment.AMOUNT) as amount from Fact_Payment payment "
		+ "	  inner join Fact_Facture facture on payment.FACTURE_ID = facture.ID "
		+ " where "
		+ "	  facture.owner_id = ? " 
		+ "	  and payment.PAYABLE_TYPE_ID = ? " 
		+ "	  and payment.DUE_DATE < ? "
		+ "	  and payment.PAID = 0";
	
	private static String selectSQLMaturityDebts = 
		"select p.*, f.OWNER_ID, f.CLIENT_CODE as PAYER_CODE, f.CLIENT_NAME as PAYER_NAME, f.DESCRIPTION as PAYER_DESCRIPTION, "
		+ " f.TYPE_ID as FACTURE_TYPE, f.DOC_DATE as FACTURE_DATE, f.NUMBER_STRING as FACTURE_NUMBER "
		+ " from Fact_Payment p inner join Fact_Facture f on p.FACTURE_ID = f.ID "
		+ " where owner_id = ? and PAYABLE_TYPE_ID = " + AEFactureUtil.PayableType.REQUISITION.getTypeId() 
		+ " and p.DUE_DATE BETWEEN ? AND ?"; 
	
	private static String selectSQLAdvancedInvoicedToDevis = 
		"select advPayment.* from "
		+ " Fact_Payment advPayment inner join Fact_Facture advFacture on advPayment.FACTURE_ID = advFacture.ID"
		+ " inner join Fact_Facture devise on advFacture.SRC_FACTURE_ID = devise.ID" 
		+ " where devise.ID = ? "
		+ " and advFacture.TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID() 
		+ " and advFacture.SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.ADVANCE.getSubTypeId() 
		+ " and advPayment.PAYABLE_TYPE_ID = " + AEFactureUtil.PayableType.JUNK.getTypeId();
	
	private static String selectSQLValidateOwner = 
		"select payment.id, facture.OWNER_ID from Fact_Payment payment "
		+ " inner join Fact_Facture facture on payment.FACTURE_ID = facture.ID "
        + " where payment.ID = ? and facture.OWNER_ID = ?";
	
	public AEPaymentDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(AEPayment payment) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(payment, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				payment.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			payment.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private void build(AEPayment p, ResultSet rs) throws SQLException, AEException {
		// ID
		p.setID(rs.getLong("ID"));
		
		// f.owner_id
		try {
			long owner_id = rs.getLong("owner_id");
			p.setCompany(Organization.lazyDescriptor(owner_id));
		} catch(Exception e){}
		
		// CODE
		p.setCode(rs.getString("CODE"));
		
		// NAME
		p.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		p.setDescription(rs.getString("DESCRIPTION"));
		
		// PROPERTIES
		p.setProperties(rs.getLong("PROPERTIES"));
		
		// FACTURE_ID
		long factureId = rs.getLong("FACTURE_ID");
		if(factureId > 0) {
			AEFactureDescr facture = AEFacture.lazyDescriptor(factureId);
			
			try {
				long typeId = rs.getLong("FACTURE_TYPE");
				facture.setDocumentType(AEDocumentType.valueOf(typeId));
			} catch(Exception e){}
			
			try {
				long subTypeId = rs.getLong("FACTURE_SUB_TYPE");
				facture.setSubType(AEFactureUtil.FactureSubType.valueOf(subTypeId));
			} catch(Exception e){}
			
			try {
				long stateId = rs.getLong("FACTURE_STATE");
				facture.setState(AEFactureUtil.FactureState.valueOf(stateId));
			} catch(Exception e){}
			
			try {
				facture.setDate(rs.getDate("FACTURE_DATE"));
			} catch(Exception e){}
			
			try {
				facture.setNumber(rs.getString("FACTURE_NUMBER"));
			} catch(Exception e){}
			
			p.setToFacture(facture);
		}
		
		// PAYER_ID
		AEDescriptor payer = AEClient.lazyDescriptor(rs.getLong("PAYER_ID"));

		try {
			payer.setCode(rs.getString("PAYER_CODE"));
		} catch(Exception e){}

		try {
			payer.setName(rs.getString("PAYER_NAME"));
		} catch(Exception e){}

		try {
			payer.setDescription(rs.getString("PAYER_DESCRIPTION"));
		} catch(Exception e){}

		p.setPayer(payer);
		
		// PAYMENT_TERMS_ID
		long paymentTermsId = rs.getLong("PAYMENT_TERMS_ID");
		if(!rs.wasNull()) {
			try {
				p.setPaymentTerms(AEFactureUtil.getPaymentTerms(paymentTermsId));
			} catch (Exception e) {
			}
		}
		
		// PAYABLE_TYPE_ID
		p.setPayableType(AEFactureUtil.PayableType.valueOf(rs.getLong("PAYABLE_TYPE_ID")));
		
		// PAYMENT_TYPE_ID
		p.setPaymentType(AEFactureUtil.PaymentType.valueOf(rs.getLong("PAYMENT_TYPE_ID")));
		
		// DATE
		p.setDate(rs.getDate("DATE"));
		
		// DUE_DATE
		p.setDueDate(rs.getDate("DUE_DATE"));
		
		// AMOUNT
		p.setAmount(rs.getDouble("AMOUNT"));
		
		// PAID
		p.setPaid(rs.getBoolean("PAID"));
		
		// PAID_DATE
		p.setPaidDate(rs.getDate("PAID_DATE"));
	}
	
	private int build(AEPayment p, PreparedStatement ps, int i) throws SQLException, AEException {
		// CODE
		ps.setString(i++, p.getCode());
		
		// NAME
		ps.setString(i++, p.getName());
		
		// DESCRIPTION
		ps.setString(i++, p.getDescription());
		
		// PROPERTIES
		ps.setLong(i++, p.getProperties());
		
		// FACTURE_ID
		if(p.getToFacture() != null) {
			ps.setLong(i++, p.getToFacture().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYER_ID
		if(p.getPayer() != null) {
			if(p.getPayer().getDescriptor().getID() > 0) {
				ps.setLong(i++, p.getPayer().getDescriptor().getID());
			} else {
				ps.setNull(i++, java.sql.Types.BIGINT);
			}
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYMENT_TERMS_ID
		if(p.getPaymentTerms() != null) {
			ps.setLong(i++, p.getPaymentTerms().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYABLE_TYPE_ID
		if(p.getPayableType() != null) {
			ps.setLong(i++, p.getPayableType().getTypeId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYMENT_TYPE_ID
		if(p.getPaymentType() != null) {
			ps.setLong(i++, p.getPaymentType().getTypeId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(p.getDate()));
		
		// DUE_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(p.getDueDate()));
		
		// AMOUNT
		ps.setDouble(i++, p.getAmount());
		
		// PAID
		ps.setBoolean(i++, p.isPaid());
		
		// PAID_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(p.getPaidDate()));
		
		// return the current ps position 
		return i;
	}
	
	public void update(AEPayment payment) throws AEException {
		assert(payment != null);
		assert(payment.isPersistent());
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(payment, ps, 1);
			ps.setLong(i++, payment.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			payment.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updatePayableType(long payableTypeId, long paymentId) throws AEException {
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLPayableType);

			// build statement
			ps.setLong(1, payableTypeId);
			ps.setLong(2, paymentId);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateToPaid(Date paidDate, long paymentId) throws AEException {
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLToPaid);

			// build statement
			ps.setDate(1, AEPersistentUtil.getSQLDate(paidDate));
			ps.setLong(2, paymentId);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateToUnpaid(long paymentId) throws AEException {
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLToUnpaid);

			// build statement
			ps.setLong(1, paymentId);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPayment load(AEDescriptor paymentDescr) throws AEException {
		AEPayment payment = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, paymentDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				payment = new AEPayment();
				build(payment, rs);
				payment.setView();
			}
			return payment;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPayment loadFull(AEDescriptor paymentDescr) throws AEException {
		AEPayment payment = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFull);
			ps.setLong(1, paymentDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				payment = new AEPayment();
				build(payment, rs);
				payment.setView();
			}
			return payment;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public AEPaymentsList loadToFacture(AEDescriptor factDescr) throws AEException {
		AEPaymentsList pList = new AEPaymentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToFacture);
			ps.setLong(1, factDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPayment payment = new AEPayment();
				build(payment, rs);
				payment.setView();
				
				pList.add(payment);
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPayment loadAdvancePaymentToFacture(AEDescriptor factDescr) throws AEException {
		AEPayment payment = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAdvancePaymentToFacture);
			ps.setLong(1, factDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				payment = new AEPayment();
				build(payment, rs);
				payment.setView();
			}
			return payment;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(long id) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, id);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPaymentsList load(AEPaymentsFilter filter) throws AEException {
		AEPaymentsList results = new AEPaymentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Append filter
			 */
			String sql = selectSQLByFilter;
			StringBuffer sb = new StringBuffer(sql);
			
			// period
			if(filter.getDateFrom() != null) {
				sb
					.append(" and DATE >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateFrom()));
			};
			if(filter.getDateTo() != null) {
				sb
					.append(" and DATE <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateTo()));
			};
			
//			// state
//			if(filter.getState() != null && !AEFactureUtil.FactureState.NA.equals(filter.getState())) {
//				sb
//				.append(" and STATE = ")
//				.append(filter.getState().getStateId());
//			}
			
//			// companyName 
//			if(!AEStringUtil.isEmpty(filter.getCompanyName())) {
//				sb
//					.append(" and UPPER(COMPANY_NAME) like '%")
//				    .append(filter.getCompanyName().toUpperCase()).append("%' ");
//			}
//			
//			// number 
//			if(!AEStringUtil.isEmpty(filter.getNumber())) {
//				sb
//					.append(" and UPPER(NUMBER_STRING) like '%")
//				    .append(filter.getNumber().toUpperCase()).append("%' ");
//			}
//			
//			// description
//			if(!AEStringUtil.isEmpty(filter.getDescription())) {
//				sb
//					.append(" and UPPER(DESCRIPTION) like '%")
//				    .append(filter.getDescription().toUpperCase()).append("%' ");
//			}

			
			/**
			 * Append order
			 */
			sb.append(" order by DATE desc");
			
			/**
			 * Create statement
			 */
			ps = getAEConnection().prepareStatement(sb.toString());

			/**
			 * Append system filter
			 */
			ps.setLong(1, filter.getCompany().getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPayment res = new AEPayment();
				build(res, rs);
				res.setView();
				
				results.add(res);
			}
			return results;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPaymentsList loadAdvancedInvoicedToDevise(AEDescriptor devisDescr) throws AEException {
		AEPaymentsList pList = new AEPaymentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAdvancedInvoicedToDevis);
			ps.setLong(1, devisDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPayment payment = new AEPayment();
				build(payment, rs);
				payment.setView();
				
				pList.add(payment);
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void validateOwner(long paymentId, long ownerId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLValidateOwner);
			ps.setLong(1, paymentId);
			ps.setLong(2, ownerId);
			rs = ps.executeQuery();
			if(!rs.next()) {
				AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPaymentsList loadOverdueDebts(AEDescriptor ownerDescr) throws AEException {
		AEPaymentsList pList = new AEPaymentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLOverdueDebts);
			ps.setLong(1, ownerDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(new Date())));
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPayment payment = new AEPayment();
				build(payment, rs);
				payment.setView();
				
				pList.add(payment);
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public double loadOverdueDebtsSum(AEDescriptor ownerDescr, Date toDate) throws AEException {
		double amount = 0.0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLOverdueDebtsSum);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEFactureUtil.PayableType.REQUISITION.getTypeId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(toDate)));
			rs = ps.executeQuery();
			if(rs.next()) {
				amount = rs.getDouble("amount");
			}
			return amount;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPaymentsList loadMaturityDebts(AEDescriptor ownerDescr) throws AEException {
		AEPaymentsList pList = new AEPaymentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			Date dateNow = new Date();
			ps = getAEConnection().prepareStatement(selectSQLMaturityDebts);
			ps.setLong(1, ownerDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(AEDateUtil.addDaysToDate(dateNow, -5))));
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(AEDateUtil.addDaysToDate(dateNow, 5))));
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPayment payment = new AEPayment();
				build(payment, rs);
				payment.setView();
				
				pList.add(payment);
			}
			return pList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
