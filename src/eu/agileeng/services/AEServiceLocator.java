/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 22.11.2009 17:30:08
 */
package eu.agileeng.services;



/**
 *
 */
@SuppressWarnings("serial")
public class AEServiceLocator extends AEBean implements AEService {

	private static AEServiceLocator inst = new AEServiceLocator();
	
	/**
	 * 
	 */
	private AEServiceLocator() {
	}

	public static final AEServiceLocator getInstance() {
		return inst;
	}
}
