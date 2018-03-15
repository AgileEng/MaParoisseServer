/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 16:28:03
 */
package eu.agileeng.persistent.dao.file;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.file.FileAttachment;
import eu.agileeng.domain.file.FileAttachmentFilter;
import eu.agileeng.domain.file.FileAttachmentsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.util.AEStringUtil;

/**
 *
 */
public class FileAttachmentDAO extends AbstractDAO {

	private static String selectSQL = "select * from FileAttachment where id = ?";
	
	private static String deleteSQL = "delete from FileAttachment where id = ?";
	
	private static String deleteSQLTo = 
		"delete from FileAttachment where ATTACHED_TO_ID = ? AND ATTACHED_TO_CLASS_ID = ?";
	
	private static String insertSQL = "insert into FileAttachment "
		+ "(CLASS_ID, CODE, AE_OPTION, NAME, ALIAS, ABBREVATION, TIME_CREATED, CREATOR, TIME_MODIFIED, "
        + " MODIFIER, PROPERTIES, DESCRIPTION, NOTE, ATTACHED_TO_ID, ATTACHED_TO_CLASS_ID, LOCAL_PATH, "
        + " FILE_LENGTH, FILE_TYPE, FILE_LAST_MODIFIED, REMOTE_ROOT, REMOTE_PATH, REMOTE_PATH_OLD, ID_COMPANY, "
        + "SENDER_ID, RECIPIENT_ID) "
        + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, "
        + " ?, ?, ?, ?, ?, ?, ?, "
        + " ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update FileAttachment set"
		+ " CLASS_ID = ?, CODE = ?, AE_OPTION = ?, NAME = ?, ALIAS = ?, ABBREVATION = ?, TIME_CREATED = ?, "
		+ " CREATOR = ?, TIME_MODIFIED = ?, "
        + " MODIFIER = ?, PROPERTIES = ?, DESCRIPTION = ?, NOTE = ?, ATTACHED_TO_ID = ?, "
        + " ATTACHED_TO_CLASS_ID = ?, LOCAL_PATH = ?, "
        + " FILE_LENGTH = ?, FILE_TYPE = ?, FILE_LAST_MODIFIED = ?, REMOTE_ROOT = ?, "
        + " REMOTE_PATH = ?, REMOTE_PATH_OLD = ?, ID_COMPANY = ?, SENDER_ID = ?, RECIPIENT_ID = ? "
        + " where id = ?";
	
