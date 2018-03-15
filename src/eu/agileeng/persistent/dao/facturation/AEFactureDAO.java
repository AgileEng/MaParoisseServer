package eu.agileeng.persistent.dao.facturation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.facturation.AEClient;
import eu.agileeng.domain.facturation.AEFacture;
import eu.agileeng.domain.facturation.AEFactureDescr;
import eu.agileeng.domain.facturation.AEFactureFilter;
import eu.agileeng.domain.facturation.AEFactureUtil;
import eu.agileeng.domain.facturation.AEFacturesList;
import eu.agileeng.domain.facturation.AEVatItem;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEStringUtil;

public class AEFactureDAO extends AbstractDAO {

	private static String insertSQL = 
		"insert into Fact_Facture "
		+ "(OWNER_ID, TYPE_ID, SUB_TYPE_ID, STATE, CODE, NAME, DESCRIPTION, PROPERTIES, DATE_OF_EXPIRY, "
		+ "CLIENT_ID, CLIENT_CODE, CLIENT_NAME, CLIENT_ACC_ID, CLIENT_ACC_AUXILIARY, CLIENT_ADDRESS, CLIENT_SEC_ADDRESS, "
		+ "CLIENT_POST_CODE, CLIENT_TOWN, CLIENT_VAT_NUMBER, PAYMENT_TERMS_ID, "
		+ "DISCOUNT_MODE, DISCOUNT_PERCENT, DISCOUNT_AMOUNT, TIME_CREATED, CREATOR, "
		+ "TIME_MODIFIED, MODIFIER, NOTE, NUMBER, NUMBER_STRING, REG_NUMBER, "
		+ "REG_NUMBER_STRING, DOC_DATE, REG_DATE, TEMPLATE, TEMPLATE_ID, "
		+ "JOURNAL_ID, JOURNAL_CODE, SHIPPING_TAXABLE_AMOUNT, SHIPPING_VAT_ID, "
		+ "SHIPPING_VAT_RATE, SHIPPING_VAT_AMOUNT, SHIPPING_AMOUNT, ITEMS_DISCOUNT_AMOUNT, "
		+ "BRUTO_ANOUNT, TAXABLE_AMOUNT, VAT_AMOUNT, AMOUNT, "
		+ "ADVANCE_MODE, ADVANCE_PERCENT, ADVANCE_AMOUNT, SRC_FACTURE_ID, PAID_AMOUNT) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, "
		+ "?, ?, ?, ?, "
		+ "?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update Fact_Facture set "
		+ "OWNER_ID = ?, TYPE_ID = ?, SUB_TYPE_ID = ?, STATE = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, DATE_OF_EXPIRY = ?, "
		+ "CLIENT_ID = ?, CLIENT_CODE = ?, CLIENT_NAME = ?, CLIENT_ACC_ID = ?, CLIENT_ACC_AUXILIARY = ?, CLIENT_ADDRESS = ?, CLIENT_SEC_ADDRESS = ?, "
		+ "CLIENT_POST_CODE = ?, CLIENT_TOWN = ?, CLIENT_VAT_NUMBER = ?, PAYMENT_TERMS_ID = ?, "
		+ "DISCOUNT_MODE = ?, DISCOUNT_PERCENT = ?, DISCOUNT_AMOUNT = ?, TIME_CREATED = ?, CREATOR = ?, "
		+ "TIME_MODIFIED = ?, MODIFIER = ?, NOTE = ?, NUMBER = ?, NUMBER_STRING = ?, REG_NUMBER = ?, "
		+ "REG_NUMBER_STRING = ?, DOC_DATE = ?, REG_DATE = ?, TEMPLATE = ?, TEMPLATE_ID = ?, "
		+ "JOURNAL_ID = ?, JOURNAL_CODE = ?, SHIPPING_TAXABLE_AMOUNT = ?, SHIPPING_VAT_ID = ?, "
		+ "SHIPPING_VAT_RATE = ?, SHIPPING_VAT_AMOUNT = ?, SHIPPING_AMOUNT = ?, ITEMS_DISCOUNT_AMOUNT = ?, "
		+ "BRUTO_ANOUNT = ?, TAXABLE_AMOUNT = ?, VAT_AMOUNT = ?, AMOUNT = ?, "
		+ "ADVANCE_MODE = ?, ADVANCE_PERCENT = ?, ADVANCE_AMOUNT = ?, SRC_FACTURE_ID = ?, PAID_AMOUNT = ? "
		+ " where ID = ?";
	
	private static String updateSQLState = "update Fact_Facture set STATE = ? where ID = ?";
	
	private static String selectSQL = 
		"select * from Fact_Facture where id = ?";
	
