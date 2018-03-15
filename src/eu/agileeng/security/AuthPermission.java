package eu.agileeng.security;

import java.security.Permission;
import java.security.PermissionCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * This class represents access to a resource.  An AuthPermission consists
 * of a pathname and a set of actions valid for that resource.
 * <P>
 * Pathname is the pathname of the resource in the <b>System infrastructure resource hierachy</b>
 * granted the specified actions.
 * <p>
 * A pathname that ends in "/*" indicates
 * all immediate children of that resource. A pathname
 * that ends with "/-" indicates all (recursively) children 
 * of that resource. A pathname consisting of
 * the special token "&lt;&lt;ALL FILES&gt;&gt;" matches <b>any</b> resource.
 * <P>
 * Note: A pathname consisting of a single "*" indicates all immediate children
 * of the current resource, while a pathname consisting of a single "-"
 * indicates all immediate children of the current resource and
 * (recursively) all children of the current resource.
 * <P>
 * Be careful when granting AuthPermission. Think about the implications
 * of granting read and especially write access to various resources.
 * The "&lt;&lt;ALL FILES>>" permission with write action is
 * especially dangerous. This grants permission to write to the entire
 * system.
 *
 */

public final class AuthPermission extends Permission {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3204701729110962177L;

	/**
	 * Execute action.
	 */
	public final static int CREATE = 0x1;
	
	/**
	 * Write action.
	 */
	public final static int READ   = 0x2;
	
	/**
	 * Read action.
	 */
	public final static int UPDATE  = 0x4;
	
	/**
	 * Delete action.
	 */
	public final static int DELETE  = 0x8;

	/**
	 * Save action
	 */
	public final static int SAVE     = UPDATE | CREATE;
	
	/**
	 * Save and Delete action
	 */
	public final static int SAVE_AND_DELETE = UPDATE | CREATE | DELETE;
	
	/**
	 * All actions (create,read,update,delete)
	 */
	public final static int ALL     = UPDATE | READ | CREATE | DELETE;
	
	/**
	 * No actions.
	 */
	private final static int NONE    = 0x0;

	// the actions mask
	private transient int mask;

	// does path indicate a directory? (wildcard or recursive)
	private transient boolean directory;

	// is it a recursive directory specification?
	private transient boolean recursive;

	// canonicalized dir path. In the case of
	// directories, it is the name "/blah/*" or "/blah/-" without
	// the last character (the "*" or "-").
	private transient String cpath;

	// static Strings used by init(int mask)
	private static final char RECURSIVE_CHAR = '-';
	private static final char WILD_CHAR = '*';
	private static final char SEPARATOR_CHAR = '/';

	/**
	 * initialize a FilePermission object. Common to all constructors.
	 * Also called during de-serialization.
	 *
	 * @param mask the actions mask to use.
	 *
	 */
	private void init(int mask) {
		if ((mask & ALL) != mask)
			throw new IllegalArgumentException("invalid actions mask");

		if (mask == NONE)
			throw new IllegalArgumentException("invalid actions mask");

		if ((cpath = getName()) == null)
			throw new NullPointerException("name can't be null");

		this.mask = mask;

		if (cpath.equals("<<ALL FILES>>")) {
			directory = true;
			recursive = true;
			cpath = "";
			return;
		}

		int len = cpath.length();
		char last = ((len > 0) ? cpath.charAt(len - 1) : 0);

		if (last == RECURSIVE_CHAR &&
				cpath.charAt(len - 2) == SEPARATOR_CHAR) {
			directory = true;
			recursive = true;
			cpath = cpath.substring(0, --len);
		} else if (last == WILD_CHAR &&
				cpath.charAt(len - 2) == SEPARATOR_CHAR) {
			directory = true;
			//recursive = false;
			cpath = cpath.substring(0, --len);
		} else {
			// overkill since they are initialized to false, but
			// commented out here to remind us...
			//directory = false;
			//recursive = false;
		}

		// XXX: at this point the path should be absolute. die if it isn't?
	}

