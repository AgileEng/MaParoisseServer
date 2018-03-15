/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 23.05.2010 13:05:07
 */
package eu.agileeng.accbureau;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import org.jboss.logging.Logger;

import eu.agileeng.security.AuthLoginToken;

/**
 *
 */
@SuppressWarnings("serial")
public class AEApp implements Serializable {

	public static String PROPERTY_FILE = "aes.properties";
	
	public static String fileRepository = "fileRepository";
	public static String imageRepository = "imageRepository";
	public static String tmpRepository = "tmpRepository";
	public static String resourceRepository = "resourceRepository";
	public static String defaultVatCode = "defaultVatCode";
	
	// system
	public static String systemPrincipalName = "system.principal.name";
	public static String systemPrincipalPassword = "system.principal.password";
	
	// ftp Transfer
	public static String ftpTransfer = "ftp.transfer";
	public static String ftpFolder = "ftp.folder";
	public static String ftpServer = "ftp.server";
	public static String ftpRemoteFolder = "ftp.remote.folder";
	public static String ftpPort = "ftp.port";
	public static String ftpUser = "ftp.user";
	public static String ftpPass = "ftp.pass";
	
	// truststore
	public static String truststoreFile = "truststoreFile";
	public static String truststorePass = "truststorePass";
	public static String keystoreFile = "keystoreFile";
	public static String keystorePass = "keystorePass";
	
	// Dematbox
	public static String dematboxFrontalEndpoint = "dematboxFrontalEndpoint";
	
	public static long CASH_MODULE_ID = 1; 
	
	public static long ACCOUNTING_MODULE_ID = 2; 
	
	public static long PURCHASE_MODULE_ID = 100; 
	
	public static long PURCHASE_FNP_MODULE_ID = 110; 
	
	public static long SALE_MODULE_ID = 200; 
	
	public static long CFC_MODULE_ID = 300;
	
	public static long BANK_MODULE_ID = 400;
	
	public static long MANDAT_MODULE_ID = 500;
	
	public static long MANDAT_MODULE_EXT_ID = 550;
	
	public static long PAYMENT_MODULE_ID = 600;
	
	public static long PERIMES_MODULE_ID = 800;
	
	public static long STOCKS_MODULE_ID = 810;
	
	public static long SUIVI_GPL_MODULE_ID = 820;
	
	public static long DONNEES_MODULE_ID = 830;
	
	public static long PAM_MODULE_ID = 900;
	
	public static long IDE_MODULE_ID = 950;
	
	public static long SOCIAL_MODULE_ID = 1000;
	
	public static long SOCIAL_ATTESTATION_MODULE_ID = 1005;
	
	public static long SOCIAL_AVENANT_MODULE_ID = 1010;
	
	public static long SOCIAL_RIB_MODULE_ID = 1015;
	
	public static long SOCIAL_SOLDE_DE_TOUT_COMPTE_MODULE_ID = 1020;
	
	public static long SOCIAL_CERTIFICAT_DE_TRAVAIL_MODULE_ID = 1025;
	
	public static long SOCIAL_ARRET_DE_TRAVAIL_MODULE_ID = 1030;
	
	public static long SOCIAL_ACCIDENT_DE_TRAVAIL_MODULE_ID = 1035;
	
	public static long SOCIAL_DOCUMENTS_MODULE_ID = 1040;
	
	public static long SOCIAL_TIMESHEET_TEMPLATE_MODULE_ID = 1045;
	
	public static long SOCIAL_TIMESHEET_PLAN_MODULE_ID = 1050;

	public static long SOCIAL_TIMESHEET_ACTUAL_MODULE_ID = 1055;
	
	public static long FACTURATION_DEVIS = 1100;
	
	public static long FACTURATION_FACTURE = 1105;
	
	public static long FACTURATION_ARTICLE = 1110;
	
	public static long FACTURATION_CLIENT = 1115;
	
	public static long FACTURATION_PAYMENTS = 1120;
	
	public static long TABLEAU_DE_BORD_ENTRIES = 1210;
	
	public static long TABLEAU_DE_BORD_PORTAL = 1215;
	
	public static long TABLEAU_DE_BORD_EXPLORER = 1220;
	
	public static long INVENTORY_MODULE_ID = 1300;
	
	private static final AEApp inst = new AEApp();
	
    private static Logger logger = Logger.getLogger(AEApp.class);
	
	/**
	 * 
	 */
	private AEApp() {
	}

	public static final AEApp getInstance() {
		return inst;
	}
	
	public static Logger logger() {
		return logger;
	}
	
	public Properties getProperties() {
		Properties props = new Properties();
		InputStream inStream = null;
		try {
			inStream = this.getClass().getResourceAsStream(PROPERTY_FILE);
			if(inStream == null) {
				inStream = this.getClass().getClassLoader().getResourceAsStream(PROPERTY_FILE);
				if(inStream == null) {
					try {
						inStream = new FileInputStream(PROPERTY_FILE);
					} catch (Exception e) {}
					if(inStream == null) {
						ClassLoader cl = Thread.currentThread().getContextClassLoader();
						inStream = cl.getResourceAsStream(PROPERTY_FILE);
					}
				}
			}
			props.load(inStream);
			return props;
		} catch (Exception e) {
			loadDefaultProperties(props);
			return props;
		} finally {
			if(inStream != null) {
				try {
					inStream.close();
				} catch (IOException e) {
				}
			}
		}
	}
	
	/**
	 * 
	 * @param props
	 */
	private synchronized void loadDefaultProperties(Properties props) {
		props.put(fileRepository, "D:\\AccBureau\\fileRepository");
		props.put(imageRepository, "D:\\AccBureau\\imageRepository");
		props.put(ftpFolder, "D:\\AccBureau\\ftpFolder");
		props.put(defaultVatCode, "TN");
		props.put(dematboxFrontalEndpoint, "https://dematbox.sagemcom.com/Frontal/WS/FrontalService");
	}
	
	public final AuthLoginToken getSystemToken() {
		Properties properties = getProperties();
		return new AuthLoginToken(properties.getProperty(systemPrincipalName, "sa"), properties.getProperty(systemPrincipalPassword, "!me123$"));
	}
	
	public final Path getTmpRepository() {
		return Paths.get(getProperties().getProperty(tmpRepository));
	}
}