	private static String selectSQLAccepted = 
		"select COUNT(ID) as _count, SUM(AMOUNT) as _amount "
		+ " from Fact_Facture where OWNER_ID = ? and TYPE_ID = ? and state = ? and DOC_DATE between ? and ?";
	
	private static String selectSQLInvoicedRate = 
		"select "
		+ "	ROUND(CONVERT(numeric(18, 3), countInvoiced._count) / CONVERT(numeric(18, 3), countAll._count) * 100, 1) as rate from ( "
		+ "	select COUNT(ID) as _count "
		+ "		from Fact_Facture " 
		+ "	where " 
		+ "		OWNER_ID = ? " 
		+ "		and TYPE_ID = ? and " 
		+ "		state = ? " 
		+ "		and DOC_DATE between ? and ? "
		+ "	) as countInvoiced, ("
		+ "	select COUNT(ID) as _count "
		+ "		from Fact_Facture " 
		+ "	where " 
		+ "		OWNER_ID = ? " 
		+ "		and TYPE_ID = ?  "
		+ "		and DOC_DATE between ? and ? "
		+ "	) as countAll "
		+ "where countAll._count != 0 ";
	
	private static String selectSQLFinYearTurnover = 
		"select COUNT(ID) as _count, SUM(AMOUNT) as _amount "
		+ " from Fact_Facture where OWNER_ID = ? and TYPE_ID = ? "
		+ "and (state = ? or state = ?) and DOC_DATE between ? and ?";
	
	private static String selectSQLDevisByFilter = 
		"SELECT * from Fact_Facture "
	    + " where owner_id = ? and type_id = " + AEDocumentType.System.AEDevisSale.getSystemID() + " and template = 0 ";
	
	private static String selectSQLFactureByFilter = 
		"SELECT * from Fact_Facture "
	    + " where owner_id = ? and type_id = " + AEDocumentType.System.AEFactureSale.getSystemID() + " and template = 0 ";

	private static String selectSQLMostBilledCustomers = 
		"select top 10 "
		+ "f.CLIENT_NAME, "
		+ "f.OWNER_ID, " 
		+ "SUM(f.TAXABLE_AMOUNT) as taxableAmount, " 
		+ "SUM(f.VAT_AMOUNT) as vatAmount, " 
		+ "SUM(f.AMOUNT) as amount " 
		+ "from Fact_Facture f "
		+ "where f.OWNER_ID = ? and f.TYPE_ID = ? and template = 0 and (f.STATE = ? or f.STATE = ?) "
		+ "group by f.OWNER_ID, f.CLIENT_NAME order by amount desc";
	
	private static String selectSQLTrend = 
		"select facture.year, facture.month, factureAmount, devisAmount from "
		+ "(SELECT " 
		+ "		YEAR(f.DOC_DATE) AS 'year', " 
		+ "		MONTH(f.DOC_DATE) AS 'month', " 
		+ "		SUM(f.AMOUNT) AS 'factureAmount' "
		+ "	FROM Fact_Facture f "
		+ "	where " 
		+ "		f.OWNER_ID = ? " 
		+ "		and f.TYPE_ID = ? " 
		+ "		and f.DOC_DATE between ? and ? "
		+ "		and (f.STATE = ? or f.STATE = ?) "
		+ "	GROUP BY YEAR(f.DOC_DATE), MONTH(f.DOC_DATE) "
		+ ") facture full join " 
		+ "(SELECT " 
		+ "		YEAR(f.DOC_DATE) AS 'year', " 
		+ "		MONTH(f.DOC_DATE) AS 'month', " 
		+ "		SUM(f.AMOUNT) AS 'devisAmount' "
		+ "	FROM Fact_Facture f "
		+ "	where " 
		+ "		f.OWNER_ID = ? " 
		+ "		and f.TYPE_ID = ? " 
		+ "		and f.DOC_DATE between ? and ? "
		+ "		and (f.STATE = ? or f.STATE = ?) "
		+ "	GROUP BY YEAR(f.DOC_DATE), MONTH(f.DOC_DATE) "
		+ ") devis "
		+ "on facture.year = devis.year and facture.month = devis.month";
	
	private static String selectSQLValidateOwner = 
		"SELECT id from Fact_Facture where id = ? and owner_id = ?";
	
	private static String updateSQLPaidAmount1 = 
		"update Fact_Facture set PAID_AMOUNT = "
		+ " (select SUM(amount) from Fact_Payment where FACTURE_ID = ? and PAID = 1)"
		+ " where ID = ?";
	
	private static String updateSQLPaidAmount2 = 
		"IF EXISTS " 
		+ "(SELECT id FROM Fact_Facture WHERE "
		+ " ID = ? AND TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID() 
		+ " AND STATE = " + AEFactureUtil.FactureState.VALIDATED.getStateId() 
		+ " AND ABS(ISNULL(AMOUNT, 0.0) - ISNULL(PAID_AMOUNT, 0.0)) < 0.01)"
		+ " UPDATE Fact_Facture SET STATE = " + AEFactureUtil.FactureState.PAID.getStateId() +" where ID = ?";
	
