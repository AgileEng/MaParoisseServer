package eu.agileeng.persistent.dao.inventory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tomcat.util.json.JSONException;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccount;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.cash.CFC;
import eu.agileeng.domain.cash.CFCCell;
import eu.agileeng.domain.cash.CFCColumn;
import eu.agileeng.domain.cash.CFCModel;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.inventory.InventoryColumn;
import eu.agileeng.domain.inventory.InventoryStatus;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEValue;

public class InventoryDAO extends AbstractDAO {

	private static String selectSQL = 
		"select * from Inventory where owner_id = ?";
	
	private static String insertSQL = "insert into Inventory "
		+ "(OWNER_ID, CODE, NAME, DESCRIPTION) "
		+ " values (?, ?, ?, ?)";
	
	private static String updateSQL = "update Inventory "
		+ "set OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ? "
		+ "where id = ?";
	
	private static String selectSQLColumn = 
		"select * from InventoryModel where INVENTORY_ID = ? order by COL_INDEX asc";
	
	private static String insertSQLColumn = "insert into InventoryModel "
		+ "(Inventory_ID, COL_INDEX, NTYPE, CODE, NAME, DESCRIPTION, XTYPE, VALUE, HIDDEN, SALE_ATTR_ID, SUPPLY_ATTR_ID) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLColumn = "update InventoryModel "
		+ "set Inventory_ID = ?, COL_INDEX = ?, NTYPE = ?, CODE = ?, NAME = ?, " 
		+ "DESCRIPTION = ?, XTYPE = ?, VALUE = ?, HIDDEN = ?, SALE_ATTR_ID = ?, SUPPLY_ATTR_ID = ? where id = ?";
	
	private static String deleteSQLColumn = 
		"delete from InventoryModel where id = ?";
	
	private static String selectSQLCell = 
		"select * from InventoryCell where Inventory_id = ? and CELL_DATE >= ? and CELL_DATE <= ?";
	
	private static String insertSQLCell = "insert into InventoryCell "
		+ "(Inventory_ID, CELL_DATE, ROW_INDEX, COL_ID, XTYPE, VALUE) "
		+ " values (?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLCell = "update InventoryModel "
		+ "set Inventory_ID = ?, CELL_DATE = ?, ROW_INDEX = ?, COL_ID = ?, XTYPE = ?, VALUE = ? where id = ?";
	
	private static String deleteSQLCell = 
		"delete from InventoryInventory where id = ?";
	
	private static String selectSQLColumnByOwnerAndIndex = 
		"select InventoryModel.* from InventoryModel InventoryModel inner join Inventory Inventory on InventoryModel.Inventory_ID = Inventory.ID " 
		+ " where Inventory.OWNER_ID = ? and InventoryModel.COL_INDEX = ?";
	
	private static String deleteSQLCellValue = 
		"delete from InventoryCell where Inventory_ID = ? and COL_ID = ? and CELL_DATE = ? and ROW_INDEX = ?";
	
	private static String selectSQLCellValue = 
		"select ID from InventoryCell where Inventory_ID = ? and COL_ID = ? and CELL_DATE = ? and ROW_INDEX = ?";
	
	private static String unpaidInventoryCells = 
		"select cell.*, model.NAME colName from " 
		+ "InventoryCell cell inner join InventoryModel model on cell.COL_ID = model.ID "
		+ "inner join Inventory Inventory on cell.Inventory_ID = Inventory.ID "
		+ "where Inventory.OWNER_ID = ? and model.ACC_BANK_ID IS NOT NULL and cell.PAID = 0";
	
	private static String payableInventoryCell = 
		"select cell.*, model.NAME colName, model.ACC_BANK_ID accId from "
		+ "InventoryCell cell inner join InventoryModel model on cell.COL_ID = model.ID where cell.ID = ?";
	
	private static String supplySQLByAttrIdAndDate_1 = 
			"select Q.attrId, Q.entry_date, SUM(Q.qty) as qty from ( "
			+ "	select  "
			+ "     props.ATTRIBUTE_ID attrId, "
			+ "		cashJournal.ENTRY_DATE as entry_date, "
			+ "		CASE   "
			+ "			WHEN props.VALUE = '' THEN 0.0 " 
			+ "			WHEN props.VALUE is NULL THEN 0.0 " 
			+ "			ELSE CONVERT(numeric(18, 3), replace(props.value, ',', '.')) " 
			+ "		END AS qty "
			+ "	from   "
			+ "		CashJETProperties props " 
			+ "		inner join CashJournal cashJournal on props.JE_ID = cashJournal.ID " 
			+ "	where   "
			+ "		cashJournal.owner_id = ? " 
			+ "		and cashJournal.ENTRY_DATE between ? and ? " 
			+ "		and props.ATTRIBUTE_ID in ";
	private static String supplySQLByAttrIdAndDate_2 = 
		    ") as Q group by attrId, entry_date ";
	
