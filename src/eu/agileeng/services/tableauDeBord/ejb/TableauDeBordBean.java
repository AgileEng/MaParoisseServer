package eu.agileeng.services.tableauDeBord.ejb;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.Stateless;

import org.apache.tomcat.util.json.JSONArray;
import org.apache.tomcat.util.json.JSONObject;
import org.jboss.logging.Logger;

import eu.agileeng.domain.AEDescriptor;
import eu.agileeng.domain.AEDomainObject;
import eu.agileeng.domain.AEError;
import eu.agileeng.domain.AEException;
import eu.agileeng.domain.acc.AccAccountBalance;
import eu.agileeng.domain.contact.Organization;
import eu.agileeng.persistent.AEConnection;
import eu.agileeng.persistent.dao.DAOFactory;
import eu.agileeng.persistent.dao.tableauDeBord.TableauDeBordDAO;
import eu.agileeng.security.AuthPermission;
import eu.agileeng.services.AEBean;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.AEInvocationContextValidator;
import eu.agileeng.services.AERequest;
import eu.agileeng.services.AEResponse;
import eu.agileeng.util.AEDateUtil;
import eu.agileeng.util.AETimePeriod;

@Stateless
public class TableauDeBordBean extends AEBean implements TableauDeBordLocal {

	private static final long serialVersionUID = -5995659617660472467L;
	
	private static Logger LOG = Logger.getLogger(TableauDeBordBean.class);

