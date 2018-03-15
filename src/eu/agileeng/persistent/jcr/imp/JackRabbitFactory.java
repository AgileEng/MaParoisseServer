package eu.agileeng.persistent.jcr.imp;

import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import eu.agileeng.domain.AEException;
import eu.agileeng.persistent.jcr.JCRFactory;
import eu.agileeng.persistent.jcr.JcrSession;

public class JackRabbitFactory extends JCRFactory {

	private static JackRabbitFactory inst = new JackRabbitFactory();

	/**
	 * 
	 */
	private JackRabbitFactory() {
	}

	public static JackRabbitFactory getInst() {
		return inst;
	}
	
	@Override
	public Repository getRepository() throws AEException {
		try {
			InitialContext ic = new InitialContext();
			Object repObj = ic.lookup("java:jboss/jcr/jackrabbit");
			Repository repository = (Repository) repObj;
			ic.close();
			return repository;
		} catch (NamingException e) {
			throw new AEException(e);
		}
	}
	
	@Override
	public JcrSession getSession() throws AEException {
		try {
			return new JcrSession(
					getRepository()
					.login(new SimpleCredentials("MaParoisse", "MaParoisse123$".toCharArray()), "MaParoisse"), 
					true);
		} catch (RepositoryException e) {
			throw new AEException(e);
		}
	}
}
