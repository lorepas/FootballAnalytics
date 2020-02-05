package stats.persistence.n4j;

import static org.neo4j.driver.Values.parameters;

import java.util.List;

import org.neo4j.driver.Driver;
import org.neo4j.driver.Query;
import org.neo4j.driver.Session;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.exceptions.ClientException;

import stats.model.Team;
import stats.persistence.DAOException;
import stats.persistence.IDAOTeam;
import stats.utility.Utils;

public class DAOTeamN4J implements IDAOTeamGraph {

	@Override
	public boolean exists(Team team) throws DAOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void createTeam(Team team) throws DAOException {
		Driver driver = null;
		Session session = null;
		Transaction transaction = null;
		try {
			driver = Utils.getNEO4JDriver();
			session = driver.session();
			transaction = session.beginTransaction();
			String query = "CREATE(:Team {fullName: $fullName})";
			Query createTeamNode = new Query(query, 
					parameters("fullName", team.getFullName()));
			transaction.run(createTeamNode);
			transaction.commit();
		} catch(ClientException ce) {
			if(transaction != null) {
				transaction.rollback();
			}
		} finally {
			if(session != null) {
				session.close();
			}
			if(driver != null) {
				driver.close();
			}
		}
	}

	@Override
	public void createListOfTeams(List<Team> teams) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateTeam(String fullName, Team player) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteTeam(Team player) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Team> retrieveTeams(String surname) throws DAOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Team> retrieveAllTeams() throws DAOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int numberOfWonMatches(Team team) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int numberOfDrawnMatches(Team team) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int numberOfLostMatches(Team team) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public List<Team> retrieveTeamsFromLeague(String name) throws DAOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public double retrieveTeamTotalMarketValue(Team team) throws DAOException {
		return 0;
		// TODO Auto-generated method stub
		
	}

	@Override
	public long retriveNativePlayers(Team team) throws DAOException {
		// TODO Auto-generated method stub
		return 0;
	}

}