	@Override
	public AEResponse loadChauffage(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int requestedYear = arguments.getInt("year");

			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			authorize(new AuthPermission("System/TableauDeBord/Chauffage", AuthPermission.READ), invContext, ownerDescr);

			Map<Integer, AccAccountBalance> chauffage = new TreeMap<>();
			
			/**
			 * Factory and DAO
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			TableauDeBordDAO tableauDeBordBean = daoFactory.getTableauDeBordDAO(localConnection);
			
			/**
			 * Iterate time periods
			 */
			Date dateEnd = AEDateUtil.getLastDate(11, requestedYear);
			Date dateStart = AEDateUtil.beginOfTheYear(dateEnd);
			int year = requestedYear;
			
			List<String> accCodesList = new ArrayList<>();
			accCodesList.add("6061");
			accCodesList.add("6062");
			
			for (int i = 0; i < 5; i++) {
				// load balance
				AETimePeriod timePeriod = new AETimePeriod(dateStart, dateEnd);
				AccAccountBalance ab = tableauDeBordBean.loadChauffage(ownerDescr, timePeriod, accCodesList);
				ab.setPeriod(timePeriod);
				
				// add to Collection
				chauffage.put(year, ab);
				
				// prepare for next iteration
				year = year - 1;
				dateStart = AEDateUtil.addYearsToDate(dateStart, -1);
				dateEnd = AEDateUtil.addYearsToDate(dateEnd, -1);
			}

			// build response
			JSONArray chauffageArray = new JSONArray();
			Set<Integer> chauffageKeySet = chauffage.keySet();
			for (Integer chauffageYear : chauffageKeySet) {
				AccAccountBalance ab = chauffage.get(chauffageYear);
				JSONObject jsonYear = new JSONObject();
				jsonYear.put("year", AEDateUtil.getYear(ab.getPeriod().getStartDate()));
				jsonYear.put("amount", Math.abs(ab.getFinalBalance()));
				
				chauffageArray.put(jsonYear);
			}
			JSONObject payload = new JSONObject();
			payload.put("chartData", chauffageArray);
			return new AEResponse(payload);
		} catch (Exception e) {
			LOG.error("loadChauffage failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadQuetesOrdinaires(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int requestedYear = arguments.getInt("year");

			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			authorize(new AuthPermission("System/TableauDeBord/QuetesOrdinaires", AuthPermission.READ), invContext, ownerDescr);

			Map<Integer, AccAccountBalance> quetesOrdinaires = new TreeMap<>();
			
			/**
			 * Factory and DAO
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			TableauDeBordDAO tableauDeBordDAO = daoFactory.getTableauDeBordDAO(localConnection);
			
			/**
			 * Iterate time periods
			 */
			Date dateEnd = AEDateUtil.getLastDate(11, requestedYear);
			Date dateStart = AEDateUtil.beginOfTheYear(dateEnd);
			int year = requestedYear;
			
			List<String> accCodesList = new ArrayList<>();
			accCodesList.add("7010");
			
			for (int i = 0; i < 5; i++) {
				// load balance
				AETimePeriod timePeriod = new AETimePeriod(dateStart, dateEnd);
				AccAccountBalance ab = tableauDeBordDAO.loadQuetesOrdinaires(ownerDescr, timePeriod, accCodesList);
				ab.setPeriod(timePeriod);
				
				// add to Collection
				quetesOrdinaires.put(year, ab);
				
				// prepare for next iteration
				year = year - 1;
				dateStart = AEDateUtil.addYearsToDate(dateStart, -1);
				dateEnd = AEDateUtil.addYearsToDate(dateEnd, -1);
			}

			// build response
			JSONArray quetesOrdinairesArray = new JSONArray();
			Set<Integer> quetesOrdinairesKeySet = quetesOrdinaires.keySet();
			for (Integer quetesOrdinairesYear : quetesOrdinairesKeySet) {
				AccAccountBalance ab = quetesOrdinaires.get(quetesOrdinairesYear);
				JSONObject jsonYear = new JSONObject();
				jsonYear.put("year", AEDateUtil.getYear(ab.getPeriod().getStartDate()));
				jsonYear.put("amount", Math.abs(ab.getFinalBalance()));
				
				quetesOrdinairesArray.put(jsonYear);
			}
			JSONObject payload = new JSONObject();
			payload.put("chartData", quetesOrdinairesArray);
			return new AEResponse(payload);
		} catch (Exception e) {
			LOG.error("loadChauffage failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadQuetesParticuleres(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int requestedYear = arguments.getInt("year");

			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			authorize(new AuthPermission("System/TableauDeBord/QuetesParticuleres", AuthPermission.READ), invContext, ownerDescr);

			Map<Integer, AccAccountBalance> quetesParticuleres = new TreeMap<>();
			
			/**
			 * Factory and DAO
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			TableauDeBordDAO tableauDeBordBean = daoFactory.getTableauDeBordDAO(localConnection);
			
			/**
			 * Iterate time periods
			 */
			Date dateEnd = AEDateUtil.getLastDate(11, requestedYear);
			Date dateStart = AEDateUtil.beginOfTheYear(dateEnd);
			int year = requestedYear;
			
			List<String> accCodesList = new ArrayList<>();
			accCodesList.add("7018");
			
			for (int i = 0; i < 5; i++) {
				// load balance
				AETimePeriod timePeriod = new AETimePeriod(dateStart, dateEnd);
				AccAccountBalance ab = tableauDeBordBean.loadQuetesParticuleres(ownerDescr, timePeriod, accCodesList);
				ab.setPeriod(timePeriod);
				
				// add to Collection
				quetesParticuleres.put(year, ab);
				
				// prepare for next iteration
				year = year - 1;
				dateStart = AEDateUtil.addYearsToDate(dateStart, -1);
				dateEnd = AEDateUtil.addYearsToDate(dateEnd, -1);
			}

			// build response
			JSONArray quetesParticuleresArray = new JSONArray();
			Set<Integer> quetesParticuleresKeySet = quetesParticuleres.keySet();
			for (Integer quetesParticuleresYear : quetesParticuleresKeySet) {
				AccAccountBalance ab = quetesParticuleres.get(quetesParticuleresYear);
				JSONObject jsonYear = new JSONObject();
				jsonYear.put("year", AEDateUtil.getYear(ab.getPeriod().getStartDate()));
				jsonYear.put("amount", Math.abs(ab.getFinalBalance()));
				
				quetesParticuleresArray.put(jsonYear);
			}
			JSONObject payload = new JSONObject();
			payload.put("chartData", quetesParticuleresArray);
			return new AEResponse(payload);
		} catch (Exception e) {
			LOG.error("loadChauffage failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSyntheseCharges(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int requestedYear = arguments.getInt("year");

			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			authorize(new AuthPermission("System/TableauDeBord/SyntheseCharges", AuthPermission.READ), invContext, ownerDescr);

			Map<String, AccAccountBalance> syntheseCharges = new TreeMap<>();
			
			/**
			 * Factory and DAO
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			TableauDeBordDAO tableauDeBordBean = daoFactory.getTableauDeBordDAO(localConnection);
			
			/**
			 * Iterate time periods
			 */
			Date dateEnd = AEDateUtil.getLastDate(11, requestedYear);
			Date dateStart = AEDateUtil.beginOfTheYear(dateEnd);
			
			// Articles de culte et de stands
			List<String> accCodesList = new ArrayList<>();
			accCodesList.add("601%");
			accCodesList.add("6068");
			AccAccountBalance ab = tableauDeBordBean.loadSyntheseCharges(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseCharges.put("Articles de culte et de stands", ab);

			// Chauffage
			accCodesList = new ArrayList<>();
			accCodesList.add("6030");
			accCodesList.add("6061");
			ab = tableauDeBordBean.loadSyntheseCharges(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseCharges.put("Chauffage", ab);
			
			// Bâtiments : Entretien, assurance
			accCodesList = new ArrayList<>();
			accCodesList.add("6060");
			accCodesList.add("6062");
			accCodesList.add("6063");
			accCodesList.add("6064");
			accCodesList.add("6066");
			accCodesList.add("6067");
			accCodesList.add("6069");
			accCodesList.add("615%");
			accCodesList.add("6160");
			ab = tableauDeBordBean.loadSyntheseCharges(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseCharges.put("Bâtiments : Entretien, assurance", ab);
			
			// Autres charges / divers
			accCodesList = new ArrayList<>();
			accCodesList.add("6065");
			accCodesList.add("613%");
			accCodesList.add("618%");
			accCodesList.add("6220");
			accCodesList.add("6230");
			accCodesList.add("625%");
			accCodesList.add("6260");
			accCodesList.add("6270");
			accCodesList.add("628%");
			accCodesList.add("6310");
			accCodesList.add("6510");
			accCodesList.add("6610");
			accCodesList.add("671%");
			accCodesList.add("6810");
			accCodesList.add("6815");
			ab = tableauDeBordBean.loadSyntheseCharges(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseCharges.put("Autres charges / divers", ab);
			
			// Charges des personnel
			accCodesList = new ArrayList<>();
			accCodesList.add("6410");
			accCodesList.add("645%");
			accCodesList.add("6480");			
			ab = tableauDeBordBean.loadSyntheseCharges(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseCharges.put("Charges des personnel", ab);

			// build response
			JSONArray syntheseChargesArray = new JSONArray();
			Set<String> syntheseChargesKeySet = syntheseCharges.keySet();
			for (String syntheseCharge : syntheseChargesKeySet) {
				ab = syntheseCharges.get(syntheseCharge);
				JSONObject jsonEntry = new JSONObject()
					.put("year", requestedYear)
					.put("entry", syntheseCharge)
					.put("amount", Math.abs(ab.getFinalBalance()));
				
				syntheseChargesArray.put(jsonEntry);
			}
			JSONObject payload = new JSONObject();
			payload.put("chartData", syntheseChargesArray);
			return new AEResponse(payload);
		} catch (Exception e) {
			LOG.error("loadChauffage failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

	@Override
	public AEResponse loadSyntheseRecettes(AERequest aeRequest, AEInvocationContext invContext) throws AEException {
		AEConnection localConnection = null;
		try {
			/**
			 * Validate invocation context
			 */
			AEInvocationContextValidator invContextValidator = AEInvocationContextValidator.getInstance();
			invContextValidator.validate(invContext);

			/**
			 * Extract request arguments  
			 */
			JSONObject arguments = aeRequest.getArguments();

			// common attributes
			long ownerId = arguments.getLong(AEDomainObject.JSONKey.ownerId.name());
			AEDescriptor ownerDescr = Organization.lazyDescriptor(ownerId);
			long sOwnerId = arguments.getLong(AEDomainObject.JSONKey.sOwnerId.name());
			int requestedYear = arguments.getInt("year");

			/**
			 * Authorize
			 */
			// state validation
			if(ownerId != sOwnerId) {
				throw AEError.System.UNSUFFICIENT_RIGHTS.toException();
			}

			// whether this principal is authorized for specified tenant
			authorize(new AuthPermission("System/TableauDeBord/SyntheseRecettes", AuthPermission.READ), invContext, ownerDescr);

			Map<String, AccAccountBalance> syntheseRecettes= new TreeMap<>();
			
			/**
			 * Factory and DAO
			 */
			DAOFactory daoFactory = DAOFactory.getInstance();
			localConnection = daoFactory.getConnection((AEConnection) invContext.getProperty(AEInvocationContext.AEConnection));
			TableauDeBordDAO tableauDeBordBean = daoFactory.getTableauDeBordDAO(localConnection);
			
			/**
			 * Iterate time periods
			 */
			Date dateEnd = AEDateUtil.getLastDate(11, requestedYear);
			Date dateStart = AEDateUtil.beginOfTheYear(dateEnd);
			
			// Quêtes ordinaires
			List<String> accCodesList = new ArrayList<>();
			accCodesList.add("7010");
			AccAccountBalance ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Quêtes ordinaires", ab);

			// Quêtes particulières / chauffage
			accCodesList = new ArrayList<>();
			accCodesList.add("7011");
			accCodesList.add("7012");
			accCodesList.add("7013");
			accCodesList.add("7014");
			accCodesList.add("7015");
			accCodesList.add("7016");
			accCodesList.add("7017");
			accCodesList.add("7018");
			accCodesList.add("7019");
			ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Quêtes particulières / chauffage", ab);
			
			// Dons et Troncs
			accCodesList = new ArrayList<>();
			accCodesList.add("702%");
			accCodesList.add("704%");
			ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Dons et Troncs", ab);
			
			// Recettes autres
			accCodesList = new ArrayList<>();
			accCodesList.add("7030");
			accCodesList.add("751%");
			accCodesList.add("7610");
			accCodesList.add("7810");
			accCodesList.add("7910");
			ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Recettes autres", ab);

			// Subvention et aides
			accCodesList = new ArrayList<>();
			accCodesList.add("741%");
			ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Subvention et aides", ab);

			// Recettes  exceptionnelles
			accCodesList = new ArrayList<>();
			accCodesList.add("771%");
			ab = tableauDeBordBean.loadSyntheseRecettes(ownerDescr, new AETimePeriod(dateStart, dateEnd), accCodesList);
			syntheseRecettes.put("Recettes  exceptionnelles", ab);
			
			// build response
			JSONArray syntheseRecettesArray = new JSONArray();
			Set<String> syntheseRecettesKeySet = syntheseRecettes.keySet();
			for (String syntheseRecette : syntheseRecettesKeySet) {
				ab = syntheseRecettes.get(syntheseRecette);
				JSONObject jsonEntry = new JSONObject()
					.put("year", requestedYear)
					.put("entry", syntheseRecette)
					.put("amount", Math.abs(ab.getFinalBalance()));
				
				syntheseRecettesArray.put(jsonEntry);
			}
			JSONObject payload = new JSONObject();
			payload.put("chartData", syntheseRecettesArray);
			return new AEResponse(payload);
		} catch (Exception e) {
			LOG.error("loadChauffage failed: ", e);
			throw new AEException(e);
		} finally {
			AEConnection.close(localConnection);
		}
	}

}