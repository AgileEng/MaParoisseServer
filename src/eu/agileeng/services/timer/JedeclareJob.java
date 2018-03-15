package eu.agileeng.services.timer;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;

import org.apache.commons.io.FileUtils;
import org.jboss.logging.Logger;

import eu.agileeng.accbureau.AEApp;
import eu.agileeng.security.AuthLoginToken;
import eu.agileeng.security.AuthPrincipal;
import eu.agileeng.security.AuthService;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.ServiceLocator;
import eu.agileeng.services.ServiceLocator.Services;
import eu.agileeng.services.bank.BankService;
import eu.agileeng.services.imp.AEInvocationContextImp;

// @Singleton
public class JedeclareJob {
	
	private static Logger logger = Logger.getLogger(JedeclareJob.class);

	private Properties aeAppProps = null;
	
	public JedeclareJob() {
	}

	// String jedeclareSchedule = "0 0 0/3 ? * *"; // triger that fires every 3 hours, starting at 0 hour 0 min 0 sec
	// @Schedule(persistent=false, second="0", minute="0", hour="0/3")
	public void execute() {
		try {
			logger.info("Jedeclare job execution start");
			
			/**
			 * Get messages from Jedeclare
			 */
			getMessages();
			
			/**
			 * Process files
			 */
			// login
			AuthLoginToken authToken = AEApp.getInstance().getSystemToken();
			AuthService authService = (AuthService) ServiceLocator.getInstance().getService(Services.AUTH);
			AuthPrincipal authPrincipal = authService.login(authToken);
			AEInvocationContext invContext = new AEInvocationContextImp(authPrincipal);
			
			// execute the task
			BankService bankService = (BankService) ServiceLocator.getInstance().getService(Services.BANK);
			bankService.importEbicsFiles(invContext);
			
			/**
			 * Move processed files
			 */
			
			logger.info("Jedeclare job excution succeeded");
		} catch (Exception e) {
			logger.error("Jedeclare job execution failed ", e);
		}
	}

	private void getMessages() {
		Session session = null;
		Store store = null;
		Folder inbox = null;
		Folder imported = null;
		Message[] copyMessages = new Message[1];
		try {
			/**
			 * Execute the task
			 */

			logger.info("Jedeclare getMessages start");
			
			// Refresh application's properties
			aeAppProps = AEApp.getInstance().getProperties();
			
			// Set up the properties we will use for the connection.
			Properties props = new Properties();
			props.setProperty("mail.store.protocol", aeAppProps.getProperty("jedeclare.mail.store.protocol"));

			// Get a Session object.
			session = Session.getInstance(props, null);

			// Use the session�s getStore() method to return a Store.
			store = session.getStore();

			// Connect to the store.
			store.connect(
					aeAppProps.getProperty("jedeclare.mail.host"), 
					aeAppProps.getProperty("jedeclare.mail.user"), 
					aeAppProps.getProperty("jedeclare.mail.password"));

			// Get the INBOX folder from the store with the getFolder() method.
			inbox = store.getFolder(aeAppProps.getProperty("jedeclare.mail.folder.inbox"));

			// Open the Inbox folder.
			try {
				inbox.open(Folder.READ_WRITE);
			} catch (MessagingException e) {
				inbox.open(Folder.READ_ONLY);
			}
			
			// ensure and open Imported folder 
			imported = store.getFolder(aeAppProps.getProperty("jedeclare.mail.folder.imported"));
			if (!imported.exists()) {
				imported.create(Folder.HOLDS_MESSAGES);
			}
			imported.open(Folder.READ_WRITE);

			// Open the folder you want inside the INBOX folder.
			// Repeat as many times as necessary to reach the folder you�re seeking.

			// Get the messages count from the folder.
			int messageCount = inbox.getMessageCount();

			// Iterate through messages, processing each one in turn using the methods of the Message class.
			for (int i = 1; i <= messageCount; i++) {
				Message msg = inbox.getMessage(i);

				logger.info("------------ Message " + i + " ------------");

				// Get the headers
				String from = InternetAddress.toString(msg.getFrom());
				if (from != null) {
					logger.info("From: " + from);
				}

				String to = InternetAddress.toString(msg.getRecipients(Message.RecipientType.TO));
				if (to != null) {
					logger.info("To: " + to);
				}

				String subject = msg.getSubject();
				if (subject != null) {
					logger.info("Subject: " + subject);
				}

				// Process parts
				// Use this optimized method because we are interesting only for attachments
				if (msg.isMimeType("multipart/*")) {
					// p is a Multipart
					processMultipart((Multipart) msg.getContent());
				} 
				
				// move message
				Arrays.fill(copyMessages, msg);
				inbox.copyMessages(copyMessages, imported);
				msg.setFlag(Flags.Flag.DELETED, true);
				Arrays.fill(copyMessages, null);
			}
			
			logger.info("Jedeclare getMessages succeeded");
		} catch (Exception e) {
			logger.error("Jedeclare getMessages failed ", e);
		} finally {
			// close inbox and expunge deleted messages
			if(inbox != null && inbox.isOpen()) {
				try {
					inbox.close(true);
				} catch (MessagingException e) {}
			}
			
			// close imported and don't remove deleted messages
			if(imported != null && imported.isOpen()) {
				try {
					imported.close(false);
				} catch (MessagingException e) {}
			}

			// close store
			if(store != null && store.isConnected()) {
				try {
					store.close();
				} catch (MessagingException e) {}
			}
		}
	}
	
	private void processMultipart(Multipart mp) throws MessagingException, IOException {
		for (int i = 0; i < mp.getCount(); i++) {
			processPart(mp.getBodyPart(i));
		}
	}

	private void processPart(Part p) throws MessagingException, IOException {
		if (p.isMimeType("multipart/*")) {
			// p is a Multipart
			processMultipart((Multipart) p.getContent());
		} else if (p instanceof MimeBodyPart) {
			String disposition = p.getDisposition();
			if (disposition != null && Part.ATTACHMENT.equalsIgnoreCase(disposition)) {
				// clearly attachment
				processAttachment(p);
			}
		}
	}

	private void processAttachment(Part p) throws MessagingException, IOException {
		String folder = aeAppProps.getProperty("jedeclare.mail.local.folder.inbox");

		String fileName = p.getFileName();
		if (fileName == null) { 
			// likely inline
			fileName = "Attachment_" + System.currentTimeMillis();
		}

		// find a file that does not yet exist
		File f = new File(folder, fileName);
		for (int i = 1; f.exists(); i++) {
			String newName = fileName + "." + i;
			f = new File(folder, newName);
		}

		// save to file
		logger.info("Attachment: " + f.getAbsolutePath());
		InputStream in = p.getInputStream();
		FileUtils.copyInputStreamToFile(in, f);
		try {
			in.close();
		} catch (Exception e) {
			logger.error("Cannot close input stream", e);
		}
	}
}
