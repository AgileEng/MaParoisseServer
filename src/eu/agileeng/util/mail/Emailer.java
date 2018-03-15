/**
 * 
 */
package eu.agileeng.util.mail;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.mail.SimpleEmail;
import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONException;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.AEExceptionsList;
import eu.agileeng.domain.acc.AccPeriod;
import eu.agileeng.domain.contact.Employee;
import eu.agileeng.domain.document.AEDocument;
import eu.agileeng.domain.document.AEDocumentType;
import eu.agileeng.domain.document.social.AESocialDocument;
import eu.agileeng.domain.jcr.JcrNode;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthPrincipalsList;
import eu.agileeng.security.AuthRole;
import eu.agileeng.security.ejb.dao.AuthPrincipalDAO;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AEStringUtil;

/**
 * @author vvatov
 * @author Niki Mihaylov
 *
 */
public class Emailer {
	
	private static final ExecutorService executor = Executors.newFixedThreadPool(3);

	private Properties props;
	
	private static Logger logger = Logger.getLogger(Emailer.class);
	
	class PeriodClosed implements Runnable {
		private AEDescriptor customerDescr;
		
		private AEInvocationContext invContext;
		
		private AccPeriod accPeriod;
		
		private long appModuleId;
		
		/**
		 * 
		 */
		public PeriodClosed(AEDescriptor customerDescr, AEInvocationContext invContext, AccPeriod accPeriod, long appModuleId) {
			this.customerDescr = customerDescr;
			this.invContext = invContext;
			this.accPeriod = accPeriod;
			this.appModuleId = appModuleId;
		}
		
		@Override
		public void run() {
			AEConnection localConnection = null;
			try {
				DAOFactory daoFactory = DAOFactory.getInstance();
				localConnection = daoFactory.getConnection();
				AuthPrincipalDAO authPrincipalDAO = daoFactory.getAuthPrincipalDAO(localConnection);
				
				String firstName = invContext.getAuthPrincipal().getFirstName();
				String lastName = invContext.getAuthPrincipal().getLastName();
				StringBuffer name = new StringBuffer();
				if(!AEStringUtil.isEmpty(lastName)) {
					name.append(lastName);
				}
				if(!AEStringUtil.isEmpty(firstName)) {
					if(!AEStringUtil.isEmpty(name)) {
						name.append(" ");
					}
					name.append(firstName);
				}
				String companyName = customerDescr.getName();
				String appModuleName = authPrincipalDAO.loadAppModuleName(appModuleId);
				if(AEStringUtil.isEmpty(appModuleName)) {
					appModuleName = AEStringUtil.EMPTY_STRING;
				}
				
				props.setProperty(
						"email.subject", 
						"MonEntreprise: " + companyName + ": nouveau fichier mis à disposition");
				
				// recipients
				AuthPrincipalsList pList = authPrincipalDAO.loadAccountants(customerDescr);
				JSONArray recipients = new JSONArray();
				for (Iterator<AuthPrincipal> iterator = pList.iterator(); iterator.hasNext();) {
					AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
					if(!AEStringUtil.isEmpty(authPrincipal.getEMail())) {
						JSONObject recipient = new JSONObject();
						
						// email
						recipient.put("email", authPrincipal.getEMail());
						
						// name
						StringBuffer recipientName = new StringBuffer();
						if(!AEStringUtil.isEmpty(authPrincipal.getLastName())) {
							recipientName.append(authPrincipal.getLastName());
						}
						if(!AEStringUtil.isEmpty(authPrincipal.getFirstName())) {
							recipientName.append(" ").append(authPrincipal.getFirstName());
						}
						recipient.put("name", recipientName);

						// add to the array
						recipients.put(recipient);
					}
				}
				
				// send email
				if(recipients.length() > 0) {
					// send mail
					StringBuffer sb = new StringBuffer();
					sb.append("Dossier: ")
						.append(companyName)
						.append("\nLe journal ")
						.append(appModuleName)
						.append(" a été clos pour la période ")
						.append(AEDateUtil.convertToString(accPeriod.getEndDate(), "MM/yyyy"))
						.append(" par ") 
						.append(name)
						.append(".")
						.append("\n\nPS: Ceci est un message automatique, merci de ne pas répondre.");
					sendEmail(sb.toString(), recipients);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				AEConnection.close(localConnection);
			}
		}
	}
	