	/**
	 * Creates a new FilePermission object using an action mask.
	 * More efficient than the FilePermission(String, String) constructor.
	 * Can be used from within
	 * code that needs to create a FilePermission object to pass into the
	 * <code>implies</code> method.
	 *
	 * @param path the pathname of the file/directory.
	 * @param mask the action mask to use.
	 */

	public AuthPermission(String path, int mask) {
		super(path);
		init(mask);
	}

	/**
	 * Checks if this FilePermission object "implies" the specified permission.
	 * <P>
	 * More specifically, this method returns true if:<p>
	 * <ul>
	 * <li> <i>p</i> is an instanceof AuthPermission,<p>
	 * <li> <i>p</i>'s actions are a proper subset of this
	 * object's actions, and <p>
	 * <li> <i>p</i>'s pathname is implied by this object's
	 *      pathname. For example, "/tmp/*" implies "/tmp/foo", since
	 *      "/tmp/*" encompasses all files in the "/tmp" directory,
	 *      including the one named "foo".
	 * </ul>
	 *
	 * @param p the permission to check against.
	 *
	 * @return <code>true</code> if the specified permission is not
	 *                  <code>null</code> and is implied by this object,
	 *                  <code>false</code> otherwise.
	 */
	public boolean implies(Permission p) {
		if (!(p instanceof AuthPermission))
			return false;

		AuthPermission that = (AuthPermission) p;

		// we get the effective mask. i.e., the "and" of this and that.
		// They must be equal to that.mask for implies to return true.

		return ((this.mask & that.mask) == that.mask) && impliesIgnoreMask(that);
	}
	
	/**
	 * Checks if this FilePermission object "denies" the specified permission.
	 * <P>
	 * More specifically, this method returns true if:<p>
	 * <ul>
	 * <li> <i>p</i> is not an instanceof AuthPermission,<p>
	 * <li> <i>p</i>'s pathname is implied by this object's
	 *   pathname. For example, "/tmp/*" implies "/tmp/foo", since
	 *   "/tmp/*" encompasses all files in the "/tmp" directory,
	 *   including the one named "foo" and
	 * <li> this object's actions<p> contains one of <i>p</i>'s actions
	 * </ul>
	 *
	 * @param p the permission to check against.
	 *
	 * @return <code>true</code> if the specified permission is not
	 *                  <code>null</code> and is implied by this object,
	 *                  <code>false</code> otherwise.
	 */
	public boolean denies(Permission p) {
		if (!(p instanceof AuthPermission))
			return false;

		AuthPermission that = (AuthPermission) p;

		// we get the "or" of this and that.
		// They must be not equal to 0 for deny to return true.
		return impliesIgnoreMask(that) && ((this.mask & that.mask) != 0);
	}

	/**
	 * Checks if the Permission's actions are a proper subset of the
	 * this object's actions. Returns the effective mask iff the
	 * this FilePermission's path also implies that FilePermission's path.
	 *
	 * @param that the FilePermission to check against.
	 * @param exact return immediately if the masks are not equal
	 * @return the effective mask
	 */
	boolean impliesIgnoreMask(AuthPermission that) {
		if (this.directory) {
			if (this.recursive) {
				// make sure that.path is longer then path so
				// something like /foo/- does not imply /foo
				if (that.directory) {
					return (that.cpath.length() >= this.cpath.length()) &&
					that.cpath.startsWith(this.cpath);
				}  else {
					return ((that.cpath.length() > this.cpath.length()) &&
							that.cpath.startsWith(this.cpath));
				}
			} else {
				if (that.directory) {
					// if the permission passed in is a directory
					// specification, make sure that a non-recursive
					// permission (i.e., this object) can't imply a recursive
					// permission.
					if (that.recursive)
						return false;
					else
						return (this.cpath.equals(that.cpath));
				} else {
					int last = that.cpath.lastIndexOf(SEPARATOR_CHAR);
					if (last == -1)
						return false;
					else {
						// this.cpath.equals(that.cpath.substring(0, last+1));
						// Use regionMatches to avoid creating new string
						return (this.cpath.length() == (last + 1)) &&
						this.cpath.regionMatches(0, that.cpath, 0, last+1);
					}
				}
			}
		} else if (that.directory) {
			// if this is NOT recursive/wildcarded,
			// do not let it imply a recursive/wildcarded permission
			return false;
		} else {
			return (this.cpath.equals(that.cpath));
		}
	}

