/**
 * 
 */
package eu.agileeng.persistent.dao.pam;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEDescriptive;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCModel;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEValue;

/**
 * @author vvatov
 *
 */
public class PAMDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from PAM where owner_id = ?";
	
	private static String insertSQL = "insert into PAM "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION) "
		+ " values (?, ?, ?, ?)";
	
	private static String updateSQL = "update PAM "
		+ "set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ? "
		+ "where id = ?";
	
	private static String selectSQLColumn = 
		"select * from PAMModel where pam_id = ? order by pam_id asc, col_index asc";
	
	private static String insertSQLColumn = "insert into pamModel "
		+ "(pam_ID, COL_INDEX, NTYPE, CODE, NAME, DESCRIPTION, XTYPE, VALUE, HIDDEN, ACC_BANK_ID, ACC_ID, ACC_ENTRY_TYPE) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLColumn = "update pamModel "
		+ "set pam_ID = ?, COL_INDEX = ?, NTYPE = ?, CODE = ?, NAME = ?, " 
		+ "DESCRIPTION = ?, XTYPE = ?, VALUE = ?, HIDDEN = ?, ACC_BANK_ID = ?, ACC_ID = ?, ACC_ENTRY_TYPE = ? where id = ?";
	
	private static String deleteSQLColumn = 
		"delete from pamModel where id = ?";
	
	private static String selectSQLCell = 
		"select * from pamCell where pam_id = ? and CELL_DATE >= ? and CELL_DATE <= ?";
	
	private static String insertSQLCell = "insert into pamCell "
		+ "(pam_ID, CELL_DATE, ROW_INDEX, COL_ID, XTYPE, VALUE) "
		+ " values (?, ?, ?, ?, ?, ?)";
	
