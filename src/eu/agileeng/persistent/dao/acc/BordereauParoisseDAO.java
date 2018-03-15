package eu.agileeng.persistent.dao.acc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.cashbasis.BordereauParoisse;
import eu.agileeng.domain.acc.cashbasis.BordereauParoissesList;
import eu.agileeng.domain.acc.cashbasis.Quete;
import eu.agileeng.domain.acc.cashbasis.QuetesList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistent;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;

public class BordereauParoisseDAO extends AbstractDAO {

	public BordereauParoisseDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	private static String selectSQLBordereauParoissesByTenantAndYear = 
			"select bp.*, q.code, q.name "
			+ "from (select * from BordereauParoisse where owner_id = ? and year = ?) bp right outer join Quete q on bp.QUETE_CODE = q.CODE";
	public BordereauParoissesList loadBordereauParoisses(AEDescriptor tenantDescr, int year) throws AEException {
		BordereauParoissesList bpList = new BordereauParoissesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLBordereauParoissesByTenantAndYear);
			ps.setLong(1, tenantDescr.getID());
			ps.setLong(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {				
				// create and build
				BordereauParoisse bp = new BordereauParoisse();
				build(bp, rs);
				
				// new or view
				if(AEPersistent.ID.isPersistent(bp.getID())) {
					bp.setView();
				}
				
				// add to the list
				bpList.add(bp);
			}
			return bpList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(BordereauParoisse bp, ResultSet rs) throws SQLException {
		super.build(bp, rs);
		
		// process ID
		if(!AEPersistent.ID.isPersistent(bp.getID())) {
			bp.setID(AEPersistentUtil.getTmpID());
		}
		
		// YEAR
		bp.setYear(rs.getInt("YEAR"));
		
		// AMOUNT_ACC
		bp.setAccAmount(rs.getDouble("AMOUNT_ACC"));
		
		// AMOUNT_PAID
		bp.setPaidAmount(rs.getDouble("AMOUNT_PAID"));
		
		// AMOUNT_TO_PAY
		bp.setToPayAmount(rs.getDouble("AMOUNT_TO_PAY"));
		
		// AMOUNT_CURRENT
		bp.setCurrAmount(rs.getDouble("AMOUNT_CURRENT"));
	}
	
	private static String insertSQL = 
			"insert into BordereauParoisse (OWNER_ID, QUETE_CODE, YEAR, DESCRIPTION, AMOUNT_ACC, AMOUNT_PAID, AMOUNT_TO_PAY, AMOUNT_CURRENT) "
			+ " values(?, ?, ?, ?, ?, ?, ?, ?)";
	public void insert(BordereauParoisse bp) throws AEException {
		assert(!bp.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			buildInsert(bp, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				bp.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			bp.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	private static String updateSQL = 
			"update BordereauParoisse set DESCRIPTION = ?, AMOUNT_ACC = ?, AMOUNT_TO_PAY = ?, AMOUNT_CURRENT = ? "
			+ " where ID = ? and OWNER_ID = ? AND QUETE_CODE = ? AND YEAR = ?";
	public void update(BordereauParoisse bp) throws AEException {
		assert(bp != null);
		assert(bp.isPersistent());
		
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			buildUpdate(bp, ps, 1);

			// execute
			ps.executeUpdate();
			
			// set view
			bp.setView();
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	protected int buildInsert(BordereauParoisse bp, PreparedStatement ps, int i) throws SQLException, AEException {
		// OWNER_ID
		if(bp.getCompany() != null) {
			ps.setLong(i++, bp.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}
		
		// QUETE_CODE
		ps.setString(i++, bp.getCode());
		
		// YEAR
		ps.setInt(i++, bp.getYear());
		
		// DESCRIPTION
		ps.setString(i++, bp.getDescription());
		
		// AMOUNT_ACC
		ps.setDouble(i++, bp.getAccAmount());
		
		// AMOUNT_PAID
		ps.setDouble(i++, bp.getPaidAmount());
		
		// AMOUNT_TO_PAY
		ps.setDouble(i++, bp.getToPayAmount());
		
		// AMOUNT_CURRENT
		ps.setDouble(i++, bp.getCurrAmount());
		
		// return the current ps position 
		return i;
	}
	
	protected int buildUpdate(BordereauParoisse bp, PreparedStatement ps, int i) throws SQLException, AEException {
		// DESCRIPTION
		ps.setString(i++, bp.getDescription());

		// AMOUNT_ACC
		ps.setDouble(i++, bp.getAccAmount());

		// AMOUNT_TO_PAY
		ps.setDouble(i++, bp.getToPayAmount());

		// AMOUNT_CURRENT
		ps.setDouble(i++, bp.getCurrAmount());
		
		// ID
		ps.setLong(i++, bp.getID());
		
		// OWNER_ID
		if(bp.getCompany() != null) {
			ps.setLong(i++, bp.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, Types.BIGINT);
		}

		// QUETE_CODE
		ps.setString(i++, bp.getCode());

		// YEAR
		ps.setInt(i++, bp.getYear());

		// return the current ps position 
		return i;
	}
	
	private static String selectSQLQuetesListByTenantAndYear = 
			"select * from BordereauParoisse where owner_id = ? and year = ?";
	public QuetesList loadQuetes(AEDescriptor tenantDescr, int year) throws AEException {
		QuetesList qList = new QuetesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLQuetesListByTenantAndYear);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {
				Quete q = new Quete();
								
				q.setCode(rs.getString("quete_code"));
				q.setAmount(rs.getDouble("AMOUNT_CURRENT"));
				
				qList.add(q);
			}
			return qList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private static String updateSQLProcessCurrentAmount = 
			"update BordereauParoisse set AMOUNT_PAID = AMOUNT_PAID + AMOUNT_CURRENT, "
			+ " AMOUNT_CURRENT = 0.0,  AMOUNT_TO_PAY = AMOUNT_ACC - AMOUNT_PAID "
			+ " where OWNER_ID = ? AND YEAR = ?";
//	private static String updateSQLNullCurrentAmount = 
//			"update BordereauParoisse set AMOUNT_CURRENT = 0.0 where OWNER_ID = ? AND YEAR = ?";
	public void processCurrentAmount(AEDescriptor tenantDescr, int year) throws AEException {		
		PreparedStatement ps = null;
		try {
			// updateSQLProcessCurrentAmount
			ps = getAEConnection().prepareStatement(updateSQLProcessCurrentAmount);
			ps.setLong(1, tenantDescr.getID());
			ps.setInt(2, year);
			ps.executeUpdate();
			AEConnection.close(ps);
			
//			// updateSQLProcessCurrentAmount
//			ps = getAEConnection().prepareStatement(updateSQLProcessCurrentAmount);
//			ps.setLong(1, tenantDescr.getID());
//			ps.setInt(2, year);
//			ps.executeUpdate();
			
		} catch (SQLException ex) {
			throw new AEException(ex);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
