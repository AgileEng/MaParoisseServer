package eu.agileeng.services.bank;

import java.io.File;
import java.util.Comparator;

public class EbicsFilesComparator implements Comparator<File> {

	private static EbicsFilesComparator inst = new EbicsFilesComparator();
	
	private EbicsFilesComparator() {
	}

	@Override
	public int compare(File f1, File f2) {

		String fName1 = f1.getName();
		String fName2 = f2.getName();
		
		return fName1.compareTo(fName2);
	}
	
	public static EbicsFilesComparator getInstance() {
		return inst;
	}
}
