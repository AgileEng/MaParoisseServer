package eu.agileeng.ee.cdi.event;

import java.io.Serializable;

/**
 * Common log event that handles Login and Logoff log types.
 */
public class LogEvent implements Serializable {
    
    private static final long serialVersionUID = -6407967360613478424L;
    
    private String principalName;

    public String getPrincipalName() {
		return principalName;
	}

	public void setPrincipalName(String principalName) {
		this.principalName = principalName;
	}

	public LogEvent() {
    }

    @Override
    public String toString() {
        return principalName;
    }
}