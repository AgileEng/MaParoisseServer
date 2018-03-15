/**
 * 
 */
package eu.agileeng.persistent.dao.app;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import eu.agileeng.accbureau.AEAppConfig;
import eu.agileeng.accbureau.AEAppConfigList;
import eu.agileeng.accbureau.AEAppModule;
import eu.agileeng.accbureau.AEAppModulesList;
import eu.agileeng.accbureau.AEModuleAlert;
import eu.agileeng.accbureau.AEModuleAlertesList;
import eu.agileeng.accbureau.AppModuleTemplate;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDescriptorsList;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.AEPersistentUtil;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.util.AEDateUtil;

/**
 * @author vvatov
 *
 */
public class AppDAO extends AbstractDAO {

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	public AppDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	private static String selectSQL = "select * from AppConfig where id = ?";
	
	private static String selectSQLByCompany = 
		"select ac.*, am.CODE as moduleCode, am.NAME as moduleName "
		+ "from AppConfig ac inner join AppModule am on ac.MODULE_ID = am.ID where ac.COMPANY_ID = ?";
	
	//This will return only modules present in the AppConfig table. 
	//In other words, the AppConfig table describes allowed modules only.
	private static String selectSQLAvailableAppModules = 
		"select * from AppModule module "
		+ " where EXISTS "
		+ " (select ID from AppConfig appConfig where appConfig.company_id = ? "
		+ " and appConfig.module_id = module.id)";
	
	private static String selectSQLIsComponentEnabled = 
		"select 1 from AppConfig appConfig " 
		+ "where appConfig.company_id = ? "
		+ "and appConfig.module_id in ";
	
	private static String deleteSQLByCompany = 
		"delete from AppConfig where COMPANY_ID = ?";
	
	private static String insertSQL = "insert into AppConfig ( "
		+ "CODE, NAME, DESCRIPTION, COMPANY_ID, MODULE_ID, PROPERTIES) values ("
		+ "?, ?, ?, ?, ?, ?)";
	
	private static String updateSQL = "update AppConfig set "
		+ "CODE = ?, NAME = ?, DESCRIPTION = ?, COMPANY_ID = ?, MODULE_ID = ?, PROPERTIES = ?"
		+ " where ID = ?";
	
	private static String selectSQLAppModules = "select * from AppModule";
	
	private static String selectSQLAppModuleByCode = "select * from AppModule where code = ?";
	
	private static String selectSQLAppModule = "select * from AppModule where id = ?";
	
	private static String selectSQLAppModulePath = "with Modules as"
			+" ("
			+" select * from AppModule where CODE = ?"
			+" UNION ALL"
			+" select am.* from AppModule am inner join Modules on am.ID = Modules.PARENT_ID"
			+" where Modules.PARENT_ID IS NOT NULL"
			+" )"
			+" select * from Modules order by ID ASC";
	
	private static String selectSQLAppModuleRepositoryPath = 
			"select path from AppModule where code = ?";
	
	//////////////////////////////
	
	private static String selectSQLModuleAlert = 
		"select mAlert.*, module.ID as moduleId, module.CODE as moduleCode, module.NAME as moduleName "
		+ " from AppModuleAlert mAlert inner join AppModule module "
		+ " on mAlert.module_id = module.ID "
		+ " where mAlert.owner_id = ? "
		+ " and NOT EXISTS (select ID from AppConfig appConfig where mAlert.MODULE_ID = appConfig.MODULE_ID "
		+ " and mAlert.OWNER_ID = appConfig.COMPANY_ID) order by SINDEX";
	
	private static String insertSQLModuleAlert = 
		"insert into AppModuleAlert "
		+ "(CODE, NAME, DESCRIPTION, OWNER_ID, MODULE_ID, PROPERTIES, DELAY, SINDEX) "
		+ " values (?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLModuleAlert = 
		"update AppModuleAlert set "
		+ " CODE = ?, NAME = ?, DESCRIPTION = ?, OWNER_ID = ?, MODULE_ID = ?, PROPERTIES = ?, DELAY = ?, SINDEX = ?"
		+ " where id = ?";
	
	private static String deleteSQLModuleAlert = 
		"delete from AppModuleAlert where id = ?";
	