	/**
	 * Checks two FilePermission objects for equality. Checks that <i>obj</i> is
	 * a FilePermission, and has the same pathname and actions as this object.
	 * <P>
	 * @param obj the object we are testing for equality with this object.
	 * @return <code>true</code> if obj is a FilePermission, and has the same
	 *          pathname and actions as this FilePermission object,
	 *          <code>false</code> otherwise.
	 */
	public boolean equals(Object obj) {
		if (obj == this)
			return true;

		if (! (obj instanceof AuthPermission))
			return false;

		AuthPermission that = (AuthPermission) obj;

		return (this.mask == that.mask) &&
		this.cpath.equals(that.cpath) &&
		(this.directory == that.directory) &&
		(this.recursive == that.recursive);
	}

	/**
	 * Returns the hash code value for this object.
	 *
	 * @return a hash code value for this object.
	 */

	public int hashCode() {
		return this.cpath.hashCode();
	}

//	/**
//	 * Converts an actions String to an actions mask.
//	 *
//	 * @param action the action string.
//	 * @return the actions mask.
//	 */
//	private static int getMask(String actions) {
//
//		int mask = NONE;
//
//		// Null action valid?
//		if (actions == null) {
//			return mask;
//		}
//		// Check against use of constants (used heavily within the JDK)
//		if (actions == SecurityConstants.FILE_READ_ACTION) {
//			return UPDATE;
//		} else if (actions == SecurityConstants.FILE_WRITE_ACTION) {
//			return READ;
//		} else if (actions == SecurityConstants.FILE_EXECUTE_ACTION) {
//			return CREATE;
//		} else if (actions == SecurityConstants.FILE_DELETE_ACTION) {
//			return DELETE;
//		}
//
//		char[] a = actions.toCharArray();
//
//		int i = a.length - 1;
//		if (i < 0)
//			return mask;
//
//		while (i != -1) {
//			char c;
//
//			// skip whitespace
//			while ((i!=-1) && ((c = a[i]) == ' ' ||
//					c == '\r' ||
//					c == '\n' ||
//					c == '\f' ||
//					c == '\t'))
//				i--;
//
//			// check for the known strings
//			int matchlen;
//
//			if (i >= 3 && (a[i-3] == 'r' || a[i-3] == 'R') &&
//					(a[i-2] == 'e' || a[i-2] == 'E') &&
//					(a[i-1] == 'a' || a[i-1] == 'A') &&
//					(a[i] == 'd' || a[i] == 'D'))
//			{
//				matchlen = 4;
//				mask |= UPDATE;
//
//			} else if (i >= 4 && (a[i-4] == 'w' || a[i-4] == 'W') &&
//					(a[i-3] == 'r' || a[i-3] == 'R') &&
//					(a[i-2] == 'i' || a[i-2] == 'I') &&
//					(a[i-1] == 't' || a[i-1] == 'T') &&
//					(a[i] == 'e' || a[i] == 'E'))
//			{
//				matchlen = 5;
//				mask |= READ;
//
//			} else if (i >= 6 && (a[i-6] == 'e' || a[i-6] == 'E') &&
//					(a[i-5] == 'x' || a[i-5] == 'X') &&
//					(a[i-4] == 'e' || a[i-4] == 'E') &&
//					(a[i-3] == 'c' || a[i-3] == 'C') &&
//					(a[i-2] == 'u' || a[i-2] == 'U') &&
//					(a[i-1] == 't' || a[i-1] == 'T') &&
//					(a[i] == 'e' || a[i] == 'E'))
//			{
//				matchlen = 7;
//				mask |= CREATE;
//
//			} else if (i >= 5 && (a[i-5] == 'd' || a[i-5] == 'D') &&
//					(a[i-4] == 'e' || a[i-4] == 'E') &&
//					(a[i-3] == 'l' || a[i-3] == 'L') &&
//					(a[i-2] == 'e' || a[i-2] == 'E') &&
//					(a[i-1] == 't' || a[i-1] == 'T') &&
//					(a[i] == 'e' || a[i] == 'E'))
//			{
//				matchlen = 6;
//				mask |= DELETE;
//
//			} else {
//				// parse error
//				throw new IllegalArgumentException(
//						"invalid permission: " + actions);
//			}
//
//			// make sure we didn't just match the tail of a word
//			// like "ackbarfaccept".  Also, skip to the comma.
//			boolean seencomma = false;
//			while (i >= matchlen && !seencomma) {
//				switch(a[i-matchlen]) {
//				case ',':
//					seencomma = true;
//					/*FALLTHROUGH*/
//				case ' ': case '\r': case '\n':
//				case '\f': case '\t':
//					break;
//				default:
//					throw new IllegalArgumentException(
//							"invalid permission: " + actions);
//				}
//				i--;
//			}
//
//			// point i at the location of the comma minus one (or -1).
//			i -= matchlen;
//		}
//
//		return mask;
//	}

