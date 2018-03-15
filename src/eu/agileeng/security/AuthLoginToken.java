/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 20.05.2010 18:05:50
 */
package eu.agileeng.security;

import java.io.Serializable;
import java.net.InetAddress;

import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;

import eu.agileeng.util.json.JSONSerializable;

/**
 * <p>A simple username/password authentication token to support the most widely-used authentication mechanism.  This
 * class also implements the {@link RememberMeAuthenticationToken RememberMeAuthenticationToken} interface to support
 * &quot;Remember Me&quot; services across user sessions as well as the
 * {@link InetAuthenticationToken InetAuthenticationToken} interface to retain the IP address location from where the
 * authentication attempt is occuring.</p>
 *
 * <p>&quot;Remember Me&quot; authentications are disabled by default, but if the application developer wishes to allow
 * it for a login attempt, all that is necessary is to call {@link #setRememberMe setRememberMe(true)}.  If the underlying
 * <tt>SecurityManager</tt> implementation also supports <tt>RememberMe</tt> services, the user's identity will be
 * remembered across sessions.
 *
 * <p>Note that this class stores a password as a char[] instead of a String
 * (which may seem more logical).  This is because Strings are immutable and their
 * internal value cannot be overwritten - meaning even a nulled String instance might be accessible in memory at a later
 * time (e.g. memory dump).  This is not good for sensitive information such as passwords. For more information, see the
 * <a href="http://java.sun.com/j2se/1.5.0/docs/guide/security/jce/JCERefGuide.html#PBEEx">
 * Java Cryptography Extension Reference Guide</a>.</p>
 *
 * <p>To avoid this possibility of later memory access, the application developer should always call
 * {@link #clear() clear()} after using the token to perform a login attempt.</p>
 */
public class AuthLoginToken implements JSONSerializable, Serializable {

	private static final long serialVersionUID = -383553226306253228L;

	/** The username */
    private String username;

    /** The password, in char[] format */
    private char[] password;

    /**
     * The location from where the login attempt occurs, or <code>null</code> if not known or explicitly
     * omitted.
     */
    private InetAddress inetAddress;
    
    private String remoteAddress;

    /**
     * no-arg constructor.
     */
    public AuthLoginToken() {
    }

    /**
     * Constructs a new UsernamePasswordToken encapsulating the username and password submitted
     * during an authentication attempt, with a <tt>null</tt> {@link #getInetAddress() inetAddress} and
     * a <tt>rememberMe</tt> default of <tt>false</tt>
     *
     * <p>This is a convience constructor and maintains the password internally via a character
     * array, i.e. <tt>password.toCharArray();</tt>.  Note that storing a password as a String
     * in your code could have possible security implications as noted in the class JavaDoc.</p>
     *
     * @param username the username submitted for authentication
     * @param password the password string submitted for authentication
     */
    public AuthLoginToken(final String username, final String password) {
        this(username, password, (String) null);
    }

    /**
     * Constructs a new UsernamePasswordToken encapsulating the username and password submitted, the
     * inetAddress from where the attempt is occurring, and a default <tt>rememberMe</tt> value of <tt>false</tt>
     *
     * <p>This is a convience constructor and maintains the password internally via a character
     * array, i.e. <tt>password.toCharArray();</tt>.  Note that storing a password as a String
     * in your code could have possible security implications as noted in the class JavaDoc.</p>
     *
     * @param username    the username submitted for authentication
     * @param password    the password string submitted for authentication
     * @param inetAddress the inetAddress from where the attempt is occuring
     * @since 0.2
     */
    public AuthLoginToken(final String username, final String password, final InetAddress inetAddress) {
    	this.username = username;
    	this.password = password != null ? password.toCharArray() : null;
    	this.inetAddress = inetAddress;
    	this.remoteAddress = null;
    }
    
    public AuthLoginToken(final String username, final String password, final String remoteAddress) {
    	this.username = username;
    	this.password = password != null ? password.toCharArray() : null;
    	this.inetAddress = null;
    	this.remoteAddress = remoteAddress;
    }

     /**
     * Returns the username submitted during an authentication attempt.
     *
     * @return the username submitted during an authentication attempt.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets the username for submission during an authentication attempt.
     *
     * @param username the username to be used for submission during an authentication attempt.
     */
    public void setUsername(String username) {
        this.username = username;
    }


    /**
     * Returns the password submitted during an authentication attempt as a character array.
     *
     * @return the password submitted during an authentication attempt as a character array.
     */
    public char[] getPassword() {
        return password;
    }

    /**
     * Sets the password for submission during an authentication attempt.
     *
     * @param password the password to be used for submission during an authentication attemp.
     */
    public void setPassword(char[] password) {
        this.password = password;
    }

    /**
     * Simply returns {@link #getUsername() getUsername()}.
     *
     * @return the {@link #getUsername() username}.
     * @see org.jsecurity.authc.AuthenticationToken#getPrincipal()
     */
    public Object getPrincipal() {
        return getUsername();
    }

    /**
     * Returns the {@link #getPassword() password} char array.
     *
     * @return the {@link #getPassword() password} char array.
     * @see org.jsecurity.authc.AuthenticationToken#getCredentials()
     */
    public Object getCredentials() {
        return getPassword();
    }

    /**
     * Returns the inetAddress from where the authentication attempt occurs.  May be <tt>null</tt> if the inetAddress
     * is unknown or explicitly omitted.  It is up to the Authenticator implementation processing this token if
     * an authentication attempt without an inetAddress is valid or not.
     *
     * <p>(JSecurity's default Authenticator
     * allows <tt>null</tt> IPs to support localhost and proxy server environments).</p>
     *
     * @return the inetAddress from where the authentication attempt occurs, or <tt>null</tt> if it is unknown or
     *         explicitly omitted.
     * @since 0.2
     */
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    /**
     * Sets the inetAddress from where the authentication attempt occurs.  It is up to the Authenticator
     * implementation processing this token if an authentication attempt without an inetAddress is valid or not.
     *
     * <p>(JSecurity's default Authenticator
     * allows <tt>null</tt> IPs to allow localhost and proxy server environments).</p>
     *
     * @param inetAddress the inetAddress from where the authentication attempt occurs.
     * @since 0.2
     */
    public void setInetAddress(InetAddress inetAddress) {
        this.inetAddress = inetAddress;
    }

    /**
     * Clears out (nulls) the username, password, rememberMe, and inetAddress.  The password bytes are explicitly set to
     * <tt>0x00</tt> before nulling to eliminate the possibility of memory access at a later time.
     */
    public void clear() {
        this.username = null;
        this.inetAddress = null;
        if (this.password != null) {
            for (int i = 0; i < password.length; i++) {
                this.password[i] = 0x00;
            }
            this.password = null;
        }
    }

    /**
     * Returns the String representation.  It does not include the password in the resulting
     * string for security reasons to prevent accidentially printing out a password
     * that might be widely viewable).
     *
     * @return the String representation of the <tt>UsernamePasswordToken</tt>, omitting
     *         the password.
     */
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(getClass().getName());
        sb.append(" - ");
        sb.append(username);
        if (inetAddress != null) {
            sb.append(" (").append(inetAddress).append(")");
        }
        return sb.toString();
    }

	@Override
	public JSONObject toJSONObject() throws JSONException {
		return null;
	}

	@Override
	public void create(JSONObject jsonObject) throws JSONException {
		setUsername(jsonObject.optString(""));
		
	}

	public String getRemoteAddress() {
		return remoteAddress;
	}

	public void setRemoteAddress(String remoteAddress) {
		this.remoteAddress = remoteAddress;
	}
}