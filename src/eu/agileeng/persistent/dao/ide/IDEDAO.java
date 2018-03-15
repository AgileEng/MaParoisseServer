package eu.agileeng.persistent.dao.ide;

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

public class IDEDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from IDE where owner_id = ?";
	
	private static String insertSQL = "insert into IDE "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION) "
		+ " values (?, ?, ?, ?)";
	
	private static String updateSQL = "update IDE "
		+ "set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ? "
		+ "where id = ?";
	
	private static String selectSQLColumn = 
		"select * from IDEModel where ide_id = ? order by ide_id asc, col_index asc";
	
	private static String insertSQLColumn = "insert into IDEModel "
		+ "(IDE_ID, COL_INDEX, NTYPE, CODE, NAME, DESCRIPTION, XTYPE, VALUE, HIDDEN, ACC_BANK_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLColumn = "update IDEModel "
		+ "set IDE_ID = ?, COL_INDEX = ?, NTYPE = ?, CODE = ?, NAME = ?, " 
		+ "DESCRIPTION = ?, XTYPE = ?, VALUE = ?, HIDDEN = ?, ACC_BANK_ID = ? where id = ?";
	
	private static String deleteSQLColumn = 
		"delete from IDEModel where id = ?";
	
	private static String selectSQLCell = 
		"select * from IDECell where ide_id = ? and CELL_DATE >= ? and CELL_DATE <= ?";
	
	private static String insertSQLCell = "insert into IDECell "
		+ "(IDE_ID, CELL_DATE, ROW_INDEX, COL_ID, XTYPE, VALUE) "
		+ " values (?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLCell = "update IDEModel "
		+ "set IDE_ID = ?, CELL_DATE = ?, ROW_INDEX = ?, COL_ID = ?, XTYPE = ?, VALUE = ? where id = ?";
	
	private static String deleteSQLCell = 
		"delete from IDE where id = ?";
	
	private static String selectSQLColumnByOwnerAndIndex = 
		"select ideModel.* from IDEModel ideModel inner join IDE ide on ideModel.IDE_ID = ide.ID " 
		+ " where ide.OWNER_ID = ? and ideModel.COL_INDEX = ?";
	
	private static String deleteSQLCellValue = 
		"delete from IDECell where IDE_ID = ? and COL_ID = ? and CELL_DATE = ?";
	
	private static String unpaidIDECells = 
		"select cell.*, model.NAME colName from " 
		+ "IDECell cell inner join IDEModel model on cell.COL_ID = model.ID "
		+ "inner join IDE ide on cell.IDE_ID = ide.ID "
		+ "where ide.OWNER_ID = ? and model.ACC_BANK_ID IS NOT NULL and cell.PAID = 0";
	
	private static String payableIDECell = 
		"select cell.*, model.NAME colName, model.ACC_BANK_ID accId from "
		+ "IDECell cell inner join IDEModel model on cell.COL_ID = model.ID where cell.ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public IDEDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public CFCModel loadIDEModel(AEDescriptor ideDescr) throws AEException, JSONException {
		CFCModel ideModel = new CFCModel();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumn);
			ps.setLong(1, ideDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCColumn ideColumn = new CFCColumn();
				buildIDEColumn(ideColumn, rs);
				ideColumn.setView();
				ideModel.add(ideColumn);
			}
			return ideModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFC loadIDE(AEDescriptor ownerDescr) throws AEException, JSONException {
		CFC ide = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ide = new CFC();
				buildIDE(ide, rs);
				ide.setView();
			}
			return ide;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildIDEColumn(CFCColumn ideColumn, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		ideColumn.setID(rs.getLong("ID"));
		
		// IDE_ID
		ideColumn.setToCFC(CFC.lazyDescriptor(rs.getLong("IDE_ID")));
		
		// COL_INDEX
		ideColumn.setColIndex(rs.getInt("COL_INDEX"));
		
		// NTYPE
		ideColumn.setNType(CFCColumn.NType.valueOf(rs.getString("NTYPE")));
		
		// CODE
		ideColumn.setCode(rs.getString("CODE"));
		
		// NAME
		ideColumn.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		ideColumn.setDescription(rs.getString("DESCRIPTION"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			ideColumn.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// HIDDEN
		ideColumn.setHidden(rs.getBoolean("HIDDEN"));
		
		// ACC_BANK_ID
		long accBankId = rs.getLong("ACC_BANK_ID");
		if(!rs.wasNull()) {
			AEDescriptive accBank = AccAccount.lazyDescriptor(accBankId);
			ideColumn.setAccBank(accBank);
		}
	}
	
	private void buildIDE(CFC ide, ResultSet rs) throws SQLException {
		// ID
		ide.setID(rs.getLong("ID"));
		
		// OWNER_ID
		ide.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// CODE
		ide.setCode(rs.getString("CODE"));
		
		// NAME
		ide.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		ide.setDescription(rs.getString("DESCRIPTION"));
	}
	
	public void insertIDEColumn(CFCColumn ideColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLColumn);
			buildIDEColumn(ideColumn, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				ideColumn.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			ideColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertIDE(CFC ide) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildIDE(ide, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				ide.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			ide.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateIDE(CFC ide) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = buildIDE(ide, ps, 1);
			ps.setLong(i++, ide.getID());

			// execute
			ps.executeUpdate();

			// set view state
			ide.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateIDEColumn(CFCColumn ideColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLColumn);
			int i = buildIDEColumn(ideColumn, ps, 1);
			ps.setLong(i++, ideColumn.getID());

			// execute
			ps.executeUpdate();

			// set view state
			ideColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteIDEColumn(AEDescriptor ideColumnDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLColumn);
			ps.setLong(1, ideColumnDescr.getID());

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
	
	private int buildIDEColumn(CFCColumn ideColumn, PreparedStatement ps, int i) throws SQLException, AEException {
		// IDE_ID
		if(ideColumn.getToCFC() != null) {
			ps.setLong(i++, ideColumn.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// COL_INDEX
		ps.setInt(i++, ideColumn.getColIndex());
		
		// NTYPE
		if(ideColumn.getName() != null) {
			ps.setString(i++, ideColumn.getNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// CODE
		ps.setString(i++, ideColumn.getCode());
		
		// NAME
		ps.setString(i++, ideColumn.getName());
		
		// DESCRIPTION
		ps.setString(i++, ideColumn.getDescription());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(ideColumn.getValue())) {
			ps.setString(i++, ideColumn.getValue().getXType().toString());
			ps.setString(i++, ideColumn.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// HIDDEN
		ps.setBoolean(i++, ideColumn.isHidden());
		
		// ACC_BANK_ID
		if(ideColumn.getAccBank() != null) {
			ps.setLong(i++, ideColumn.getAccBank().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	private int buildIDE(CFC ide, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(ide.getCompany() != null) {
			ps.setLong(i++, ide.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		ps.setString(i++, ide.getCode());
		
		// NAME
		ps.setString(i++, ide.getName());
		
		// DESCRIPTION
		ps.setString(i++, ide.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public ArrayList<CFCCell> loadIDECells(AEDescriptor ideDescr, Date startDate, Date endDate) throws AEException, JSONException {
		ArrayList<CFCCell> ideCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCell);
			ps.setLong(1, ideDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell ideCell = new CFCCell();
				buildIDECell(ideCell, rs);
				ideCell.setView();
				ideCells.add(ideCell);
			}
			return ideCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public ArrayList<CFCCell> loadUnpaidIDECells(long ownerId) throws AEException, JSONException {
		ArrayList<CFCCell> ideCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(unpaidIDECells);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell ideCell = new CFCCell();
				ideCell.setSysId(AEDocumentType.System.IDE.getSystemID());
				buildIDECell(ideCell, rs);
				ideCell.setName(rs.getString("colName"));
				ideCell.setView();
				ideCells.add(ideCell);
			}
			return ideCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildIDECell(CFCCell ideCell, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		ideCell.setID(rs.getLong("ID"));
		
		// IDE_ID
		ideCell.setToCFC(CFC.lazyDescriptor(rs.getLong("IDE_ID")));
		
		// CELL_DATE
		ideCell.setEntryDate(rs.getDate("CELL_DATE"));
		
		// ROW_INDEX
		ideCell.setRowIndex(rs.getInt("ROW_INDEX"));
		
		// COL_ID
		ideCell.setColumnId(rs.getLong("COL_ID"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			ideCell.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// PAID
		ideCell.setPaid(rs.getBoolean("PAID"));
	}
	
	public void insertIDECell(CFCCell ideCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCell);
			buildIDECell(ideCell, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				ideCell.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			ideCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildIDECell(CFCCell ideCell, PreparedStatement ps, int i) throws SQLException, AEException {
		// IDE_ID
		if(ideCell.getToCFC() != null) {
			ps.setLong(i++, ideCell.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CELL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(ideCell.getEntryDate()));
		
		// ROW_INDEX
		ps.setInt(i++, ideCell.getRowIndex());
		
		// COL_ID
		ps.setLong(i++, ideCell.getColumnId());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(ideCell.getValue())) {
			ps.setString(i++, ideCell.getValue().getXType().toString());
			ps.setString(i++, ideCell.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void deleteIDECell(AEDescriptor ideCellDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCell);
			ps.setLong(1, ideCellDescr.getID());

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
	
	public void updateIDECell(CFCCell ideCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLCell);
			int i = buildIDECell(ideCell, ps, 1);
			ps.setLong(i++, ideCell.getID());

			// execute
			ps.executeUpdate();

			// set view state
			ideCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCColumn loadColumnByOwnerAndIndex(long ownerId, int colIndex) throws AEException, JSONException {
		CFCColumn ideColumn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndIndex);
			ps.setLong(1, ownerId);
			ps.setInt(2, colIndex);
			rs = ps.executeQuery();
			if(rs.next()) {
				ideColumn = new CFCColumn();
				buildIDEColumn(ideColumn, rs);
				ideColumn.setView();
			}
			return ideColumn;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell updateIDECellValue(CFCCell ideCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// delete old entry
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCellValue);
			ps.setLong(1, ideCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, ideCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(ideCell.getEntryDate()));
			ps.executeUpdate();
			
			// insert the new one
			insertIDECell(ideCell);
			
			return ideCell;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCCell loadPayableItem(AEDescriptor ideCellDescr) throws AEException, JSONException {
		CFCCell ideCell = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(payableIDECell);
			ps.setLong(1, ideCellDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ideCell = new CFCCell();
				buildIDECell(ideCell, rs);
				ideCell.setName(rs.getString("colName"));
				long accId = rs.getLong("accId");
				if(!rs.wasNull()) {
					ideCell.setAccBank(AccAccount.lazyDescriptor(accId));
				}
				ideCell.setView();
			}
			return ideCell;
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
			ps = getAEConnection().prepareStatement("update IDECell set paid = ? where ID = ?");

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
