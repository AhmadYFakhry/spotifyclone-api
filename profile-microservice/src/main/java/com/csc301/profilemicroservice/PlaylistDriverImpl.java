package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Repository
public class PlaylistDriverImpl implements PlaylistDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitPlaylistDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nPlaylist:playlist) ASSERT exists(nPlaylist.plName)";
				trans.run(queryStr);
				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus likeSong(String userName, String songId) {
		OkHttpClient client = new OkHttpClient();
		try (Session session = driver.session()) {
			StatementResult plExists = session.run("MATCH (n:playlist) WHERE n.plName = $x RETURN n",
			parameters("x", userName+"-favorites"));
			if(!plExists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Check if the song already exists
			StatementResult songXists = session.run("MATCH (n:song) WHERE n.songId = $x RETURN n",
			parameters("x", songId));
			// Create the song in the neo4j db if it doesn't exist
			if(!songXists.hasNext()) {
				try {
					// Check to see if the song exists in  mongo DB.
					String url = String.format("http://localhost:3001/getSongById/%s",  songId);
					Request request = new Request.Builder().url(url).get().build();
					Response response =  client.newCall(request).execute();
					String res = response.body().string();
					JSONObject obj = new JSONObject(res);
					if(obj.getString("status").equals("NOT_FOUND")) {
						// If it does not exist, then return an error
						return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
					}
					// If it does exist, then create the song in neo4j with the ID
					if(obj.getString("data").equals("found")) {
							session.run("CREATE (p:song {songId: $x}) RETURN p",
								parameters("x", songId));
					}
				} catch (Exception e) {
					return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
			}
			// Create the relationship between the two nodes
			session.run(
					"MATCH (a:playlist), (b:song) WHERE a.plName = $x AND b.songId = $y CREATE (a)-[r:includes]->(b)",
					parameters("x", userName+"-favorites", "y", songId));
			try {
				// Increment the song favourites count for the song
				String url = String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=false",  songId);
				JSONObject json = new JSONObject();
				json.put("shouldDecrement", false);
			    RequestBody body = RequestBody.create(null, new byte[0]);
				Request request = new Request.Builder().url(url).put(body).build();
				client.newCall(request).execute();
			} catch (Exception e) {
				System.out.println(e);
				return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch(Exception e) {
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus unlikeSong(String userName, String songId) {
		OkHttpClient client = new OkHttpClient();
		try (Session session = driver.session()) {
			// Check if the song exists, if it does not return an error
			StatementResult songXists = session.run("MATCH (n:song) WHERE n.songId = $x RETURN n",
			parameters("x", songId));
			if(!songXists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Check if the playlist exists, if it does not return an error
			StatementResult plExists = session.run("MATCH (n:playlist) WHERE n.plName = $x RETURN n",
			parameters("x", userName+"-favorites"));
			if(!plExists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}

			// Check if the song is in the playlist, if it does not return an error
			StatementResult relExists = session.run(
				"RETURN EXISTS( (:playlist {plName: $x})-[:includes]-(:song {songId: $y}) )",
				parameters("x", userName+"-favorites", "y", songId));
			if (relExists.hasNext()) {
			Record record = relExists.single();
			if (record.get(0).toString().equals("TRUE")) {
				// If if the song exists in the playlist, remove it from the playlist
				session.run(
						"MATCH (n)-[rel:includes]->(r) WHERE n.plName=$x AND r.songId=$y DELETE rel",
						parameters("x", userName+"-favorites", "y", songId));
				try {
					// Decrements the song favourites count
					String url = String.format("http://localhost:3001/updateSongFavouritesCount/%s?shouldDecrement=true",  songId);
					JSONObject json = new JSONObject();
					json.put("shouldDecrement", false);
					RequestBody body = RequestBody.create(null, new byte[0]);
					Request request = new Request.Builder().url(url).put(body).build();
					client.newCall(request).execute();
					} catch (Exception e) {
						System.out.println(e);
						return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
					}
					return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				} 
			}
		return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		} catch (Exception e) {
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus deleteSongFromDb(String songId) {

		try (Session session = driver.session()) {
			// Check if the song exists
			StatementResult songXists = session.run("MATCH (n:song) WHERE n.songId = $x RETURN n",
			parameters("x", songId));
			if(!songXists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Delete the song as well as all other relationships
			session.run("MATCH (n:song) WHERE n.songId = $x DETACH DELETE n",
						parameters("x", songId));
			return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch (Exception e) {
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		} 
	}
}
