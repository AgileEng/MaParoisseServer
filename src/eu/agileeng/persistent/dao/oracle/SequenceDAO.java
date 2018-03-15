/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 20:13:32
 */
package eu.agileeng.persistent.dao.oracle;

import java.sql.ResultSet;
import java.sql.Statement;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;


/**
 *
 */
public class SequenceDAO extends AbstractDAO {

	public static enum Sequence {
		COMMON;
	}

	/**
	 * @param aeConnection
	 * @throws AEException
	 */
	SequenceDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}

	public long nextValue(Sequence sequence) throws AEException {
		Statement s = null;
		ResultSet rs = null;
		try {
			long nextID = 0;
			switch(sequence) {
				case COMMON:
					s = getAEConnection().createStatement(); 
					rs = s.executeQuery("select common.nextval from dual");
					if(rs.next()) {
						nextID = rs.getLong(1);
					}
					break;
				default:
					assert(false) : "The sequence is unknown";
					break;
			}
			return nextID;
		} catch (Exception e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			AEConnection.close(rs);
			AEConnection.close(s);
			close();
		}
	}
}