	private static String selectSQLTo = 
		"select * from FileAttachment where ATTACHED_TO_ID = ? and ATTACHED_TO_CLASS_ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public FileAttachmentDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public int build(FileAttachment fileAtt, PreparedStatement ps, int i) throws SQLException, AEException {
		// CLASS_ID
		ps.setLong(i++, fileAtt.getClazz().getID());
		// CODE
		ps.setString(i++, fileAtt.getCode());
		// AE_OPTION
		ps.setString(i++, fileAtt.getOption());
		// NAME
		ps.setString(i++, fileAtt.getName());
		// ALIAS
		ps.setString(i++, fileAtt.getAlias());
		// ABBREVATION
		ps.setString(i++, fileAtt.getAbbreviation());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(fileAtt.getTimeCreated()));
		// CREATOR
		ps.setString(i++, fileAtt.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(fileAtt.getTimeModified()));
		// MODIFIER
		ps.setString(i++, fileAtt.getModifier());
		// PROPERTIES
		ps.setLong(i++, fileAtt.getProperties());
		// DESCRIPTION
		ps.setString(i++, fileAtt.getDescription());
		// NOTE
		ps.setString(i++, fileAtt.getNote());
		// ATTACHED_TO_ID, ATTACHED_TO_CLASS_ID
		AEDescriptive attachedTo = fileAtt.getAttachedTo();
		if(attachedTo != null) {
			ps.setLong(i++, attachedTo.getDescriptor().getID());
			ps.setLong(i++, attachedTo.getDescriptor().getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.BIGINT);
		}
		// LOCAL_PATH
		ps.setString(i++, fileAtt.getLocalPath());
        // FILE_LENGTH
		ps.setLong(i++, fileAtt.getFileLength());
		// FILE_TYPE
		ps.setString(i++, fileAtt.getFileType());
		// FILE_LAST_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestamp(fileAtt.getFileLastModified()));
		// REMOTE_ROOT
		ps.setString(i++, fileAtt.getRemoteRoot());
		// REMOTE_PATH
		ps.setString(i++, fileAtt.getRemotePath());
		// REMOTE_PATH_OLD
		ps.setString(i++, fileAtt.getOldRemotePath());
		// ID_COMPANY
		if(fileAtt.getCompany() != null) {
			ps.setLong(i++, fileAtt.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// SENDER_ID
		AEDescriptive sender = fileAtt.getSender();
		if(sender != null) {
			ps.setLong(i++, sender.getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// RECIPIENT_ID
		AEDescriptive recipient = fileAtt.getRecipient();
		if(recipient != null) {
			ps.setLong(i++, recipient.getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}

	public void build(FileAttachment fileAtt, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(fileAtt, rs);

		// AE_OPTION
		fileAtt.setOption(rs.getString("AE_OPTION"));
		// ALIAS
		fileAtt.setAlias(rs.getString("ALIAS"));
		// ABBREVATION
		fileAtt.setAbbreviation(rs.getString("ABBREVATION"));
		// ATTACHED_TO_ID, ATTACHED_TO_CLASS_ID
		AEDescriptor attachedToDescr = new AEDescriptorImp(
				rs.getLong("ATTACHED_TO_ID"),
				DomainClass.valueOf(rs.getLong("ATTACHED_TO_CLASS_ID")));
		fileAtt.setAttachedTo(attachedToDescr);
		// LOCAL_PATH
		fileAtt.setLocalPath(rs.getString("LOCAL_PATH"));
        // FILE_LENGTH
		fileAtt.setFileLength(rs.getLong("FILE_LENGTH"));
		// FILE_TYPE
		fileAtt.setFileType(rs.getString("FILE_TYPE"));
		// FILE_LAST_MODIFIED
		fileAtt.setFileLastModified(rs.getTimestamp("FILE_LAST_MODIFIED"));
		// REMOTE_ROOT
		fileAtt.setRemoteRoot(rs.getString("REMOTE_ROOT"));
		// REMOTE_PATH
		fileAtt.setRemotePath(rs.getString("REMOTE_PATH"));
		// REMOTE_PATH_OLD
		fileAtt.setOldRemotePath(rs.getString("REMOTE_PATH_OLD"));
		// SENDER_ID
		fileAtt.setSender(AuthPrincipal.lazyDescriptor(rs.getLong("SENDER_ID")));
		// RECIPIENT_ID
		fileAtt.setRecipient(AuthPrincipal.lazyDescriptor(rs.getLong("RECIPIENT_ID")));
	}
	
	public void insert(FileAttachment fileAtt) throws AEException {
		assert(!fileAtt.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(fileAtt, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				fileAtt.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			fileAtt.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(FileAttachment fileAtt) throws AEException {
		assert(fileAtt != null);
		assert(fileAtt.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(fileAtt, ps, 1);
			ps.setLong(i++, fileAtt.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			fileAtt.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
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
			ps.setLong(1, id);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteTo(AEDescriptor aeDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLTo);
			ps.setLong(1, aeDescr.getID());
			ps.setLong(2, aeDescr.getClazz().getID());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String deleteSQLTenantSafety = "delete from FileAttachment where id = ? and ID_COMPANY = ?";
	public long delete(AEDescriptor attDescr, AEDescriptor tenantDescr) throws AEException {
		long cnt = 0L;
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLTenantSafety);
			ps.setLong(1, attDescr.getID());
			ps.setLong(2, tenantDescr.getID());
			cnt = ps.executeUpdate();
			return cnt;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public FileAttachment load(AEDescriptor fileAttDescr) throws AEException {
		FileAttachment fileAtt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, fileAttDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				fileAtt = new FileAttachment();
				build(fileAtt, rs);
				fileAtt.setView();
			}
			return fileAtt;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public FileAttachment loadTo(AEDescriptor objDescr) throws AEException {
		FileAttachment fileAtt = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLTo);
			ps.setLong(1, objDescr.getID());
			ps.setLong(2, objDescr.getClazz().getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				fileAtt = new FileAttachment();
				build(fileAtt, rs);
				fileAtt.setView();
			}
			return fileAtt;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String selectSQLToToMultipleLazzy = 
			"select * from FileAttachment where ATTACHED_TO_CLASS_ID = " + Long.toString(DomainClass.AccJournalEntry.getID()) + " and ATTACHED_TO_ID in  ";
	public JSONArray loadToMultipleLazzy(AEDescriptorsList descrList) throws AEException {
		JSONArray fileAttachments = new JSONArray();
		Statement s = null;
		ResultSet rs = null;
		try {
			// don't execute this query if descrList is null or empty
			if(descrList != null && !descrList.isEmpty()) {
				s = getAEConnection().createStatement();
				rs = s.executeQuery(selectSQLToToMultipleLazzy + AEPersistentUtil.createInClause(descrList));
				while(rs.next()) {
					JSONObject fileAtt = new JSONObject();

					String root = rs.getString("REMOTE_ROOT");
					String path = rs.getString("REMOTE_PATH");
					Path nioPath = Paths.get(root, path);

					fileAtt.put("entryId", rs.getLong("ATTACHED_TO_ID"));
					fileAtt.put("attId", rs.getLong("id"));
					fileAtt.put("remoteName", nioPath.toString());
					fileAtt.put("name", nioPath.getFileName().toString());

					fileAttachments.put(fileAtt);
				}
			}
			return fileAttachments;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(s);
			close();
		}
	}
	
	public FileAttachmentsList load(FileAttachmentFilter filter) throws AEException {
		FileAttachmentsList fileAttList = new FileAttachmentsList();
		Statement stmt = null;
		ResultSet rs = null;
		try {
			stmt = getAEConnection().createStatement();
			rs = stmt.executeQuery(buildQuery(filter));
			while(rs.next()) {
				FileAttachment fileAtt = new FileAttachment();
				build(fileAtt, rs);
				fileAtt.setView();
				fileAttList.add(fileAtt);
			}
			return fileAttList;
		} catch (Exception e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(stmt);
			close();
		}
	}
	
	private String buildQuery(FileAttachmentFilter filter) {
		StringBuffer query = new StringBuffer("select * from FileAttachment where id_company = ");
		query.append(Long.toString(filter.getAuthSubject().getCompany().getDescriptor().getID()));
		switch(filter.getFolder()) {
			case all:
				break;
			case inbox:
				query.append(" and ").append("recipient_id").append(" = ").append(
						filter.getAuthSubject().getPerson().getDescriptor().getID());
				break;
			case sent:
				query.append(" and ").append("sender_id").append(" = ").append(
						filter.getAuthSubject().getPerson().getDescriptor().getID());
				break;
		}
		return query.toString();
	}
	
//	private static String selectSQLByTenant = 
//			"select fa.*, ft.DESCRIPTION  as ATTACHED_TO_DESCR from FileAttachment fa "
//			+ " inner join FinancialTransaction ft on "
//			+ " fa.ATTACHED_TO_CLASS_ID = ? and fa.ATTACHED_TO_ID = ft.ID and fa.ID_COMPANY = ? "
//			+ " union all " 
//			+ " select fa.*, doc.DESCRIPTION as ATTACHED_TO_DESCR from FileAttachment fa "
//			+ " inner join Document doc on fa.ATTACHED_TO_CLASS_ID = ? and fa.ATTACHED_TO_ID = doc.ID and fa.ID_COMPANY = ? "
//			+ " where doc.TYPE_ID != " + AEDocumentType.System.BordereauParoisse.getSystemID();
		private static String selectSQLFTAttachmentsByTenant = 
			"select fa.*, ft.DESCRIPTION  as ATTACHED_TO_DESCR from FileAttachment fa "
			+ " inner join FinancialTransaction ft on "
			+ " fa.ATTACHED_TO_CLASS_ID = ? and fa.ATTACHED_TO_ID = ft.ID and fa.ID_COMPANY = ? ";
		public FileAttachmentsList listFTAttachments(FileAttachmentFilter filter) throws AEException {
			FileAttachmentsList fileAttList = new FileAttachmentsList();
			PreparedStatement ps = null;
			ResultSet rs = null;
			try {
				String sql = selectSQLFTAttachmentsByTenant;
				// build query
				
				ps = getAEConnection().prepareStatement(sql);
				ps.setLong(1, DomainClass.FinancialTransaction.getID());
				ps.setLong(2, filter.getTenant().getDescriptor().getID());
				rs = ps.executeQuery();
				while(rs.next()) {
					FileAttachment fileAtt = new FileAttachment();
					build(fileAtt, rs);
					fileAtt.setDescription(rs.getString("ATTACHED_TO_DESCR"));
					fileAtt.setView();
					fileAttList.add(fileAtt);
				}
				return fileAttList;
			} catch (Exception e) {
				throw new AEException(e);
			} finally {
				AEConnection.close(rs);
				AEConnection.close(ps);
				close();
			}
		}
	
	private static String selectSQLDocAttachmentsByTenant = 
			"select fa.*, doc.DESCRIPTION as ATTACHED_TO_DESCR from FileAttachment fa "
			+ " inner join Document doc on fa.ATTACHED_TO_CLASS_ID = ? and fa.ATTACHED_TO_ID = doc.ID and fa.ID_COMPANY = ? ";
	public FileAttachmentsList listDocAttachments(FileAttachmentFilter filter) throws AEException {
		FileAttachmentsList fileAttList = new FileAttachmentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			String sql = selectSQLDocAttachmentsByTenant;

			// build where part
			StringBuilder sqlWhere = new StringBuilder();
			List<AEDocumentType> docTypes = filter.getDocTypes();
			if(docTypes != null && !docTypes.isEmpty()) {
				if(AEStringUtil.isEmpty(sqlWhere)) {
					sqlWhere.append(" where ");
				}
				int i = 0;
				for (AEDocumentType aeDocumentType : docTypes) {
					if(i == 0) {
						sqlWhere.append(" (doc.TYPE_ID = ").append(aeDocumentType.getSystemID());
					} else {
						sqlWhere.append(" or doc.TYPE_ID = ").append(aeDocumentType.getSystemID());
					}
					i++;
				}
				sqlWhere.append(" ) ");
			}
			
			sql += sqlWhere.toString();
			ps = getAEConnection().prepareStatement(sql);
			ps.setLong(1, DomainClass.AeDocument.getID());
			ps.setLong(2, filter.getTenant().getDescriptor().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				FileAttachment fileAtt = new FileAttachment();
				build(fileAtt, rs);
				fileAtt.setDescription(rs.getString("ATTACHED_TO_DESCR"));
				fileAtt.setView();
				fileAttList.add(fileAtt);
			}
			return fileAttList;
		} catch (Exception e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
//	public FileAttachmentsList listDocAttachments≈xcluding(FileAttachmentFilter filter) throws AEException {
//		FileAttachmentsList fileAttList = new FileAttachmentsList();
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			String sql = selectSQLDocAttachmentsByTenant;
//			
//			// build query
//			if(filter.getDocType() != null) {
//				sql += " where doc.TYPE_ID != " + filter.getDocType().getSystemID();
//			}
//			
//			ps = getAEConnection().prepareStatement(sql);
//			ps.setLong(1, DomainClass.AeDocument.getID());
//			ps.setLong(2, filter.getTenant().getDescriptor().getID());
//			rs = ps.executeQuery();
//			while(rs.next()) {
//				FileAttachment fileAtt = new FileAttachment();
//				build(fileAtt, rs);
//				fileAtt.setDescription(rs.getString("ATTACHED_TO_DESCR"));
//				fileAtt.setView();
//				fileAttList.add(fileAtt);
//			}
//			return fileAttList;
//		} catch (Exception e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}
//	}
}
