/**
 * 
 */
package eu.agileeng.persistent.dao.mandat;

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
import eu.agileeng.domain.mandat.Mandat;
import eu.agileeng.domain.mandat.MandatExt;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEValue;

/**
 * @author vvatov
 *
 */
public class MandatDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from Mandat where owner_id = ?";
	
	private static String insertSQL = "insert into Mandat "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION) "
		+ " values (?, ?, ?, ?)";
	
	private static String updateSQL = "update Mandat "
		+ "set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ? "
		+ "where id = ?";
	
	private static String selectSQLColumn = 
		"select * from MandatModel where mandat_id = ? order by mandat_id asc, col_index asc";
	
	private static String insertSQLColumn = "insert into mandatModel "
		+ "(mandat_ID, COL_INDEX, NTYPE, CODE, NAME, DESCRIPTION, XTYPE, VALUE, HIDDEN, ACC_BANK_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLColumn = "update mandatModel "
		+ "set mandat_ID = ?, COL_INDEX = ?, NTYPE = ?, CODE = ?, NAME = ?, " 
		+ "DESCRIPTION = ?, XTYPE = ?, VALUE = ?, HIDDEN = ?, ACC_BANK_ID = ? where id = ?";
	
	private static String deleteSQLColumn = 
		"delete from mandatModel where id = ?";
	
	private static String selectSQLCell = 
		"select * from mandatCell where mandat_id = ? and CELL_DATE >= ? and CELL_DATE <= ? and GRID_ID = ?";
	
	private static String insertSQLCell = "insert into mandatCell "
		+ "(mandat_ID, CELL_DATE, ROW_INDEX, COL_ID, XTYPE, VALUE, GRID_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?)";
	
//	private static String updateSQLCell = "update mandatModel "
//		+ "set mandat_ID = ?, CELL_DATE = ?, ROW_INDEX = ?, COL_ID = ?, XTYPE = ?, VALUE = ? where id = ?";
	
	private static String deleteSQLCell = 
		"delete from mandatmandat where id = ?";
	
