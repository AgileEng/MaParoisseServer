package eu.agileeng.persistent.dao.document.trade;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.document.imp.AEDocumentDescriptorImp;
import eu.agileeng.domain.document.social.contractdetravail.ContractDeTravail;
import eu.agileeng.domain.document.trade.AEDocumentItem;
import eu.agileeng.domain.document.trade.AEDocumentItemsList;
import eu.agileeng.domain.document.trade.AETradeDocument;
import eu.agileeng.domain.document.trade.AETradeDocumentFilter;
import eu.agileeng.domain.document.trade.AETradeDocumentResult;
import eu.agileeng.domain.document.trade.AETradeDocumentResultsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;
import eu.agileeng.util.AECollectionUtil;
import eu.agileeng.util.AEStringUtil;

public class AETradeDocumentDAO extends AEDocumentDAO {
	private static String selectSQL = 
		"select document.*, TradeDocument.* from TradeDocument inner join document "
		+ " on TradeDocument.DOC_ID = document.id"
		+ " where TradeDocument.DOC_ID = ?";

	private static String insertSQL = "insert into TradeDocument "
		+ " (DOC_ID, ISSUER_ID, ISSUER_ACC_ID, RECIPIENT_ID, RECIPIENT_ACC_ID, TAXABLE_AMOUNT, "
		+ " VAT_CODE, VAT_AMOUNT, AMOUNT, PAYMENT_DUE_DATE, CURRENCY, VAT_ACCOUNT_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update TradeDocument set "
		+ "DOC_ID = ?, ISSUER_ID = ?, ISSUER_ACC_ID = ?, RECIPIENT_ID = ?, RECIPIENT_ACC_ID = ?, TAXABLE_AMOUNT = ?, "
		+ " VAT_CODE = ?, VAT_AMOUNT = ?, AMOUNT = ?, PAYMENT_DUE_DATE = ?, CURRENCY = ?, VAT_ACCOUNT_ID = ? "
		+ " where DOC_ID = ?";
	
	private static String selectSQL2 = 	
		"select TradeDocument.*, document.* "
		+ " from TradeDocument inner join document on TradeDocument.DOC_ID = document.id"
		+ " where document.reg_date >= ? and document.reg_date <= ? order by DOC_ID desc";
	
	private static String selectSQLSupplyByFilter = 
		"SELECT %s "
		+ " doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, doc.VALIDATED, doc.VALIDATED_BY, doc.VALIDATED_TIME, "
		+ " tDoc.ISSUER_ID, issuer.NAME as ISSUER_NAME, "
	    + " doc.DESCRIPTION, tDoc.PAYMENT_DUE_DATE, " 
	    + " tDoc.TAXABLE_AMOUNT, tDoc.VAT_AMOUNT, tDoc.AMOUNT, "
	    + " att.FILE_LENGTH, att.FILE_TYPE, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as ATT_NAME "
	    + " from TradeDocument tDoc "
	    + " inner join Document doc on tDoc.DOC_ID = doc.ID"
	    + " %s "
	    + " left join Party issuer on tDoc.ISSUER_ID = issuer.ID "
	    + " left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = doc.CLASS_ID and att.ATTACHED_TO_ID = doc.ID"
	    + " where doc.type_id = 100 and doc.template = 0 and doc.OWNER_ID = ? and doc.TYPE_ID = ? ";
	
	private static String selectSQLSupplyFNPByFilter = 
		"SELECT %s "
		+ " doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, tDoc.ISSUER_ID, issuer.NAME as ISSUER_NAME, "
	    + " doc.VALIDATED, doc.VALIDATED_BY, doc.VALIDATED_TIME, doc.DESCRIPTION, tDoc.PAYMENT_DUE_DATE, " 
	    + " tDoc.TAXABLE_AMOUNT, tDoc.VAT_AMOUNT, tDoc.AMOUNT, "
	    + " att.FILE_LENGTH, att.FILE_TYPE, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as ATT_NAME "
	    + " from TradeDocument tDoc "
	    + " inner join Document doc on tDoc.DOC_ID = doc.ID"
	    + " %s "
	    + " left join Party issuer on tDoc.ISSUER_ID = issuer.ID "
	    + " left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = doc.CLASS_ID and att.ATTACHED_TO_ID = doc.ID"
	    + " where doc.type_id = 110 and doc.template = 0 and doc.OWNER_ID = ? and doc.TYPE_ID = ? ";
	
