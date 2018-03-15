package eu.agileeng.persistent.dao.facturation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.facturation.AEArticle;
import eu.agileeng.domain.facturation.AEFactureItem;
import eu.agileeng.domain.facturation.AEFactureItemsList;
import eu.agileeng.domain.facturation.AEFactureUtil;
import eu.agileeng.domain.facturation.AEPayment;
import eu.agileeng.domain.facturation.AEVatItem;
import eu.agileeng.domain.measurement.UnitOfMeasurement;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class AEFactureItemDAO extends AbstractDAO {

	private static String insertSQL = 
		"insert into Fact_FactureItem "
		+ "(CODE, NAME, DESCRIPTION, PROPERTIES, FACTURE_ID, ARTICLE_ID, TYPE_ID, "
		+ "QTY, UOM_ID, PRICE_EX_VAT, GROSS_AMOUNT, DISCOUNT_PERCENT, DISCOUNT_AMOUNT, "
		+ "BRUTO_AMOUNT, VAT_ID, VAT_RATE, PRICE_IN_VAT, PRICE_IN_VAT_PINNED, TAXABLE_AMOUNT, VAT_AMOUNT, AMOUNT, "
		+ "ITEM_DEDUCTED_ID, PAYMENT_INVOICED_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, "
		+ "?, ?, ?, ?, ?, ?, ?, ?, "
		+ "?, ?)";
	
	private static String updateSQL = 
		"update Fact_FactureItem set "
		+ "CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, FACTURE_ID = ?, ARTICLE_ID = ?, TYPE_ID = ?, "
		+ "QTY = ?, UOM_ID = ?, PRICE_EX_VAT = ?, GROSS_AMOUNT = ?, DISCOUNT_PERCENT = ?, DISCOUNT_AMOUNT = ?, "
		+ "BRUTO_AMOUNT = ?, VAT_ID = ?, VAT_RATE = ?, PRICE_IN_VAT = ?, PRICE_IN_VAT_PINNED = ?, "
		+ "TAXABLE_AMOUNT = ?, VAT_AMOUNT = ?, AMOUNT = ?, ITEM_DEDUCTED_ID = ?, PAYMENT_INVOICED_ID = ? "
		+ " where ID = ?";
	
	private static String deleteSQL = 
		"delete from Fact_FactureItem where id = ?";
	
	private static String selectSQL = 
		"select * from Fact_FactureItem where id = ?";
	
	private static String selectSQLAdvancedInvoicedToDevis = 
		" select advItem.* from " 
		+ " Fact_FactureItem advItem inner join Fact_Facture advFacture on advItem.FACTURE_ID = advFacture.ID "
		+ " inner join Fact_Facture devise on advFacture.SRC_FACTURE_ID = devise.ID "
		+ " where devise.ID = ? "
		+ " and advFacture.TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID() 
		+ " and advFacture.SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.ADVANCE.getSubTypeId() 
		+ " and advItem.TYPE_ID = " + AEFactureUtil.FactureItemType.ADVANCE.getTypeId();
	
	private static String selectSQLInvoicedToDevis = 
		" select advItem.* from " 
		+ " Fact_FactureItem advItem inner join Fact_Facture advFacture on advItem.FACTURE_ID = advFacture.ID "
		+ " inner join Fact_Facture devise on advFacture.SRC_FACTURE_ID = devise.ID "
		+ " where devise.ID = ? "
		+ " and advFacture.TYPE_ID = " + AEDocumentType.System.AEFactureSale.getSystemID() 
		+ " and advFacture.SUB_TYPE_ID = " + AEFactureUtil.FactureSubType.REGULAR.getSubTypeId() 
		+ " and advItem.TYPE_ID = " + AEFactureUtil.FactureItemType.ADVANCE.getTypeId();
	
	private static String selectSQLToFacture = 
		"select * from Fact_FactureItem where facture_id = ?";
	
	public AEFactureItemDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public void insert(AEFactureItem item) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
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
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEFactureItem item) throws AEException {
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
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private void build(AEFactureItem item, ResultSet rs) throws SQLException, AEException {
		long tmpLong = 0;
		
		// ID
		item.setID(rs.getLong("id"));
		
		// CODE
		item.setCode(rs.getString("CODE"));
		
		// NAME
		item.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		item.setDescription(rs.getString("DESCRIPTION"));
		
		// PROPERTIES
		item.setProperties(rs.getLong("PROPERTIES"));
		
		// FACTURE_ID
		long factureId = rs.getLong("FACTURE_ID");
		if(factureId > 0) {
			item.setDocument(new AEDocumentDescriptorImp(
					factureId, 
					AEDocumentType.valueOf(AEDocumentType.System.NA)));
		}
		
		// ARTICLE_ID
		long articleId = rs.getLong("ARTICLE_ID");
		if(articleId > 0) {
			item.setArticle(AEArticle.lazyDescriptor(articleId));
		}
		
		// TYPE_ID
		item.setTypeItem(AEFactureUtil.FactureItemType.valueOf(rs.getLong("TYPE_ID")));
		
		// QTY
		item.setQty(rs.getDouble("QTY"));
		
		// UOM_ID
		item.setUom(UnitOfMeasurement.getByID(rs.getLong("UOM_ID")));
		
		// PRICE_EX_VAT
		item.setPriceExVat(rs.getDouble("PRICE_EX_VAT"));
		
		// GROSS_AMOUNT
		item.setGrossAmount(rs.getDouble("GROSS_AMOUNT"));
		
		// DISCOUNT_PERCENT
		item.setDiscountPercent(rs.getDouble("DISCOUNT_PERCENT"));
		
		// DISCOUNT_AMOUNT
		item.setDiscountAmount(rs.getDouble("DISCOUNT_AMOUNT"));
		
		// BRUTO_AMOUNT
		item.setBrutoAmount(rs.getDouble("BRUTO_AMOUNT"));
		
		// VAT_ID, VAT_RATE
		AEVatItem vatItem = item.grantVatItem();
		vatItem.setID(rs.getLong("VAT_ID"));
		vatItem.setRate(rs.getDouble("VAT_RATE"));
		
		// PRICE_IN_VAT
		item.setPriceInVat(rs.getDouble("PRICE_IN_VAT"));
		
		// PRICE_IN_VAT_PINNED
		item.setPriceInVatPinned(rs.getBoolean("PRICE_IN_VAT_PINNED"));
		
		// TAXABLE_AMOUNT
		item.setTaxableAmount(rs.getDouble("TAXABLE_AMOUNT"));
		
		// VAT_AMOUNT
		item.setVatAmount(rs.getDouble("VAT_AMOUNT"));
		
		// AMOUNT
		item.setAmount(rs.getDouble("AMOUNT"));
		
		// ITEM_DEDUCTED_ID
		tmpLong = rs.getLong("ITEM_DEDUCTED_ID");
		if(tmpLong > 0) {
			item.setItemDeducted(AEFactureItem.lazyDescriptor(tmpLong));
		}
		
		// PAYMENT_INVOICED_ID
		tmpLong = rs.getLong("PAYMENT_INVOICED_ID");
		if(tmpLong > 0) {
			item.setPaymentInvoiced(AEPayment.lazyDescriptor(tmpLong));
		}
	}
	
	private int build(AEFactureItem item, PreparedStatement ps, int i) throws SQLException, AEException {
		// CODE
		ps.setString(i++, item.getCode());
		
		// NAME
		ps.setString(i++, item.getName());
		
		// DESCRIPTION
		ps.setString(i++, item.getDescription());
		
		// PROPERTIES
		ps.setLong(i++, item.getProperties());
		
		// FACTURE_ID
		if(item.getDocument() != null) {
			ps.setLong(i++, item.getDocument().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ARTICLE_ID
		if(item.getArticle() != null) {
			ps.setLong(i++, item.getArticle().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// TYPE_ID
		if(item.getTypeItem() != null) {
			ps.setLong(i++, item.getTypeItem().getTypeId());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// QTY
		ps.setDouble(i++, item.getQty());
		
		// UOM_ID
		if(item.getUom() != null) {
			ps.setLong(i++, item.getUom().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PRICE_EX_VAT
		ps.setDouble(i++, item.getPriceExVat());
		
		// GROSS_AMOUNT
		ps.setDouble(i++, item.getGrossAmount());
		
		// DISCOUNT_PERCENT
		ps.setDouble(i++, item.getDiscountPercent());
		
		// DISCOUNT_AMOUNT
		ps.setDouble(i++, item.getDiscountAmount());
		
		// BRUTO_AMOUNT
		ps.setDouble(i++, item.getBrutoAmount());
		
		// VAT_ID, VAT_RATE
		if(item.getVatItem() != null) {
			ps.setLong(i++, item.getVatItem().getID());
			ps.setDouble(i++, item.getVatItem().getRate());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
			ps.setNull(i++, java.sql.Types.NUMERIC);
		}
		
		// PRICE_IN_VAT
		ps.setDouble(i++, item.getPriceInVat());
		
		// PRICE_IN_VAT_PINNED
		ps.setBoolean(i++, item.isPriceInVatPinned());
		
		// TAXABLE_AMOUNT
		ps.setDouble(i++, item.getTaxableAmount());
		
		// VAT_AMOUNT
		ps.setDouble(i++, item.getVatAmount());
		
		// AMOUNT
		ps.setDouble(i++, item.getAmount());
		
		// ITEM_DEDUCTED_ID
		if(item.getItemDeducted() != null) {
			ps.setLong(i++, item.getItemDeducted().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// PAYMENT_INVOICED_ID
		if(item.getPaymentInvoiced() != null) {
			ps.setLong(i++, item.getPaymentInvoiced().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
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
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFactureItemsList loadToFacture(AEDescriptor toFactureDescr) throws AEException {
		AEFactureItemsList items = new AEFactureItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToFacture);
			ps.setLong(1, toFactureDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFactureItem item = new AEFactureItem();
				build(item, rs);
				item.setView();
				items.add(item);
			}
			return items;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFactureItemsList loadAdvancedInvoicedToDevis(AEDescriptor devisDescr) throws AEException {
		AEFactureItemsList items = new AEFactureItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAdvancedInvoicedToDevis);
			ps.setLong(1, devisDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFactureItem item = new AEFactureItem();
				build(item, rs);
				item.setView();
				items.add(item);
			}
			return items;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFactureItemsList loadInvoicedToDevis(AEDescriptor devisDescr) throws AEException {
		AEFactureItemsList items = new AEFactureItemsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLInvoicedToDevis);
			ps.setLong(1, devisDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEFactureItem item = new AEFactureItem();
				build(item, rs);
				item.setView();
				items.add(item);
			}
			return items;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEFactureItem load(AEDescriptor factureItemDescr) throws AEException {
		AEFactureItem item = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, factureItemDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				item = new AEFactureItem();
				build(item, rs);
				item.setView();
			}
			return item;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
