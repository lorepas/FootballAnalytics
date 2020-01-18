package stats.persistence;

import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import stats.model.Player;
import stats.utility.Utils;

public class DAOPlayerMongo implements IDAOPlayer {

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
			MongoDatabase mongoDatabase = mongoClient.getDatabase("footballDB");
			MongoCollection<Document> mongoCollection = mongoDatabase.getCollection("players");
			mongoCollection.insertMany(documents);
		} catch(MongoWriteException mwe) {
			throw new DAOException(mwe);
		}
	}

	@Override
	public void updatePlayer(Player player) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deletePlayer(Player player) throws DAOException {
		// TODO Auto-generated method stub

	}

	@Override
	public List<Player> retrievePlayers(String surname) throws DAOException {
		// TODO Auto-generated method stub
		return null;
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