	private static String deleteSQLModuleAlertByOwnerAndModule = 
		"delete from AppModuleAlert where owner_id = ? and module_id = ?";
	
	/////// Start AppModuleTemplate
	
	private static String selectSQLAppModuleTemplate = 
			"select * From AppModuleTemplate where APP_MODULE_ID = ? and ? >= valid_from and ? <= valid_to";
	
	private static String insertSQLModuleTemplate = 
			"insert into AppModuleTemplate "
			+ "(APP_MODULE_ID, OWNER_ID, CODE, NAME, DESCRIPTION, PROPERTIES, VALID_FROM, VALID_TO) "
			+ " values (?, ?, ?, ?, ?, ?, ?, ?)";
	
	private static String updateSQLModuleTemplate = 
			"update AppModuleTemplate "
			+ "set APP_MODULE_ID = ?, OWNER_ID = ?, CODE = ?, NAME = ?, DESCRIPTION = ?, PROPERTIES = ?, VALID_FROM = ?, VALID_TO = ? "
			+ " where id = ?";
	
	/////// End AppModuleTemplate
	
	
	/////// Start AppModuleRelation
	
	private static String selectSQLAppModuleByRelationTo = 
			"select ap.* from AppModule as ap inner join AppModuleRelation as apr " 
			+ "on ap.CODE = apr.app_module_code_from and apr.APP_MODULE_CODE_TO = ? and apr.RELATION_ID = ?";
	
	private static String selectSQLAppModuleByRelationFrom = 
			"select ap.* from AppModule as ap inner join AppModuleRelation as apr " 
			+ "on ap.CODE = apr.app_module_code_to and apr.app_module_code_from = ? and apr.RELATION_ID = ?";
	
	/////// Start AppModuleRelation
	
	protected void build(AEAppConfig appConfig, ResultSet rs) throws SQLException, AEException {
		// ID
		appConfig.setID(rs.getLong("ID"));
	    // CODE
		appConfig.setCode(rs.getString("CODE"));
	    // NAME
		appConfig.setName(rs.getString("NAME"));
	    // DESCRIPTION
		appConfig.setDescription(rs.getString("DESCRIPTION"));
	    // COMPANY_ID
		long compId = rs.getLong("COMPANY_ID");
		if(!rs.wasNull()) {
			appConfig.setCompany(Organization.lazyDescriptor(compId));
		}
	    // MODULE_ID
		long moduleId = rs.getLong("MODULE_ID");
		if(!rs.wasNull()) {
			AEDescriptor moduleDescr = AEAppModule.lazyDescriptor(moduleId);
			moduleDescr.setCode(rs.getString("moduleCode"));
			moduleDescr.setName(rs.getString("moduleName"));
			appConfig.setModule(moduleDescr);
		}
		appConfig.setProperties(rs.getLong("PROPERTIES"));
	}
	