	private static String updateSQLPaidAmount3 = 
		"IF EXISTS " 
		+ "(SELECT id FROM Fact_Facture WHERE "
		+ " ID = ? AND TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID() 
		+ " AND STATE = " + AEFactureUtil.FactureState.PAID.getStateId() 
		+ " AND ABS(ISNULL(AMOUNT, 0.0) - ISNULL(PAID_AMOUNT, 0.0)) >= 0.01)"
		+ " UPDATE Fact_Facture SET STATE = " + AEFactureUtil.FactureState.VALIDATED.getStateId() +" where ID = ?";
	
	private static String updateSQLInvoicedAmount1 = 
		"update Fact_Facture set PAID_AMOUNT = "
		+ " (select SUM(amount) from Fact_Payment where FACTURE_ID = ? and PAID = 1)"
		+ " where ID = ?";
	
	private static String updateSQLInvoicedAmount2 = 
		"IF EXISTS " 
		+ "(SELECT id FROM Fact_Facture WHERE "
		+ " ID = ? AND TYPE_ID = " + AEDocumentType.System.AEDevisSale.getSystemID() 
		+ " AND STATE = " + AEFactureUtil.FactureState.ACCEPTED.getStateId() 
		+ " AND ABS(ISNULL(AMOUNT, 0.0) - ISNULL(PAID_AMOUNT, 0.0)) < 0.01)"
		+ " UPDATE Fact_Facture SET STATE = " + AEFactureUtil.FactureState.INVOICED.getStateId() +" where ID = ?";
	
	private static String updateSQLInvoicedAmount3 = 
		"IF EXISTS " 
		+ "(SELECT id FROM Fact_Facture WHERE "
		+ " ID = ? AND TYPE_ID = " + AEDocumentType.System.AEDevisSale.getSystemID() 
		+ " AND STATE = " + AEFactureUtil.FactureState.INVOICED.getStateId() 
		+ " AND ABS(ISNULL(AMOUNT, 0.0) - ISNULL(PAID_AMOUNT, 0.0)) >= 0.01)"
		+ " UPDATE Fact_Facture SET STATE = " + AEFactureUtil.FactureState.ACCEPTED.getStateId() +" where ID = ?";
	private static String deleteSQL1 = 
		"update Fact_FactureItem set ITEM_DEDUCTED_ID = null, PAYMENT_INVOICED_ID = null "
		+ " where FACTURE_ID = ?";
	
	private static String deleteSQL2 = 
		"delete Fact_Facture where ID = ?";
	
