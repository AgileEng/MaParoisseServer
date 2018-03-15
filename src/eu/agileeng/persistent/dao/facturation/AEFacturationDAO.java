package eu.agileeng.persistent.dao.facturation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.business.AEPaymentTerms;
import eu.agileeng.domain.business.AEPaymentTermsList;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.contact.Person;
import eu.agileeng.domain.contact.PersonDescr;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.facturation.AEArticle;
import eu.agileeng.domain.facturation.AEArticlesList;
import eu.agileeng.domain.facturation.AEAttribute;
import eu.agileeng.domain.facturation.AEAttributeValue;
import eu.agileeng.domain.facturation.AEClient;
import eu.agileeng.domain.facturation.AEClientsList;
import eu.agileeng.domain.facturation.AEFacturePrintTemplate;
import eu.agileeng.domain.facturation.AEFacturePrintTemplatesList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEMath;
import eu.agileeng.util.AEValue;

public class AEFacturationDAO extends AbstractDAO {

	/**
	 * Article
	 */
	private static String selectSQLArticles = 
		"select * from Fact_Article where OWNER_ID = ?";
	
	private static String insertSQLArticle = 
		"insert into Fact_Article "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION, PROPERTIES, GROUP_ID, GROUP_NAME, "
		+ "UOM_ID, UOM_CODE, ACCOUNT_ID, VAT_ID, VAT_CODE, VAT_RATE, PRICE_EX_VAT, PRICE_IN_VAT) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLArticle = 
		"update Fact_Article set "
		+ " OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, GROUP_ID = ?, GROUP_NAME = ?, "
		+ "UOM_ID = ?, UOM_CODE = ?, ACCOUNT_ID = ?, VAT_ID = ?, VAT_CODE = ?, VAT_RATE = ?, "
		+ "PRICE_EX_VAT = ?, PRICE_IN_VAT = ? "
		+ " where id = ?";
	
	private static String updateSQLArticleNumber = "update Fact_Article set code_number = ? where id = ?";
	
	private static String deleteSQLArticle = 
		"delete from Fact_Article where id = ?";
	
	/**
	 * Client 
	 */
	private static String selectSQLClients = 
		"select * from Fact_Client where OWNER_ID = ?";
	
	private static String insertSQLClient = 
		"insert into Fact_Client "
		+ "(OWNER_ID, CODE,	NAME, DESCRIPTION, PROPERTIES, GROUP_ID, GROUP_NAME, ADDRESS, SEC_ADDRESS, "
		+ " POST_CODE, TOWN, PHONE, MOBILE, FAX, EMAIL, HOMEPAGE, VAT_NUMBER, PAYMENT_TERMS_ID, "
		+ " ACCOUNT_ID, ACCOUNT_AUXILIARY, DISCOUNT_RATE, PERSON_SALUTATION_ID, PERSON_NAME, PERSON_POSITION) "
		+ " values (?, ?,	?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLClient = 
		"update Fact_Client set "
		+ " OWNER_ID = ?, CODE = ?,	NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, GROUP_ID = ?, GROUP_NAME = ?, "
		+ " ADDRESS = ?, SEC_ADDRESS = ?, "
		+ " POST_CODE = ?, TOWN = ?, PHONE = ?, MOBILE = ?, FAX = ?, EMAIL = ?, HOMEPAGE = ?, VAT_NUMBER = ?, "
		+ " PAYMENT_TERMS_ID = ?, "
		+ " ACCOUNT_ID = ?, ACCOUNT_AUXILIARY = ?, DISCOUNT_RATE = ?, PERSON_SALUTATION_ID = ?, "
		+ " PERSON_NAME = ?, PERSON_POSITION = ? "
		+ " where id = ?";
	
	private static String updateSQLClientNumber = "update Fact_Client set code_number = ? where id = ?";
	
	private static String deleteSQLClient = 
		"delete from Fact_Client where id = ?";
	
	/**
	 * PrintTemplates 
	 */
	private static String selectSQLTemplate = 
		"select * from Fact_PrintTemplate where ID = ?";
	
