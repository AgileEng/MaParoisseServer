/**
 * 
 */
package eu.agileeng.persistent.dao.cash;

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
public class CFCDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from CFC where owner_id = ?";
	
	private static String insertSQL = "insert into CFC "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION) "
		+ " values (?, ?, ?, ?)";
	
	private static String updateSQL = "update CFC "
		+ "set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ? "
		+ "where id = ?";
	
	private static String selectSQLColumn = 
		"select * from CFCModel where cfc_id = ? order by cfc_id asc, col_index asc";
	
	private static String insertSQLColumn = "insert into CFCModel "
		+ "(CFC_ID, COL_INDEX, NTYPE, CODE, NAME, DESCRIPTION, XTYPE, VALUE, HIDDEN, ACC_BANK_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLColumn = "update CFCModel "
		+ "set CFC_ID = ?, COL_INDEX = ?, NTYPE = ?, CODE = ?, NAME = ?, " 
		+ "DESCRIPTION = ?, XTYPE = ?, VALUE = ?, HIDDEN = ?, ACC_BANK_ID = ? where id = ?";
	
	private static String deleteSQLColumn = 
		"delete from CFCModel where id = ?";
	
	private static String selectSQLCell = 
		"select * from CFCCell where cfc_id = ? and CELL_DATE >= ? and CELL_DATE <= ?";
	
	private static String insertSQLCell = "insert into CFCCell "
		+ "(CFC_ID, CELL_DATE, ROW_INDEX, COL_ID, XTYPE, VALUE) "
		+ " values (?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLCell = "update CFCModel "
		+ "set CFC_ID = ?, CELL_DATE = ?, ROW_INDEX = ?, COL_ID = ?, XTYPE = ?, VALUE = ? where id = ?";
	
	private static String deleteSQLCell = 
		"delete from CFCCFC where id = ?";
	
	private static String selectSQLColumnByOwnerAndIndex = 
		"select cfcModel.* from CFCModel cfcModel inner join CFC cfc on cfcModel.CFC_ID = cfc.ID " 
		+ " where cfc.OWNER_ID = ? and cfcModel.COL_INDEX = ?";
	
	private static String deleteSQLCellValue = 
		"delete from CFCCell where CFC_ID = ? and COL_ID = ? and CELL_DATE = ?";
	
	private static String unpaidCFCCells = 
		"select cell.*, model.NAME colName from " 
		+ "CFCCell cell inner join CFCModel model on cell.COL_ID = model.ID "
		+ "inner join CFC cfc on cell.CFC_ID = cfc.ID "
		+ "where cfc.OWNER_ID = ? and model.ACC_BANK_ID IS NOT NULL and cell.PAID = 0";
	
	private static String payableCFCCell = 
		"select cell.*, model.NAME colName, model.ACC_BANK_ID accId from "
		+ "CFCCell cell inner join CFCModel model on cell.COL_ID = model.ID where cell.ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public CFCDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public CFCModel loadCFCModel(AEDescriptor cfcDescr) throws AEException, JSONException {
		CFCModel cfcModel = new CFCModel();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumn);
			ps.setLong(1, cfcDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCColumn cfcColumn = new CFCColumn();
				buildCFCColumn(cfcColumn, rs);
				cfcColumn.setView();
				cfcModel.add(cfcColumn);
			}
			return cfcModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFC loadCFC(AEDescriptor ownerDescr) throws AEException, JSONException {
		CFC cfc = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				cfc = new CFC();
				buildCFC(cfc, rs);
				cfc.setView();
			}
			return cfc;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildCFCColumn(CFCColumn cfcColumn, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		cfcColumn.setID(rs.getLong("ID"));
		
		// CFC_ID
		cfcColumn.setToCFC(CFC.lazyDescriptor(rs.getLong("CFC_ID")));
		
		// COL_INDEX
		cfcColumn.setColIndex(rs.getInt("COL_INDEX"));
		
		// NTYPE
		cfcColumn.setNType(CFCColumn.NType.valueOf(rs.getString("NTYPE")));
		
		// CODE
		cfcColumn.setCode(rs.getString("CODE"));
		
		// NAME
		cfcColumn.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		cfcColumn.setDescription(rs.getString("DESCRIPTION"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			cfcColumn.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// HIDDEN
		cfcColumn.setHidden(rs.getBoolean("HIDDEN"));
		
		// ACC_BANK_ID
		long accBankId = rs.getLong("ACC_BANK_ID");
		if(!rs.wasNull()) {
			AEDescriptive accBank = AccAccount.lazyDescriptor(accBankId);
			cfcColumn.setAccBank(accBank);
		}
	}
	
	private void buildCFC(CFC cfc, ResultSet rs) throws SQLException {
		// ID
		cfc.setID(rs.getLong("ID"));
		
		// OWNER_ID
		cfc.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// CODE
		cfc.setCode(rs.getString("CODE"));
		
		// NAME
		cfc.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		cfc.setDescription(rs.getString("DESCRIPTION"));
	}
	
	public void insertCFCColumn(CFCColumn cfcColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLColumn);
			buildCFCColumn(cfcColumn, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				cfcColumn.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			cfcColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertCFC(CFC cfc) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildCFC(cfc, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				cfc.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			cfc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateCFC(CFC cfc) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = buildCFC(cfc, ps, 1);
			ps.setLong(i++, cfc.getID());

			// execute
			ps.executeUpdate();

			// set view state
			cfc.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateCFCColumn(CFCColumn cfcColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLColumn);
			int i = buildCFCColumn(cfcColumn, ps, 1);
			ps.setLong(i++, cfcColumn.getID());

			// execute
			ps.executeUpdate();

			// set view state
			cfcColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteCFCColumn(AEDescriptor cfcColumnDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLColumn);
			ps.setLong(1, cfcColumnDescr.getID());

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
	
	private int buildCFCColumn(CFCColumn cfcColumn, PreparedStatement ps, int i) throws SQLException, AEException {
		// CFC_ID
		if(cfcColumn.getToCFC() != null) {
			ps.setLong(i++, cfcColumn.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// COL_INDEX
		ps.setInt(i++, cfcColumn.getColIndex());
		
		// NTYPE
		if(cfcColumn.getName() != null) {
			ps.setString(i++, cfcColumn.getNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// CODE
		ps.setString(i++, cfcColumn.getCode());
		
		// NAME
		ps.setString(i++, cfcColumn.getName());
		
		// DESCRIPTION
		ps.setString(i++, cfcColumn.getDescription());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(cfcColumn.getValue())) {
			ps.setString(i++, cfcColumn.getValue().getXType().toString());
			ps.setString(i++, cfcColumn.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// HIDDEN
		ps.setBoolean(i++, cfcColumn.isHidden());
		
		// ACC_BANK_ID
		if(cfcColumn.getAccBank() != null) {
			ps.setLong(i++, cfcColumn.getAccBank().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	private int buildCFC(CFC cfc, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(cfc.getCompany() != null) {
			ps.setLong(i++, cfc.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		ps.setString(i++, cfc.getCode());
		
		// NAME
		ps.setString(i++, cfc.getName());
		
		// DESCRIPTION
		ps.setString(i++, cfc.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public ArrayList<CFCCell> loadCFCCells(AEDescriptor cfcDescr, Date startDate, Date endDate) throws AEException, JSONException {
		ArrayList<CFCCell> cfcCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCell);
			ps.setLong(1, cfcDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell cfcCell = new CFCCell();
				buildCFCCell(cfcCell, rs);
				cfcCell.setView();
				cfcCells.add(cfcCell);
			}
			return cfcCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public ArrayList<CFCCell> loadUnpaidCFCCells(long ownerId) throws AEException, JSONException {
		ArrayList<CFCCell> cfcCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(unpaidCFCCells);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell cfcCell = new CFCCell();
				cfcCell.setSysId(AEDocumentType.System.CFC.getSystemID());
				buildCFCCell(cfcCell, rs);
				cfcCell.setName(rs.getString("colName"));
				cfcCell.setView();
				cfcCells.add(cfcCell);
			}
			return cfcCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildCFCCell(CFCCell cfcCell, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		cfcCell.setID(rs.getLong("ID"));
		
		// CFC_ID
		cfcCell.setToCFC(CFC.lazyDescriptor(rs.getLong("CFC_ID")));
		
		// CELL_DATE
		cfcCell.setEntryDate(rs.getDate("CELL_DATE"));
		
		// ROW_INDEX
		cfcCell.setRowIndex(rs.getInt("ROW_INDEX"));
		
		// COL_ID
		cfcCell.setColumnId(rs.getLong("COL_ID"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			cfcCell.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// PAID
		cfcCell.setPaid(rs.getBoolean("PAID"));
	}
	
	public void insertCFCCell(CFCCell cfcCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCell);
			buildCFCCell(cfcCell, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				cfcCell.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			cfcCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildCFCCell(CFCCell cfcCell, PreparedStatement ps, int i) throws SQLException, AEException {
		// CFC_ID
		if(cfcCell.getToCFC() != null) {
			ps.setLong(i++, cfcCell.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CELL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(cfcCell.getEntryDate()));
		
		// ROW_INDEX
		ps.setInt(i++, cfcCell.getRowIndex());
		
		// COL_ID
		ps.setLong(i++, cfcCell.getColumnId());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(cfcCell.getValue())) {
			ps.setString(i++, cfcCell.getValue().getXType().toString());
			ps.setString(i++, cfcCell.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void deleteCFCCell(AEDescriptor cfcCellDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCell);
			ps.setLong(1, cfcCellDescr.getID());

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
	
	public void updateCFCCell(CFCCell cfcCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLCell);
			int i = buildCFCCell(cfcCell, ps, 1);
			ps.setLong(i++, cfcCell.getID());

			// execute
			ps.executeUpdate();

			// set view state
			cfcCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCColumn loadColumnByOwnerAndIndex(long ownerId, int colIndex) throws AEException, JSONException {
		CFCColumn cfcColumn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndIndex);
			ps.setLong(1, ownerId);
			ps.setInt(2, colIndex);
			rs = ps.executeQuery();
			if(rs.next()) {
				cfcColumn = new CFCColumn();
				buildCFCColumn(cfcColumn, rs);
				cfcColumn.setView();
			}
			return cfcColumn;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell updateCFCCellValue(CFCCell cfcCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// delete old entry
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCellValue);
			ps.setLong(1, cfcCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, cfcCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(cfcCell.getEntryDate()));
			ps.executeUpdate();
			
			// insert the new one
			insertCFCCell(cfcCell);
			
			return cfcCell;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCCell loadPayableItem(AEDescriptor cfcCellDescr) throws AEException, JSONException {
		CFCCell cfcCell = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(payableCFCCell);
			ps.setLong(1, cfcCellDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				cfcCell = new CFCCell();
				buildCFCCell(cfcCell, rs);
				cfcCell.setName(rs.getString("colName"));
				long accId = rs.getLong("accId");
				if(!rs.wasNull()) {
					cfcCell.setAccBank(AccAccount.lazyDescriptor(accId));
				}
				cfcCell.setView();
			}
			return cfcCell;
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
			ps = getAEConnection().prepareStatement("update CFCCell set paid = ? where ID = ?");

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
