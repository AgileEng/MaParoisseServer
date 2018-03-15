package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.contact.OrganizationTemplate;
import eu.agileeng.domain.contact.OrganizationTemplatesList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class OrganizationTemplateDAO extends AbstractDAO {

	private static String selectSQL = 
			"select * from CompanyTemplate order by code";
	
	protected OrganizationTemplateDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	/**
	 * @return <code>not null, but can be empty</code> OrganizationTemplate list fetched from CompanyTemplate table. 
	 * @throws AEException
	 */
	public OrganizationTemplatesList load() throws AEException {
		OrganizationTemplatesList loaded = new OrganizationTemplatesList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQL);
			rs = ps.executeQuery();
			while(rs.next()) {
				OrganizationTemplate org = new OrganizationTemplate();
				
				// build
				build(org, rs);
				
				// set view
				org.setView();
				
				loaded.add(org);
			}
			return loaded;
		} catch (SQLException e) {
			logger.errorv("{0} in {1}#{2}: {3}", e.getClass().getSimpleName(), this.getClass().getSimpleName(), "load", e.getMessage());
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	protected void build(OrganizationTemplate org, ResultSet rs) throws SQLException, AEException {
		// build common attributes
		super.build(org, rs);

	    // [POSTAL_CODE]
		org.getAttributes().put(OrganizationTemplate.JSONKey.postCode.name(), rs.getString("POSTAL_CODE"));
		
	    // [CITY]
		org.getAttributes().put(OrganizationTemplate.JSONKey.town.name(), rs.getString("CITY"));
		
		// NATURE
		org.getAttributes().put(OrganizationTemplate.JSONKey.nature.name(), rs.getString("NATURE"));
		
		// STATUT
		org.getAttributes().put(OrganizationTemplate.JSONKey.statut.name(), rs.getString("STATUT"));
	}
}