	/**
	 * Return the current action mask. Used by the FilePermissionCollection.
	 *
	 * @return the actions mask.
	 */

	public int getMask() {
		return mask;
	}

	/**
	 * Return the canonical string representation of the actions.
	 * Always returns present actions in the following order:
	 * create, read, update, deslete.
	 *
	 * @return the canonical string representation of the actions.
	 */
	private static String getActions(int mask) {
		StringBuilder sb = new StringBuilder();
		boolean comma = false;

		if ((mask & CREATE) == CREATE) {
			if (comma) sb.append(',');
			else comma = true;
			sb.append("create");
		}
		
		if ((mask & READ) == READ) {
			if (comma) sb.append(',');
			else comma = true;
			sb.append("read");
		}
		
		if ((mask & UPDATE) == UPDATE) {
			comma = true;
			sb.append("read");
		}

		if ((mask & DELETE) == DELETE) {
			if (comma) sb.append(',');
			else comma = true;
			sb.append("delete");
		}

		return sb.toString();
	}

	/**
	 * Returns the "canonical string representation" of the actions.
	 * That is, this method always returns present actions in the following order:
	 * read, write, execute, delete. For example, if this FilePermission object
	 * allows both write and read actions, a call to <code>getActions</code>
	 * will return the string "read,write".
	 *
	 * @return the canonical string representation of the actions.
	 */
	public String getActions() {
		return getActions(this.mask);
	}


	/**
	 * Returns a new PermissionCollection object for storing FilePermission
	 * objects.
	 * <p>
	 * FilePermission objects must be stored in a manner that allows them
	 * to be inserted into the collection in any order, but that also enables the
	 * PermissionCollection <code>implies</code>
	 * method to be implemented in an efficient (and consistent) manner.
	 *
	 * <p>For example, if you have two FilePermissions:
	 * <OL>
	 * <LI>  <code>"/tmp/-", "read"</code>
	 * <LI>  <code>"/tmp/scratch/foo", "write"</code>
	 * </OL>
	 *
	 * <p>and you are calling the <code>implies</code> method with the FilePermission:
	 *
	 * <pre>
	 *   "/tmp/scratch/foo", "read,write",
	 * </pre>
	 *
	 * then the <code>implies</code> function must
	 * take into account both the "/tmp/-" and "/tmp/scratch/foo"
	 * permissions, so the effective permission is "read,write",
	 * and <code>implies</code> returns true. The "implies" semantics for
	 * FilePermissions are handled properly by the PermissionCollection object
	 * returned by this <code>newPermissionCollection</code> method.
	 *
	 * @return a new PermissionCollection object suitable for storing
	 * FilePermissions.
	 */