	private static String summarizedSQLByAttrIdAndPeriod_1 = 
		"select Q.attrId, SUM(Q.qty) as qty from ( "
		+ "	select  "
		+ "     props.ATTRIBUTE_ID attrId, "
		+ "		CASE   "
		+ "			WHEN props.VALUE = '' THEN 0.0 " 
		+ "			WHEN props.VALUE is NULL THEN 0.0 " 
		+ "			ELSE CONVERT(numeric(18, 3), replace(props.value, ',', '.')) " 
		+ "		END AS qty "
		+ "	from   "
		+ "		CashJETProperties props " 
		+ "		inner join CashJournal cashJournal on props.JE_ID = cashJournal.ID " 
		+ "	where   "
		+ "		cashJournal.owner_id = ? " 
		+ "		and cashJournal.ENTRY_DATE between ? and ? " 
		+ "		and props.ATTRIBUTE_ID in ";
	private static String summarizedSQLByAttrIdAndPeriod_2 = 
		    ") as Q group by attrId ";
	
	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public InventoryDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	// used
	public CFCModel loadInventoryModel(AEDescriptor invDescr) throws AEException, JSONException {
		CFCModel inventoryModel = new CFCModel();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumn);
			ps.setLong(1, invDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				InventoryColumn inventoryColumn = new InventoryColumn();
				buildInventoryColumn(inventoryColumn, rs);
				inventoryColumn.setView();
				inventoryModel.add(inventoryColumn);
			}
			return inventoryModel;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	// Used
	public CFC loadInventory(AEDescriptor ownerDescr) throws AEException, JSONException {
		CFC inventory = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				inventory = new CFC();
				buildInventory(inventory, rs);
				inventory.setView();
			}
			return inventory;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public InventoryStatus loadInventoryStatus(AEDescriptor ownerDescr) throws AEException, JSONException {
		InventoryStatus inventory = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, ownerDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				inventory = new InventoryStatus();
				buildInventory(inventory, rs);
				inventory.setView();
			}
			return inventory;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void buildInventoryColumn(InventoryColumn invColumn, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		invColumn.setID(rs.getLong("ID"));
		
		// INVENTORY_ID
		invColumn.setToCFC(CFC.lazyDescriptor(rs.getLong("INVENTORY_ID")));
		
		// COL_INDEX
		invColumn.setColIndex(rs.getInt("COL_INDEX"));
		
		// NTYPE
		invColumn.setNType(CFCColumn.NType.valueOf(rs.getString("NTYPE")));
		
		// CODE
		invColumn.setCode(rs.getString("CODE"));
		
		// NAME
		invColumn.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		invColumn.setDescription(rs.getString("DESCRIPTION"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			invColumn.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// HIDDEN
		invColumn.setHidden(rs.getBoolean("HIDDEN"));
		
		// SALE_ATTR_ID
		invColumn.setSaleAttrId(rs.getLong("SALE_ATTR_ID"));
		
		// SUPPLY_ATTR_ID
		invColumn.setSupplyAttrId(rs.getLong("SUPPLY_ATTR_ID"));
	}
	
	private void buildInventory(CFC inv, ResultSet rs) throws SQLException {
		// ID
		inv.setID(rs.getLong("ID"));
		
		// OWNER_ID
		inv.setCompany(Organization.lazyDescriptor(rs.getLong("OWNER_ID")));
		
		// CODE
		inv.setCode(rs.getString("CODE"));
		
		// NAME
		inv.setName(rs.getString("NAME"));
		
		// DESCRIPTION
		inv.setDescription(rs.getString("DESCRIPTION"));
	}
	
	public void insertInvetoryColumn(InventoryColumn invColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLColumn);
			buildInventoryColumn(invColumn, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				invColumn.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			invColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void insertInventory(CFC inv) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);
			buildInv(inv, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				inv.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			inv.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateInv(CFC inv) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement
			ps = getAEConnection().prepareStatement(updateSQL);
			int i = buildInv(inv, ps, 1);
			ps.setLong(i++, inv.getID());

			// execute
			ps.executeUpdate();

			// set view state
			inv.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void updateInvColumn(InventoryColumn invColumn) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLColumn);
			int i = buildInventoryColumn(invColumn, ps, 1);
			ps.setLong(i++, invColumn.getID());

			// execute
			ps.executeUpdate();

			// set view state
			invColumn.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void deleteInvColumn(AEDescriptor invColumnDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLColumn);
			ps.setLong(1, invColumnDescr.getID());

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
	
	private int buildInventoryColumn(InventoryColumn invColumn, PreparedStatement ps, int i) throws SQLException, AEException {
		// CFC_ID
		if(invColumn.getToCFC() != null) {
			ps.setLong(i++, invColumn.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// COL_INDEX
		ps.setInt(i++, invColumn.getColIndex());
		
		// NTYPE
		if(invColumn.getName() != null) {
			ps.setString(i++, invColumn.getNType().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// CODE
		ps.setString(i++, invColumn.getCode());
		
		// NAME
		ps.setString(i++, invColumn.getName());
		
		// DESCRIPTION
		ps.setString(i++, invColumn.getDescription());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(invColumn.getValue())) {
			ps.setString(i++, invColumn.getValue().getXType().toString());
			ps.setString(i++, invColumn.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// HIDDEN
		ps.setBoolean(i++, invColumn.isHidden());
		
		// SALE_ATTR_ID
		ps.setLong(i++, invColumn.getSaleAttrId());
		
		// SUPPLY_ATTR_ID
		ps.setLong(i++, invColumn.getSupplyAttrId());
		
		// return the current ps position 
		return i;
	}
	
	private int buildInv(CFC inv, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(inv.getCompany() != null) {
			ps.setLong(i++, inv.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CODE
		ps.setString(i++, inv.getCode());
		
		// NAME
		ps.setString(i++, inv.getName());
		
		// DESCRIPTION
		ps.setString(i++, inv.getDescription());
		
		// return the current ps position 
		return i;
	}
	
	public ArrayList<CFCCell> loadInvCells(AEDescriptor invDescr, Date startDate, Date endDate) throws AEException, JSONException {
		ArrayList<CFCCell> cfcCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCell);
			ps.setLong(1, invDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(startDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(endDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell cfcCell = new CFCCell();
				buildInvCell(cfcCell, rs);
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
	
	public ArrayList<CFCCell> loadUnpaidInvCells(long ownerId) throws AEException, JSONException {
		ArrayList<CFCCell> cfcCells = new ArrayList<CFCCell>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(unpaidInventoryCells);
			ps.setLong(1, ownerId);
			rs = ps.executeQuery();
			while(rs.next()) {
				CFCCell cfcCell = new CFCCell();
				cfcCell.setSysId(AEDocumentType.System.CFC.getSystemID());
				buildInvCell(cfcCell, rs);
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
	
	private void buildInvCell(CFCCell invCell, ResultSet rs) throws SQLException, AEException, JSONException {
		// ID
		invCell.setID(rs.getLong("ID"));
		
		// CFC_ID
		invCell.setToCFC(CFC.lazyDescriptor(rs.getLong("INVENTORY_ID")));
		
		// CELL_DATE
		invCell.setEntryDate(rs.getDate("CELL_DATE"));
		
		// ROW_INDEX
		invCell.setRowIndex(rs.getInt("ROW_INDEX"));
		
		// COL_ID
		invCell.setColumnId(rs.getLong("COL_ID"));
		
		// XTYPE, VALUE
		String value = rs.getString("VALUE");
		if(!rs.wasNull()) {
			invCell.setValue(new AEValue(value, rs.getString("XTYPE")));
		}
		
		// PAID
		invCell.setPaid(rs.getBoolean("PAID"));
	}
	
	public void insertInvCell(CFCCell invCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLCell);
			buildInvCell(invCell, ps, 1);

			// execute
			ps.executeUpdate();

			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				invCell.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}

			// set view state
			invCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private int buildInvCell(CFCCell invCell, PreparedStatement ps, int i) throws SQLException, AEException {
		// CFC_ID
		if(invCell.getToCFC() != null) {
			ps.setLong(i++, invCell.getToCFC().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
		// CELL_DATE
		ps.setDate(i++, AEPersistentUtil.getSQLDate(invCell.getEntryDate()));
		
		// ROW_INDEX
		ps.setInt(i++, invCell.getRowIndex());
		
		// COL_ID
		ps.setLong(i++, invCell.getColumnId());
		
		// XTYPE, VALUE
		if(!AEValue.isNull(invCell.getValue())) {
			ps.setString(i++, invCell.getValue().getXType().toString());
			ps.setString(i++, invCell.getValue().toString());
		} else {
			ps.setNull(i++, java.sql.Types.NVARCHAR);
			ps.setNull(i++, java.sql.Types.NVARCHAR);
		}
		
		// return the current ps position 
		return i;
	}
	
	public void deleteInvCell(AEDescriptor invCellDescr) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCell);
			ps.setLong(1, invCellDescr.getID());

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
	
	public void updateInvCell(CFCCell invCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareStatement(updateSQLCell);
			int i = buildInvCell(invCell, ps, 1);
			ps.setLong(i++, invCell.getID());

			// execute
			ps.executeUpdate();

			// set view state
			invCell.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCColumn loadColumnByOwnerAndIndex(long ownerId, int colIndex) throws AEException, JSONException {
		InventoryColumn invColumn = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLColumnByOwnerAndIndex);
			ps.setLong(1, ownerId);
			ps.setInt(2, colIndex);
			rs = ps.executeQuery();
			if(rs.next()) {
				invColumn = new InventoryColumn();
				buildInventoryColumn(invColumn, rs);
				invColumn.setView();
			}
			return invColumn;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public CFCCell updateInvCellValue(CFCCell invCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			// delete old entry
			ps = getAEConnection().prepareGenKeyStatement(deleteSQLCellValue);
			ps.setLong(1, invCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, invCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(invCell.getEntryDate()));
			ps.setLong(4, invCell.getRowIndex());
			ps.executeUpdate();
			
			// insert the new one
			insertInvCell(invCell);
			
			return invCell;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public boolean existInvCellValue(CFCCell invCell) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			boolean exist = false;
			ps = getAEConnection().prepareGenKeyStatement(selectSQLCellValue);
			ps.setLong(1, invCell.getToCFC().getDescriptor().getID());
			ps.setLong(2, invCell.getColumnId());
			ps.setDate(3, AEPersistentUtil.getSQLDate(invCell.getEntryDate()));
			ps.setLong(4, invCell.getRowIndex());
			rs = ps.executeQuery();
			if(rs.next()) {
				exist = true;
			}
			return exist;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public CFCCell loadPayableItem(AEDescriptor invCellDescr) throws AEException, JSONException {
		CFCCell cfcCell = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(payableInventoryCell);
			ps.setLong(1, invCellDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				cfcCell = new CFCCell();
				buildInvCell(cfcCell, rs);
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
	
	public Map<Date, Map<Long, Double>> loadSupply(long ownerId, List<Long> ids, AccPeriod accPeriod) throws AEException, JSONException {
		Map<Date, Map<Long, Double>> supply = new HashMap<Date, Map<Long, Double>>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			StringBuilder sb = new StringBuilder(supplySQLByAttrIdAndDate_1);
			sb.append(AEPersistentUtil.createInClause(ids));
			sb.append(supplySQLByAttrIdAndDate_2);
			ps = getAEConnection().prepareStatement(sb.toString());
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(accPeriod.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accPeriod.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				Date date = rs.getDate("entry_date");
				Map<Long, Double> attrMap = supply.get(date);
				if(attrMap == null) {
					attrMap = new HashMap<Long, Double>();
					supply.put(date, attrMap);
				}
				long attrId = rs.getLong("attrId");
				Double qty = rs.getDouble("qty");
				if(!rs.wasNull()) {
					attrMap.put(attrId, qty);
				}
			}
			return supply;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Map<Long, Double> loadStocksSummarized(long ownerId, List<Long> ids, AccPeriod accPeriod) throws AEException, JSONException {
		Map<Long, Double> stocks = new HashMap<Long, Double>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			StringBuilder sb = new StringBuilder(summarizedSQLByAttrIdAndPeriod_1);
			sb.append(AEPersistentUtil.createInClause(ids));
			sb.append(summarizedSQLByAttrIdAndPeriod_2);
			ps = getAEConnection().prepareStatement(sb.toString());
			ps.setLong(1, ownerId);
			ps.setDate(2, AEPersistentUtil.getSQLDate(accPeriod.getStartDate()));
			ps.setDate(3, AEPersistentUtil.getSQLDate(accPeriod.getEndDate()));
			rs = ps.executeQuery();
			while(rs.next()) {
				long attrId = rs.getLong("attrId");
				Double qty = rs.getDouble("qty");
				if(!rs.wasNull()) {
					stocks.put(attrId, qty);
				}
			}
			return stocks;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