	class ETEBACQuery implements Runnable {
		private AEExceptionsList exceptionsList;
		
		private AEInvocationContext invContext;
		
		
		/**
		 * 
		 */
		public ETEBACQuery(AEExceptionsList exceptionsList, AEInvocationContext invContext) {
			this.exceptionsList = exceptionsList;
			this.invContext = invContext;

		}
		
		@Override
		public void run() {
			try {
				String firstName = invContext.getAuthPrincipal().getFirstName();
				String lastName = invContext.getAuthPrincipal().getLastName();
				StringBuffer name = new StringBuffer();
				if(!AEStringUtil.isEmpty(lastName)) {
					name.append(lastName);
				}
				if(!AEStringUtil.isEmpty(firstName)) {
					if(!AEStringUtil.isEmpty(name)) {
						name.append(" ");
					}
					name.append(firstName);
				}
				
				props.setProperty(
						"email.subject", 
						"MonEntreprise: Ebics import report");
				
				// recipients
				JSONArray recipients = new JSONArray();
				JSONObject recipient = new JSONObject();
				
				// email
				Properties appProperties = AEApp.getInstance().getProperties();
				String toEmail = appProperties.getProperty("jedeclare.mail.send.report.to");
				if(AEStringUtil.isEmpty(toEmail)) {
					toEmail = "monentreprise@secal.fr";
				}
				recipient.put("email", toEmail);
				
				// name
				recipient.put("name", "Monentreprise");

				// add to the array
				recipients.put(recipient);

				// send email
				if(this.exceptionsList != null && !this.exceptionsList.isEmpty() && recipients.length() > 0) {
					// send mail
					StringBuffer sb = new StringBuffer();
					
					for (AEException e : this.exceptionsList) {
						sb.append(e.getMessage());
						sb.append("\r\n");
					}
					
					sb.append("\n\nPS: Ceci est un message automatique, merci de ne pas répondre.");
					sendEmail(sb.toString(), recipients);
				}
			} catch (Throwable t) {
				logger.error("Send email failed ", t);
			} finally {
			}
		}
	}
	
	class JcrFileUploaded implements Runnable {
		
		private AEDescriptor customerDescr;
		
		private AEInvocationContext invContext;
		
		private JcrNode jcrNode;
		
		/**
		 * 
		 */
		public JcrFileUploaded(AEDescriptor customerDescr, AEInvocationContext invContext, JcrNode jcrNode) {
			this.customerDescr = customerDescr;
			this.invContext = invContext;
			this.jcrNode = jcrNode;
		}
		
