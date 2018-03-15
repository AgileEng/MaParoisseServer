/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.05.2010 11:48:30
 */
package eu.agileeng.persistent.dao.document;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.business.bank.BankAccount;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentDescriptor;
import eu.agileeng.domain.document.AEDocumentFilter;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
public abstract class AEDocumentDAO extends AbstractDAO {
	
	private static String insertSQL = "insert into document "
		+ "(CLASS_ID, CODE, NAME, TIME_CREATED, CREATOR, TIME_MODIFIED, "
		+ "	MODIFIER, PROPERTIES, DESCRIPTION, NOTE, TYPE_ID, NUMBER, "
		+ " NIMBER_STRING, DOC_DATE, REG_NUMBER, REG_NUMBER_STRING, REG_DATE, OWNER_ID, "
		+ " TEMPLATE, TEMPLATE_ID, JOURNAL_ID, JOURNAL_CODE, BANK_ACC_ID, CONTRIBUTOR_DONATION_ID) "
		+ " values (?, ?, ?, ?, ?, ?, "
		+ "	?, ?, ?, ?, ?, ?, "
		+ " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

	private static String updateSQL = "update document set "
		+ "CLASS_ID = ?, CODE = ?, NAME = ?, TIME_CREATED = ?, CREATOR = ?, TIME_MODIFIED = ?, "
		+ "	MODIFIER = ?, PROPERTIES = ?, DESCRIPTION = ?, NOTE = ?, TYPE_ID = ?, NUMBER = ?, "
		+ " NIMBER_STRING = ?, DOC_DATE = ?, REG_NUMBER = ?, REG_NUMBER_STRING = ?, REG_DATE = ?, OWNER_ID = ?, "
		+ " TEMPLATE = ?, TEMPLATE_ID = ?, JOURNAL_ID = ?, JOURNAL_CODE = ?, BANK_ACC_ID = ?, CONTRIBUTOR_DONATION_ID = ? "
		+ " where ID = ?";
	
	private static String onValidatedSQL = "update document set "
		+ "VALIDATED = ?, VALIDATED_BY = ?, VALIDATED_TIME = ? "
		+ " where ID = ?";
	
	private static String onValidatedSQLExt = "update document set "
		+ "VALIDATED = ?, VALIDATED_BY = ?, VALIDATED_TIME = ? "
		+ " where ID = ? and OWNER_ID = ?";
	
	private static String updateSQLNumbers = "update document set "
		+ "NUMBER = ?, NIMBER_STRING = ?, REG_NUMBER = ?, REG_NUMBER_STRING = ? "
		+ " where ID = ?";
	
	private static String selectSQLSupplyTemplates = 
		"select * from document where type_id = 100 and template = 1 and owner_id = ?";
	
	private static String selectSQLSaleTemplates = 
		"select * from document where type_id = 200 and template = 1 and owner_id = ?";
	
	private static String selectSQLTemplates = 
		"select * from document where type_id = ? and template = 1 and owner_id = ?";
	
	private static String selectSQLDocType = 
			"select type_id from document where id = ?";
	
	private static String selectSQLByFilter = 
		"select * from document where OWNER_ID = ? ";
	
	private static String deleteSQL = 
		"delete from document where id = ?";
	
	private static String updateToLocked = 
		"update Document set PROPERTIES = (PROPERTIES | 4) where ID = ?";
	
	private static String isLocked = 
		"select id from Document where ID = ? and (properties & 4 = 4)";
	
	private static String updateToNotLocked = 
		"update Document set PROPERTIES = (PROPERTIES & ~4) where ID = ?";
	
	private static String ownershipValidationSQL = 
		"select id from Document where id = ? and OWNER_ID = ?";
	
