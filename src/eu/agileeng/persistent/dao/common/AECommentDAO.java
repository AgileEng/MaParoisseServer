/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 13.06.2010 22:29:35
 */
package eu.agileeng.persistent.dao.common;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.common.AEComment;
import eu.agileeng.domain.common.AECommentsList;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;

/**
 *
 */
public class AECommentDAO extends AbstractDAO {

	private static String selectSQL = "select * from Comment where id = ?";
	
	private static String selectSQLToObj = 
		"select * from Comment where TO_OBJ_ID = ? and TO_CLASS_ID = ?";
	
	private static String insertSQL = "insert into Comment (NAME, CODE, TIME_CREATED, CREATOR, "
		+ "TIME_MODIFIED, MODIFIER, PROPERTIES, CLASS_ID, DESCRIPTION, NOTE, "
		+ "TO_OBJ_ID, TO_CLASS_ID) values ("
		+ "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update party set NAME = ?, CODE = ?, TIME_CREATED = ?, CREATOR = ?, "
		+ " TIME_MODIFIED = ?, MODIFIER = ?, PROPERTIES = ?, CLASS_ID = ?, DESCRIPTION = ?, NOTE = ?, "
		+ "TO_OBJ_ID = ?, TO_CLASS_ID = ?"
		+ " where ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AECommentDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public int build(AEComment comment, PreparedStatement ps, int i) throws SQLException, AEException {
		// NAME
		ps.setString(i++, comment.getName());
		// CODE
		ps.setString(i++, comment.getCode());
		// TIME_CREATED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(comment.getTimeCreated()));
		// CREATOR
		ps.setString(i++, comment.getCreator());
		// TIME_MODIFIED
		ps.setTimestamp(i++, AEPersistentUtil.getTimestampNotNull(comment.getTimeModified()));
		// MODIFIER
		ps.setString(i++, comment.getModifier());
		// PROPERTIES
		ps.setLong(i++, comment.getProperties());
		// CLASS_ID
		if(comment.getClazz() != null) {
			ps.setLong(i++, comment.getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		// DESCRIPTION
		ps.setString(i++, comment.getDescription());
		// NOTE
		ps.setString(i++, comment.getNote());
		// TO_OBJ_ID, TO_CLASS_ID
		AEDescriptive toObj = comment.getToObject();
		if(toObj != null) {
			ps.setLong(i++, toObj.getDescriptor().getID());
			ps.setLong(i++, toObj.getDescriptor().getClazz().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
			ps.setNull(i++, Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}

	public void build(AEComment comment, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(comment, rs);

		// TO_OBJ_ID, TO_CLASS_ID
		AEDescriptor toObjectDescr = new AEDescriptorImp(
				rs.getLong("TO_OBJ_ID"),
				DomainClass.valueOf(rs.getLong("TO_CLASS_ID")));
		comment.setToObject(toObjectDescr);
	}
	
	public void insert(AEComment comment) throws AEException {
		assert(!comment.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(comment, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				comment.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			comment.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEComment comment) throws AEException {
		assert(comment != null);
		assert(comment.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(comment, ps, 1);
			ps.setLong(i++, comment.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			comment.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEComment load(AEDescriptor commentDescr) throws AEException {
		AEComment comment = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, commentDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				comment = new AEComment();
				build(comment, rs);
				comment.setView();
			}
			return comment;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AECommentsList loadToObj(AEDescriptor toObjDescr) throws AEException {
		AECommentsList commentsList = new AECommentsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLToObj);
			ps.setLong(1, toObjDescr.getID());
			ps.setLong(2, toObjDescr.getClazz().getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEComment comment = new AEComment();
				build(comment, rs);
				comment.setView();
				commentsList.add(comment);
			}
			return commentsList;
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
