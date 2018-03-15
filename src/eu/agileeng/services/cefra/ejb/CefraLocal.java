package eu.agileeng.services.cefra.ejb;

import javax.ejb.Local;

import eu.agileeng.domain.AEException;
import eu.agileeng.domain.cefra.n11580_03.BalanceDataSource;
import eu.agileeng.domain.cefra.n11580_03.BalanceRequest;
import eu.agileeng.domain.cefra.n11580_03.BilanDataSource;
import eu.agileeng.domain.cefra.n11580_03.BilanRequest;
import eu.agileeng.domain.cefra.n11580_03.BordereauParoisseDataSource;
import eu.agileeng.domain.cefra.n11580_03.BordereauParoisseRequest;
import eu.agileeng.domain.cefra.n11580_03.BudgetRealizationDataSource;
import eu.agileeng.domain.cefra.n11580_03.BudgetRealizationRequest;
import eu.agileeng.domain.cefra.n11580_03.Cefra11580_03DataSource;
import eu.agileeng.domain.cefra.n11580_03.Cefra11580_03Request;
import eu.agileeng.domain.cefra.n11580_03.CompteDeResultatDataSource;
import eu.agileeng.domain.cefra.n11580_03.CompteDeResultatRequest;
import eu.agileeng.domain.cefra.n11580_03.CouncilDataSource;
import eu.agileeng.domain.cefra.n11580_03.CouncilRequest;
import eu.agileeng.domain.cefra.n11580_03.DonorsDataSource;
import eu.agileeng.domain.cefra.n11580_03.DonorsRequest;
import eu.agileeng.domain.cefra.n11580_03.FinancesDataSource;
import eu.agileeng.domain.cefra.n11580_03.FinancesRequest;
import eu.agileeng.domain.cefra.n11580_03.GrandLivreDataSource;
import eu.agileeng.domain.cefra.n11580_03.GrandLivreRequest;
import eu.agileeng.domain.cefra.n11580_03.JournauxDataSource;
import eu.agileeng.domain.cefra.n11580_03.JournauxRequest;
import eu.agileeng.services.AEInvocationContext;
import eu.agileeng.services.cefra.CefraService;

@Local
public interface CefraLocal extends CefraService {
	
	public Cefra11580_03DataSource generateDataSource(Cefra11580_03Request request, AEInvocationContext invContext) throws AEException; 
	
	public BordereauParoisseDataSource generateDataSource(BordereauParoisseRequest request, AEInvocationContext invContext) throws AEException; 
	
	public CompteDeResultatDataSource generateDataSource(CompteDeResultatRequest request, AEInvocationContext invContext) throws AEException;
	
	public BalanceDataSource generateDataSource(BalanceRequest request, AEInvocationContext invContext) throws AEException;
	
	public BilanDataSource generateDataSource(BilanRequest request, AEInvocationContext invContext) throws AEException;
	
	public JournauxDataSource generateDataSource(JournauxRequest request, AEInvocationContext invContext) throws AEException;
	
	public GrandLivreDataSource generateDataSource(GrandLivreRequest request, AEInvocationContext invContext) throws AEException;
	
	public BudgetRealizationDataSource generateDataSource(BudgetRealizationRequest request, AEInvocationContext invContext) throws AEException;
	
	public CouncilDataSource generateDataSource(CouncilRequest request, AEInvocationContext invContext) throws AEException;
	
	public FinancesDataSource generateDataSource(FinancesRequest request, AEInvocationContext invContext) throws AEException;
	
	public DonorsDataSource generateDataSource(DonorsRequest request, AEInvocationContext invContext) throws AEException;
}
