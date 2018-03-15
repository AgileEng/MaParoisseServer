/**
 * Agile Engineering Ltd
 * @author Vesko Vatov
 * @date 17.11.2009 14:52:02
 */
package eu.agileeng.persistent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import eu.agileeng.domain.AEException;


/**
 *
 */
public final class AEConnection {

	private Connection connection;

	/**
	 * Where this wrapper owns <code>connection</code> or not
	 */
	private boolean connectionOwner;
	
	/**
	 * Where this wrapper owns <code>transaction</code> or not
	 */
	private boolean transactionOwner;

	/**
	 * @throws SQLException 
	 * 
	 */
	public AEConnection(Connection connection, boolean connectionOwner) {
		this.connection = connection;
		this.connectionOwner = connectionOwner;
	}

	/**
	 * @return the connection
	 */
	public final Connection getConnection() {
		return connection;
	}

	/**
	 * @return the connectionOwner
	 */
	public final boolean isConnectionOwner() {
		return connectionOwner;
	}

	public final Statement createStatement() throws SQLException {
		if(this.connection == null) { 
			throw new SQLException("Underlaying connection is null");
		}
		return this.connection.createStatement();
	}

	public final PreparedStatement prepareStatement(String sql) throws SQLException {
		if(this.connection == null) { 
			throw new SQLException("Underlaying connection is null");
		}
		return this.connection.prepareStatement(sql);
	}
	
	public final PreparedStatement prepareGenKeyStatement(String sql) throws SQLException {
		if(this.connection == null) { 
			throw new SQLException("Underlaying connection is null");
		}
		return this.connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
	}
	
	public static final void close(AEConnection aeConnection) {
		if(aeConnection != null 
				&& aeConnection.getConnection() != null 
				&& aeConnection.isConnectionOwner()) {
			try {
				aeConnection.getConnection().close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final void rollback(AEConnection aeConnection) {
		if(aeConnection != null) {
			aeConnection.rollback();
		}
	}
	
	public static final void close(Statement s) {
		if(s != null) {
			try {
				s.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final void close(PreparedStatement ps) {
		if(ps != null) {
			try {
				ps.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public static final void close(ResultSet rs) {
		if(rs != null) {
			try {
				rs.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	public final void beginTransaction() throws AEException {
		if(this.connection != null) {
			try {
				if(this.connection.getAutoCommit()) {
					// not in transaction, so begin transaction and set transaction owner
					this.connection.setAutoCommit(false);
					this.transactionOwner = true;
				} else {
					// already in transaction
					this.transactionOwner = false;
				}
			} catch (SQLException e) {
				throw new AEException(e.getMessage(), e);
			}
		}
	}
	
	public final void commit() throws AEException {
		if(this.connection != null) {
			try {
				if((!this.connection.getAutoCommit())
						&& (isTransactionOwner() || isConnectionOwner())) {
					
					this.connection.commit();
					this.connection.setAutoCommit(true);
					this.transactionOwner = false;
				}
			} catch (SQLException e) {
				throw new AEException(e.getMessage(), e);
			}
		}
	}
	
	public final void rollback() {
		if(this.connection != null) {
			try {
				if((!this.connection.getAutoCommit())
						&& (isTransactionOwner() || isConnectionOwner())) {
					
					this.connection.rollback();
					this.transactionOwner = false;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	public final boolean isTransactionOwner() {
		return transactionOwner;
	}
}
