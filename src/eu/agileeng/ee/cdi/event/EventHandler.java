package eu.agileeng.ee.cdi.event;

import java.io.Serializable;

import javax.ejb.Asynchronous;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.event.Observes;

import org.jboss.logging.Logger;

/**
 * Handler for the two kinds of LogEvent.
 * 
 * Singleton EJB as Event Receiver With Read Lock:
 * @Asynchronous added to the observes method makes the magic of being able to serve asynchronous.
 * @Lock(READ) added to the observes method makes the magic of being able to serve multiple events in paralel.
 * Different threads serving events at the same time are giving you a bigger throughput. So this is:
 * 	- Asynchronous: yes
 *	- Thread-safe observer method: no
 */
@Singleton
public class EventHandler implements Serializable {

    private static final Logger logger = Logger.getLogger(EventHandler.class);
    
    private static final long serialVersionUID = 2013564481486393525L;
    
    private static final String LOGGED_IN_MSG = "Logged in: {0}";
    
    private static final String LOGGED_OFF_MSG = "Logged off: {0}";

    public EventHandler() {
    	logger.debug("EventHandler created: " + this);
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void afterLogin(@Observes @Login LogEvent event) {
    	logger.infov(LOGGED_IN_MSG, event.toString());
	        
        // call a specific login handler class...
    }

    @Asynchronous
    @Lock(LockType.READ)
    public void afterLogoff(@Observes @Logoff LogEvent event) {
    	logger.infov(LOGGED_OFF_MSG, event.toString());
 
        // call a specific logoff handler class...
    }
}