package eu.agileeng.persistent.quete;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.AbstractDAO;

public class QueteDAO extends AbstractDAO {

	public QueteDAO(AEConnection aeConnection) throws AEException {
		super(aeConnection);
	}
}