	public PermissionCollection newPermissionCollection() {
		return new AuthPermissionCollection();
	}
}

/**
 * A FilePermissionCollection stores a set of FilePermission permissions.
 * FilePermission objects
 * must be stored in a manner that allows them to be inserted in any
 * order, but enable the implies function to evaluate the implies
 * method.
 * For example, if you have two FilePermissions:
 * <OL>
 * <LI> "/tmp/-", "read"
 * <LI> "/tmp/scratch/foo", "write"
 * </OL>
 * And you are calling the implies function with the FilePermission:
 * "/tmp/scratch/foo", "read,write", then the implies function must
 * take into account both the /tmp/- and /tmp/scratch/foo
 * permissions, so the effective permission is "read,write".
 *
 * @see java.security.Permission
 * @see java.security.Permissions
 * @see java.security.PermissionCollection
 *
 *
 * @author Marianne Mueller
 * @author Roland Schemers
 *
 * @serial include
 *
 */

final class AuthPermissionCollection extends PermissionCollection {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3681512339057658108L;
	
	private transient List<Permission> perms;

	/**
	 * Create an empty FilePermissions object.
	 *
	 */

	public AuthPermissionCollection() {
		perms = new ArrayList<Permission>();
	}

	/**
	 * Adds a permission to the FilePermissions. The key for the hash is
	 * permission.path.
	 *
	 * @param permission the Permission object to add.
	 *
	 * @exception IllegalArgumentException - if the permission is not a
	 *                                       FilePermission
	 *
	 * @exception SecurityException - if this FilePermissionCollection object
	 *                                has been marked readonly
	 */

	public void add(Permission permission)
	{
		if (! (permission instanceof AuthPermission))
			throw new IllegalArgumentException("invalid permission: "+
					permission);
		if (isReadOnly())
			throw new SecurityException(
					"attempt to add a Permission to a readonly PermissionCollection");

		synchronized (this) {
			perms.add(permission);
		}
	}

	/**
	 * Check and see if this set of permissions implies the permissions
	 * expressed in "permission".
	 *
	 * @param p the Permission object to compare
	 *
	 * @return true if "permission" is a proper subset of a permission in
	 * the set, false if not.
	 */

	public boolean implies(Permission permission)
	{
		if (! (permission instanceof AuthPermission))
			return false;

		AuthPermission fp = (AuthPermission) permission;

		int desired = fp.getMask();
		int effective = 0;
		int needed = desired;

		synchronized (this) {
			int len = perms.size();
			for (int i = 0; i < len; i++) {
				AuthPermission x = (AuthPermission) perms.get(i);
				if (((needed & x.getMask()) != 0) && x.impliesIgnoreMask(fp)) {
					effective |=  x.getMask();
					if ((effective & desired) == desired)
						return true;
					needed = (desired ^ effective);
				}
			}
		}
		return false;
	}

	public boolean denies(Permission permission) {
		if (!(permission instanceof AuthPermission)) {
			return true;
		}

		synchronized (this) {
			int len = perms.size();
			for (int i = 0; i < len; i++) {
				AuthPermission x = (AuthPermission) perms.get(i);
				if (x.denies(permission)) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Returns an enumeration of all the FilePermission objects in the
	 * container.
	 *
	 * @return an enumeration of all the FilePermission objects.
	 */

	public Enumeration<Permission> elements() {
		// Convert Iterator into Enumeration
		synchronized (this) {
			return Collections.enumeration(perms);
		}
	}
}
