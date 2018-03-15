/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.11.2009 15:50:43
 */
package eu.agileeng.domain.contact;

import eu.agileeng.domain.EnumeratedType;
import eu.agileeng.domain.DomainModel.DomainClass;

/**
 *
 */
@SuppressWarnings("serial")
public class TaskCategory extends EnumeratedType {

	/**
	 * @param clazz
	 */
	public TaskCategory() {
		super(DomainClass.TASK_CATEGORY);
	}
}