	private static String selectSQLTemplates = 
		"select * from Fact_PrintTemplate where OWNER_ID = ?";
	
	private static String insertSQLTemplate = 
		"insert into Fact_PrintTemplate "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION, PROPERTIES, BODY, ISSUER, CLIENT, "
		+ " BY_DEFAULT, FOR_REMINDER, FOR_DEVIS, FOR_FACTURE) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLTemplate = 
		"update Fact_PrintTemplate set "
		+ " OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, BODY = ?, ISSUER = ?, CLIENT = ?, "
		+ " BY_DEFAULT = ?, FOR_REMINDER = ?, FOR_DEVIS = ?, FOR_FACTURE = ?"
		+ " where id = ?";
	
	private static String deleteSQLTemplate = 
		"delete from Fact_PrintTemplate where id = ?";
	
	/**
	 * Payment term templates
	 */
	private static String insertSQLPaymentTerm = 
			"insert into PaymentTerms " +
			"(OWNER_ID, CODE, NAME, DESCRIPTION, PROPERTIES, BY_DECADE, DELAY, IS_DEFAULT, IS_END_OF_MONTH, " +
			"DAY_OF_MONTH, USE_FINANCIAL_MONTH, PAY_TYPE_ID) " +
			"values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String loadPaymentTermTemplates = "select * from PaymentTerms where OWNER_ID = ?";
	
	private static String selectSQLById = "select * from PaymentTerms where id = ?";
	
	private static String updateSQLPaymentTerm = 
		"update PaymentTerms set " +
		"OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, BY_DECADE = ?, DELAY = ?, " +
		"IS_DEFAULT = ?, IS_END_OF_MONTH = ?, DAY_OF_MONTH = ?, USE_FINANCIAL_MONTH = ?, PAY_TYPE_ID = ? " +
		"where ID = ?";
	
	private static String deleteSQLPaymentTerm = "delete from PaymentTerms where id = ?";

	private final String selectSQLClientAttrValuesByOwner = 
		"select * from Fact_ClientAttrValue where OWNER_ID = ?";
	