	public AEFactureDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(AEFacture facture) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(facture, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				facture.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			facture.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private void build(AEFacture facture, ResultSet rs) throws SQLException, AEException {
		// ID
		facture.setID(rs.getLong("ID"));
		
		// OWNER_ID
		facture.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// TYPE_ID
		long typeId = rs.getLong("TYPE_ID");
		facture.setType(AEDocumentType.valueOf(typeId));
		
		// SUB_TYPE_ID
		long subTypeId = rs.getLong("SUB_TYPE_ID");
		facture.setSubType(AEFactureUtil.FactureSubType.valueOf(subTypeId));
		
		// STATE
		long stateId = rs.getLong("STATE");
		facture.setState(AEFactureUtil.FactureState.valueOf(stateId));
		
		// CODE
		facture.setCode(rs.getString("CODE"));
		
		// NAME
		facture.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		facture.setDescription(rs.getString("DESCRIPTION"));
		
		// PROPERTIES
		facture.setProperties(rs.getLong("PROPERTIES"));
		
		// DATE_OF_EXPIRY
		facture.setDateOfExpiry(rs.getDate("DATE_OF_EXPIRY"));
		
		/**
		 * Client
		 */
		AEClient client = facture.grantClient();
		
		// CLIENT_ID
		client.setID(rs.getLong("CLIENT_ID"));
		
		// CLIENT_CODE
		client.setCode(rs.getString("CLIENT_CODE"));
		
		// CLIENT_NAME
		client.setName(rs.getString("CLIENT_NAME"));

		// CLIENT_ACC_ID
		long clientAccId = rs.getLong("CLIENT_ACC_ID");
		if(!rs.wasNull()) {
			client.setAccount(AccAccount.lazyDescriptor(clientAccId));
		}
		
		// CLIENT_ACC_AUXILIARY
		client.setAccountAuxiliary(rs.getString("CLIENT_ACC_AUXILIARY"));
		
		// CLIENT_ADDRESS, CLIENT_SEC_ADDRESS, CLIENT_POST_CODE, CLIENT_TOWN
		Address clientAddress = client.grantAddress();
		clientAddress.setStreet(rs.getString("CLIENT_ADDRESS"));
		clientAddress.setSecondaryStreet(rs.getString("CLIENT_SEC_ADDRESS"));
		clientAddress.setPostalCode(rs.getString("CLIENT_POST_CODE"));
		clientAddress.setCity(rs.getString("CLIENT_TOWN"));
		
		// CLIENT_VAT_NUMBER
		client.setVatNumber(rs.getString("CLIENT_VAT_NUMBER"));
		
		// PAYMENT_TERMS_ID
		long paymentTermsId = rs.getLong("PAYMENT_TERMS_ID");
		if(!rs.wasNull()) {
			try {
				facture.setPaymentTerms(AEFactureUtil.getPaymentTerms(paymentTermsId));
			} catch (Exception e) {
			}
		}
		
		// DISCOUNT_MODE, DISCOUNT_PERCENT, DISCOUNT_AMOUNT
		long advanceModeId = rs.getLong("DISCOUNT_MODE");
		facture.setDiscountMode(AEFactureUtil.PercentAmountMode.valueOf(advanceModeId));
		facture.setDiscountPercent(rs.getDouble("DISCOUNT_PERCENT"));
		facture.setDiscountAmount(rs.getDouble("DISCOUNT_AMOUNT"));
		
		// TIME_CREATED, CREATOR, TIME_MODIFIED, MODIFIER
	    facture.setTimeCreated(rs.getTimestamp("TIME_CREATED"));
	    facture.setCreator(rs.getString("CREATOR"));
	    facture.setTimeModified(rs.getTimestamp("TIME_MODIFIED"));
	    facture.setModifier(rs.getString("MODIFIER"));
		
		// NOTE
	    facture.setNote(rs.getString("NOTE"));
		
		// NUMBER, NUMBER_STRING, REG_NUMBER, REG_NUMBER_STRING
		facture.setNumber(rs.getLong("NUMBER"));
		String number = rs.getString("NUMBER_STRING");
		if(rs.wasNull()) {
			facture.setNumberString(AEStringUtil.EMPTY_STRING);
		} else {
			facture.setNumberString(number);
		}
		facture.setRegNumber(rs.getLong("REG_NUMBER"));
		facture.setRegNumberString(rs.getString("REG_NUMBER_STRING"));
		
		// DOC_DATE, REG_DATE
		facture.setDate(rs.getDate("DOC_DATE"));
		facture.setRegDate(rs.getDate("REG_DATE"));
		
		// TEMPLATE, TEMPLATE_ID
		facture.setIsTemplate(rs.getBoolean("TEMPLATE"));
		long templateId = rs.getLong("TEMPLATE_ID");
		if(!rs.wasNull()) {
			facture.setTemplate(new AEDescriptorImp(templateId));
		}
		
		// JOURNAL_ID, JOURNAL_CODE
		AEDescriptorImp journal = new AEDescriptorImp();
		journal.setID(rs.getLong("JOURNAL_ID"));
		journal.setCode(rs.getString("JOURNAL_CODE"));
		facture.setJournal(journal);
		
		// SHIPPING_TAXABLE_AMOUNT, SHIPPING_VAT_ID, SHIPPING_VAT_RATE, SHIPPING_VAT_AMOUNT, SHIPPING_AMOUNT
		AEVatItem shipping = facture.grantShipping();
		shipping.setTaxableAmount(rs.getDouble("SHIPPING_TAXABLE_AMOUNT"));
		shipping.setID(rs.getLong("SHIPPING_VAT_ID"));
		shipping.setRate(rs.getDouble("SHIPPING_VAT_RATE"));
		shipping.setVatAmount(rs.getDouble("SHIPPING_VAT_AMOUNT"));
		shipping.setAmount(rs.getDouble("SHIPPING_AMOUNT"));
		
		// ITEMS_DISCOUNT_AMOUNT
		facture.setItemsDiscountAmount(rs.getDouble("ITEMS_DISCOUNT_AMOUNT"));
		
		// BRUTO_ANOUNT
		facture.setBrutoAmount(rs.getDouble("BRUTO_ANOUNT"));
		
		// TAXABLE_AMOUNT
		facture.setTaxableAmount(rs.getDouble("TAXABLE_AMOUNT"));
		
		// VAT_AMOUNT
		facture.setVatAmount(rs.getDouble("VAT_AMOUNT"));
		
		// AMOUNT
		facture.setAmount(rs.getDouble("AMOUNT"));
		
		// ADVANCE_MODE
		facture.setAdvanceMode(AEFactureUtil.PercentAmountMode.valueOf(rs.getLong("ADVANCE_MODE")));

		// ADVANCE_PERCENT
		facture.setAdvancePercent(rs.getDouble("ADVANCE_PERCENT"));
		
		// ADVANCE_AMOUNT
		facture.setAdvanceAmount(rs.getDouble("ADVANCE_AMOUNT"));
		
		// SRC_FACTURE_ID
		long srcDocId = rs.getLong("SRC_FACTURE_ID");
		if(srcDocId > 0) {
			facture.setSrcDoc(AEFacture.lazyDescriptor(srcDocId));
		}
		
		// PAID_AMOUNT
		facture.setPaidAmount(rs.getDouble("PAID_AMOUNT"));
	}
	
	private int build(AEFacture f, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(f.getCompany() != null) {
			ps.setLong(i++, f.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// TYPE_ID
		ps.setLong(i++, f.getType().getSystemID());
		
		// SUB_TYPE_ID
		ps.setLong(i++, f.getSubType().getSubTypeId());
		
		// STATE
		ps.setLong(i++, f.getState().getStateId());
		
		// CODE
		ps.setString(i++, f.getCode());
		
		// NAME
		ps.setString(i++, f.getName());
		
		// DESCRIPTION
		ps.setString(i++, f.getDescription());
		
		// PROPERTIES
		ps.setLong(i++, f.getProperties());
		
		// DATE_OF_EXPIRY
		ps.setDate(i++, AEPersistentUtil.getSQLDate(f.getDateOfExpiry()));
		
		// CLIENT_ID
		if(f.getClient() != null) {
			ps.setLong(i++, f.getClient().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// CLIENT_CODE
		if(f.getClient() != null) {
			ps.setString(i++, f.getClient().getCode());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// CLIENT_NAME
		if(f.getClient() != null) {
			ps.setString(i++, f.getClient().getName());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// CLIENT_ACC_ID
		if(f.getClient() != null && f.getClient().getAccount() != null) {
			ps.setLong(i++, f.getClient().getAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// CLIENT_ACC_AUXILIARY
		if(f.getClient() != null) {
			ps.setString(i++, f.getClient().getAccountAuxiliary());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// CLIENT_ADDRESS, CLIENT_SEC_ADDRESS, CLIENT_POST_CODE, CLIENT_TOWN
		if(f.getClient() != null) {
			Address address = f.getClient().grantAddress();
			ps.setString(i++, address.getStreet());
			ps.setString(i++, address.getSecondaryStreet());
			ps.setString(i++, address.getPostalCode());
			ps.setString(i++, address.getCity());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
			ps.setNull(i++, Types.NVARCHAR);
			ps.setNull(i++, Types.NVARCHAR);
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// CLIENT_VAT_NUMBER
		if(f.getClient() != null) {
			ps.setString(i++, f.getClient().getVatNumber());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// PAYMENT_TERMS_ID
		if(f.getPaymentTerms() != null) {
			ps.setLong(i++, f.getPaymentTerms().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// DISCOUNT_MODE, DISCOUNT_PERCENT, DISCOUNT_AMOUNT
		ps.setLong(i++, f.getDiscountMode().getModeId());
		ps.setDouble(i++, f.getDiscountPercent());
		ps.setDouble(i++, f.getDiscountAmount());
		
		// TIME_CREATED, CREATOR, TIME_MODIFIED, MODIFIER
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(f.getTimeCreated()));
		ps.setString(i++, f.getCreator());
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(f.getTimeModified()));
		ps.setString(i++, f.getModifier());
		
		// NOTE
		ps.setString(i++, f.getNote());
		
		// NUMBER, NUMBER_STRING, REG_NUMBER, REG_NUMBER_STRING
		ps.setLong(i++, f.getNumber());
		ps.setString(i++, f.getNumberString());
		ps.setLong(i++, f.getRegNumber());
		ps.setString(i++, f.getRegNumberString());
		
		// DOC_DATE, REG_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(f.getDate()));
		ps.setDate(i++, AEPersistentUtil.getSQLDate(f.getRegDate()));
		
		// TEMPLATE, TEMPLATE_ID
		ps.setBoolean(i++, f.isTemplate());
		if(f.getTemplate() != null) {
			ps.setLong(i++, f.getTemplate().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// JOURNAL_ID, JOURNAL_CODE
		if(f.getJournal() != null) {
			ps.setLong(i++, f.getJournal().getDescriptor().getID());
			ps.setString(i++, f.getJournal().getDescriptor().getCode());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		
		// SHIPPING_TAXABLE_AMOUNT, SHIPPING_VAT_ID, SHIPPING_VAT_RATE, SHIPPING_VAT_AMOUNT, SHIPPING_AMOUNT
		if(f.getShipping() != null) {
			ps.setDouble(i++, f.getShipping().getTaxableAmount());
			ps.setLong(i++, f.getShipping().getID());
			ps.setDouble(i++, f.getShipping().getRate());
			ps.setDouble(i++, f.getShipping().getVatAmount());
			ps.setDouble(i++, f.getShipping().getAmount());
		} else {
			ps.setNull(i++, Types.NUMERIC);
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NUMERIC);
			ps.setNull(i++, Types.NUMERIC);
			ps.setNull(i++, Types.NUMERIC);
		}
		
		// ITEMS_DISCOUNT_AMOUNT
		ps.setDouble(i++, f.getItemsDiscountAmount());
		
		// BRUTO_ANOUNT
		ps.setDouble(i++, f.getBrutoAmount());
		
		// TAXABLE_AMOUNT
		ps.setDouble(i++, f.getTaxableAmount());
		
		// VAT_AMOUNT
		ps.setDouble(i++, f.getVatAmount());
		
		// AMOUNT
		ps.setDouble(i++, f.getAmount());
		
		// ADVANCE_MODE
		ps.setLong(i++, f.getAdvanceMode().getModeId());

		// ADVANCE_PERCENT
		ps.setDouble(i++, f.getAdvancePercent());
		
		// ADVANCE_AMOUNT
		ps.setDouble(i++, f.getAdvanceAmount());
		
		// SRC_FACTURE_ID
		if(f.getSrcDoc() != null && f.getSrcDoc().getDescriptor().getID() > 0) {
			ps.setLong(i++, f.getSrcDoc().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// PAID_AMOUNT
		ps.setDouble(i++, f.getPaidAmount());
		
		// return the current ps position 
		return i;
	}
	
	public void update(AEFacture facture) throws AEException {
		assert(facture != null);
		assert(facture.isPersistent());
		assert facture instanceof AEFacture : "doc instanceof AEFacture failed";
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(facture, ps, 1);
			ps.setLong(i++, facture.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			facture.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateState(long stateId, long factureId) throws AEException {
		PreparedStatement ps = null;
		try {
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLState);

			// build statement
			ps.setLong(1, stateId);
			ps.setLong(2, factureId);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFacture load(AEDescriptor factureDescr) throws AEException {
		AEFacture facture = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, factureDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				facture = new AEFacture(AEDocumentType.valueOf(AEDocumentType.System.AEDevisSale));
				build(facture, rs);
				facture.setView();
			}
			return facture;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFacturesList load(AEFactureFilter filter) throws AEException {
		AEFacturesList results = new AEFacturesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Append filter
			 */
			String sql = null;
			if(AEMath.longValue(filter.getDocType()) == AEDocumentType.System.AEDevisSale.getSystemID()) {
				sql = selectSQLDevisByFilter;
			} else if((AEMath.longValue(filter.getDocType()) == AEDocumentType.System.AEFactureSale.getSystemID())) {
				sql = selectSQLFactureByFilter;
			} else {
				throw new AEException("System error: The doc type is unknown");
			}
			
			StringBuffer sb = new StringBuffer(sql);
			
			// period
			if(filter.getDocDateFrom() != null) {
				sb
					.append(" and DOC_DATE >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDocDateFrom()));
			};
			if(filter.getDocDateTo() != null) {
				sb
					.append(" and DOC_DATE <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDocDateTo()));
			};
			
			// state
			if(filter.getState() != null && !AEFactureUtil.FactureState.NA.equals(filter.getState())) {
				sb
				.append(" and STATE = ")
				.append(filter.getState().getStateId());
			}
			
			// subType
			if(filter.getSubType() != null && !AEFactureUtil.FactureSubType.NA.equals(filter.getSubType())) {
				sb
				.append(" and SUB_TYPE_ID = ")
				.append(filter.getSubType().getSubTypeId());
			}
			
			// companyName 
			if(!AEStringUtil.isEmpty(filter.getCompanyName())) {
				sb
					.append(" and UPPER(COMPANY_NAME) like '%")
				    .append(filter.getCompanyName().toUpperCase()).append("%' ");
			}
			
			// number 
			if(!AEStringUtil.isEmpty(filter.getNumber())) {
				sb
					.append(" and UPPER(NUMBER_STRING) like '%")
				    .append(filter.getNumber().toUpperCase()).append("%' ");
			}
			
			// description
			if(!AEStringUtil.isEmpty(filter.getDescription())) {
				sb
					.append(" and UPPER(DESCRIPTION) like '%")
				    .append(filter.getDescription().toUpperCase()).append("%' ");
			}

			
			/**
			 * Append order
			 */
			sb.append(" order by DOC_DATE desc");
			
			/**
			 * Create statement
			 */
			ps = getAEConnection().prepareStatement(sb.toString());

			/**
			 * Append system filter
			 */
			ps.setLong(1, filter.getOwnerId());
//			ps.setLong(2, filter.getDocType());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFacture res = new AEFacture();
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
	
	public long getNextNumber(AEFactureUtil.ReceiptBook rb, AEDescriptor ownerDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long lastNumber = 0;
			String sql;
			switch(rb) {
			case DEVIS: 
				sql = "select max(number) from Fact_Facture "
					+ " where TYPE_ID = " + AEDocumentType.System.AEDevisSale.getSystemID() + " and OWNER_ID = ?";
				break;
			case FACTURE: 
				sql = "select max(number) from Fact_Facture "
					+ " where TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID()
					+ " and (SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.REGULAR.getSubTypeId() + " or SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.ADVANCE.getSubTypeId() + ")"
					+ " and (STATE = " + AEFactureUtil.FactureState.VALIDATED.getStateId() + " or STATE = " + AEFactureUtil.FactureState.PAID.getStateId() + ")"
					+ " and OWNER_ID = ?";
				break;
			case AVOIR: 
				sql = "select max(number) from Fact_Facture "
					+ " where TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID()
					+ " and SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.CREDIT_NOTE.getSubTypeId()
					+ " and OWNER_ID = ?";
				break;
			case BROUILLON: 
				sql = "select max(number) from Fact_Facture "
					+ " where TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID()
					+ " and SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.REGULAR.getSubTypeId()
					+ " and STATE = " + AEFactureUtil.FactureState.DRAFT.getStateId()
					+ " and OWNER_ID = ?";
				break;
			default: 
				sql = AEStringUtil.EMPTY_STRING;
				break;
			}
			
			// prepare statement
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerDescr.getID());

			// set generated key
			rs = ps.executeQuery();
			if (rs.next()) {
				lastNumber = rs.getLong(1);
			} else {
				throw new AEException("System error: Cannot get next number");
			}

			// return next number
			return (lastNumber + 1);
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public long getCurrentNumber(AEDescriptor factDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long currentNumber = 0;
			
			// prepare statement
			ps = getAEConnection().prepareStatement("select number from Fact_Facture where ID = ?");
			ps.setLong(1, factDescr.getID());
			rs = ps.executeQuery();
			if (rs.next()) {
				currentNumber = rs.getLong(1);
			} else {
				throw new AEException("System error: Cannot get current number");
			}

			// return current number
			return currentNumber;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void validateOwner(long factureId, long ownerId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLValidateOwner);
			ps.setLong(1, factureId);
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
	
	public void updatePaidAmount(long factureId) throws AEException {
		PreparedStatement ps = null;
		try {
			getAEConnection().beginTransaction();
			
			ps = getAEConnection().prepareStatement(updateSQLPaidAmount1);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			ps = getAEConnection().prepareStatement(updateSQLPaidAmount2);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			ps = getAEConnection().prepareStatement(updateSQLPaidAmount3);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			getAEConnection().commit();
			
		} catch (SQLException e) {
			getAEConnection().rollback();
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateInvoicedAmount(long factureId) throws AEException {
		PreparedStatement ps = null;
		try {
			getAEConnection().beginTransaction();
			
			ps = getAEConnection().prepareStatement(updateSQLInvoicedAmount1);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			ps = getAEConnection().prepareStatement(updateSQLInvoicedAmount2);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			ps = getAEConnection().prepareStatement(updateSQLInvoicedAmount3);
			ps.setLong(1, factureId);
			ps.setLong(2, factureId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			getAEConnection().commit();
			
		} catch (SQLException e) {
			getAEConnection().rollback();
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(AEDescriptor facture) throws AEException {
		PreparedStatement ps = null;
		try {
			
			// stage 1
			ps = getAEConnection().prepareStatement(deleteSQL1);
			ps.setLong(1, facture.getID());
			ps.executeUpdate();
			ps.close();

			// stage 2
			ps = getAEConnection().prepareStatement(deleteSQL2);
			ps.setLong(1, facture.getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	/**
	 * 
	 * 
	 * @param facture
	 * @throws AEException
	 */
	public void deleteInvoicedDevisValidator(AEFacture facture) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			if(AEDocumentType.System.AEDevisSale.getSystemID() == facture.getType().getSystemID()) {
				final String sql = "select id from fact_facture where SRC_FACTURE_ID = ?";
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, facture.getID());
				rs = ps.executeQuery();
				if(rs.next()) {
					throw AEError.System.FACTURATION_INVOICED_DEVIS_CANNOT_BE_DELETED.toException();
				}
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public AEDescriptor loadWhereItemIsDeducted(AEDescriptor itemDescr) throws AEException {
		AEFactureDescr factureDescr = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			final String sql = "select facture.id from Fact_FactureItem item inner join Fact_Facture facture" 
				+ " on item.FACTURE_ID = facture.ID where item.ITEM_DEDUCTED_ID = ? ";
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, itemDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				factureDescr = new AEFactureDescr();
				factureDescr.setID(rs.getLong("id"));
			}
			return factureDescr;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadMostBilledCustomers(AEDescriptor ownerDescr) throws AEException {
		JSONArray results = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLMostBilledCustomers);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEDocumentType.System.AEFactureSale.getSystemID());
			ps.setLong(3, AEFactureUtil.FactureState.VALIDATED.getStateId());
			ps.setLong(4, AEFactureUtil.FactureState.PAID.getStateId());
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject res = new JSONObject();
				
				res.put("clientName", rs.getString("CLIENT_NAME"));
				res.put("ownerId", rs.getString("OWNER_ID"));
				res.put("taxableAmount", AEMath.toAmountString(rs.getDouble("taxableAmount")));
				res.put("vatAmount", AEMath.toAmountString(rs.getDouble("vatAmount")));
				res.put("amount", AEMath.toAmountString(rs.getDouble("amount")));
				
				results.put(res);
			}
			return results;
		} catch (SQLException e) {
			throw new AEException(e);
		}  catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONArray loadTrend(AEDescriptor ownerDescr) throws AEException {
		JSONArray results = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTrend);
			
			Date dateNow = AEDateUtil.getClearDate(new Date());

			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEDocumentType.System.AEFactureSale.getSystemID());
			ps.setDate(3, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(AEDateUtil.addMonthsToDate(dateNow, -6))));
			ps.setDate(4, AEPersistentUtil.getSQLDate(dateNow));
			ps.setLong(5, AEFactureUtil.FactureState.VALIDATED.getStateId());
			ps.setLong(6, AEFactureUtil.FactureState.PAID.getStateId());
			
			ps.setLong(7, ownerDescr.getID());
			ps.setLong(8, AEDocumentType.System.AEDevisSale.getSystemID());
			ps.setDate(9, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(AEDateUtil.addMonthsToDate(dateNow, -6))));
			ps.setDate(10, AEPersistentUtil.getSQLDate(dateNow));
			ps.setLong(11, AEFactureUtil.FactureState.ACCEPTED.getStateId());
			ps.setLong(12, AEFactureUtil.FactureState.INVOICED.getStateId());
			
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject res = new JSONObject();
				
				res.put("year", rs.getString("year"));
				res.put("month", rs.getString("month"));
				res.put("factureAmount", rs.getDouble("factureAmount"));
				res.put("devisAmount", rs.getDouble("devisAmount"));
				
				results.put(res);
			}
			return results;
		} catch (SQLException e) {
			throw new AEException(e);
		}  catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONObject loadAccepted(AEDescriptor ownerDescr, Date startDate, Date endDate) throws AEException {
		JSONObject result = new JSONObject();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAccepted);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEDocumentType.System.AEDevisSale.getSystemID());
			ps.setLong(3, AEFactureUtil.FactureState.ACCEPTED.getStateId());
			ps.setDate(4, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(5, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			rs = ps.executeQuery();
			if(rs.next()) {
				result.put("count", rs.getLong("_count"));
				result.put("amount", rs.getDouble("_amount"));
			}
			return result;
		} catch (SQLException e) {
			throw new AEException(e);
		}  catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public double loadInvoicedRate(AEDescriptor ownerDescr, Date startDate, Date endDate) throws AEException {
		double rate = 0.0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLInvoicedRate);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEDocumentType.System.AEDevisSale.getSystemID());
			ps.setLong(3, AEFactureUtil.FactureState.INVOICED.getStateId());
			ps.setDate(4, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(5, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			ps.setLong(6, ownerDescr.getID());
			ps.setLong(7, AEDocumentType.System.AEDevisSale.getSystemID());
			ps.setDate(8, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(9, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			rs = ps.executeQuery();
			if(rs.next()) {
				rate = rs.getDouble("rate");
			}
			return rate;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public JSONObject loadFinYearTurnover(AEDescriptor ownerDescr, Date startDate, Date endDate) throws AEException {
		JSONObject result = new JSONObject();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLFinYearTurnover);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, AEDocumentType.System.AEFactureSale.getSystemID());
			ps.setLong(3, AEFactureUtil.FactureState.VALIDATED.getStateId());
			ps.setLong(4, AEFactureUtil.FactureState.PAID.getStateId());
			ps.setDate(5, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(startDate)));
			ps.setDate(6, AEPersistentUtil.getSQLDate(AEDateUtil.getClearDate(endDate)));
			rs = ps.executeQuery();
			if(rs.next()) {
				long count = rs.getLong("_count");
				double amount = rs.getDouble("_amount");
				result.put("count", count);
				result.put("amount", amount);
				if(count != 0) {
					result.put("average", AEMath.toAmountString(amount / count));
				} else {
					result.put("average", AEMath.toAmountString(0.0));
				}
			}
			return result;
		} catch (SQLException e) {
			throw new AEException(e);
		}  catch (JSONException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
