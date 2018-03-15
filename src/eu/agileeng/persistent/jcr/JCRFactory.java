package eu.agileeng.persistent.jcr;

import javax.jcr.Repository;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.jcr.imp.JackRabbitFactory;

public abstract class JCRFactory {
	
	/**
	 * Provider enumeration
	 */
	public static enum Provider {
		JackRabbit;
	}

	public final static JCRFactory getDefaultInstance() {
		return getInstance(Provider.JackRabbit);
	}

	public final static JCRFactory getInstance(Provider provider) {
		JCRFactory jcrFactory = null;
		switch(provider) {
			case JackRabbit:
				jcrFactory = JackRabbitFactory.getInst();
				break;
			default:
				assert(false) : "Undefined AEDataSource";
				break;
		}
		return jcrFactory;
	}
	
	public abstract Repository getRepository() throws AEException;

	public abstract JcrSession getSession() throws AEException;

	public final JcrSession getSession(JcrSession jcrSession) throws AEException {
		return jcrSession == null ? 
				getSession() :
					new JcrSession(jcrSession.getSession(), false);
	}
}
