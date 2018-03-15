/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 14:04:27
 */
package eu.agileeng.util;

import org.jboss.logging.Logger;

import eu.agileeng.domain.AEException;

/**
 *
 */
public interface AEValidator {
	
	public static Logger logger = Logger.getLogger(AEValidator.class);
	
	public void validate(Object o) throws AEException;
}