	private static String selectSQLSaleByFilter = 
		"SELECT %s "
		+ " doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, tDoc.recipient_ID, recipient.NAME as recipient_NAME, "
	    + " doc.VALIDATED, doc.VALIDATED_BY, doc.VALIDATED_TIME, doc.DESCRIPTION, tDoc.PAYMENT_DUE_DATE, " 
	    + " tDoc.TAXABLE_AMOUNT, tDoc.VAT_AMOUNT, tDoc.AMOUNT, "
	    + " att.FILE_LENGTH, att.FILE_TYPE, att.REMOTE_ROOT, att.REMOTE_PATH, att.NAME as ATT_NAME "
	    + " from TradeDocument tDoc "
	    + " inner join Document doc on tDoc.DOC_ID = doc.ID" 
	    + " %s "
	    + " left join Party recipient on tDoc.recipient_ID = recipient.ID "
	    + " left join FileAttachment att on att.ATTACHED_TO_CLASS_ID = doc.CLASS_ID and att.ATTACHED_TO_ID = doc.ID"
	    + " where doc.type_id = 200 and doc.template = 0 and doc.OWNER_ID = ? and doc.TYPE_ID = ? ";
	
	private static String selectSQLBank = 
		"SELECT doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, doc.CODE, doc.DESCRIPTION, tDoc.AMOUNT, "
		+ " bankAcc.id bankAccId, bankAcc.name bankAccName"
		+ " from TradeDocument tDoc "
		+ " inner join Document doc on tDoc.DOC_ID = doc.ID "
		+ " left join BankAccount bankAcc on doc.BANK_ACC_ID = bankAcc.ID " 
		+ " where "
		+ " doc.type_id >= 500 and doc.type_id < 600 " 
		+ " and doc.template = 0 "
		+ " and doc.OWNER_ID = ? "
		+ " and doc.doc_date >= ? and doc.doc_date <= ? "
		+ " order by doc.DOC_DATE desc, doc.ID desc";
	
	private static String selectSQLSaleInvoicesUnpaid = 
		"SELECT doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, doc.CODE, doc.DESCRIPTION, tDoc.AMOUNT "
		+ " from TradeDocument tDoc "
		+ " inner join Document doc on tDoc.DOC_ID = doc.ID "
		+ " where "
		+ " doc.type_id = 200 and doc.template = 0 and tDoc.paid = 0 "
		+ " and doc.OWNER_ID = ? "
		+ " order by doc.DOC_DATE desc, doc.ID desc";
	
	private static String selectSQLBankUnpaid = 
		"SELECT doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE, doc.CODE, doc.DESCRIPTION, tDoc.AMOUNT, "
		+ " bankAcc.id bankAccId, bankAcc.name bankAccName"
		+ " from TradeDocument tDoc "
		+ " inner join Document doc on tDoc.DOC_ID = doc.ID "
		+ " inner join TradeDocumentItem docItem on doc.ID = docItem.DOC_ID "
		+ " left join BankAccount bankAcc on doc.BANK_ACC_ID = bankAcc.ID " 
		+ " where "
		+ " doc.type_id >= 500 and doc.type_id < 600 " 
		+ " and doc.template = 0 "
		+ " and doc.OWNER_ID = ? "
		+ " and doc.BANK_ACC_ID = ? "
		+ " and docItem.paid = 0 "
		+ " order by doc.DOC_DATE desc, doc.ID desc";
	
	private static String selectSQLByPeriod = 
		"SELECT doc.ID, doc.TYPE_ID, doc.NIMBER_STRING, doc.DOC_DATE from Document doc " 
	    + " where doc.template = 0 and doc.OWNER_ID = ? and doc.TYPE_ID = ? "
	    + " and doc_date >= ? and doc_date <= ?";
	