		@Override
		public void run() {
			AEConnection localConnection = null;
			try {
				DAOFactory daoFactory = DAOFactory.getInstance();
				localConnection = daoFactory.getConnection();
				
				// current user
				AuthPrincipal currUser = this.invContext.getAuthPrincipal();
				String firstName = currUser.getFirstName();
				String lastName = currUser.getLastName();
				StringBuffer name = new StringBuffer();
				if(!AEStringUtil.isEmpty(lastName)) {
					name.append(lastName);
				}
				if(!AEStringUtil.isEmpty(firstName)) {
					if(!AEStringUtil.isEmpty(name)) {
						name.append(" ");
					}
					name.append(firstName);
				}
				String companyName = customerDescr.getName();
				
				props.setProperty(
						"email.subject", 
						"MonEntreprise: " + companyName + ": nouveau fichier mis à disposition");
				
				// recipients
				AuthPrincipalDAO authPrincipalDAO = daoFactory.getAuthPrincipalDAO(localConnection);
				Set<AuthPrincipal> pSet = new HashSet<AuthPrincipal>();
				if(currUser.isMemberOf(AuthRole.System.accountant) && this.jcrNode != null && this.jcrNode.getPath().contains(JcrNode.JcrModule.Comptabilite)) {
					pSet.addAll(authPrincipalDAO.loadOperativeAccountant(customerDescr));
				}
				if(currUser.isMemberOf(AuthRole.System.social)  && this.jcrNode != null && this.jcrNode.getPath().contains(JcrNode.JcrModule.Social)) {
					pSet.addAll(authPrincipalDAO.loadOperativeSocial(customerDescr)); 
				}
				if(currUser.isMemberOf(AuthRole.System.operative_accountant) && this.jcrNode != null && this.jcrNode.getPath().contains(JcrNode.JcrModule.Comptabilite)) {
					pSet.addAll(authPrincipalDAO.loadAccountants(customerDescr));
				}
				if(currUser.isMemberOf(AuthRole.System.operative_social) && this.jcrNode != null && this.jcrNode.getPath().contains(JcrNode.JcrModule.Social)) {
					pSet.addAll(authPrincipalDAO.loadSocials(customerDescr));
				}
				
				// remove current user from the e-mail list
				pSet.remove(currUser);
				
				JSONArray recipients = new JSONArray();
				Map<String, JSONObject> eMails = new HashMap<String, JSONObject>();
				for (Iterator<AuthPrincipal> iterator = pSet.iterator(); iterator.hasNext();) {
					AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
					if(!AEStringUtil.isEmpty(authPrincipal.getEMail())) {
						JSONObject recipient = new JSONObject();
						
						if(eMails.containsKey(authPrincipal.getEMail())) {
							recipient = eMails.get(authPrincipal.getEMail());
							
							// name
							StringBuffer recipientName = new StringBuffer();
							
							// append existing names
							String exRecipientName = recipient.optString("name");
							recipientName.append(exRecipientName);
							if(!AEStringUtil.isEmpty(exRecipientName)) {
								recipientName.append(", ");
							}
							
							// append the new name
							if(!AEStringUtil.isEmpty(authPrincipal.getLastName())) {
								recipientName.append(authPrincipal.getLastName());
							}
							if(!AEStringUtil.isEmpty(authPrincipal.getFirstName())) {
								recipientName.append(" ").append(authPrincipal.getFirstName());
							}
							
							// update the name for this eMail
							recipient.put("name", recipientName);
						} else {
							recipient = new JSONObject();
							
							// email
							recipient.put("email", authPrincipal.getEMail());
							
							// name
							StringBuffer recipientName = new StringBuffer();
							if(!AEStringUtil.isEmpty(authPrincipal.getLastName())) {
								recipientName.append(authPrincipal.getLastName());
							}
							if(!AEStringUtil.isEmpty(authPrincipal.getFirstName())) {
								recipientName.append(" ").append(authPrincipal.getFirstName());
							}
							recipient.put("name", recipientName);

							// add to the array
							recipients.put(recipient);
							
							// add to the eMails
							eMails.put(authPrincipal.getEMail(), recipient);
						}
					}
				}
				
				// send email
				if(recipients.length() > 0) {
					String note = jcrNode.getJcrProperties().getString(JcrNode.JcrProperty.description);
					
					// send mail
					StringBuffer sb = new StringBuffer();
					sb.append("Dossier: ")
						.append(companyName)
						.append("\nNouveau fichier mis à disposition ")
						.append(jcrNode.getPath())
						.append(" par ") 
						.append(name)
						.append(".")
						.append(AEStringUtil.isEmpty(note) ? AEStringUtil.EMPTY_STRING : ("\nNoter: " + note))
						.append("\n\nPS: Ceci est un message automatique, merci de ne pas répondre.");
					sendEmail(sb.toString(), recipients);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				AEConnection.close(localConnection);
			}
		}
	}
	
	class SocialDocumentAction implements Runnable {
		
		private AEDescriptor customerDescr;
		
		private AEInvocationContext invContext;
		
		private AEDocument aeDocument;
		
		private String action;
		
		/**
		 * 
		 */
		public SocialDocumentAction(AEDescriptor customerDescr, AEInvocationContext invContext, AEDocument aeDocument, String action) {
			this.customerDescr = customerDescr;
			this.invContext = invContext;
			this.aeDocument = aeDocument;
			this.action = action;
		}
		
