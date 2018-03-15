package eu.agileeng.util.http;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.util.AEStringUtil;
import eu.agileeng.util.security.CryptoUtils;

public class HttpUtil {
    private static volatile HttpUtil instance = null;

    public final static String SESSION_ATTRIBUTE_AUTH_PRINCIPAL = "authPrincipal";
    final static int MAX_COOKIE_LEN = 4096;            // From RFC 2109
	final static int MAX_COOKIE_PAIRS = 20;			   // From RFC 2109
	final static String CSRF_TOKEN_NAME = "ctoken";
	public final static String MONENTREPRISE_TOKEN_NAME = "metoken";
	public final static String MONENTREPRISE_REQUEST_TOKEN_NAME = "mereqtoken";
    
    public static HttpUtil getInstance() {
        if ( instance == null ) {
            synchronized ( HttpUtil.class ) {
                if ( instance == null ) {
                    instance = new HttpUtil();
                }
            }
        }
        return instance;
    }

	/**
     * No arg constructor.
     */
    private HttpUtil() {
	}

	/**
	 * {@inheritDoc}
     *
     * @param request
     * @param response
     */
    public void killAllCookies(HttpServletRequest request, HttpServletResponse response) {
    	Cookie[] cookies = request.getCookies();
    	if (cookies != null) {
    		for (Cookie cookie : cookies) {
    			killCookie(request, response, cookie.getName());
    		}
    	}
    }

	/**
	 * {@inheritDoc}
     *
     * @param request
     * @param response
     * @param name
     */
	public void killCookie(HttpServletRequest request, HttpServletResponse response, String name) {
		String path = "//";
		String domain="";
		Cookie cookie = getFirstCookie(request, name);
		if ( cookie != null ) {
			path = cookie.getPath();
			domain = cookie.getDomain();
		}
		Cookie deleter = new Cookie( name, "deleted" );
		deleter.setMaxAge( 0 );
		if ( domain != null ) deleter.setDomain( domain );
		if ( path != null ) deleter.setPath( path );
		response.addCookie( deleter );
	}
	
	public void killMetokenCookie(HttpServletResponse response) {
		Cookie deleterCookie = null;
		
		// delete at the / 
		deleterCookie = new Cookie(HttpUtil.MONENTREPRISE_TOKEN_NAME, "deleted");
		deleterCookie.setPath("/");
		deleterCookie.setMaxAge(0);
		addCookie(response, deleterCookie);
		
		// delete at the /AccBureau
		deleterCookie = new Cookie(HttpUtil.MONENTREPRISE_TOKEN_NAME, "deleted");
		deleterCookie.setPath("/AccBureau");
		deleterCookie.setMaxAge(0);
		addCookie(response, deleterCookie);
	}
	
	/**
	 * Utility to return the first cookie matching the provided name.
	 * @param request
	 * @param name
	 */
	private Cookie getFirstCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					return cookie;
				}
			}
		}
		return null;
	}
	
	/**
	 * Utility to return the first cookie matching the provided name.
	 * @param request
	 * @param name
	 */
	public Cookie getTheCookie(HttpServletRequest request, String name) {
		Cookie namedCookie = null;
		int count = 0;
		Cookie[] cookies = request.getCookies();
		if (cookies != null) {
			for (Cookie cookie : cookies) {
				if (cookie.getName().equals(name)) {
					namedCookie = cookie;
					count++;
				}
			}
			if(count != 1) {
				namedCookie = null;
			}
		}
		return namedCookie;
	}
	
    /**
	 * {@inheritDoc}
     * This implementation uses a custom "set-cookie" header rather than Java's
     * cookie interface which doesn't allow the use of HttpOnly. Configure the
     * HttpOnly and Secure settings in ESAPI.properties.
	 */
	public void addCookie(HttpServletResponse response, Cookie cookie) {
//		String name = cookie.getName();
//		String value = cookie.getValue();
//		int maxAge = cookie.getMaxAge();
//		String domain = cookie.getDomain();
//		String path = cookie.getPath();
//		boolean secure = cookie.getSecure();

		// validate the name and value
		//        ValidationErrorList errors = new ValidationErrorList();
		//        String cookieName = ESAPI.validator().getValidInput("cookie name", name, "HTTPCookieName", 50, false, errors);
		//        String cookieValue = ESAPI.validator().getValidInput("cookie value", value, "HTTPCookieValue", 5000, false, errors);

		// if there are no errors, then set the cookie either with a header or normally

		// Issue 23 - If the ESAPI Configuration is set to force secure cookies, force the secure flag on the cookie before setting it
		cookie.setSecure(true);
		cookie.setHttpOnly(true);
		response.addCookie(cookie);
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * This implementation ignores the built-in isSecure() method
	 * and uses the URL to determine if the request was transmitted over SSL.
	 * This is because SSL may have been terminated somewhere outside the
	 * container.
	 */
	public void isSecure(HttpServletRequest request) throws AEException {
	    if (request == null) {
	    	throw new AEException( "Insecure request received: HTTP request was null" );
	    }
	    StringBuffer sb = request.getRequestURL();
	    if (sb == null) {
	    	throw new AEException( "Insecure request received: HTTP request URL was null" );
	    }
	    String url = sb.toString();
	    if (!url.startsWith( "https" ) || !request.isSecure()) {
	    	throw new AEException( "Insecure request received: HTTP request did not use SSL" );
	    }
	}
	
	public String createToken(String sessionId, AuthPrincipal ap) throws AEException {
		try {			
			return createTokenValue(sessionId, ap);
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
	
	public void validateToken(String sessionId, AuthPrincipal ap, String token) throws AEException {
		if(AEStringUtil.isEmpty(token)) {
			throw AEError.System.SECURITY_VIOLATION.toException();
		}
		if(!token.equals(createToken(sessionId, ap))) {
			throw AEError.System.SECURITY_VIOLATION.toException();
		}
	}
	
	private String createTokenValue(String sessionId, AuthPrincipal ap) throws AEException {
		try {
			StringBuilder s = new StringBuilder(sessionId).append("@").append(ap.getID());
			String value = s.toString();
			String encryptionKey = CryptoUtils.hmac(value); 
			String cryptData = CryptoUtils.encrypt(value);
			return CryptoUtils.hmac(cryptData, encryptionKey);
		} catch (Exception e) {
			throw new AEException(e);
		}
	}
}