//	private static String updateSQLCell = "update pamModel "
//		+ "set pam_ID = ?, CELL_DATE = ?, ROW_INDEX = ?, COL_ID = ?, XTYPE = ?, VALUE = ? where id = ?";
	
	private static String deleteSQLCell = 
		"delete from pampam where id = ?";
	
	private static String selectSQLColumnByOwnerAndIndex = 
		"select pamModel.* from pamModel pamModel inner join pam pam on pamModel.pam_ID = pam.ID " 
		+ " where pam.OWNER_ID = ? and pamModel.COL_INDEX = ?";
	
	private static String deleteSQLCellValue = 
		"delete from pamCell where pam_ID = ? and COL_ID = ? and CELL_DATE = ?";
	
	private static String unpaidPAMCells = 
		"select cell.*, model.NAME colName from " 
		+ "PAMCell cell inner join PAMModel model on cell.COL_ID = model.ID "
		+ "inner join PAM pam on cell.MANDAT_ID = pam.ID "
		+ "where pam.OWNER_ID = ? and model.ACC_BANK_ID IS NOT NULL and cell.PAID = 0";
	
	private static String payablePAMCell = 
		"select cell.*, model.NAME colName, model.ACC_BANK_ID accId from "
		+ "PAMCell cell inner join PAMModel model on cell.COL_ID = model.ID where cell.ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public PAMDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public CFCModel loadPAMModel(AEDescriptor pamDescr) throws AEException, JSONException {
		CFCModel pamModel = new CFCModel();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumn);
			ps.setLong(1, pamDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCColumn pamColumn = new CFCColumn();
				buildPAMColumn(pamColumn, rs);
				pamColumn.setView();
				pamModel.add(pamColumn);
			}
			return pamModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFC loadPAM(AEDescriptor ownerDescr) throws AEException, JSONException {
		CFC pam = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				pam = new CFC();
				buildPAM(pam, rs);
				pam.setView();
			}
			return pam;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildPAMColumn(CFCColumn pamColumn, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		pamColumn.setID(rs.getLong("ID"));
		
		// pam_ID
		pamColumn.setToCFC(CFC.lazyDescriptor(rs.getLong("pam_ID")));
		
		// COL_INDEX
		pamColumn.setColIndex(rs.getInt("COL_INDEX"));
		
		// NTYPE
		try {
			pamColumn.setNType(CFCColumn.NType.valueOf(rs.getString("NTYPE")));
		} catch(Exception e) {
		}
		
		// CODE
		pamColumn.setCode(rs.getString("CODE"));
		
		// NAME
		pamColumn.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		pamColumn.setDescription(rs.getString("DESCRIPTION"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			pamColumn.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// HIDDEN
		pamColumn.setHidden(rs.getBoolean("HIDDEN"));
		
		// ACC_BANK_ID
		long accBankId = rs.getLong("ACC_BANK_ID");
		if(!rs.wasNull()) {
			AEDescriptive accBank = AccAccount.lazyDescriptor(accBankId);
			pamColumn.setAccBank(accBank);
		}
		
		// ACC_ID
		long accAccountId = rs.getLong("ACC_ID");
		if(!rs.wasNull()) {
			AEDescriptive accAccount = AccAccount.lazyDescriptor(accAccountId);
			pamColumn.setAccAccount(accAccount);
		}
		
		// ACC_ENTRY_TYPE
		try {
			pamColumn.setAccEntryType(CFCColumn.AType.valueOf(rs.getString("ACC_ENTRY_TYPE")));
		} catch (Exception e) {
		}
	}
	
	private void buildPAM(CFC pam, ResultSet rs) throws SQLException {
		// ID
		pam.setID(rs.getLong("ID"));
		
		// OWNER_ID
		pam.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// CODE
		pam.setCode(rs.getString("CODE"));
		
		// NAME
		pam.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		pam.setDescription(rs.getString("DESCRIPTION"));
	}
	
	public void insertPAMColumn(CFCColumn pamColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLColumn);
			buildPAMColumn(pamColumn, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				pamColumn.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			pamColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertPAM(CFC pam) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildPAM(pam, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				pam.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			pam.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updatePAM(CFC pam) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = buildPAM(pam, ps, 1);
			ps.setLong(i++, pam.getID());

			// execute
			ps.executeUpdate();

			// set view state
			pam.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updatePAMColumn(CFCColumn pamColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLColumn);
			int i = buildPAMColumn(pamColumn, ps, 1);
			ps.setLong(i++, pamColumn.getID());

			// execute
			ps.executeUpdate();

			// set view state
			pamColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deletePAMColumn(AEDescriptor pamColumnDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLColumn);
			ps.setLong(1, pamColumnDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildPAMColumn(CFCColumn pamColumn, PreparedStatement ps, int i) throws SQLException, AEException {
		// pam_ID
		if(pamColumn.getToCFC() != null) {
			ps.setLong(i++, pamColumn.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// COL_INDEX
		ps.setInt(i++, pamColumn.getColIndex());
		
		// NTYPE
		if(pamColumn.getNType() != null) {
			ps.setString(i++, pamColumn.getNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// CODE
		ps.setString(i++, pamColumn.getCode());
		
		// NAME
		ps.setString(i++, pamColumn.getName());
		
		// DESCRIPTION
		ps.setString(i++, pamColumn.getDescription());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(pamColumn.getValue())) {
			ps.setString(i++, pamColumn.getValue().getXType().toString());
			ps.setString(i++, pamColumn.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// HIDDEN
		ps.setBoolean(i++, pamColumn.isHidden());
		
		// ACC_BANK_ID
		if(pamColumn.getAccBank() != null) {
			ps.setLong(i++, pamColumn.getAccBank().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACC_ID
		if(pamColumn.getAccAccount() != null) {
			ps.setLong(i++, pamColumn.getAccAccount().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// ACC_ENTRY_TYPE
		if(pamColumn.getAccEntryType() != null) {
			ps.setString(i++, pamColumn.getAccEntryType().name());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// return the current ps position 
		return i;
	}
	
	private int buildPAM(CFC pam, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(pam.getCompany() != null) {
			ps.setLong(i++, pam.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		ps.setString(i++, pam.getCode());
		
		// NAME
		ps.setString(i++, pam.getName());
		
		// DESCRIPTION
		ps.setString(i++, pam.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public ArrayList<CFCCell> loadPAMCells(AEDescriptor pamDescr, Date startDate, Date endDate) throws AEException, JSONException {
		ArrayList<CFCCell> pamCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCell);
			ps.setLong(1, pamDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell pamCell = new CFCCell();
				buildPAMCell(pamCell, rs);
				pamCell.setView();
				pamCells.add(pamCell);
			}
			return pamCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildPAMCell(CFCCell pamCell, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		pamCell.setID(rs.getLong("ID"));
		
		// pam_ID
		pamCell.setToCFC(CFC.lazyDescriptor(rs.getLong("pam_ID")));
		
		// CELL_DATE
		pamCell.setEntryDate(rs.getDate("CELL_DATE"));
		
		// ROW_INDEX
		pamCell.setRowIndex(rs.getInt("ROW_INDEX"));
		
		// COL_ID
		pamCell.setColumnId(rs.getLong("COL_ID"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			pamCell.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// PAID
		pamCell.setPaid(rs.getBoolean("PAID"));
	}
	
	public void insertPAMCell(CFCCell pamCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCell);
			buildPAMCell(pamCell, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				pamCell.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			pamCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildPAMCell(CFCCell pamCell, PreparedStatement ps, int i) throws SQLException, AEException {
		// pam_ID
		if(pamCell.getToCFC() != null) {
			ps.setLong(i++, pamCell.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CELL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(pamCell.getEntryDate()));
		
		// ROW_INDEX
		ps.setInt(i++, pamCell.getRowIndex());
		
		// COL_ID
		ps.setLong(i++, pamCell.getColumnId());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(pamCell.getValue())) {
			ps.setString(i++, pamCell.getValue().getXType().toString());
			ps.setString(i++, pamCell.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void deletepamCell(AEDescriptor pamCellDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCell);
			ps.setLong(1, pamCellDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
//	public void updatePAMCell(CFCCell pamCell) throws AEException {
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			// prepare statement and insert
//			ps = getAEConnection().prepareStatement(updateSQLCell);
//			int i = buildPAMCell(pamCell, ps, 1);
//			ps.setLong(i++, pamCell.getID());
//
//			// execute
//			ps.executeUpdate();
//
//			// set view state
//			pamCell.setView();
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(ps);
//			AEConnection.close(rs);
//			close();
//		}
//	}
	
	public CFCColumn loadColumnByOwnerAndIndex(long ownerId, int colIndex) throws AEException, JSONException {
		CFCColumn pamColumn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndIndex);
			ps.setLong(1, ownerId);
			ps.setInt(2, colIndex);
			rs = ps.executeQuery();
			if(rs.next()) {
				pamColumn = new CFCColumn();
				buildPAMColumn(pamColumn, rs);
				pamColumn.setView();
			}
			return pamColumn;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell updatePAMCellValue(CFCCell pamCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// delete old entry
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCellValue);
			ps.setLong(1, pamCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, pamCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(pamCell.getEntryDate()));
			ps.executeUpdate();
			
			// insert the new one
			insertPAMCell(pamCell);
			
			return pamCell;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public ArrayList<CFCCell> loadUnpaidPAMCells(long ownerId) throws AEException, JSONException {
		ArrayList<CFCCell> pamCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(unpaidPAMCells);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell pamCell = new CFCCell();
				pamCell.setSysId(AEDocumentType.System.MANDAT.getSystemID());
				buildPAMCell(pamCell, rs);
				pamCell.setName(rs.getString("colName"));
				pamCell.setView();
				pamCells.add(pamCell);
			}
			return pamCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell loadPayableItem(AEDescriptor pamCellDescr) throws AEException, JSONException {
		CFCCell pamCell = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(payablePAMCell);
			ps.setLong(1, pamCellDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				pamCell = new CFCCell();
				buildPAMCell(pamCell, rs);
				pamCell.setName(rs.getString("colName"));
				long accId = rs.getLong("accId");
				if(!rs.wasNull()) {
					pamCell.setAccBank(AccAccount.lazyDescriptor(accId));
				}
				pamCell.setView();
			}
			return pamCell;
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
			ps = getAEConnection().prepareStatement("update PAMCell set paid = ? where ID = ?");

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