	private static String getDocDateSQL = 
			"select doc_date from Document where id = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AEDocumentDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public AEDocument load(AEDescriptor docDescr) throws AEException {
		return null;
	}
	
	public AEDocumentType loadTypeId(long docId) throws AEException {
		AEDocumentType docType = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLDocType);
			ps.setLong(1, docId);
			rs = ps.executeQuery();
			if(rs.next()) {
				docType = AEDocumentType.valueOf(rs.getLong("type_id"));
			}
			return docType;
		} catch (SQLException e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void insert(AEDocument doc) throws AEException {
		assert(doc != null && !doc.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
	    boolean updateNumbers = false;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(doc, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				String idString = new Long(id).toString();
				
				//required
				doc.setID(id);
				
				// option
//				if(doc.getNumber() == 0) {
//					doc.setNumber(id);
//					updateNumbers = true;
//				}
//				if(AEStringUtil.isEmpty(doc.getNumberString())) {
//					doc.setNumberString(idString);
//					updateNumbers = true;
//				}
				if(doc.getRegNumber() == 0) {
					doc.setRegNumber(id);
					updateNumbers = true;
				}
				if(AEStringUtil.isEmpty(doc.getRegNumberString())) {
					doc.setRegNumberString(idString);
					updateNumbers = true;
				}
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// update numbers
			if(updateNumbers) {
				updateNumbers(doc);
			}
			
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

	private void updateNumbers(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLNumbers);

			// build statement
			int i = 1;
			ps.setLong(i++, doc.getNumber());
			ps.setString(i++, doc.getNumberString());
			ps.setLong(i++, doc.getRegNumber());
			ps.setString(i++, doc.getRegNumberString());

			// where id = ?
			ps.setLong(i++, doc.getID());

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

	public int build(AEDocument doc, PreparedStatement ps, int i) throws SQLException, AEException {
		// CLASS_ID
		ps.setLong(i++, doc.getClazz().getID());
		// CODE
		ps.setString(i++, doc.getCode());
		// NAME
		ps.setString(i++, doc.getName());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(doc.getTimeCreated()));
		// CREATOR
		ps.setString(i++, doc.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(doc.getTimeModified()));
		// MODIFIER
		ps.setString(i++, doc.getModifier());
		// PROPERTIES
		ps.setLong(i++, doc.getProperties());
		// DESCRIPTION
		ps.setString(i++, doc.getDescription());
		// NOTE
		ps.setString(i++, doc.getNote());
		// TYPE_ID
		ps.setLong(i++, doc.getType().getSystemID());
		// NUMBER
		ps.setLong(i++, doc.getNumber());
		// NIMBER_STRING
		ps.setString(i++, doc.getNumberString());
		// DOC_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getDate()));
		// REG_NUMBER
		ps.setLong(i++, doc.getRegNumber());
		// REG_NUMBER_STRING
		ps.setString(i++, doc.getRegNumberString());
		// REG_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(doc.getRegDate()));
		// OWNER_ID
		if(doc.getCompany() != null) {
			ps.setLong(i++, doc.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//TEMPLATE
		ps.setBoolean(i++, doc.isTemplate());
		//TEMPLATE_ID
		if(doc.getTemplate() != null) {
			ps.setLong(i++, doc.getTemplate().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		//JOURNAL_ID, JOURNAL_CODE
		if(doc.getJournal() != null) {
			ps.setLong(i++, doc.getJournal().getDescriptor().getID());
			ps.setString(i++, doc.getJournal().getDescriptor().getCode());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.NVARCHAR);
		}
		// BANK_ACC_ID
		if(doc.getBankAcc() != null) {
			ps.setLong(i++, doc.getBankAcc().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// CONTRIBUTOR_DONATION_ID
		if(doc.getContributorDonationDescr() != null) {
			ps.setLong(i++, doc.getContributorDonationDescr().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}

	public void build(AEDocument doc, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(doc, rs);

		doc.setType(AEDocumentType.valueOf(rs.getLong("TYPE_ID")));
		doc.setNumber(rs.getLong("NUMBER"));
		String number = rs.getString("NIMBER_STRING");
		if(rs.wasNull()) {
			doc.setNumberString(AEStringUtil.EMPTY_STRING);
		} else {
			doc.setNumberString(number);
		}
		doc.setDate(rs.getDate("DOC_DATE"));
		doc.setRegNumber(rs.getLong("REG_NUMBER"));
		doc.setRegNumberString(rs.getString("REG_NUMBER_STRING"));
		doc.setRegDate(rs.getDate("REG_DATE"));
		//TEMPLATE
		doc.setIsTemplate(rs.getBoolean("TEMPLATE"));
		//TEMPLATE_ID
		long templateId = rs.getLong("TEMPLATE_ID");
		if(!rs.wasNull()) {
			doc.setTemplate(new AEDescriptorImp(templateId));
		}
		//JOURNAL_ID, JOURNAL_CODE
		long journalId = rs.getLong("JOURNAL_ID");
		if(!rs.wasNull()) {
			AEDescriptorImp journal = new AEDescriptorImp(journalId);
			journal.setCode(rs.getString("JOURNAL_CODE"));
			doc.setJournal(journal);
		}
		// BANK_ACC_ID
		long bankId = rs.getLong("BANK_ACC_ID");
		if(!rs.wasNull()) {
			doc.setBankAcc(BankAccount.lazyDescriptor(bankId));
		}
		//VALIDATED
		doc.setValidated(rs.getBoolean("VALIDATED"));
		//VALIDATED_BY
		doc.setValidatedBy(rs.getString("VALIDATED_BY"));
		//VALIDATED_TIME
		doc.setValidatedTime(rs.getDate("VALIDATED_TIME"));
	}
	
	public void update(AEDocument doc) throws AEException {
		assert(doc != null);
		assert(doc.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(doc, ps, 1);
			ps.setLong(i++, doc.getID());

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
	
	public void onValidated(long docId, boolean validated, String validatedBy, Date validatedTime) throws AEException {
		assert(docId > 0);
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(onValidatedSQL);

			// build statement
			int i = 1;
			ps.setBoolean(i++, validated);
			if(validatedBy != null) {
				ps.setString(i++, validatedBy);
			} else {
				ps.setNull(i++, java.sql.Types.NVARCHAR);
			}
			if(validatedTime != null) {
				ps.setDate(i++, AEPersistentUtil.getSQLDate(validatedTime));
			} else {
				ps.setNull(i++, java.sql.Types.DATE);
			}
			ps.setLong(i++, docId);

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
	
	public List<Long> onValidated(List<Long> docIds, long ownerId, boolean validated, String validatedBy, Date validatedTime) throws AEException {
		assert(docIds != null);
		List<Long> res = new ArrayList<Long>();
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(onValidatedSQLExt);

			// multiple execution
			for (Long docId : docIds) {
				// build statement
				int i = 1;
				ps.setBoolean(i++, validated);
				if(validatedBy != null) {
					ps.setString(i++, validatedBy);
				} else {
					ps.setNull(i++, java.sql.Types.NVARCHAR);
				}
				if(validatedTime != null) {
					ps.setDate(i++, AEPersistentUtil.getSQLDate(validatedTime));
				} else {
					ps.setNull(i++, java.sql.Types.DATE);
				}
				ps.setLong(i++, docId);
				ps.setLong(i++, ownerId);

				// execute
				int count = ps.executeUpdate();
				if(count > 0) {
					res.add(docId);
				}
			}
			return res;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDocumentsList loadContratDeTravail(AEDocumentFilter filter) throws AEException {
		return null;
	}
	
	public AEDocumentsList loadSupplyTemplates(long ownerId) throws AEException {
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSupplyTemplates);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
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
	
	public AEDescriptorsList loadTemplatesDescr(long ownerId, AEDocumentType docType) throws AEException {
		AEDescriptorsList descrList = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTemplates);
			ps.setLong(1, ownerId);
			ps.setLong(2, docType.getSystemID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
				build(doc, rs);
				doc.setView();
				descrList.add(doc.getDescriptor());
			}
			return descrList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDescriptorsList loadDescriptors(AEDocumentFilter filter) throws AEException {
		AEDescriptorsList descrList = new AEDescriptorsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			/**
			 * Append filter
			 */
			StringBuffer sb = new StringBuffer(selectSQLByFilter);
			
			// document type
			if(filter.getDocType() != null) {
				sb
					.append(" and TYPE_ID = ")
					.append(filter.getDocType().getSystemID());
			};
			
			// date period
			if(filter.getDateFrom() != null) {
				sb
					.append(" and DOC_DATE >= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateFrom()));
			};
			
			if(filter.getDateTo() != null) {
				sb
					.append(" and DOC_DATE <= ")
					.append(AEPersistentUtil.escapeToDate(filter.getDateTo()));
			};
			
			// template
			if(filter.getTemplate() != null) {
				sb
					.append(" and TEMPLATE = ")
					.append(filter.getTemplate().booleanValue() == true ? 1 : 0);
			};
			
			/**
			 * Prepare statement and esecute
			 */
			ps = getAEConnection().prepareStatement(sb.toString());
			if(filter.getOwner() != null) {
				ps.setLong(1, filter.getOwner().getDescriptor().getID());
			} else {
				ps.setNull(1, java.sql.Types.BIGINT);
			}
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
				build(doc, rs);
				doc.setView();
				
				descrList.add(doc.getDescriptor());
			}
			return descrList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEDocumentsList loadSaleTemplates(long ownerId) throws AEException {
		AEDocumentsList docsList = new AEDocumentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLSaleTemplates);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
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
	
	public void delete(long docId) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);
			ps.setLong(1, docId);

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public final void updateToLocked(AEDocumentDescriptor docDescr) throws AEException {
		assert(docDescr != null);
		assert(docDescr.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateToLocked);

			// build statement
			ps.setLong(1, docDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public final void updateToNotLocked(AEDocumentDescriptor docDescr) throws AEException {
		assert(docDescr != null);
		assert(docDescr.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateToNotLocked);

			// build statement
			ps.setLong(1, docDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public final void ownershipValidation(long docId, long ownerId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(ownershipValidationSQL);
			ps.setLong(1, docId);
			ps.setLong(2, ownerId);
			rs = ps.executeQuery();
			if(!rs.next()) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public final Date getDocDate(long docId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		Date docDate = null;
		try {
			ps = getAEConnection().prepareStatement(getDocDateSQL);
			ps.setLong(1, docId);
			rs = ps.executeQuery();
			if(rs.next()) {
				docDate = rs.getDate("doc_date");
			}
			return docDate;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public boolean isLocked(long docId) throws AEException {
		boolean res = false;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(isLocked);
			ps.setLong(1, docId);
			rs = ps.executeQuery();
			if(rs.next()) {
				res = true;
			}
			return res;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public long countAll(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		return 0;
	}
	
	public long countValidated(AEDescriptor ownerDescr, AEDocumentType docType) throws AEException {
		return 0;
	}
}