//	private static String selectSQLColumnByOwnerAndIndex = 
//		"select mandatModel.* from mandatModel mandatModel inner join mandat mandat on mandatModel.mandat_ID = mandat.ID " 
//		+ " where mandat.OWNER_ID = ? and mandatModel.COL_INDEX = ?";
	
	private static String selectSQLColumnByOwnerAndId = 
		"select mandatModel.* from mandatModel mandatModel inner join mandat mandat on mandatModel.mandat_ID = mandat.ID " 
		+ " where mandat.OWNER_ID = ? and mandatModel.ID = ?";
	
	private static String deleteSQLCellValue = 
		"delete from mandatCell where mandat_ID = ? and COL_ID = ? and CELL_DATE = ? and GRID_ID = ? and ROW_INDEX = ?";
	
	private static String unpaidMandatCells = 
		"select cell.*, model.NAME colName from " 
		+ "MandatCell cell inner join MandatModel model on cell.COL_ID = model.ID "
		+ "inner join Mandat mandat on cell.MANDAT_ID = mandat.ID "
		+ "where mandat.OWNER_ID = ? and model.ACC_BANK_ID IS NOT NULL and cell.PAID = 0";
	
	private static String payableMandatCell = 
		"select cell.*, model.NAME colName, model.ACC_BANK_ID accId from "
		+ "MandatCell cell inner join MandatModel model on cell.COL_ID = model.ID where cell.ID = ?";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public MandatDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public CFCModel loadMandatModel(AEDescriptor mandatDescr) throws AEException, JSONException {
		CFCModel mandatModel = new CFCModel();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumn);
			ps.setLong(1, mandatDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCColumn mandatColumn = new CFCColumn();
				buildMandatColumn(mandatColumn, rs);
				mandatColumn.setView();
				mandatModel.add(mandatColumn);
			}
			return mandatModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCModel loadMandatModelExt(AEDescriptor mandatDescr) throws AEException, JSONException {
		CFCModel mandatModel = new CFCModel();
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			ps = getAEConnection().prepareStatement(selectSQLColumn);
//			ps.setLong(1, mandatDescr.getID());
//			rs = ps.executeQuery();
//			while(rs.next()) {
//				CFCColumn mandatColumn = new CFCColumn();
//				buildMandatColumn(mandatColumn, rs);
//				mandatColumn.setView();
//				mandatModel.add(mandatColumn);
//			}
//			return mandatModel;
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}
		/**
		 * A, B column
		 */
		CFCColumn mandatColumn = new CFCColumn();
		mandatColumn.setID(1);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(0);
		mandatColumn.setNType(CFCColumn.NType.LABEL);
		mandatColumn.setCode(null);
		mandatColumn.setName("A");
		mandatColumn.setDescription("A");
		mandatColumn.setValue(new AEValue(AEValue.XType.STRING.name(), AEValue.XType.STRING));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		// 
		mandatColumn = new CFCColumn();
		mandatColumn.setID(2);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(1);
		mandatColumn.setNType(CFCColumn.NType.VALUE);
		mandatColumn.setCode(null);
		mandatColumn.setName("B");
		mandatColumn.setDescription("B");
		mandatColumn.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		/**
		 * C, D column
		 */
		mandatColumn = new CFCColumn();
		mandatColumn.setID(3);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(2);
		mandatColumn.setNType(CFCColumn.NType.LABEL);
		mandatColumn.setCode(null);
		mandatColumn.setName("C");
		mandatColumn.setDescription("C");
		mandatColumn.setValue(new AEValue(AEValue.XType.STRING.name(), AEValue.XType.STRING));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		// 
		mandatColumn = new CFCColumn();
		mandatColumn.setID(4);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(3);
		mandatColumn.setNType(CFCColumn.NType.VALUE);
		mandatColumn.setCode(null);
		mandatColumn.setName("D");
		mandatColumn.setDescription("D");
		mandatColumn.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		/**
		 * E, F column
		 */
		mandatColumn = new CFCColumn();
		mandatColumn.setID(5);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(4);
		mandatColumn.setNType(CFCColumn.NType.LABEL);
		mandatColumn.setCode(null);
		mandatColumn.setName("E");
		mandatColumn.setDescription("E");
		mandatColumn.setValue(new AEValue(AEValue.XType.STRING.name(), AEValue.XType.STRING));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		// 
		mandatColumn = new CFCColumn();
		mandatColumn.setID(6);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(5);
		mandatColumn.setNType(CFCColumn.NType.VALUE);
		mandatColumn.setCode(null);
		mandatColumn.setName("F");
		mandatColumn.setDescription("F");
		mandatColumn.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		/**
		 * G, H column
		 */
		mandatColumn = new CFCColumn();
		mandatColumn.setID(7);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(6);
		mandatColumn.setNType(CFCColumn.NType.LABEL);
		mandatColumn.setCode(null);
		mandatColumn.setName("G");
		mandatColumn.setDescription("G");
		mandatColumn.setValue(new AEValue(AEValue.XType.STRING.name(), AEValue.XType.STRING));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		// 
		mandatColumn = new CFCColumn();
		mandatColumn.setID(8);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(7);
		mandatColumn.setNType(CFCColumn.NType.VALUE);
		mandatColumn.setCode(null);
		mandatColumn.setName("H");
		mandatColumn.setDescription("H");
		mandatColumn.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		/**
		 * I, J column
		 */
		mandatColumn = new CFCColumn();
		mandatColumn.setID(9);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(8);
		mandatColumn.setNType(CFCColumn.NType.LABEL);
		mandatColumn.setCode(null);
		mandatColumn.setName("I");
		mandatColumn.setDescription("I");
		mandatColumn.setValue(new AEValue(AEValue.XType.STRING.name(), AEValue.XType.STRING));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		// 
		mandatColumn = new CFCColumn();
		mandatColumn.setID(10);
		mandatColumn.setToCFC(mandatDescr);
		mandatColumn.setColIndex(9);
		mandatColumn.setNType(CFCColumn.NType.VALUE);
		mandatColumn.setCode(null);
		mandatColumn.setName("G");
		mandatColumn.setDescription("G");
		mandatColumn.setValue(new AEValue("0.0", AEValue.XType.DOUBLE));		
		mandatColumn.setHidden(false);
		
		mandatColumn.setView();
		mandatModel.add(mandatColumn);
		
		return mandatModel;
	}
	
