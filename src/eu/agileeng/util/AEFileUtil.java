package eu.agileeng.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class AEFileUtil {

	static final SecureRandom random = new SecureRandom();

	public static final Map<String, String> fileTypeIconClassMap = new HashMap<String, String>();
	static {
		fileTypeIconClassMap.put("unknown", "file-icon-blank");
		fileTypeIconClassMap.put("doc", "file-icon-doc");
		fileTypeIconClassMap.put("docx", "file-icon-docx");
		fileTypeIconClassMap.put("pdf", "file-icon-pdf");
		fileTypeIconClassMap.put("ppt", "file-icon-ppt");
		fileTypeIconClassMap.put("pptx", "file-icon-pptx");
		fileTypeIconClassMap.put("sql", "file-icon-sql");
		fileTypeIconClassMap.put("tex", "file-icon-tex");
		fileTypeIconClassMap.put("txt", "file-icon-txt");
		fileTypeIconClassMap.put("xls", "file-icon-xls");
		fileTypeIconClassMap.put("xlsx", "file-icon-xlsx");
		fileTypeIconClassMap.put("zip", "file-icon-zip");
		fileTypeIconClassMap.put("rar", "file-icon-zip");
		fileTypeIconClassMap.put("7z", "file-icon-zip");
		fileTypeIconClassMap.put("tar", "file-icon-zip");
		fileTypeIconClassMap.put("bmp", "file-icon-image");
		fileTypeIconClassMap.put("dib", "file-icon-image");
		fileTypeIconClassMap.put("jpg", "file-icon-image");
		fileTypeIconClassMap.put("jpeg", "file-icon-image");
		fileTypeIconClassMap.put("jpe", "file-icon-image");
		fileTypeIconClassMap.put("jfif", "file-icon-image");
		fileTypeIconClassMap.put("gif", "file-icon-image");
		fileTypeIconClassMap.put("tif", "file-icon-image");
		fileTypeIconClassMap.put("tiff", "file-icon-image");
		fileTypeIconClassMap.put("png", "file-icon-image");
		fileTypeIconClassMap.put("ico", "file-icon-image");
	}

	public static void deleteParentQuietly(File tmpFile) {
		if(tmpFile != null && tmpFile.exists()) {
			File parent = tmpFile.getParentFile();
			if(parent != null && parent.exists()) {
				FileUtils.deleteQuietly(parent);
			}
		}
	}

	public static void deleteParentQuietlyExt(File tmpFile) {
		if(tmpFile != null) {
			File parent = tmpFile.getParentFile();
			if(parent != null && parent.exists()) {
				FileUtils.deleteQuietly(parent);
			}
		}
	}

	public static void deleteFileQuietly(File tmpFile) {
		if(tmpFile != null && tmpFile.exists()) {
			FileUtils.deleteQuietly(tmpFile);
		}
	}

	public static String getIconClass(String fileName) {
		String iconClass = null;
		String extension = null;
		if(!AEStringUtil.isEmpty(fileName)) {
			extension = FilenameUtils.getExtension(fileName);
			iconClass = fileTypeIconClassMap.get(extension != null ? extension.toLowerCase() : "");
		}
		if(AEStringUtil.isEmpty(iconClass)) {
			iconClass = fileTypeIconClassMap.get("unknown");
		}
		return iconClass;
	}

	/**
	 * 
	 * 
	 * @param prefix The prefix string to be used in generating the file's name; must be at least three characters long
	 * @param suffix The suffix string to be used in generating the file's name; may be null, in which case the suffix "tmp" will be used
	 * @param directory The directory in which the file is to be created, must exists
	 * @return
	 * @throws IOException
	 */
	public static String createTempFileName(String prefix, String ext, File directory) throws IOException {
		if (prefix == null) {
			throw new NullPointerException();
		}
		if (prefix.length() < 3) {
			throw new IllegalArgumentException("Prefix string too short");
		}
		String s = (ext == null) ? "tmp" : ext;
		if (!directory.exists()) {
			throw new IllegalArgumentException("Directory doesn't exists");
		}

		long n = random.nextLong();
		if (n == Long.MIN_VALUE) {
			n = 0;      // corner case
		} else {
			n = Math.abs(n);
		}
		String fName = prefix + Long.toString(n) + "." + s;
		File f = new File(directory, fName);
		for (int i = 0; f.exists(); i++) {
			String newFName = FilenameUtils.getBaseName(fName) + "_" + Integer.toString(i) + "." + s;
			f = new File(directory, newFName);
		}

		return f.getName();
	}
	
	/**
	 * Creates the next version of the file in specified folder if the file
	 * already exists in this folder.
	 * Typical usage: Copy or move the file to folder keeping the previous files.
	 * Example how a new version is created: vesko.dat.n
	 * 
	 * @param file
	 * @param destFolder
	 * @return
	 */
	public static File createNextVersionFile(File file, File destFolder) {
		File f = new File(destFolder, file.getName());
		for (int i = 1; f.exists(); i++) {
			String newVersionName = file.getName() + "." + i;
			f = new File(destFolder, newVersionName);
		}
		return f;
	}
	
	public static List<Path> fileList(Path directory) throws IOException {
		List<Path> directoryNames = new ArrayList<Path>();
		DirectoryStream.Filter<Path> filter = new DirectoryStream.Filter<Path>() {
			public boolean accept(Path path) throws IOException {
				return !Files.isDirectory(path);
			}
		};
		DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, filter);
		for (Path path : directoryStream) {
			directoryNames.add(path);
		}
		return directoryNames;
	}
}