		@Override
		public void run() {
			AEConnection localConnection = null;
			try {
				DAOFactory daoFactory = DAOFactory.getInstance();
				localConnection = daoFactory.getConnection();
				AuthPrincipalDAO authPrincipalDAO = daoFactory.getAuthPrincipalDAO(localConnection);
				
				AuthPrincipal currUser = this.invContext.getAuthPrincipal();
				String firstName = currUser.getFirstName();
				String lastName = currUser.getLastName();
				StringBuffer name = new StringBuffer();
				if(!AEStringUtil.isEmpty(lastName)) {
					name.append(lastName);
				}
				if(!AEStringUtil.isEmpty(firstName)) {
					if(!AEStringUtil.isEmpty(name)) {
						name.append(" ");
					}
					name.append(firstName);
				}
				String companyName = customerDescr.getName();
				
				props.setProperty(
						"email.subject", 
						"MonEntreprise: " + companyName);
				
				// Employee
				String emplName = AEStringUtil.EMPTY_STRING;;
				if(this.aeDocument instanceof AESocialDocument) {
					AESocialDocument socDoc = (AESocialDocument) aeDocument;
					Employee empl = socDoc.getEmployee();
					if(empl != null) {
						emplName = empl.getName();
					}
				}
				
				// recipients
				Set<AuthPrincipal> pSet = new HashSet<AuthPrincipal>();
				if(currUser.isMemberOf(AuthRole.System.social)) {
					pSet.addAll(authPrincipalDAO.loadOperativeSocial(customerDescr)); 
				}
				if(currUser.isMemberOf(AuthRole.System.operative_social)) {
					pSet.addAll(authPrincipalDAO.loadSocials(customerDescr));
				}
				
				// remove current user from the e-mail list
				pSet.remove(currUser);
				
				JSONArray recipients = new JSONArray();
				Map<String, JSONObject> eMails = new HashMap<String, JSONObject>();
				for (Iterator<AuthPrincipal> iterator = pSet.iterator(); iterator.hasNext();) {
					AuthPrincipal authPrincipal = (AuthPrincipal) iterator.next();
					if(!AEStringUtil.isEmpty(authPrincipal.getEMail())) {
						JSONObject recipient = new JSONObject();
						
						if(eMails.containsKey(authPrincipal.getEMail())) {
							recipient = eMails.get(authPrincipal.getEMail());
							
							// name
							StringBuffer recipientName = new StringBuffer();
							
							// append existing names
							String exRecipientName = recipient.optString("name");
							recipientName.append(exRecipientName);
							if(!AEStringUtil.isEmpty(exRecipientName)) {
								recipientName.append(", ");
							}
							
							// append the new name
							if(!AEStringUtil.isEmpty(authPrincipal.getLastName())) {
								recipientName.append(authPrincipal.getLastName());
							}
							if(!AEStringUtil.isEmpty(authPrincipal.getFirstName())) {
								recipientName.append(" ").append(authPrincipal.getFirstName());
							}
							
							// update the name for this eMail
							recipient.put("name", recipientName);
						} else {
							recipient = new JSONObject();
							
							// email
							recipient.put("email", authPrincipal.getEMail());
							
							// name
							StringBuffer recipientName = new StringBuffer();
							if(!AEStringUtil.isEmpty(authPrincipal.getLastName())) {
								recipientName.append(authPrincipal.getLastName());
							}
							if(!AEStringUtil.isEmpty(authPrincipal.getFirstName())) {
								recipientName.append(" ").append(authPrincipal.getFirstName());
							}
							recipient.put("name", recipientName);

							// add to the array
							recipients.put(recipient);
							
							// add to the eMails
							eMails.put(authPrincipal.getEMail(), recipient);
						}
					}
				}
				
				// send email
				if(recipients.length() > 0) {
					// send mail
					StringBuffer sb = new StringBuffer();
					sb
						.append("\nUn document a été mis à jour")
						.append(" par ") 
						.append(name)
						.append(".")
						.append("\n" + getDocType())
						.append("\nDossier: ")
						.append(companyName)
						.append("\nSalarié: " + emplName)
						.append("\nAction: " + action)
						.append(", " + AEDateUtil.formatDateTimeToSystem(new Date()))
						.append("\n\nPS: Ceci est un message automatique, merci de ne pas répondre.");
					sendEmail(sb.toString(), recipients);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			} finally {
				AEConnection.close(localConnection);
			}
		}
		
		private String getDocType() {
			String ret = AEStringUtil.EMPTY_STRING;
			AEDocumentType docType = aeDocument.getType();
			if(AEDocumentType.System.AccidentDuTravail.getSystemID() == docType.getSystemID()) {
				ret = "Accident du Travail ou Maladie Professionnelle";
			} else if(AEDocumentType.System.ContractDeTravail.getSystemID() == docType.getSystemID()) {
				ret = "Attestation d'embauche";
			} else if(AEDocumentType.System.ContractDeTravailAnex.getSystemID() == docType.getSystemID()) {
				ret = "Attestation d'embauche";
			} else if(AEDocumentType.System.Rib.getSystemID() == docType.getSystemID()) {
				ret = "RELEVE D'IDENTITE BANCAIRE";
			} else if(AEDocumentType.System.FinDuTravail.getSystemID() == docType.getSystemID()) {
				ret = "Solde de tout Compte";
			} else if(AEDocumentType.System.CertificatDeTravail.getSystemID() == docType.getSystemID()) {
				ret = "Certificat de travail";
			} else if(AEDocumentType.System.ArretDeTravail.getSystemID() == docType.getSystemID()) {
				ret = "Avis d'Arrét de Travail";
			}
			return ret;
		}
	}
	
