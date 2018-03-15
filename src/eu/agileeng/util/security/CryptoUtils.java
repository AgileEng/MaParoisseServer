package eu.agileeng.util.security;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.Scanner;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class CryptoUtils {

	public static final String AES = "AES";
	private static final String KEY_FILE = "aes.key";
	private static final String PROPERTIES_FILE = "aes.properties";
	public static final String SECURITY_PRINCIPAL = "security_principal";
	public static final String SECURITY_CREDENTIALS = "security_credentials";
	public static final String PROVIDER_URL = "provider_url";
	public static final String PROVIDER_ROOT = "provider_root";

	/**
	 * encrypt a value and generate a keyfile if the keyfile is not found then a
	 * new one is created
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static String encrypt(String value, File keyFile) throws GeneralSecurityException, IOException {
		if (!keyFile.exists()) {
			KeyGenerator keyGen = KeyGenerator.getInstance(CryptoUtils.AES);
			keyGen.init(128);
			SecretKey sk = keyGen.generateKey();
			FileWriter fw = new FileWriter(keyFile);
			fw.write(byteArrayToHexString(sk.getEncoded()));
			fw.flush();
			fw.close();
		}

		SecretKeySpec sks = getSecretKeySpec(keyFile);
		Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
		cipher.init(Cipher.ENCRYPT_MODE, sks, cipher.getParameters());
		byte[] encrypted = cipher.doFinal(value.getBytes());
		return byteArrayToHexString(encrypted);
	}

	public static String encrypt(String value) throws GeneralSecurityException, IOException {
		URL url = CryptoUtils.class.getClassLoader().getResource(KEY_FILE);
		return CryptoUtils.encrypt(value, new File(url.getPath()));
	}
	
	/**
	 * decrypt a value
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static String decrypt(String message, File keyFile) throws GeneralSecurityException, IOException {
		SecretKeySpec sks = getSecretKeySpec(keyFile);
		Cipher cipher = Cipher.getInstance(CryptoUtils.AES);
		cipher.init(Cipher.DECRYPT_MODE, sks);
		byte[] decrypted = cipher.doFinal(hexStringToByteArray(message));
		return new String(decrypted);
	}

	/**
	 * decrypt a value with default <code>KEY_FILE</code> key file
	 * 
	 * @throws GeneralSecurityException
	 * @throws IOException
	 */
	public static String decrypt(String message) throws GeneralSecurityException, IOException {
		URL url = CryptoUtils.class.getClassLoader().getResource(KEY_FILE);
		return CryptoUtils.decrypt(message, new File(url.getPath()));
	}
	
	public static String getValue(String key) throws IOException {
		Properties aesProps = new Properties();
		aesProps.load(CryptoUtils.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE));
		return aesProps.getProperty(key);
	}
	
	private static SecretKeySpec getSecretKeySpec(File keyFile) throws NoSuchAlgorithmException, IOException {
		byte[] key = readKeyFile(keyFile);
		SecretKeySpec sks = new SecretKeySpec(key, CryptoUtils.AES);
		return sks;
	}

	private static byte[] readKeyFile(File keyFile) throws FileNotFoundException {
		Scanner scanner = new Scanner(keyFile).useDelimiter("\\Z");
		String keyValue = scanner.next();
		scanner.close();
		return hexStringToByteArray(keyValue);
	}

	private static String byteArrayToHexString(byte[] b) {
		StringBuffer sb = new StringBuffer(b.length * 2);
		for (int i = 0; i < b.length; i++) {
			int v = b[i] & 0xff;
			if (v < 16) {
				sb.append('0');
			}
			sb.append(Integer.toHexString(v));
		}
		return sb.toString().toUpperCase();
	}

	private static byte[] hexStringToByteArray(String s) {
		byte[] b = new byte[s.length() / 2];
		for (int i = 0; i < b.length; i++) {
			int index = i * 2;
			int v = Integer.parseInt(s.substring(index, index + 2), 16);
			b[i] = (byte) v;
		}
		return b;
	}

	public static void main(String[] args) throws Exception {
		final String KEY_FILE = "c:/temp/aes.key";
		final String PWD_FILE = "c:/temp/aes.properties";

		String value = "";

		Properties p1 = new Properties();
		String encryptedValue = CryptoUtils.encrypt(value, new File(KEY_FILE));
		p1.put("encryptedValue", encryptedValue);
		p1.store(new FileWriter(PWD_FILE), "");

		// ==================
		Properties p2 = new Properties();

		p2.load(new FileReader(PWD_FILE));
		encryptedValue = p2.getProperty("encryptedValue");
		System.out.println(encryptedValue);
		System.out.println(CryptoUtils.decrypt(encryptedValue, new File(KEY_FILE)));
	}
	
	public static String hmac(String data) throws Exception {
		URL url = CryptoUtils.class.getClassLoader().getResource(KEY_FILE);
		SecretKeySpec sks = getSecretKeySpec(new File(url.getPath()));

		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(sks);

		return byteArrayToHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
	}
	
	public static String hmac(String data, String key) throws Exception {
		SecretKeySpec sks = new SecretKeySpec(key.getBytes("UTF-8"), CryptoUtils.AES);;

		Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
		sha256_HMAC.init(sks);

		return byteArrayToHexString(sha256_HMAC.doFinal(data.getBytes("UTF-8")));
	}
}