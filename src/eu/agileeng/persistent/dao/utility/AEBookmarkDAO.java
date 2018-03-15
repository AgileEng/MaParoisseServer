package eu.agileeng.persistent.dao.utility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.bookmark.AEBookmark;
import eu.agileeng.domain.bookmark.AEBookmarksList;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class AEBookmarkDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from Bookmark where OWNER_ID = ?";
	
	private static String insertSQL = 
		"insert into Bookmark "
		+ "(OWNER_ID, GROUP_NAME, CODE, NAME, URL, DESCRIPTION, PROPERTIES) "
		+ " values (?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = 
		"update Bookmark set "
		+ " OWNER_ID = ?, GROUP_NAME = ?, CODE = ?, NAME = ?, "
		+ " URL = ?, DESCRIPTION = ?, PROPERTIES = ? "
		+ " where id = ?";
	
	private static String deleteSQL = 
		"delete from Bookmark where id = ?";
	
	public AEBookmarkDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public AEBookmarksList load(AEDescriptor ownerDescr) throws AEException {
		AEBookmarksList bookmarks = new AEBookmarksList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEBookmark b = new AEBookmark();
				build(b, rs);
				b.setView();
				bookmarks.add(b);
			}
			return bookmarks;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	} 
	
	private void build(AEBookmark bookmark, ResultSet rs) throws SQLException, AEException {
		// ID
		bookmark.setID(rs.getLong("ID"));
		// OWNER_ID
		bookmark.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		// GROUP_NAME
		bookmark.setGroup(rs.getString("GROUP_NAME"));
		// CODE
		bookmark.setCode(rs.getString("CODE"));
		// NAME
		bookmark.setName(rs.getString("NAME"));
		// URL
		bookmark.setUrl(rs.getString("URL"));
		// DESCRIPTION
		bookmark.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		bookmark.setProperties(rs.getLong("PROPERTIES"));
	}
	
	public int build(AEBookmark bookmark, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(bookmark.getCompany() != null) {
			ps.setLong(i++, bookmark.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		// GROUP_NAME
		ps.setString(i++, bookmark.getGroup());
		// CODE
		ps.setString(i++, bookmark.getCode());
		// NAME
		ps.setString(i++, bookmark.getName());
		// URL
		ps.setString(i++, bookmark.getUrl());
		// DESCRIPTION
		ps.setString(i++, bookmark.getDescription());
		// PROPERTIES
		ps.setLong(i++, bookmark.getProperties());
		
		// return the current ps position 
		return i;
	}
	
	public void insert(AEBookmark bookmark) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			build(bookmark, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				bookmark.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			bookmark.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEBookmark bookmark) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(bookmark, ps, 1);
			ps.setLong(i, bookmark.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			bookmark.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public void delete(AEDescriptor bookmarkDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQL);

			// build statement
			ps.setLong(1, bookmarkDescr.getID());

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
