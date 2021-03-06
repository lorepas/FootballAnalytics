package stats.persistence.mongo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.google.gson.Gson;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;
import static com.mongodb.client.model.Filters.eq;

import stats.model.DetailedPerformance;
import stats.model.League;
import stats.model.Player;
import stats.model.Team;
import stats.persistence.DAOException;
import stats.persistence.IDAOPlayer;
import stats.utility.Utils;

public class DAOPlayerMongo implements IDAOPlayer {
	
	@Override
	public boolean exists(Player player) throws DAOException {
		MongoClient mongoClient = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			Document filter = new Document();
			filter.append("fullName", player.getFullName());
			MongoCursor<Document> cursor = mongoCollection.find(filter).iterator();
			if(cursor.hasNext()) {
				return true;
			} else {
				return false;
			}
		} catch (MongoException mwe) {
			throw new DAOException(mwe);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
	}

	@Override
	public void createPlayer(Player player) throws DAOException{
		MongoClient mongoClient = null;
		try {
			mongoClient = Utils.getMongoClient();
			Document obj = Document.parse(player.toJSON());
			SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
			if(player.getBornDate() != null) {
				obj.put("bornDate", dateFormatter.parse(player.getBornDate()));
			}
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			mongoCollection.insertOne(obj);
		} catch(MongoException mwe) {
			throw new DAOException(mwe);
		} catch (ParseException e) {
			throw new DAOException(e);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
	}
	
	@Override
	public void createListOfPlayers(List<Player> players) throws DAOException {
		MongoClient mongoClient = null;
		try {
			List<Document> documents = new ArrayList<>();
			for (Player player : players) {
				SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
				Document obj = Document.parse(player.toJSON());
				obj.put("bornDate", dateFormatter.parse(player.getBornDate()));
				documents.add(obj);
			}
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			mongoCollection.insertMany(documents);
		} catch(MongoException mwe) {
			throw new DAOException(mwe);
		} catch(ParseException pe) {
			throw new DAOException(pe);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
	}

	@Override
	public void updatePlayer(String fullName, Player player) throws DAOException {
		MongoClient mongoClient = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			SimpleDateFormat dateFormatter = new SimpleDateFormat("dd/MM/yyyy");
			Bson query = eq("fullName", player.getFullName());
			Document setData = Document.parse(player.toJSON());
			if(player.getBornDate() != null) {
				setData.put("bornDate", dateFormatter.parse(player.getBornDate()));
			}
			Document updateDocument = new Document("$set", setData);
			//System.out.println("Update document: " + updateDocument);
			mongoCollection.updateOne(query, updateDocument);
			//System.out.println("Query: " + query);
		} catch(MongoException mwe) {
			throw new DAOException(mwe);
		} catch (ParseException e) {
			throw new DAOException(e);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
	}

	@Override
	public void deletePlayer(Player player) throws DAOException {
		MongoClient mongoClient = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			Document query = new Document();
			query.append("fullName", player.getFullName());
			mongoCollection.deleteOne(query);
		} catch(MongoException mwe) {
			throw new DAOException(mwe);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
	}

	@Override
	public List<Player> retrievePlayers(String surname) throws DAOException {
		MongoClient mongoClient = null;
		MongoCursor<Document> cursor = null;
		List<Player> players = new ArrayList<Player>();
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document query = new Document();
			query.append("surname", Pattern.compile(".*" + surname + ".*" , Pattern.CASE_INSENSITIVE));
			cursor = mongoDatabase.getCollection("players").find(query).iterator();
			while (cursor.hasNext()) { 
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				Player player = Player.playerFromJson(json);
				players.add(player);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(cursor != null) {
				cursor.close(); 
			}
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return players;
		
	}
	
	@Override
	public List<Player> retrievePlayersFromTeam(Team team) throws DAOException {
		MongoClient mongoClient = null;
		MongoCursor<Document> cursor = null;
		List<Player> players = new ArrayList<Player>();
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");			
			Bson filter = Filters.and(
		              Filters.eq("team", Pattern.compile(".*" + team.getFullName() + ".*" , Pattern.CASE_INSENSITIVE)),
		              Filters.eq("championshipCode", team.getChampionshipCode()));
			cursor = mongoDatabase.getCollection("players").find(filter).iterator();
			while (cursor.hasNext()) { 
				Document document = cursor.next();
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				Player player = Player.playerFromJson(json);
				players.add(player);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(cursor != null) {
				cursor.close(); 
			}
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return players;
		
	}


	@Override
	public List<Player> retrieveAllPlayers() throws DAOException {
		MongoClient mongoClient = null;
		MongoCursor<Document> cursor = null;
		List<Player> players = new ArrayList<Player>();
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			cursor = mongoDatabase.getCollection("players").find().iterator();
			while (cursor.hasNext()) { 
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				Player player = Player.playerFromJson(json);
				players.add(player);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(cursor != null) {
				cursor.close();
			}
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return players;
	}

	@Override
	public Player retrieveYoungerPlayer() throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", 1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveOlderPlayer() throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", -1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} catch(IllegalStateException ie) {
			throw new DAOException(ie);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveMostValuedPlayer() throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("marketValue", -1);
			Document filter = new Document();
			filter.append("marketValue", new Document().append("$exists", true));
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveYougerPlayer(League league) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", -1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			filter.append("championshipCode", league.getChampionshipCode());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveOlderPlayer(League league) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", 1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			filter.append("championshipCode", league.getChampionshipCode());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveMostValuedPlayer(League league) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("marketValue", -1);
			Document filter = new Document();
			filter.append("marketValue", new Document().append("$exists", true));
			filter.append("championshipCode", league.getChampionshipCode());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveYougerPlayer(Team team) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", 1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			filter.append("team", team.getName());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public Player retrieveOlderPlayer(Team team) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("bornDate", -1);
			Document filter = new Document();
			filter.append("bornDate", new Document().append("$exists", true));
			filter.append("team", team.getName());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		
		return player;
	}

	@Override
	public Player retrieveMostValuedPlayer(Team team) throws DAOException {
		MongoClient mongoClient = null;
		Player player = null;
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			Document sort = new Document();
			sort.append("marketValue", -1);
			Document filter = new Document();
			filter.append("marketValue", new Document().append("$exists", true));
			filter.append("league", team.getFullName());
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(filter).sort(sort).limit(1).iterator();
			if(cursor.hasNext()) {
				Document document = cursor.next();
				System.out.println(document.get("bornDate"));
				Date bornDate = (Date) document.get("bornDate");
				if(bornDate != null) {
					String dateString = new SimpleDateFormat("dd/MM/yyyy").format(bornDate);
					document.put("bornDate", dateString);
				}
				String json = document.toJson();
				System.out.println(json);
				player = Player.playerFromJson(json);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return player;
	}

	@Override
	public List<DetailedPerformance> retrieveOverallStatistics() throws DAOException {
		MongoClient mongoClient = null;
		List<DetailedPerformance> d_performances = new ArrayList<>();
		try {
			mongoClient = Utils.getMongoClient();
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCursor<Document> cursor = mongoDatabase.getCollection("players").aggregate(
					Arrays.asList(new Document("$unwind", 
						    new Document("path", "$detailedPerformances")), 
						    new Document("$match", 
						    new Document("role", 
						    new Document("$ne", "Portiere"))), 
						    new Document("$group", 
						    new Document("_id", "$fullName")
						            .append("assists", 
						    new Document("$sum", "$detailedPerformances.assists"))
						            .append("penalityGoals", 
						    new Document("$sum", "$detailedPerformances.penalityGoals"))
						            .append("minutesPerGoal", 
						    new Document("$sum", "$detailedPerformances.minutesPerGoals"))
						            .append("calls", 
						    new Document("$sum", "$detailedPerformances.calls"))
						            .append("presences", 
						    new Document("$sum", "$detailedPerformances.presences"))
						            .append("goals", 
						    new Document("$sum", "$detailedPerformances.goals"))
						            .append("ownGoals", 
						    new Document("$sum", "$detailedPerformances.ownGoals"))
						            .append("substitutionOn", 
						    new Document("$sum", "$detailedPerformances.substitutionOn"))
						            .append("substitutionOff", 
						    new Document("$sum", "$detailedPerformances.substitutionOff"))
						            .append("doubleYellowCards", 
						    new Document("$sum", "$detailedPerformances.doubleYellowCards"))
						            .append("yellowCards", 
						    new Document("$sum", "$detailedPerformances.yellowCards"))
						            .append("redCards", 
						    new Document("$sum", "$detailedPerformances.redCards"))
						            .append("minutesPlayed", 
						    new Document("$sum", "$detailedPerformances.minutesPlayed"))), 
						    new Document("$project", 
						    new Document("_id", 0L)
						            .append("fullName", "$_id")
						            .append("assists", 1L)
						            .append("penalityGoals", 1L)
						            .append("minutesPerGoal", 1L)
						            .append("calls", 1L)
						            .append("presences", 1L)
						            .append("goals", 1L)
						            .append("ownGoals", 1L)
						            .append("substitutionOn", 1L)
						            .append("substitutionOff", 1L)
						            .append("doubleYellowCards", 1L)
						            .append("yellowCards", 1L)
						            .append("redCards", 1L)
						            .append("minutesPlayed", 1L)), 
						    new Document("$match", 
						    new Document("calls", 
						    new Document("$gt", 0L))))
					).cursor();
			int i = 0;
			while(cursor.hasNext()) {
				i++;
				Document document = cursor.next();
				System.out.println("Player n." + i);
				System.out.println(document.toJson());
				System.out.println("\n");
				DetailedPerformance detailedPerformance = DetailedPerformance.teamFromJson(document.toJson());
				d_performances.add(detailedPerformance);
			}
		} catch(MongoException me) {
			throw new DAOException(me);
		} finally {
			if(mongoClient != null) {
				mongoClient.close();
			}
		}
		return d_performances;
	}

}