	protected int build(AEAppConfig appConfig, PreparedStatement ps, int i) throws SQLException, AEException {
	    // CODE
		ps.setString(i++, appConfig.getCode());

		// NAME
		ps.setString(i++, appConfig.getName());

	    // DESCRIPTION
		ps.setString(i++, appConfig.getDescription());

	    // COMPANY_ID
		if(appConfig.getCompany() != null) {
			ps.setLong(i++, appConfig.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}

		// MODULE_ID
		if(appConfig.getModule() != null) {
			ps.setLong(i++, appConfig.getModule().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}

		// PROPERTIES
		ps.setLong(i++, appConfig.getProperties());
		
		// return the current ps position 
		return i;
	}
	
	public void insert(AEAppConfig appConfig) throws AEException {
		assert(!appConfig.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQL);

			int i = 1;
			build(appConfig, ps, i);
			
			// execute
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				appConfig.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			appConfig.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEAppConfig appConfig) throws AEException {
		assert(appConfig != null);
		assert(appConfig.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQL);

			// build statement
			int i = build(appConfig, ps, 1);
			ps.setLong(i++, appConfig.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			appConfig.setView();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppConfig load(AEDescriptor appConfigDescr) throws AEException {
		AEAppConfig ac = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			ps.setLong(1, appConfigDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				ac = new AEAppConfig();
				build(ac, rs);
			}
			
			// set view state
			ac.setView();
			
			return ac;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public Path loadRepositoryPath(AEDescriptor moduleDescr) throws AEException {
		Path path = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleRepositoryPath);
			ps.setString(1, moduleDescr.getCode());
			rs = ps.executeQuery();
			if(rs.next()) {
				path = Paths.get(rs.getString("PATH"));
			}
			
			return path;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModulesList loadAll() throws AEException {
		AEAppModulesList list = new AEAppModulesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModules);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEAppModule am = new AEAppModule();
				build(am, rs);
				
				// set view state
				am.setView();
				
				list.add(am);
			}
			return list;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModule loadAppModuleByCode(String code) throws AEException {
		AEAppModule appModule = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleByCode);
			ps.setString(1, code);
			rs = ps.executeQuery();
			if(rs.next()) {
				appModule = new AEAppModule();
				build(appModule, rs);
				
				// set view state
				appModule.setView();
			}
			return appModule;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModule loadAppModule(long applModuleId) throws AEException {
		AEAppModule appModule = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModule);
			ps.setLong(1, applModuleId);
			rs = ps.executeQuery();
			if(rs.next()) {
				appModule = new AEAppModule();
				build(appModule, rs);
				
				// set view state
				appModule.setView();
			}
			return appModule;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppConfigList loadByCompany(AEDescriptor compDescr) throws AEException {
		AEAppConfigList appConfigList = new AEAppConfigList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLByCompany);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEAppConfig ac = new AEAppConfig();
				build(ac, rs);
				ac.setView();
				appConfigList.add(ac);
			}
			return appConfigList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public boolean isComponentAvailable(AEDescriptor compDescr, AEDescriptorsList subModules) throws AEException {
		boolean available = true;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			StringBuilder sb = new StringBuilder(selectSQLIsComponentEnabled);
			sb.append(AEPersistentUtil.createInClause(subModules));
			ps = getAEConnection().prepareStatement(sb.toString());
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			if(rs.next()) {
				available = false;
			}
			return available;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModulesList loadAvailableAppModules(AEDescriptor compDescr) throws AEException {
		AEAppModulesList list = new AEAppModulesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAvailableAppModules);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEAppModule am = new AEAppModule();
				build(am, rs);
				
				// set view state
				am.setView();
				
				list.add(am);
			}
			return list;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModulesList loadAppModulePath(String code) throws AEException {
		AEAppModulesList list = new AEAppModulesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModulePath);
			ps.setString(1, code);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEAppModule am = new AEAppModule();
				build(am, rs);
				
				// set view state
				am.setView();
				
				list.add(am);
			}
			return list;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void replace(AEAppConfigList appConfList, long compId) throws AEException {
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			
			// delete old list
			ps = getAEConnection().prepareStatement(deleteSQLByCompany);
			ps.setLong(1, compId);
			ps.executeUpdate();
			AEConnection.close(ps);
			
			// insert new list
			ps = getAEConnection().prepareStatement(insertSQL);
			for (AEAppConfig aeAppConfig : appConfList) {
				build(aeAppConfig, ps, 1);
				ps.addBatch();
				
				aeAppConfig.setView();
			}
			ps.executeBatch();
			AEConnection.close(ps);
			
			// delete from module alerts
			ps = getAEConnection().prepareStatement(deleteSQLModuleAlertByOwnerAndModule);
			for (AEAppConfig aeAppConfig : appConfList) {
				if(aeAppConfig.getModule() != null) {
					ps.setLong(1, compId);
					ps.setLong(2, aeAppConfig.getModule().getDescriptor().getID());
					ps.addBatch();
				}
			}
			ps.executeBatch();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	/////////////////////////////////////////////////
	
	protected void build(AEModuleAlert moduleAlert, ResultSet rs) throws SQLException, AEException {
		// ID
		moduleAlert.setID(rs.getLong("ID"));
	    // CODE
		moduleAlert.setCode(rs.getString("CODE"));
	    // NAME
		moduleAlert.setName(rs.getString("NAME"));
	    // DESCRIPTION
		moduleAlert.setDescription(rs.getString("DESCRIPTION"));
	    // OWNER_ID
		long ownerId = rs.getLong("OWNER_ID");
		if(!rs.wasNull()) {
			moduleAlert.setCompany(Organization.lazyDescriptor(ownerId));
		}
	    // MODULE_ID
		long moduleId = rs.getLong("MODULE_ID");
		if(!rs.wasNull()) {
			AEDescriptor moduleDescr = AEAppModule.lazyDescriptor(moduleId);
			moduleDescr.setCode(rs.getString("moduleCode"));
			moduleDescr.setName(rs.getString("moduleName"));
			moduleAlert.setModule(moduleDescr);
		}
		// PROPERTIES
		moduleAlert.setProperties(rs.getLong("PROPERTIES"));
		// DELAY
		moduleAlert.setDelay(rs.getInt("DELAY"));
		// SINDEX
		moduleAlert.setSequenceNumber(rs.getInt("SINDEX"));
	}
	
	protected int build(AEModuleAlert moduleAlert, PreparedStatement ps, int i) throws SQLException, AEException {
	    // CODE
		ps.setString(i++, moduleAlert.getCode());

		// NAME
		ps.setString(i++, moduleAlert.getName());

	    // DESCRIPTION
		ps.setString(i++, moduleAlert.getDescription());

	    // OWNER_ID
		if(moduleAlert.getCompany() != null) {
			ps.setLong(i++, moduleAlert.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}

		// MODULE_ID
		if(moduleAlert.getModule() != null) {
			ps.setLong(i++, moduleAlert.getModule().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}

		// PROPERTIES
		ps.setLong(i++, moduleAlert.getProperties());
		
		// DELAY
		ps.setInt(i++, moduleAlert.getDelay());
		
		// SINDEX
		ps.setLong(i++, moduleAlert.getSequenceNumber());
		
		// return the current ps position 
		return i;
	}
	
	public void insert(AEModuleAlert moduleAlert) throws AEException {
		assert(!moduleAlert.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLModuleAlert);
			build(moduleAlert, ps, 1);
			ps.executeUpdate();
			
			// set generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				moduleAlert.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			moduleAlert.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AEModuleAlert moduleAlert) throws AEException {
		assert(moduleAlert != null);
		assert(moduleAlert.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLModuleAlert);

			// build statement
			int i = build(moduleAlert, ps, 1);
			ps.setLong(i++, moduleAlert.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			moduleAlert.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEModuleAlertesList loadModuleAlertsByCompany(AEDescriptor compDescr) throws AEException {
		AEModuleAlertesList maList = new AEModuleAlertesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLModuleAlert);
			ps.setLong(1, compDescr.getID());
			rs = ps.executeQuery();
			while(rs.next()) {
				AEModuleAlert ma = new AEModuleAlert();
				build(ma, rs);
				ma.setView();
				maList.add(ma);
			}
			return maList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public void deleteModuleAlert(AEDescriptor itemDescr) throws AEException {
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(deleteSQLModuleAlert);

			// build statement
			ps.setLong(1, itemDescr.getID());

			// execute
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AppModuleTemplate loadAppModuleTemplate(AEDescriptor appModuleDescr, Date validOnDate) throws AEException {
		AppModuleTemplate appModuleTemplate = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleTemplate);
			ps.setLong(1, appModuleDescr.getID());
			ps.setDate(2, AEPersistentUtil.getSQLDate(validOnDate));
			ps.setDate(3, AEPersistentUtil.getSQLDate(validOnDate));
			rs = ps.executeQuery();
			if(rs.next()) {
				appModuleTemplate = new AppModuleTemplate();
				build(appModuleTemplate, rs);
				
				// set view state
				appModuleTemplate.setView();
			}
			return appModuleTemplate;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(AppModuleTemplate moduleTemplate, ResultSet rs) throws SQLException, AEException {
		// ID
		moduleTemplate.setID(rs.getLong("ID"));
	    // MODULE_ID
		long moduleId = rs.getLong("APP_MODULE_ID");
		if(!rs.wasNull()) {
			AEDescriptor moduleDescr = AEAppModule.lazyDescriptor(moduleId);
			moduleTemplate.setAppModule(moduleDescr);
		}
	    // OWNER_ID
		long ownerId = rs.getLong("OWNER_ID");
		if(!rs.wasNull()) {
			moduleTemplate.setCompany(Organization.lazyDescriptor(ownerId));
		}
	    // CODE
		moduleTemplate.setCode(rs.getString("CODE"));
	    // NAME
		moduleTemplate.setName(rs.getString("NAME"));
	    // DESCRIPTION
		moduleTemplate.setDescription(rs.getString("DESCRIPTION"));
		// PROPERTIES
		moduleTemplate.setProperties(rs.getLong("PROPERTIES"));
		// VALID_FROM
		moduleTemplate.setValidFrom(rs.getDate("VALID_FROM"));
		// VALID_TO
		moduleTemplate.setValidTo(rs.getDate("VALID_TO"));
	}
	
	protected int build(AppModuleTemplate moduleTemplate, PreparedStatement ps, int i) throws SQLException, AEException {
		// APP_MODULE_ID
		if(moduleTemplate.getAppModule() != null) {
			ps.setLong(i++, moduleTemplate.getAppModule().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
	    // OWNER_ID
		if(moduleTemplate.getCompany() != null) {
			ps.setLong(i++, moduleTemplate.getCompany().getDescriptor().getID());
		} else {
			ps.setNull(i++, java.sql.Types.BIGINT);
		}
		
	    // CODE
		ps.setString(i++, moduleTemplate.getCode());

		// NAME
		ps.setString(i++, moduleTemplate.getName());

	    // DESCRIPTION
		ps.setString(i++, moduleTemplate.getDescription());

		// PROPERTIES
		ps.setLong(i++, moduleTemplate.getProperties());
		
		// VALID_FROM
		Date validFrom = moduleTemplate.getValidFrom();
		if(validFrom == null) {
			validFrom = AEDateUtil.DATE_MIN;
		}
		ps.setDate(i++, AEPersistentUtil.getSQLDate(validFrom));
		
        // VALID_TO
		Date validTo = moduleTemplate.getValidTo();
		if(validTo == null) {
			validTo = AEDateUtil.DATE_MAX;
		}
		ps.setDate(i++, AEPersistentUtil.getSQLDate(validTo));
		
		// return the current ps position 
		return i;
	}
	
	public void insert(AppModuleTemplate moduleTemplate) throws AEException {
		assert(!moduleTemplate.isPersistent());
		PreparedStatement ps = null;
	    ResultSet rs = null;
		try {
			// prepare statement and insert
			ps = getAEConnection().prepareGenKeyStatement(insertSQLModuleTemplate);
			build(moduleTemplate, ps, 1);
			ps.executeUpdate();
			
			// process generated key
			rs = ps.getGeneratedKeys();
			if (rs.next()) {
				// propagate generated key
				long id = rs.getLong(1);
				
				//required
				moduleTemplate.setID(id);
			} else {
				throw new AEException(getClass().getName() + "::insert: No keys were generated");
			}
			
			// set view state
			moduleTemplate.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
	
	public void update(AppModuleTemplate moduleTemplate) throws AEException {
		assert(moduleTemplate != null);
		assert(moduleTemplate.isPersistent());
		PreparedStatement ps = null;
		try {
			// create statement
			ps = getAEConnection().prepareStatement(updateSQLModuleTemplate);

			// build statement
			int i = build(moduleTemplate, ps, 1);
			ps.setLong(i++, moduleTemplate.getID());

			// execute
			ps.executeUpdate();
			
			// set view state
			moduleTemplate.setView();
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModule loadAppModuleByRelationTo(String code, AEAppModule.AppModuleRelation relation) throws AEException {
		AEAppModule appModule = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleByRelationTo);
			ps.setString(1, code);
			ps.setLong(2, relation.getId());
			rs = ps.executeQuery();
			if(rs.next()) {
				appModule = new AEAppModule();
				build(appModule, rs);
				
				// set view state
				appModule.setView();
			}
			return appModule;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public AEAppModule loadAppModuleByRelationFrom(String code, AEAppModule.AppModuleRelation relation) throws AEException {
		AEAppModule appModule = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLAppModuleByRelationFrom);
			ps.setString(1, code);
			ps.setLong(2, relation.getId());
			rs = ps.executeQuery();
			if(rs.next()) {
				appModule = new AEAppModule();
				build(appModule, rs);
				
				// set view state
				appModule.setView();
			}
			return appModule;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
}
