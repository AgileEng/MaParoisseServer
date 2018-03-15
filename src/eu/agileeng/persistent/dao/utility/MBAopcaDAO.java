package eu.agileeng.persistent.dao.utility;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.DomainModel.DomainClass;
import eu.agileeng.domain.contact.Address;
import eu.agileeng.domain.contact.Contact;
import eu.agileeng.domain.imp.AEDescriptorImp;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthPrincipalsList;

public class MBAopcaDAO extends AbstractDAO {

	private static String selectSQLCompanies = 
		"select * from Company order by Company_ID asc";
	
	private static String selectSQLUsers = 
		"select * from Users where active = 1 order by User_ID asc";
	
	private static String selectCompaniesTo = 
		"select c.Company_ID as id " 
		+ " from Company c inner join User_Rights ur on c.Company_ID = ur.Company_ID "
		+ " where ur.User_ID = ?";
	
	private static String selectRolesTo = 
		"select mt.ID as id " 
		+ " from Mode m inner join ModeType mt on m.ModeType_ID = mt.ID " 
		+ " where m.User_ID = ? ";
	
	public MBAopcaDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public static MBAopcaDAO getInstance(AEConnection aeConnection) throws AEException {
		return new MBAopcaDAO(new AEConnection(aeConnection.getConnection(), false));
	}
	
	public JSONArray loadCompanies() throws AEException, JSONException {
		JSONArray companiesArray = new JSONArray();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLCompanies);
			rs = ps.executeQuery();
			while(rs.next()) {
				JSONObject org = new JSONObject();
				build(org, rs);
				org.put("dbState", 0);
				
				// add to the companies collection
				companiesArray.put(org);
			}
			return companiesArray;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void build(JSONObject org, ResultSet rs) throws SQLException, AEException, JSONException {
		// [Company_ID]
		org.put("externalId", rs.getLong("Company_ID"));
		
		org.put("externalSystem", "MonBureauAopca");
		
	    // [ParentID]
		org.put("aopcaParentId", rs.getLong("ParentID"));
		
	    // [Indent]
		
	    // [Cegid_ID]
		org.put("code", rs.getString("Cegid_ID"));
		
	    // [Rec_Impots_ID]
		
	    // [CName]
		org.put("name", rs.getString("CName"));
		
	    // [EName]
		
	    // [Address1]
		org.put(Address.key_address, rs.getString("Address1"));
		
	    // [Address2]
		org.put(Address.key_secondaryAddress, rs.getString("Address2"));
		
	    // [PostalCode]
		org.put(Address.key_postCode, rs.getString("PostalCode"));
		
	    // [City]
		org.put(Address.key_town, rs.getString("City"));
		
	    // [PhoneNumber]
		org.put(Contact.key_phone, rs.getString("PhoneNumber"));
		
	    // [FaxNumber]
		org.put(Contact.key_fax, rs.getString("FaxNumber"));
		
	    // [Siret]
		
	    // [Frp]
		
	    // [Tva_Intra]
		org.put("tva_intracon", rs.getString("Tva_Intra"));
	    
		// [Naf]
		
	    // [CC]
	    
		// [Aff_Urssaf]
	    
		// [Lieu_Urssaf]
	    
		// [Caisse_RetraiteAGIRC]
	    
		// [Caisse_RetraiteARRCO]
	    
		// [NA_AGIRC]
	    
		// [NA_ARRCO]
	    
		// [IN_Recette]
	    
		// [IN_Dossier]
	    
		// [IN_Cle]
	    
		// [CDI]
	    
		// [IN_Inspection]
	    
		// [I_Regime]
	    
		// [Company_Email]
		org.put(Contact.key_email, rs.getString("Company_Email"));
		
	    // [Copie_Email]
	    
		// [Paie_Societe]
	    
		// [Paie_Etab]
	}
	
	public AuthPrincipalsList loadUsers() throws AEException {
		AuthPrincipalsList usersList = new AuthPrincipalsList();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLUsers);
			rs = ps.executeQuery();
			while(rs.next()) {
				AuthPrincipal user = new AuthPrincipal();
				build(user, rs);
				
				// add to the companies collection
				usersList.add(user);
			}
			return usersList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public List<Long> loadCompaniesTo(long aopcaUserId) throws AEException {
		List<Long> compList = new ArrayList<Long>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectCompaniesTo);
			ps.setLong(1, aopcaUserId);
			rs = ps.executeQuery();
			while(rs.next()) {
				long id = rs.getLong("id");
				
				// add to the companies collection
				compList.add(id);
			}
			return compList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	public List<Long> loadRolesTo(long aopcaUserId) throws AEException {
		List<Long> rolesList = new ArrayList<Long>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectRolesTo);
			ps.setLong(1, aopcaUserId);
			rs = ps.executeQuery();
			while(rs.next()) {
				long id = rs.getLong("id");
				
				// add to the companies collection
				rolesList.add(id);
			}
			return rolesList;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(ps);
			close();
		}
	}
	
	private void build(AuthPrincipal p, ResultSet rs) throws SQLException {
		// User_ID
		AEDescriptor externalPrincipal = new AEDescriptorImp();
		externalPrincipal.setClazz(DomainClass.AuthSubject);
		externalPrincipal.setID(rs.getLong("User_ID"));
		externalPrincipal.setName("MonBureauAopca");
		p.setExternalPrincipal(externalPrincipal);
		
		// UserName
		p.setName(rs.getString("UserName"));
		
		// Password
		p.setPassword(rs.getString("Password"));
		
		// Company
		// Title	nvarchar(40)	Checked
		
		// FirstName
		p.setFirstName(rs.getString("FirstName"));
		
		// LastName
		p.setLastName(rs.getString("LastName"));
		
		// PostD_ID	int	Unchecked
		// DateRegistered	datetime	Checked

		// Active
		p.setActive(rs.getBoolean("Active"));
		
		// DateLastAccessed	datetime	Checked
		// Logins	int	Checked

		// PhoneNumber
		p.setPhone(rs.getString("PhoneNumber"));
		
		// FaxNumber
		
		// EmailPrimary
		p.setEMail(rs.getString("EmailPrimary"));
		
		// PrintMode	bit	Checked
		// DefaultPage	int	Checked
	}
}