	private static String setPaidSQL = 
		"update TradeDocument set paid = ? where doc_id = ?";

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AETradeDocumentDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	@Override
	public AEDocument load(AEDescriptor docDescr) throws AEException {
		AETradeDocument doc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, docDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				doc = new AETradeDocument();
				build(doc, rs);
				doc.setView();
			}
			return doc;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}

	@Override
	public AEDocumentsList loadContratDeTravail(AEDocumentFilter filter) throws AEException {
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL2);
			ps.setDate(1, AEPersistentUtil.getSQLDate(filter.getDateFrom()));
			ps.setDate(2, AEPersistentUtil.getSQLDate(filter.getDateTo()));
			rs = ps.executeQuery();
			while(rs.next()) {
				AETradeDocument doc = new AETradeDocument();
				build(doc, rs);
				doc.setView();
				docsList.add(doc);
			}
			return docsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	@Override
	public void insert(AEDocument doc) throws AEException {
		assert(doc != null && !doc.isPersistent());
		assert doc instanceof ContractDeTravail : "doc instanceof ContractDeTravail failed";
		AETradeDocument aeInv = (AETradeDocument) doc;
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// insert into common table
			super.insert(aeInv);
			
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(insertSQL);
			build(aeInv, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view state
			doc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}

	public int build(AETradeDocument doc, PreparedStatement ps, int i) throws SQLException, AEException {
		//DOC_ID
		ps.setLong(i++, doc.getID());
		//ISSUER_ID
		if(doc.getIssuer() != null) {
			ps.setLong(i++, doc.getIssuer().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.INTEGER);
		}
		//ISSUER_ACC_ID
		if(doc.getIssuerAcc() != null) {
			ps.setLong(i++, doc.getIssuerAcc().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.INTEGER);
		}
		//RECIPIENT_ID
		if(doc.getRecipient() != null) {
			ps.setLong(i++, doc.getRecipient().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.INTEGER);
		}
		//RECIPIENT_ACC_ID
		if(doc.getRecipientAcc() != null) {
			ps.setLong(i++, doc.getRecipientAcc().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.INTEGER);
		}
		//TAXABLE_AMOUNT
		ps.setDouble(i++, doc.getTaxableAmount());
		//VAT_CODE
		if(doc.getVatDescriptor() != null) {
			ps.setString(i++, doc.getVatDescriptor().getDescriptor().getCode());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		//VAT_AMOUNT
		ps.setDouble(i++, doc.getVatAmount());
		//AMOUNT
		ps.setDouble(i++, doc.getAmount());
		//PAYMENT_DUE_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getPaymentDueDate()));
		//CURRENCY
		if(doc.getCurrency() != null) {
			ps.setString(i++, doc.getCurrency().getDescriptor().getCode());
		} else {
			ps.setNull(i++, Types.NVARCHAR);
		}
		// VAT_ACCOUNT_ID
		if(doc.getVatAccount() != null) {
			ps.setLong(i++, doc.getVatAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.INTEGER);
		}
		
		// return the current ps position 
		return i;
	}

	public void build(AETradeDocument doc, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(doc, rs);

		// DOC_ID - already set
		
		//ISSUER_ID
		long issuerId = rs.getLong("ISSUER_ID");
		if(!rs.wasNull()) {
			doc.setIssuer(Organization.lazyDescriptor(issuerId));
		}
		//ISSUER_ACC_ID
		long issuerAccId = rs.getLong("ISSUER_ACC_ID");
		if(!rs.wasNull()) {
			doc.setIssuerAcc(new AEDescriptorImp(issuerAccId));
		}
		//RECIPIENT_ID
		long recipientId = rs.getLong("RECIPIENT_ID");
		if(!rs.wasNull()) {
			doc.setRecipient(Organization.lazyDescriptor(recipientId));
		}
		//RECIPIENT_ACC_ID
		long recipientAccId = rs.getLong("RECIPIENT_ACC_ID");
		if(!rs.wasNull()) {
			doc.setRecipientAcc(new AEDescriptorImp(recipientAccId));
		}
		//TAXABLE_AMOUNT
		doc.setTaxableAmount(rs.getDouble("TAXABLE_AMOUNT"));
		//VAT_CODE
		String vatCode = rs.getString("VAT_CODE");
		if(!rs.wasNull()) {
			AEDescriptorImp vatDescriptor = new AEDescriptorImp();
			vatDescriptor.setCode(vatCode);
			doc.setVatDescriptor(vatDescriptor);
		}
		//VAT_AMOUNT
		doc.setVatAmount(rs.getDouble("VAT_AMOUNT"));
		//AMOUNT
		doc.setAmount(rs.getDouble("AMOUNT"));
		//PAYMENT_DUE_DATE
		doc.setPaymentDueDate(rs.getDate("PAYMENT_DUE_DATE"));
		//CURRENCY
		String currCode = rs.getString("CURRENCY");
		if(!rs.wasNull()) {
			AEDescriptorImp currDescriptor = new AEDescriptorImp();
			currDescriptor.setCode(currCode);
			doc.setCurrency(currDescriptor);
		}
		
		//PAID
		doc.setPaid(rs.getBoolean("PAID"));
		
		// VAT_ACCOUNT_ID
		long vatAccId = rs.getLong("VAT_ACCOUNT_ID");
		if(!rs.wasNull()) {
			doc.setVatAccount(AccAccount.lazyDescriptor(vatAccId));
		}
		
		// set this record in view state
		doc.setView();
	}
	
	@Override
	public void update(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		assert doc instanceof ContractDeTravail : "doc instanceof ContractDeTravail failed";
		AETradeDocument aeInv = (AETradeDocument) doc;
		PreparedStatement ps = null;
		try {
			// update document table
			super.update(aeInv);
			
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(aeInv, ps, 1);
			ps.setLong(i++, doc.getID());
			
			// execute
			ps.executeUpdate();

			// set view state
			doc.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AETradeDocumentResultsList load(AETradeDocumentFilter filter) throws AEException {
		AETradeDocumentResultsList results = new AETradeDocumentResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Append filter
			 */
			String sqlRaw = null;
			if(filter.getDocType() != null && filter.getDocType().longValue() == 100) {
				sqlRaw = selectSQLSupplyByFilter;
			} else if(filter.getDocType() != null && filter.getDocType().longValue() == 110) {
				sqlRaw = selectSQLSupplyFNPByFilter;
			} else if(filter.getDocType() != null && filter.getDocType().longValue() == 200) {
				sqlRaw = selectSQLSaleByFilter;
			}
			
			// determine the view 
			AETradeDocumentFilter.View view = AETradeDocumentFilter.View.COARSE;
			if(AETradeDocumentFilter.View.DETAILED.equals(filter.getView()) || !AECollectionUtil.isEmpty(filter.getItemAccDescrList())) {
				// detailed view or account selected
				// the statement will include inner join to items
				view = AETradeDocumentFilter.View.DETAILED;
			}
			
			String sql = null;
			if(AETradeDocumentFilter.View.DETAILED.equals(view)) {
				sql = String.format(
						sqlRaw, 
						" item.ID as item_id, item.ACC_ID as item_acc_id, item.code as item_code, item.NAME as item_name, item.DESCRIPTION as item_description, item.TAXABLE_AMOUNT as item_taxable_amount, ", 
						" inner join TradeDocumentItem item on tDoc.DOC_ID = item.DOC_ID ");
			} else {
				// coarsed view
				sql = String.format(sqlRaw, AEStringUtil.EMPTY_STRING, AEStringUtil.EMPTY_STRING);
			}
			
			StringBuffer sb = new StringBuffer(sql);
			if(filter.getDocDateFrom() != null) {
				sb
					.append(" and doc.DOC_DATE >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDocDateFrom()));
			};
			if(filter.getDocDateTo() != null) {
				sb
					.append(" and doc.DOC_DATE <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDocDateTo()));
			};
			
			// companyId
			if(filter.getCompanyId() != null && filter.getDocType() != null && filter.getDocType().longValue() == 100) {
				sb
					.append(" and tDoc.ISSUER_ID = ")
					.append(filter.getCompanyId());
			} else if(filter.getCompanyId() != null && filter.getDocType() != null && filter.getDocType().longValue() == 200) {
				sb
					.append(" and tDoc.RECIPIENT_ID = ")
					.append(filter.getCompanyId());
			}
			
			// companyName 
			if(!AEStringUtil.isEmpty(filter.getCompanyName()) && filter.getDocType() != null && filter.getDocType().longValue() == 100) {
				sb
					.append(" and UPPER(issuer.NAME) like '%")
				    .append(filter.getCompanyName().toUpperCase()).append("%' ");
			} else if(!AEStringUtil.isEmpty(filter.getCompanyName()) && filter.getDocType() != null && filter.getDocType().longValue() == 200) {
				sb
				.append(" and UPPER(recipient.NAME) like '%")
			    .append(filter.getCompanyName().toUpperCase()).append("%' ");
			}
			
			// number 
			if(!AEStringUtil.isEmpty(filter.getNumber())) {
				sb
					.append(" and UPPER(doc.NIMBER_STRING) like '%")
				    .append(filter.getNumber().toUpperCase()).append("%' ");
			}
			
			// description
			if(!AEStringUtil.isEmpty(filter.getDescription())) {
				sb
					.append(" and UPPER(doc.DESCRIPTION) like '%")
				    .append(filter.getDescription().toUpperCase()).append("%' ");
			}

			// montantHTFrom
			if(filter.getMontantHTFrom() != null) {
				sb
					.append(" and tDoc.TAXABLE_AMOUNT >= ")
					.append(filter.getMontantHTFrom());
			}

			// montantHTTo
			if(filter.getMontantHTTo() != null) {
				sb
					.append(" and tDoc.TAXABLE_AMOUNT <= ")
					.append(filter.getMontantHTTo());
			}
			
			// montantTTCFrom
			if(filter.getMontantTTCFrom() != null) {
				sb
					.append(" and tDoc.AMOUNT >= ")
					.append(filter.getMontantTTCFrom());
			}
			
			// montantTTCTo
			if(filter.getMontantTTCTo() != null) {
				sb
					.append(" and tDoc.AMOUNT <= ")
					.append(filter.getMontantTTCTo());
			}
			
			// itemAccDescrList
			if(!AECollectionUtil.isEmpty(filter.getItemAccDescrList())) {
				sb
					.append(" and item.ACC_ID in ")
					.append(AEPersistentUtil.createInClause(filter.getItemAccDescrList()));
				
//				sb
//					.append(" and exists (select * from TradeDocumentItem item where item.DOC_ID = tDoc.DOC_ID and item.ACC_ID in ")
//					.append(AEPersistentUtil.createInClause(filter.getItemAccDescrList()))
//					.append(" )");
			}
			
			// items different than zero
			if(AETradeDocumentFilter.View.DETAILED.equals(view)) {
				// detailed view
				sb.append(" and item.TAXABLE_AMOUNT != 0 ");
			} 
			
			/**
			 * Append order
			 */
			sb.append(" order by doc.DOC_DATE desc, doc.ID desc ");
			
			/**
			 * Create statement
			 */
			ps = getAEConnection().prepareStatement(sb.toString());

			/**
			 * Append system filter
			 */
			Map<Long, AETradeDocumentResult> resultsMap = new HashMap<Long, AETradeDocumentResult>();
			ps.setLong(1, filter.getOwnerId());
			ps.setLong(2, filter.getDocType());
			rs = ps.executeQuery();
			while(rs.next()) {
				long docId = rs.getLong("ID");
				AETradeDocumentResult res = resultsMap.get(docId);
				if(res == null) {
					res = new AETradeDocumentResult();
					resultsMap.put(docId, res);
				}
				build(res, rs, filter.getDocType(), view);
				res.setView();
				
				if(!AECollectionUtil.isEmpty(filter.getItemAccDescrList())) {
					AEDocumentItemsList items = res.getTradeDocument().getItems();
					if(items != null && !items.isEmpty()) {
						double taxableAmount = 0.0;
						for (Iterator<AEDocumentItem> iterator = items.iterator(); iterator.hasNext();) {
							AEDocumentItem aeDocumentItem = (AEDocumentItem) iterator.next();
							taxableAmount += aeDocumentItem.getTaxableAmount();
						}
						res.getTradeDocument().setTaxableAmount(taxableAmount);
						res.getTradeDocument().setVatAmount(0.0);
						res.getTradeDocument().setAmount(0.0);
					}
				}
				
				results.add(res);
			}
			return results;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void build(AETradeDocumentResult result, ResultSet rs, long docType, AETradeDocumentFilter.View view) throws SQLException, AEException {
		// fill only if still not filled
		if(!result.getTradeDocument().isPersistent()) {
			//ID
			long docId = rs.getLong("ID");
			result.getTradeDocument().setID(docId);
			result.setID(docId);
			//TYPE_ID
			try {
				result.getTradeDocument().setType(AEDocumentType.valueOf(rs.getLong("TYPE_ID")));
			} catch(Exception e) {}
			//NIMBER_STRING
			try {
				result.getTradeDocument().setNumberString(rs.getString("NIMBER_STRING"));
			} catch(Exception e) {}
			//DOC_DATE
			try {
				result.getTradeDocument().setDate(rs.getDate("DOC_DATE"));
				if(docType == AEDocumentType.System.AEPurchaseInvoice.getSystemID()
						|| docType == AEDocumentType.System.AEPurchaseInvoiceFNP.getSystemID()) {
					//ISSUER_ID, ISSUER_NAME
					long issuerId = rs.getLong("ISSUER_ID");
					if(!rs.wasNull() && issuerId > 0) {
						AEDescriptor issuer = Organization.lazyDescriptor(issuerId);
						issuer.setName(rs.getString("ISSUER_NAME"));
						result.getTradeDocument().setIssuer(issuer);
					}
				} else if(docType == AEDocumentType.System.AESaleInvoice.getSystemID()) {
					//RECIPIENT_ID, RECIPIENT_NAME
					long recipientId = rs.getLong("RECIPIENT_ID");
					if(!rs.wasNull() && recipientId > 0) {
						AEDescriptor recipient = Organization.lazyDescriptor(recipientId);
						recipient.setName(rs.getString("RECIPIENT_NAME"));
						result.getTradeDocument().setRecipient(recipient);
					}
				}
			} catch (Exception e) {}
			//CODE
			try {
				result.getTradeDocument().setCode(rs.getString("CODE"));
			} catch (Exception e) {}
			//NAME
			try {
				result.getTradeDocument().setName(rs.getString("NAME"));
			} catch (Exception e) {}
			//DESCRIPTION
			try {
				result.getTradeDocument().setDescription(rs.getString("DESCRIPTION"));
			} catch (Exception e) {}
			//VALIDATED
			try {
				result.getTradeDocument().setValidated(rs.getBoolean("VALIDATED"));
			} catch (Exception e) {}
			//VALIDATED_BY
			try {
				result.getTradeDocument().setValidatedBy(rs.getString("VALIDATED_BY"));
			} catch (Exception e) {}
			//VALIDATED_TIME
			try {
				result.getTradeDocument().setValidatedTime(rs.getDate("VALIDATED_TIME"));
			} catch (Exception e) {}
			//PAYMENT_DUE_DATE
			try {
				result.getTradeDocument().setPaymentDueDate(rs.getDate("PAYMENT_DUE_DATE"));
			} catch(Exception e) {}
			//TAXABLE_AMOUNT
			try {
				result.getTradeDocument().setTaxableAmount(rs.getDouble("TAXABLE_AMOUNT"));
			} catch (Exception e) {}
			//VAT_AMOUNT
			try {
				result.getTradeDocument().setVatAmount(rs.getDouble("VAT_AMOUNT"));
			} catch (Exception e) {}
			//AMOUNT
			try {
				result.getTradeDocument().setAmount(rs.getDouble("AMOUNT"));
			} catch (Exception e) {}
			//FILE_LENGTH
			try {
				result.getAttachment().setFileLength(rs.getLong("FILE_LENGTH"));
			} catch(Exception e) {}
			//FILE_TYPE
			try {
				result.getAttachment().setFileType(rs.getString("FILE_TYPE"));
			} catch(Exception e) {}
			//REMOTE_ROOT
			try {
				result.getAttachment().setRemoteRoot(rs.getString("REMOTE_ROOT"));
			} catch (Exception e) {}
			//REMOTE_PATH
			try {
				result.getAttachment().setRemotePath(rs.getString("REMOTE_PATH"));
			} catch (Exception e) {}
			//ATT_NAME
			try {
				result.getAttachment().setName(rs.getString("ATT_NAME"));
			} catch (Exception e) {}
			// bankAccId bankAcc.name bankAccName
			try {
				long bankAccId = rs.getLong("bankAccId");
				if(!rs.wasNull()) {
					AEDescriptor bankDescr = BankAccount.lazyDescriptor(bankAccId);
					try {
						bankDescr.setName(rs.getString("bankAccName"));
					} catch (Exception e) {}
					result.getTradeDocument().setBankAcc(bankDescr);
				}
			} catch (Exception e) {}
		}
		
		// create item if it is detailed view
		if(AETradeDocumentFilter.View.DETAILED.equals(view)) {
			// build items
			AEDocumentItem item = new AEDocumentItem();
			
			// item_id
			item.setID(rs.getLong("item_id"));
			
			// item_acc_id
			long accId = rs.getLong("item_acc_id");
			if(!rs.wasNull()) {
				item.setAccount(AccAccount.lazyDescriptor(accId));
			}
			
			// item_code
			item.setCode(rs.getString("item_code"));
			
			// item_name
			item.setName(rs.getString("item_name"));
			
			// item_description
			item.setDescription(rs.getString("item_description"));
			
			// item_taxable_amount 
			item.setTaxableAmount(rs.getDouble("item_taxable_amount"));
			
			// add item
			result.getTradeDocument().addItem(item);
		}
	}
	
	/**
	 * 
	 * 
	 * @param ownerId
	 * @param beginDate
	 * @param endDate
	 * @param docType
	 * @return
	 * @throws AEException
	 */
	public AEDescriptorsList loadDescriptors(long ownerId, Date beginDate, Date endDate, long docType) throws AEException {
		AEDescriptorsList results = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByPeriod);
			ps.setLong(1, ownerId);
			ps.setLong(2, docType);
			ps.setDate(3, AEPersistentUtil.getSQLDate(beginDate));
			ps.setDate(4, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocumentDescriptor res = new AEDocumentDescriptorImp();
				res.setID(rs.getLong("ID"));
				res.setDocumentType(AEDocumentType.valueOf(AEDocumentType.System.AEPurchaseInvoice));
				// NIMBER_STRING
				res.setNumber(rs.getString("NIMBER_STRING"));
				// DOC_DATE
				res.setDate(rs.getDate("DOC_DATE"));
				results.add(res);
			}
			return results;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AETradeDocumentResultsList loadBankDescriptors(long ownerId, Date beginDate, Date endDate) throws AEException {
		AETradeDocumentResultsList results = new AETradeDocumentResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBank);
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(beginDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				AETradeDocumentResult res = new AETradeDocumentResult();
				build(res, rs, 0, AETradeDocumentFilter.View.COARSE);
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
	
	public AETradeDocumentResultsList loadUnpaidBankItems(long ownerId, long bankId) throws AEException {
		AETradeDocumentResultsList results = new AETradeDocumentResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBankUnpaid);
			ps.setLong(1, ownerId);
			ps.setLong(2, bankId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AETradeDocumentResult res = new AETradeDocumentResult();
				build(res, rs, 0,  AETradeDocumentFilter.View.COARSE);
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
	
	public AETradeDocumentResultsList loadSaleInvoicesUnpaid(long ownerId) throws AEException {
		AETradeDocumentResultsList results = new AETradeDocumentResultsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSaleInvoicesUnpaid);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AETradeDocumentResult res = new AETradeDocumentResult();
				build(res, rs, 0,  AETradeDocumentFilter.View.COARSE);
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
}
