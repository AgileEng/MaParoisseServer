/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 13:51:11
 */
package eu.agileeng.util;

import eu.agileeng.domain.AEException;

/**
 *
 */
public interface AEValidatable {
	public void validateWith(AEValidator validator) throws AEException;
}
