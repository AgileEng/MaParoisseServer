/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 16:31:02
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.PreparedStatement;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;
import eu.agileeng.persistent.dao.DAOFactory;


/**
 *
 */
public class CashDAO extends AbstractDAO {

	/**
	 * @param aeConn
	 */
	CashDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
	
	public void save() throws AEException {
		PreparedStatement ps = null;
		try {
			// get ID
			DAOFactory daoFactory = DAOFactory.getInstance();
			long idLong = daoFactory.getSequenceDAO(getAEConnection()).nextValue(SequenceDAO.Sequence.COMMON);
			
			// save
		    ps = getAEConnection().prepareStatement("insert into object_registry (obj_id) values (?)");
		    ps.setLong(1, idLong);
		    ps.executeUpdate();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			AEConnection.close(ps);
			close();
		}
	}
}
