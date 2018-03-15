package eu.agileeng.persistent.dao.document.trade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEValue;

public class AEDocumentItemDAO extends AbstractDAO {
	private static String selectSQL = 
		"select * from TradeDocumentItem where DOC_ID = ?";

	private static String insertSQL = "insert into TradeDocumentItem "
		+ " (DOC_ID, CODE, NAME, DESCRIPTION, ACC_ID, ACC_TYPE, TAXABLE_AMOUNT, "
		+ " VAT_CODE, VAT_RATE, VAT_AMOUNT, AMOUNT, PARTY_ID, ACC_ID_SECONDARY, "
		+ " MD_NTYPE, MD_XTYPE, MD_VALUE, S_INDEX) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update TradeDocumentItem set "
		+ "DOC_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, ACC_ID = ?, ACC_TYPE = ?, TAXABLE_AMOUNT = ?, "
		+ " VAT_CODE = ?, VAT_RATE = ?, VAT_AMOUNT = ?, AMOUNT = ?, PARTY_ID = ?, ACC_ID_SECONDARY = ?, "
		+ " MD_NTYPE = ?, MD_XTYPE = ?, MD_VALUE = ?, S_INDEX = ? "
		+ " where ID = ?";
	
	private static String setPaidSQL = 
		"update TradeDocumentItem set paid = ? where ID = ?";
	
	private static String deleteSQL = "delete from TradeDocumentItem where ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AEDocumentItemDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public AEDocumentItemsList load(AEDescriptor docDescr) throws AEException {
		AEDocumentItemsList items = new AEDocumentItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, docDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocumentItem item = new AEDocumentItem();
				build(item, rs);
				item.setView();
				items.add(item);
			}
			return items;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	public void insert(AEDocumentItem item) throws AEException {
		assert(item != null && !item.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(item, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				item.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			item.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(AEDocumentItem item, PreparedStatement ps, int i) throws SQLException, AEException {
		// DOC_ID
		ps.setLong(i++, item.getDocument().getDescriptor().getID());
		// CODE
		ps.setString(i++, item.getCode());
		// NAME
		ps.setString(i++, item.getName());
		// DESCRIPTION
		ps.setString(i++, item.getDescription());
		// ACC_ID
		if(item.getAccount() != null) {
			ps.setLong(i++, item.getAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// ACC_TYPE
		ps.setString(i++, item.getAccType());
		// TAXABLE_AMOUNT
		ps.setDouble(i++, item.getTaxableAmount());
		// VAT_CODE
		if(item.getVatDescriptor() != null) {
			ps.setString(i++, item.getVatDescriptor().getDescriptor().getCode());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		// VAT_RATE
		ps.setDouble(i++, item.getVatRate());
		// VAT_AMOUNT
		ps.setDouble(i++, item.getVatAmount());
		// AMOUNT
		ps.setDouble(i++, item.getAmount());
		// PARTY
		if(item.getParty() != null) {
			ps.setLong(i++, item.getParty().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// ACC_ID_SECONDARY
		if(item.getAccountSecondary() != null) {
			ps.setLong(i++, item.getAccountSecondary().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// NTYPE
		if(item.getMDNType() != null) {
			ps.setString(i++, item.getMDNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// XTYPE, VALUE
		if(!AEValue.isNull(item.getMDValue())) {
			ps.setString(i++, item.getMDValue().getXType().toString());
			ps.setString(i++, item.getMDValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// S_INDEX
		ps.setLong(i++, item.getSequenceNumber());
		
		// return the current ps position 
		return i;
	}

	public void build(AEDocumentItem item, ResultSet rs) throws SQLException, AEException {
		// ID
		item.setID(rs.getLong("ID"));
		// DOC_ID
		item.setDocument(Organization.lazyDescriptor(rs.getLong("DOC_ID")));
		// CODE
		item.setCode(rs.getString("CODE"));
		// NAME
		item.setName(rs.getString("NAME"));
		// DESCRIPTION
		item.setDescription(rs.getString("DESCRIPTION"));
		// ACC_ID
		long accId = rs.getLong("ACC_ID");
		if(!rs.wasNull()) {
			item.setAccount(new AEDescriptorImp(accId));
		}
		// ACC_TYPE
		item.setAccType(rs.getString("ACC_TYPE"));
		// TAXABLE_AMOUNT
		item.setTaxableAmount(rs.getDouble("TAXABLE_AMOUNT"));
		// VAT_CODE
		String vatCode = rs.getString("VAT_CODE");
		if(!rs.wasNull()) {
			AEDescriptorImp vatDescr = new AEDescriptorImp();
			vatDescr.setCode(vatCode);
			item.setVatDescriptor(vatDescr);
		}
		// VAT_RATE
		item.setVatRate(rs.getDouble("VAT_RATE"));
		// VAT_AMOUNT
		item.setVatAmount(rs.getDouble("VAT_AMOUNT"));
		// AMOUNT
		item.setAmount(rs.getDouble("AMOUNT"));
		// PARTY_ID
		long partyId = rs.getLong("PARTY_ID");
		if(!rs.wasNull()) {
			item.setParty(Organization.lazyDescriptor(partyId));
		}
		// paid
		item.setPaid(rs.getBoolean("PAID"));
		// ACC_ID_SECONDARY
		long accIdSecondary = rs.getLong("ACC_ID_SECONDARY");
		if(!rs.wasNull()) {
			item.setAccountSecondary(AccAccount.lazyDescriptor(accIdSecondary));
		}
		
		// NTYPE
		String mdNType = rs.getString("MD_NTYPE");
		if(!rs.wasNull()) {
			item.setMDNType(AEDocumentItem.MDNType.valueOf(mdNType));
		}
		
		// XTYPE, VALUE
		String value = rs.getString("MD_VALUE");
		if(!rs.wasNull()) {
			item.setMDValue(new AEValue(value, rs.getString("MD_XTYPE")));
		}
		
		// S_INDEX
		item.setSequenceNumber(rs.getLong("S_INDEX"));
	}
	
	public void update(AEDocumentItem item) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(item, ps, 1);
			ps.setLong(i, item.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			item.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void setPaid(long id, boolean paid) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(setPaidSQL);

			// build statement
			ps.setBoolean(1, paid);
			ps.setLong(2, id);
			
			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
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
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