	public AEFacturationDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public AEArticlesList loadArticlesList(AEDescriptor ownerDescr) throws AEException {
		AEArticlesList articles = new AEArticlesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLArticles);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEArticle art = new AEArticle();
				build(art, rs);
				art.setView();
				articles.add(art);
			}
			return articles;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public void insert(AEArticle art) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQLArticle);
			build(art, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				art.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			art.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEArticle art) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLArticle);

			// build statement
			int i = build(art, ps, 1);
			ps.setLong(i, art.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			art.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(AEArticle article, ResultSet rs) throws SQLException, AEException {
		// ID
		article.setID(rs.getLong("ID"));
		// OWNER_ID
		article.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// CODE
		article.setCode(rs.getString("CODE"));
		// NAME
		article.setName(rs.getString("NAME"));
		// DESCRIPTION
		article.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		article.setProperties(rs.getLong("PROPERTIES"));
		// GROUP_ID, GROUP_NAME
		Long groupId = rs.getLong("GROUP_ID");
		if(rs.wasNull()) {
			groupId = null;
		}
		String groupName = rs.getString("GROUP_NAME");
		if(rs.wasNull()) {
			groupName = null;
		}
		if(groupId != null || groupName != null) {
			AEDescriptor group = new AEDescriptorImp(AEMath.longValue(groupId));
			group.setName(groupName);
			article.setGroup(group);
		}
		// UOM_ID, UOM_CODE
		Long uomId = rs.getLong("UOM_ID");
		if(rs.wasNull()) {
			uomId = null;
		}
		String uomCode = rs.getString("UOM_CODE");
		if(rs.wasNull()) {
			uomCode = null;
		}
		if(uomId != null || uomCode != null) {
			AEDescriptor uom = new AEDescriptorImp(AEMath.longValue(uomId));
			uom.setCode(uomCode);
			article.setUom(uom);
		}
		// ACCOUNT_ID
		long accountId = rs.getLong("ACCOUNT_ID");
		if(!rs.wasNull()) {
			article.setAccount(AccAccount.lazyDescriptor(accountId));
		}
		// VAT_ID, VAT_CODE
		Long vatId = rs.getLong("VAT_ID");
		if(rs.wasNull()) {
			vatId = null;
		}
		String vatCode = rs.getString("VAT_CODE");
		if(rs.wasNull()) {
			vatCode = null;
		}
		if(vatId != null || vatCode != null) {
			AEDescriptor vat = new AEDescriptorImp(AEMath.longValue(vatId));
			vat.setCode(vatCode);
			article.setVatDescriptor(vat);
		}
		// VAT_RATE
		article.setVatRate(rs.getDouble("VAT_RATE"));
		
		// PRICE_EX_VAT
		article.setPriceExVat(rs.getDouble("PRICE_EX_VAT"));
		
		// PRICE_IN_VAT
		article.setPriceInVat(rs.getDouble("PRICE_IN_VAT"));
	}
	
	public int build(AEArticle article, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(article.getCompany() != null) {
			ps.setLong(i++, article.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, article.getCode());
		// NAME
		ps.setString(i++, article.getName());
		// DESCRIPTION
		ps.setString(i++, article.getDescription());
		// PROPERTIES
		ps.setLong(i++, article.getProperties());
		// GROUP_ID, GROUP_NAME
		if(article.getGroup() != null) {
			ps.setLong(i++, article.getGroup().getDescriptor().getID());
			ps.setString(i++, article.getGroup().getDescriptor().getName());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// UOM_ID, UOM_CODE
		if(article.getUom() != null) {
			ps.setLong(i++, article.getUom().getDescriptor().getID());
			ps.setString(i++, article.getUom().getDescriptor().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// ACCOUNT_ID
		if(article.getAccount() != null) {
			ps.setLong(i++, article.getAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// VAT_ID, VAT_CODE
		if(article.getVatDescriptor() != null) {
			ps.setLong(i++, article.getVatDescriptor().getDescriptor().getID());
			ps.setString(i++, article.getVatDescriptor().getDescriptor().getCode());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		// VAT_RATE
		ps.setDouble(i++, article.getVatRate());
		// PRICE_EX_VAT
		ps.setDouble(i++, article.getPriceExVat());
		// PRICE_IN_VAT
		ps.setDouble(i++, article.getPriceInVat());
		
		// return the current ps position 
		return i;
	}
	
	public void deleteArticle(AEDescriptor artDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLArticle);

			// build statement
			ps.setLong(1, artDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public int build(AEPaymentTerms pt, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(pt.getCompany() != null) {
			ps.setLong(i++, pt.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, pt.getCode());
		// NAME
		ps.setString(i++, pt.getName());
		// DESCRIPTION
		ps.setString(i++, pt.getDescription());
		// PROPERTIES
		ps.setLong(i++, pt.getProperties());
		//	BY_DECADE
		ps.setLong(i++, pt.isByDecade() ? 1 : 0);
		//	DELAY
		ps.setLong(i++, pt.getDelay());
		//	IS_DEFAULT
		ps.setLong(i++, pt.isDefault() ? 1 : 0);
		// IS_END_OF_MONTH
		ps.setLong(i++, pt.isEndOfMonth() ? 1 : 0);
		//	DAY_OF_MONTH
		ps.setLong(i++, pt.getDayOfMonth());
		//	USE_FINANCIAL_MONTH
		ps.setLong(i++, pt.isUseFinancialMonth() ? 1 : 0);
		// PAY_TYPE_ID
		ps.setLong(i++, pt.getPayTypeId());
		
		// return the current ps position 
		return i;
	}
	
	public void build(AEPaymentTerms pt, ResultSet rs) throws SQLException, AEException {
		// ID
		pt.setID(rs.getLong("ID"));
		// OWNER_ID
		pt.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// CODE
		pt.setCode(rs.getString("CODE"));
		// NAME
		pt.setName(rs.getString("NAME"));
		// DESCRIPTION
		pt.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		pt.setProperties(rs.getLong("PROPERTIES"));
		// BY_DECADE
		pt.setByDecade(rs.getInt("BY_DECADE") == 1);
		//	DELAY
		pt.setDelay(rs.getLong("DELAY"));
		//	IS_DEFAULT
		pt.setDefault(rs.getInt("IS_DEFAULT") == 1);
		// IS_END_OF_MONTH
		pt.setEndOfMonth(rs.getInt("IS_END_OF_MONTH") == 1);
		//	DAY_OF_MONTH
		pt.setDayOfMonth(rs.getInt("DAY_OF_MONTH"));
		//	USE_FINANCIAL_MONTH
		pt.setUseFinancialMonth(rs.getInt("USE_FINANCIAL_MONTH") == 1);
		// PAY_TYPE_ID
		pt.setPayTypeId(rs.getLong("PAY_TYPE_ID"));
	}
	
	public AEPaymentTermsList loadPaymentTerms(AEDescriptor ownerDescr) throws AEException {
		try {
			return loadPaymentTermTemplates(ownerDescr);
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			close();
		}
	} 
	
	/**
	 * Client
	 */
	
	public AEClientsList loadClientsList(AEDescriptor ownerDescr) throws AEException {
		AEClientsList clients = new AEClientsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLClients);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEClient c = new AEClient();
				build(c, rs);
				c.setView();
				clients.add(c);
			}
			return clients;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public void insert(AEClient c) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQLClient);
			build(c, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				c.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			c.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEClient c) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLClient);

			// build statement
			int i = build(c, ps, 1);
			ps.setLong(i, c.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			c.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(AEClient c, ResultSet rs) throws SQLException, AEException {
		// ID
		c.setID(rs.getLong("ID"));
		// OWNER_ID
		c.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// CODE
		c.setCode(rs.getString("CODE"));
		// NAME
		c.setName(rs.getString("NAME"));
		// DESCRIPTION
		c.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		c.setProperties(rs.getLong("PROPERTIES"));
		// GROUP_ID, GROUP_NAME
		Long groupId = rs.getLong("GROUP_ID");
		if(rs.wasNull()) {
			groupId = null;
		}
		String groupName = rs.getString("GROUP_NAME");
		if(rs.wasNull()) {
			groupName = null;
		}
		if(groupId != null || groupName != null) {
			AEDescriptor group = new AEDescriptorImp(AEMath.longValue(groupId));
			group.setName(groupName);
			c.setGroup(group);
		}

		// ADDRESS, SEC_ADDRESS, POST_CODE, TOWN
		Address address = c.grantAddress();
		address.setStreet(rs.getString("ADDRESS"));
		address.setSecondaryStreet(rs.getString("SEC_ADDRESS"));
		address.setPostalCode(rs.getString("POST_CODE"));
		address.setCity(rs.getString("TOWN"));
		
		// PHONE, MOBILE, FAX, EMAIL, HOMEPAGE
		Contact contact = c.grantContact();
		contact.setPhone(rs.getString("PHONE"));
		contact.setMobile(rs.getString("MOBILE"));
		contact.setFax(rs.getString("FAX"));
		contact.seteMail(rs.getString("EMAIL"));
		contact.setHomepage(rs.getString("HOMEPAGE"));
		
		// VAT_NUMBER
		c.setVatNumber(rs.getString("VAT_NUMBER"));
		
		// PAYMENT_TERMS_ID
		long paymentTermsId = rs.getLong("PAYMENT_TERMS_ID");
		if(paymentTermsId > 0) {
			AEPaymentTermsList ptList = loadPaymentTerms(c.getCompany().getDescriptor());
			if(ptList != null) {
				AEPaymentTerms pt = ptList.getById(paymentTermsId);
				if(pt != null) {
					c.setPaymentTerms(pt.getDescriptor());
				}
			}
		}
		
		// ACCOUNT_ID
		long accountId = rs.getLong("ACCOUNT_ID");
		if(accountId > 0) {
			c.setAccount(AccAccount.lazyDescriptor(accountId));
		}
		
		// ACCOUNT_AUXILIARY
		c.setAccountAuxiliary(rs.getString("ACCOUNT_AUXILIARY"));
		
		// DISCOUNT_RATE
		c.setDiscountRate(rs.getDouble("DISCOUNT_RATE"));
		
		// PERSON_SALUTATION_ID, PERSON_NAME, PERSON_POSITION
		PersonDescr pDescr = c.grantResponsiblePerson();
		long salutationId = rs.getLong("PERSON_SALUTATION_ID");
		if(salutationId > 0) {
			pDescr.setSalutation(Person.SalutationType.valueOf(salutationId));
		}
		pDescr.setName(rs.getString("PERSON_NAME"));
		pDescr.setPosition(rs.getString("PERSON_POSITION"));
	}
	
	public int build(AEClient c, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(c.getCompany() != null) {
			ps.setLong(i++, c.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, c.getCode());
		// NAME
		ps.setString(i++, c.getName());
		// DESCRIPTION
		ps.setString(i++, c.getDescription());
		// PROPERTIES
		ps.setLong(i++, c.getProperties());
		// GROUP_ID, GROUP_NAME
		if(c.getGroup() != null) {
			ps.setLong(i++, c.getGroup().getDescriptor().getID());
			ps.setString(i++, c.getGroup().getDescriptor().getName());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}

		// ADDRESS, SEC_ADDRESS, POST_CODE, TOWN
		Address address = c.grantAddress();
		ps.setString(i++, address.getStreet());
		ps.setString(i++, address.getSecondaryStreet());
		ps.setString(i++, address.getPostalCode());
		ps.setString(i++, address.getCity());
		
		// PHONE, MOBILE, FAX, EMAIL, HOMEPAGE
		Contact contact = c.grantContact();
		ps.setString(i++, contact.getPhone());
		ps.setString(i++, contact.getMobile());
		ps.setString(i++, contact.getFax());
		ps.setString(i++, contact.geteMail());
		ps.setString(i++, contact.getHomepage());
		
		// VAT_NUMBER
		ps.setString(i++, c.getVatNumber());
		
		// PAYMENT_TERMS_ID
		if(c.getPaymentTerms() != null) {
			ps.setLong(i++, c.getPaymentTerms().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACCOUNT_ID
		if(c.getAccount() != null) {
			ps.setLong(i++, c.getAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACCOUNT_AUXILIARY
		ps.setString(i++, c.getAccountAuxiliary());
		
		// DISCOUNT_RATE
		ps.setDouble(i++, c.getDiscountRate());
		
		// PERSON_SALUTATION_ID, PERSON_NAME, PERSON_POSITION
		PersonDescr pDescr = c.grantResponsiblePerson();
		if(pDescr != null) {
			ps.setLong(i++, pDescr.getSalutation().getTypeID());
			ps.setString(i++, pDescr.getName());
			ps.setString(i++, pDescr.getPosition());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
				
		// return the current ps position 
		return i;
	}
	
	public void deleteClient(AEDescriptor cDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLClient);

			// build statement
			ps.setLong(1, cDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public long getNextNumberArticle(AEDescriptor ownerDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long lastNumber = 0;
			String sql = "select max(CODE_NUMBER) from Fact_Article where OWNER_ID = ?";
			
			// prepare statement
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerDescr.getID());

			// set generated key
			rs = ps.executeQuery();
			if (rs.next()) {
				lastNumber = rs.getLong(1);
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
	
	public long getNextNumberClient(AEDescriptor ownerDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			long lastNumber = 0;
			String sql = "select max(CODE_NUMBER) from Fact_Client where OWNER_ID = ?";
			
			// prepare statement
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerDescr.getID());

			// set generated key
			rs = ps.executeQuery();
			if (rs.next()) {
				lastNumber = rs.getLong(1);
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
	
	public void updateCodeNumber(AEArticle art) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLArticleNumber);

			// build statement
			ps.setLong(1, art.getCodeNumber());
			ps.setLong(2, art.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			art.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void updateCodeNumber(AEClient c) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLClientNumber);

			// build statement
			ps.setLong(1, c.getCodeNumber());
			ps.setLong(2, c.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			c.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void build(AEFacturePrintTemplate t, ResultSet rs) throws SQLException, AEException {
		// ID
		t.setID(rs.getLong("ID"));
		// OWNER_ID
		t.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// CODE
		t.setCode(rs.getString("CODE"));
		// NAME
		t.setName(rs.getString("NAME"));
		// DESCRIPTION
		t.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		t.setProperties(rs.getLong("PROPERTIES"));
		// BODY
		t.setBody(rs.getLong("BODY"));
		// ISSUER
		t.setIssuer(rs.getLong("ISSUER"));
		// CLIENT
		t.setClient(rs.getLong("CLIENT"));
		
		// BY_DEFAULT
		t.setByDefault(rs.getBoolean("BY_DEFAULT"));
		
		// FOR_REMINDER
		t.setForReminder(rs.getBoolean("FOR_REMINDER"));
		
		// FOR_DEVIS
		t.setForDevis(rs.getBoolean("FOR_DEVIS"));
		
		//FOR_FACTURE
		t.setForFacture(rs.getBoolean("FOR_FACTURE"));
	}
	
	public int build(AEFacturePrintTemplate t, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(t.getCompany() != null) {
			ps.setLong(i++, t.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// CODE
		ps.setString(i++, t.getCode());
		// NAME
		ps.setString(i++, t.getName());
		// DESCRIPTION
		ps.setString(i++, t.getDescription());
		// PROPERTIES
		ps.setLong(i++, t.getProperties());
		// BODY
		ps.setLong(i++, t.getBody());
		// ISSUER
		ps.setLong(i++, t.getIssuer());
		// CLIENT
		ps.setLong(i++, t.getClient());
		
		// BY_DEFAULT
		ps.setBoolean(i++, t.isByDefault());
		
		// FOR_REMINDER
		ps.setBoolean(i++, t.isForReminder());
		
		// FOR_DEVIS
		ps.setBoolean(i++, t.isForDevis());
		
		//FOR_FACTURE
		ps.setBoolean(i++, t.isForFacture());
		
		// return the current ps position 
		return i;
	}

	/**
	 * 
	 */
	public void insert(AEFacturePrintTemplate t) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQLTemplate);
			build(t, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				t.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			t.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEFacturePrintTemplate t) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLTemplate);

			// build statement
			int i = build(t, ps, 1);
			ps.setLong(i, t.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			t.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void setByDefault(AEFacturePrintTemplate t) throws AEException {
		PreparedStatement ps = null;
		try {
			// for devis
			String sql = null;
			if(t.isForDevis()) {
				sql =  "update Fact_PrintTemplate set BY_DEFAULT = 0 "
					+ "where OWNER_ID = ? and FOR_DEVIS = 1 and ID != ?";
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, t.getCompany().getDescriptor().getID());
				ps.setLong(2, t.getID());
				ps.executeUpdate();
				AEConnection.close(ps);
			}
			
			// for facture
			if(t.isForFacture()) {
				sql =  "update Fact_PrintTemplate set BY_DEFAULT = 0 "
					+ "where OWNER_ID = ? and FOR_FACTURE = 1 and ID != ?";
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, t.getCompany().getDescriptor().getID());
				ps.setLong(2, t.getID());
				ps.executeUpdate();
				AEConnection.close(ps);
			}
			
			// set view state
			t.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFacturePrintTemplatesList loadPrintTemplatesList(AEDescriptor ownerDescr) throws AEException {
		AEFacturePrintTemplatesList templates = new AEFacturePrintTemplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTemplates);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFacturePrintTemplate t = new AEFacturePrintTemplate();
				build(t, rs);
				t.setView();
				templates.add(t);
			}
			return templates;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public AEFacturePrintTemplatesList loadRelevantPrintTemplatesList(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		AEFacturePrintTemplatesList templates = new AEFacturePrintTemplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = null;
			if(AEDocumentType.System.AEDevisSale.getSystemID() == docType.getSystemID()) {
				sql =  "select * from Fact_PrintTemplate where OWNER_ID = ? and FOR_DEVIS = 1";
			} else if(AEDocumentType.System.AEFactureSale.getSystemID() == docType.getSystemID()) {
				sql =  "select * from Fact_PrintTemplate where OWNER_ID = ? and FOR_FACTURE = 1";
			} else {
				sql =  "select * from Fact_PrintTemplate where OWNER_ID = ?";
			}
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFacturePrintTemplate t = new AEFacturePrintTemplate();
				build(t, rs);
				t.setView();
				templates.add(t);
			}
			return templates;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public AEFacturePrintTemplate loadPrintTemplate(AEDescriptor templateDescr) throws AEException {
		AEFacturePrintTemplate template = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTemplate);
			ps.setLong(1, templateDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				template = new AEFacturePrintTemplate();
				build(template, rs);
				template.setView();
			}
			return template;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	public void deletePrintTemplate(AEDescriptor t) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLTemplate);

			// build statement
			ps.setLong(1, t.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insert(AEPaymentTerms pt) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQLPaymentTerm);
			build(pt, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				pt.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			pt.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			this.close();
		}
	}
	
	public AEPaymentTermsList loadPaymentTermTemplates(AEDescriptor descr) throws AEException {
		AEPaymentTermsList ptList = new AEPaymentTermsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(loadPaymentTermTemplates);
			ps.setLong(1, descr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEPaymentTerms pt = new AEPaymentTerms();
				this.build(pt, rs);
				pt.setView();
				ptList.add(pt);
			}
			return ptList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEPaymentTerms loadPaymentTerms(long payTermsId) throws AEException {
		AEPaymentTerms pt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLById);
			ps.setLong(1, payTermsId);
			rs = ps.executeQuery();
			if(rs.next()) {
				pt = new AEPaymentTerms();
				this.build(pt, rs);
				pt.setView();
			}
			return pt;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void update(AEPaymentTerms pt) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLPaymentTerm);

			// build statement
			int i = build(pt, ps, 1);
			ps.setLong(i, pt.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			pt.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void setByDefault(AEPaymentTerms pt) throws AEException {
		PreparedStatement ps = null;
		try {
			// for devis
			String sql = "update PaymentTerms set IS_DEFAULT = 0 where OWNER_ID = ? and ID != ?";
			
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, pt.getCompany().getDescriptor().getID());
			ps.setLong(2, pt.getID());
			ps.executeUpdate();
			AEConnection.close(ps);
			
			// set view state
			pt.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deletePaymentTermTemplate(AEDescriptor descr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLPaymentTerm);

			// build statement
			ps.setLong(1, descr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void buildAttributeValue(AEAttributeValue value, ResultSet rs) throws SQLException {
		// ID
		value.setID(rs.getLong("ID"));
		// OWNER_ID
		value.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// ATTRIBUTE_ID
		value.setAttribute(AEAttribute.lazyDescriptor(rs.getLong("ATTRIBUTE_ID")));

		// VALUE
		String valueType = rs.getString("VALUE_TYPE");
		if (AEValue.XType.LONG.name().equalsIgnoreCase(valueType)) {
			value.setValue(new AEValue(rs.getLong("LONG_VALUE")));
		} else if (AEValue.XType.DATE.name().equalsIgnoreCase(valueType)) {
			value.setValue(new AEValue(rs.getDate("DATE_VALUE")));
		} else if (AEValue.XType.DOUBLE.name().equalsIgnoreCase(valueType)) {
			value.setValue(new AEValue(rs.getDouble("NUMERIC_VALUE")));
		} else if (AEValue.XType.STRING.name().equalsIgnoreCase(valueType)) {
			value.setValue(new AEValue(rs.getDouble("STRING_VALUE")));
		} else if (AEValue.XType.BOOLEAN.name().equalsIgnoreCase(valueType)) {
			value.setValue(new AEValue(rs.getLong("LONG_VALUE") != 1L));
		} 
	}
}