//	public CFC loadMandat(AEDescriptor ownerDescr) throws AEException, JSONException {
//		CFC mandat = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			ps = getAEConnection().prepareStatement(selectSQL);
//			ps.setLong(1, ownerDescr.getID());
//			rs = ps.executeQuery();
//			if(rs.next()) {
//				mandat = new CFC();
//				buildMandat(mandat, rs);
//				mandat.setView();
//			}
//			return mandat;
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}
//	}
	
	public Mandat loadMandat(AEDescriptor ownerDescr) throws AEException, JSONException {
		Mandat mandat = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				mandat = new Mandat();
				buildMandat(mandat, rs);
				mandat.setView();
			}
			return mandat;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public MandatExt loadMandatExt(AEDescriptor ownerDescr) throws AEException, JSONException {
		MandatExt mandat = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				mandat = new MandatExt();
				buildMandat(mandat, rs);
				mandat.setView();
			}
			return mandat;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildMandatColumn(CFCColumn mandatColumn, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		mandatColumn.setID(rs.getLong("ID"));
		
		// mandat_ID
		mandatColumn.setToCFC(CFC.lazyDescriptor(rs.getLong("mandat_ID")));
		
		// COL_INDEX
		mandatColumn.setColIndex(rs.getInt("COL_INDEX"));
		
		// NTYPE
		mandatColumn.setNType(CFCColumn.NType.valueOf(rs.getString("NTYPE")));
		
		// CODE
		mandatColumn.setCode(rs.getString("CODE"));
		
		// NAME
		mandatColumn.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		mandatColumn.setDescription(rs.getString("DESCRIPTION"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			mandatColumn.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// HIDDEN
		mandatColumn.setHidden(rs.getBoolean("HIDDEN"));
		
		// ACC_BANK_ID
		long accBankId = rs.getLong("ACC_BANK_ID");
		if(!rs.wasNull()) {
			AEDescriptive accBank = AccAccount.lazyDescriptor(accBankId);
			mandatColumn.setAccBank(accBank);
		}
	}
	
	private void buildMandat(CFC mandat, ResultSet rs) throws SQLException {
		// ID
		mandat.setID(rs.getLong("ID"));
		
		// OWNER_ID
		mandat.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// CODE
		mandat.setCode(rs.getString("CODE"));
		
		// NAME
		mandat.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		mandat.setDescription(rs.getString("DESCRIPTION"));
	}
	
	public void insertMandatColumn(CFCColumn mandatColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLColumn);
			buildMandatColumn(mandatColumn, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				mandatColumn.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			mandatColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertMandat(CFC mandat) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildMandat(mandat, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				mandat.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			mandat.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateMandat(CFC mandat) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = buildMandat(mandat, ps, 1);
			ps.setLong(i++, mandat.getID());

			// execute
			ps.executeUpdate();

			// set view state
			mandat.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateMandatColumn(CFCColumn mandatColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLColumn);
			int i = buildMandatColumn(mandatColumn, ps, 1);
			ps.setLong(i++, mandatColumn.getID());

			// execute
			ps.executeUpdate();

			// set view state
			mandatColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteMandatColumn(AEDescriptor mandatColumnDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLColumn);
			ps.setLong(1, mandatColumnDescr.getID());

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
	
	private int buildMandatColumn(CFCColumn mandatColumn, PreparedStatement ps, int i) throws SQLException, AEException {
		// mandat_ID
		if(mandatColumn.getToCFC() != null) {
			ps.setLong(i++, mandatColumn.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// COL_INDEX
		ps.setInt(i++, mandatColumn.getColIndex());
		
		// NTYPE
		if(mandatColumn.getName() != null) {
			ps.setString(i++, mandatColumn.getNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// CODE
		ps.setString(i++, mandatColumn.getCode());
		
		// NAME
		ps.setString(i++, mandatColumn.getName());
		
		// DESCRIPTION
		ps.setString(i++, mandatColumn.getDescription());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(mandatColumn.getValue())) {
			ps.setString(i++, mandatColumn.getValue().getXType().toString());
			ps.setString(i++, mandatColumn.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// HIDDEN
		ps.setBoolean(i++, mandatColumn.isHidden());
		
		// ACC_BANK_ID
		if(mandatColumn.getAccBank() != null) {
			ps.setLong(i++, mandatColumn.getAccBank().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// return the current ps position 
		return i;
	}
	
	private int buildMandat(CFC mandat, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(mandat.getCompany() != null) {
			ps.setLong(i++, mandat.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		ps.setString(i++, mandat.getCode());
		
		// NAME
		ps.setString(i++, mandat.getName());
		
		// DESCRIPTION
		ps.setString(i++, mandat.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public ArrayList<CFCCell> loadMandatCells(AEDescriptor mandatDescr, Date startDate, Date endDate, long gridId) throws AEException, JSONException {
		ArrayList<CFCCell> mandatCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCell);
			ps.setLong(1, mandatDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			ps.setLong(4, gridId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell mandatCell = new CFCCell();
				buildMandatCell(mandatCell, rs);
				mandatCell.setView();
				mandatCells.add(mandatCell);
			}
			return mandatCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildMandatCell(CFCCell mandatCell, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		mandatCell.setID(rs.getLong("ID"));
		
		// mandat_ID
		mandatCell.setToCFC(CFC.lazyDescriptor(rs.getLong("mandat_ID")));
		
		// CELL_DATE
		mandatCell.setEntryDate(rs.getDate("CELL_DATE"));
		
		// ROW_INDEX
		mandatCell.setRowIndex(rs.getInt("ROW_INDEX"));
		
		// COL_ID
		mandatCell.setColumnId(rs.getLong("COL_ID"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			mandatCell.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// PAID
		mandatCell.setPaid(rs.getBoolean("PAID"));
		
		// GRID_ID
		mandatCell.setGridId(rs.getLong("GRID_ID"));
	}
	
	public void insertMandatCell(CFCCell mandatCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCell);
			buildMandatCell(mandatCell, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				mandatCell.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			mandatCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildMandatCell(CFCCell mandatCell, PreparedStatement ps, int i) throws SQLException, AEException {
		// mandat_ID
		if(mandatCell.getToCFC() != null) {
			ps.setLong(i++, mandatCell.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CELL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(mandatCell.getEntryDate()));
		
		// ROW_INDEX
		ps.setInt(i++, mandatCell.getRowIndex());
		
		// COL_ID
		ps.setLong(i++, mandatCell.getColumnId());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(mandatCell.getValue())) {
			ps.setString(i++, mandatCell.getValue().getXType().toString());
			ps.setString(i++, mandatCell.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// GRID_ID
		ps.setLong(i++, mandatCell.getGridId());
		
		// return the current ps position 
		return i;
	}
	
	public void deletemandatCell(AEDescriptor mandatCellDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCell);
			ps.setLong(1, mandatCellDescr.getID());

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
	
//	public void updateMandatCell(CFCCell mandatCell) throws AEException {
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			// prepare statement and insert
//			ps = getAEConnection().prepareStatement(updateSQLCell);
//			int i = buildMandatCell(mandatCell, ps, 1);
//			ps.setLong(i++, mandatCell.getID());
//
//			// execute
//			ps.executeUpdate();
//
//			// set view state
//			mandatCell.setView();
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(ps);
//			AEConnection.close(rs);
//			close();
//		}
//	}
	
//	public CFCColumn loadColumnByOwnerAndIndex(long ownerId, int colIndex) throws AEException, JSONException {
//		CFCColumn mandatColumn = null;
//		PreparedStatement ps = null;
//		ResultSet rs = null;
//		try {
//			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndIndex);
//			ps.setLong(1, ownerId);
//			ps.setInt(2, colIndex);
//			rs = ps.executeQuery();
//			if(rs.next()) {
//				mandatColumn = new CFCColumn();
//				buildMandatColumn(mandatColumn, rs);
//				mandatColumn.setView();
//			}
//			return mandatColumn;
//		} catch (SQLException e) {
//			throw new AEException(e);
//		} finally {
//			AEConnection.close(rs);
//			AEConnection.close(ps);
//			close();
//		}
//	}
	
	public CFCColumn loadColumnByOwnerAndId(long ownerId, long colId) throws AEException, JSONException {
		CFCColumn mandatColumn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndId);
			ps.setLong(1, ownerId);
			ps.setLong(2, colId);
			rs = ps.executeQuery();
			if(rs.next()) {
				mandatColumn = new CFCColumn();
				buildMandatColumn(mandatColumn, rs);
				mandatColumn.setView();
			}
			return mandatColumn;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCColumn loadColumnByOwnerAndIndexExt(long ownerId, int colIndex) throws AEException, JSONException {
		CFCColumn retColumn = null;
		MandatExt mandatExt = loadMandatExt(Organization.lazyDescriptor(ownerId));
		CFCModel mandatExtModel = loadMandatModelExt(mandatExt.getDescriptor());
		for (CFCColumn cfcColumn : mandatExtModel) {
			if(cfcColumn.getColIndex() == colIndex) {
				retColumn = cfcColumn;
				break;
			}
		}
		return retColumn;
	}
	
	public CFCCell updateMandatCellValue(CFCCell mandatCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// delete old entry
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCellValue);
			ps.setLong(1, mandatCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, mandatCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(mandatCell.getEntryDate()));
			ps.setLong(4, mandatCell.getGridId());
			ps.setLong(5, mandatCell.getRowIndex());
			ps.executeUpdate();
			
			// insert the new one
			if(!AEValue.isNull(mandatCell.getValue())) {
				insertMandatCell(mandatCell);
			}
			
			return mandatCell;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public ArrayList<CFCCell> loadUnpaidMandatCells(long ownerId) throws AEException, JSONException {
		ArrayList<CFCCell> mandatCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(unpaidMandatCells);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell mandatCell = new CFCCell();
				mandatCell.setSysId(AEDocumentType.System.MANDAT.getSystemID());
				buildMandatCell(mandatCell, rs);
				mandatCell.setName(rs.getString("colName"));
				mandatCell.setView();
				mandatCells.add(mandatCell);
			}
			return mandatCells;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell loadPayableItem(AEDescriptor mandatCellDescr) throws AEException, JSONException {
		CFCCell mandatCell = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(payableMandatCell);
			ps.setLong(1, mandatCellDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				mandatCell = new CFCCell();
				buildMandatCell(mandatCell, rs);
				mandatCell.setName(rs.getString("colName"));
				long accId = rs.getLong("accId");
				if(!rs.wasNull()) {
					mandatCell.setAccBank(AccAccount.lazyDescriptor(accId));
				}
				mandatCell.setView();
			}
			return mandatCell;
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
			ps = getAEConnection().prepareStatement("update MandatCell set paid = ? where ID = ?");

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
