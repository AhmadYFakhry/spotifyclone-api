package com.csc301.songmicroservice;

import static com.mongodb.client.model.Filters.eq;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.DeleteResult;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SongDalImpl implements SongDal {

	private final MongoTemplate db;
	private final MongoCollection<Document> songs;

	@Autowired
	public SongDalImpl(MongoTemplate mongoTemplate) {
		this.db = mongoTemplate;
		this.songs = db.getCollection("songs");
	}

	@Override
	public DbQueryStatus addSong(Song songToAdd) {
		Document document = new Document();
		document.put("songAlbum", songToAdd.getSongAlbum());
		document.put("songArtistFullName", songToAdd.getSongArtistFullName());
		document.put("songName", songToAdd.getSongName());
		document.put("songAmountFavourites", songToAdd.getSongAmountFavourites());
		try {
			try {
				Document nameExists = songs.find(eq("songName", songToAdd.getSongName())).first();
				Document songArtistFullName = songs.find(eq("songArtistFullName", songToAdd.getSongArtistFullName())).first();
				if(nameExists != null && songArtistFullName != null) {
					return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
			} catch (Exception e) {
				return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			songs.insertOne(document);
			songToAdd.setId(new ObjectId(document.get("_id").toString()));
			DbQueryStatus res = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			res.setData(songToAdd.getJsonRepresentation());
			return res;
		}
		catch(Exception e) {
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}

	}

	@Override
	public DbQueryStatus findSongById(String songId) {
		try {
			Document track = songs.find(eq("_id", new ObjectId(songId))).first();
			if(track == null) {return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);}
			DbQueryStatus res = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
			res.setData("found");
			return res;
		} catch (Exception e) {
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus getSongTitleById(String songId) {
		Document track = songs.find(eq("_id", new ObjectId(songId))).first();
		//check if track doesn't exist	
		if(track == null) {return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);}
		String name = track.getString("songName");
		DbQueryStatus res = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		res.setData(name);
		return res;
	}

	@Override
	public DbQueryStatus deleteSongById(String songId) {
		DeleteResult deletedSong = songs.deleteOne(eq("_id", new ObjectId(songId)));
		//check if nothing was deleted
		if(deletedSong.getDeletedCount() == 0){
			return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	}

	@Override
	public DbQueryStatus updateSongFavouritesCount(String songId, boolean shouldDecrement) {
		Document track = songs.find(eq("_id", new ObjectId(songId))).first();
		if(track == null) {
			return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		}
		long favouriteCount = track.getLong("songAmountFavourites");
		if(favouriteCount == 0 && shouldDecrement == true) {
			return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
		if(shouldDecrement) {
			track.replace("songAmountFavourites", favouriteCount, favouriteCount-1);
			songs.findOneAndReplace(eq("_id", new ObjectId(songId)), track);
		}
		else {

			track.replace("songAmountFavourites", favouriteCount, favouriteCount+1);
			songs.findOneAndReplace(eq("_id",  new ObjectId(songId)), track);
		}		
		return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
	}
}