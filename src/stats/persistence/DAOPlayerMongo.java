package stats.persistence;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ClusterType;

import stats.model.Player;
import stats.utility.Utils;

public class DAOPlayerMongo implements IDAOPlayer {
	
	@Override
	public boolean exists(Player player) throws DAOException {
		try {
			MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
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
		} catch (MongoWriteException mwe) {
			throw new DAOException(mwe);
		}
	}

	@Override
	public void createPlayer(Player player) throws DAOException{
		try {
			Document obj = Document.parse(player.toJSON());
			MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			mongoCollection.insertOne(obj);
		} catch(MongoWriteException mwe) {
			throw new DAOException(mwe);
		}
	}
	
	@Override
	public void createListOfPlayers(List<Player> players) throws DAOException {
		try {
			List<Document> documents = new ArrayList<>();
			for (Player player : players) {
				Document obj = Document.parse(player.toJSON());
				documents.add(obj);
			}
			MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
			ClusterDescription clusterDescription = mongoClient.getClusterDescription();
			if(clusterDescription.getType() == ClusterType.UNKNOWN) {
				throw new DAOException("Connection refused: you should connect with VPN");
			}
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			mongoCollection.insertMany(documents);
		} catch(MongoWriteException mwe) {
			throw new DAOException(mwe);
		} catch(MongoSocketOpenException msoe) {
			throw new DAOException(msoe);
		}
	}

	@Override
	public void updatePlayer(String fullName, Player player) throws DAOException {
		MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
		try {
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			Document query = new Document();
			query.append("fullName", fullName);
			Document setData = new Document();
			setData.append("fullName", player.getFullName());
			setData.append("name", player.getName());
			setData.append("surname", player.getSurname());
			setData.append("marketValueString", player.getMarketValueString());
			setData.append("link", player.getLink());
			setData.append("nation", player.getNation());
			setData.append("role", player.getRole());
			setData.append("team", player.getTeam());
			mongoCollection.findOneAndUpdate(query, setData);
		} catch(MongoWriteException mwe) {
			throw new DAOException(mwe);
		} finally {
			mongoClient.close();
		}
	}

	@Override
	public void deletePlayer(String playerName, Player player) throws DAOException {
		MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
		try {
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			Document query = new Document();
			query.append("fullName", player.getFullName());
			mongoCollection.deleteOne(query);
		} catch(MongoWriteException mwe) {
			throw new DAOException(mwe);
		} finally {
			mongoClient.close();
		}
	}

	@Override
	public List<Player> retrievePlayers(String surname) throws DAOException {
		MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
		MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
		Document query = new Document();
		query.append("surname", surname);
		MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find(query).iterator();
		List<Player> players = new ArrayList<Player>();
		try {
			while (cursor.hasNext()) { 
				players.add(Player.playerFromJson(cursor.next().toJson()));
			}
		} finally {
			cursor.close(); 
		}
        mongoClient.close();
		return players;
	}

	@Override
	public List<Player> retrieveAllPlayers() throws DAOException {
		MongoClient mongoClient = MongoClients.create("mongodb://172.16.0.132:27018");
		MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
		MongoCursor<Document> cursor = mongoDatabase.getCollection("players").find().iterator();
		List<Player> players = new ArrayList<Player>();
		try {
			while (cursor.hasNext()) { 
				players.add(Player.playerFromJson(cursor.next().toJson()));
			}
		} finally {
			cursor.close(); 
		}
        mongoClient.close();
		return players;
	}

}
