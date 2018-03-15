package eu.agileeng.persistent.dao.document.statement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.AEDocumentsList;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.document.AEDocumentDAO;

public class AEStatementDAO extends AEDocumentDAO {

	public AEStatementDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	private static String selectSQLReceipts =  
			"select doc.*, donation.PERSON_ID as personId from Document doc "
			+ " inner join ContributorDonation donation on doc.CONTRIBUTOR_DONATION_ID = donation.ID "
			+ " where doc.OWNER_ID = ? and doc.TYPE_ID = " + AEDocumentType.System.Cerfa_11580_03.getSystemID() + " "
			+ " and donation.YEAR = ?";
	public Map<Long, AEDocumentsList> loadReceiptsGroupedByPersonId(AEDescriptor ownerDescr, int year) throws AEException {
		Map<Long, AEDocumentsList> map = new HashMap<>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getAEConnection().prepareStatement(selectSQLReceipts);
			ps.setLong(1, ownerDescr.getID());
			ps.setLong(2, year);
			rs = ps.executeQuery();
			while(rs.next()) {
				AEDocument doc = new AEDocument();
				build(doc, rs);
				doc.setView();
				
				long personId = rs.getLong("personId");
				
				AEDocumentsList docs = map.get(personId);
				if(docs == null) {
					docs = new AEDocumentsList();
					map.put(personId, docs);
				}
				docs.add(doc);
			}
			return map;
		} catch (SQLException e) {
			throw new AEException(e);
		} finally {
			AEConnection.close(ps);
			AEConnection.close(rs);
			close();
		}
	}
}