	/**
	 * 
	 */
	public Emailer() {
		props = new Properties();
		props.setProperty("email.auth.user", "MonEntrepriseApp@gmail.com");
		props.setProperty("email.auth.password", "MonEntrepriseApp123*");
		props.setProperty("email.smtp.host", "smtp.googlemail.com");
		props.setProperty("email.smtp.port", "25");
		props.setProperty("email.ssl.port", "465");
		props.setProperty("email.sender.email", "MonEntrepriseApp@gmail.com");
		props.setProperty("email.sender.name", "MonEntreprise");
	}
	
	public Emailer(Properties props) {
		this.props = props;
	}
	
	/**
	 * Send a single email.
	 */
	private void sendEmail(String message, JSONArray recipients) throws AEException {
		SimpleEmail email = new SimpleEmail();
		try {
			String user = props.getProperty("email.auth.user");
			if(!AEStringUtil.isEmpty(user)) {
				email.setAuthentication(
						user, 
						AEStringUtil.trim(props.getProperty("email.auth.password")));
			}
			email.setHostName(AEStringUtil.trim(props.getProperty("email.smtp.host")));
			String portStr = props.getProperty("email.smtp.port");
			if(!AEStringUtil.isEmpty(portStr)) {
				try {
					int port = Integer.parseInt(portStr);
					if(port > 0) {
						email.setSmtpPort(port);
					}
				} catch(Exception e) {
					throw new AEException(e);
				}
			}
			email.setFrom(
					AEStringUtil.trim(props.getProperty("email.sender.email")), 
					AEStringUtil.trim(props.getProperty("email.sender.name")));
			for (int i = 0; i < recipients.length(); i++) {
				try {
					JSONObject recipient = recipients.getJSONObject(i);
					email.addTo(
							recipient.optString("email"), 
							recipient.optString("name"));
				} catch (JSONException e) {}
			}
			email.setSubject(AEStringUtil.trim(props.getProperty("email.subject")));
			email.setMsg(AEStringUtil.trim(message));
			email.setSSL(true);
			email.setCharset("utf-8");
			email.send();
		} catch (Throwable t) {
			throw new AEException(t);
		}
	};
	
	public void onPeriodClosed(AEDescriptor customerDescr, AEInvocationContext invContext, AccPeriod accPeriod, long appModuleId) {
		Runnable runner = new PeriodClosed(customerDescr, invContext, accPeriod, appModuleId);
        executor.execute(runner);
	}
	
	public void onJcrFileUploaded(AEDescriptor customerDescr, AEInvocationContext invContext, JcrNode jcrNode) {
		Runnable runner = new JcrFileUploaded(customerDescr, invContext, jcrNode);
        executor.execute(runner);
	}
	
	public void onSocialDocumentAction(AEDescriptor customerDescr, AEInvocationContext invContext, AEDocument aeDocument, String action) {
		Runnable runner = new SocialDocumentAction(customerDescr, invContext, aeDocument, action);
        executor.execute(runner);
	}
	
	public void onEtebacsReport(AEExceptionsList aeExceptionsList, AEInvocationContext invContext) {
		Runnable runner = new ETEBACQuery(aeExceptionsList, invContext);
        executor.execute(runner);
	}
} 
