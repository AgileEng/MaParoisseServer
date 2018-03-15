package eu.agileeng.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import eu.agileeng.domain.AEException;

public class AEObjectUtil {
	// so that nobody can accidentally create an AEObjectUtil object
	private AEObjectUtil() {
	}

	/**
	 * Returns a deep copy of an object
	 * 
	 * @param oldObj
	 * @return
	 * @throws Exception
	 */
	static public Object deepCopy(Object oldObj) throws AEException {
		ObjectOutputStream oos = null;
		ObjectInputStream ois = null;
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			oos = new ObjectOutputStream(bos);
			// serialize and pass the object
			oos.writeObject(oldObj);
			oos.flush();
			ByteArrayInputStream bin = new ByteArrayInputStream(bos.toByteArray());
			ois = new ObjectInputStream(bin);
			// return the new object
			return ois.readObject();
		} catch (Exception e) {
			throw new AEException(e.getMessage(), e);
		} finally {
			try {
				oos.close();
				ois.close();
			} catch (IOException e) {}
		}
	}
	
	static public boolean areEquals(Object o1, Object o2) {
		return o1 != null ? o1.equals(o2) : false;
	}
}
