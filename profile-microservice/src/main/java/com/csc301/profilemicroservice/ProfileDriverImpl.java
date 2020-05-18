package com.csc301.profilemicroservice;

import static org.neo4j.driver.v1.Values.parameters;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Record;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.StatementResult;
import org.neo4j.driver.v1.Transaction;
import org.springframework.stereotype.Repository;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Repository
public class ProfileDriverImpl implements ProfileDriver {

	Driver driver = ProfileMicroserviceApplication.driver;

	public static void InitProfileDb() {
		String queryStr;

		try (Session session = ProfileMicroserviceApplication.driver.session()) {
			try (Transaction trans = session.beginTransaction()) {
				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.userName)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT exists(nProfile.password)";
				trans.run(queryStr);

				queryStr = "CREATE CONSTRAINT ON (nProfile:profile) ASSERT nProfile.userName IS UNIQUE";
				trans.run(queryStr);

				trans.success();
			}
			session.close();
		}
	}

	@Override
	public DbQueryStatus createUserProfile(String userName, String fullName, String password) {
		try (Session session = driver.session()) {
			// Check if the profile exists already
			StatementResult profileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", userName));
			// If it exists then return a generic error
			if (profileXists.hasNext()) {
				return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			}
			// Create user profile
			session.run("MERGE (p:profile {userName: $x, fullName: $y, password: $z}) RETURN p",
					parameters("x", userName, "y", fullName, "z", password));
			// Create playlist
			session.run("MERGE (p:playlist {plName: $x}) RETURN p", parameters("x", userName + "-favorites"));
			// Create a relationship between the two
			session.run(
					"MATCH (a:profile), (b:playlist) WHERE a.userName = $x AND b.plName = $y CREATE (a)-[r:created]->(b)",
					parameters("x", userName, "y", userName + "-favorites"));
			return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch (Exception e) {
			// Any error will most likely be a server error
			return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus followFriend(String userName, String frndUserName) {
		try (Session session = driver.session()) {
			// Check if user exists
			StatementResult profileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", userName));
			// If it exists then return a generic error
			if (!profileXists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Check if friend exists
			StatementResult frndProfileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", frndUserName));
			if (!frndProfileXists.hasNext()) {
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// check if a relationship between the two already exists
			StatementResult relExists = session.run(
					"RETURN EXISTS( (:profile {userName: $x})-[:follows]-(:profile {userName: $y}) )",
					parameters("x", userName, "y", frndUserName));
			if (relExists.hasNext()) {
				Record record = relExists.single();
				if (record.get(0).toString().equals("TRUE")) {
					return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
				}
			}
			// Create a relationship between them
			session.run(
					"MATCH (a:profile), (b:profile) WHERE a.userName = $x AND b.userName = $y CREATE (a)-[r:follows]->(b)",
					parameters("x", userName, "y", frndUserName));
			return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
		} catch (Exception e) {
			System.out.print(e);
			return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus unfollowFriend(String userName, String frndUserName) {
		try (Session session = driver.session()) {
			// Check if user exists
			StatementResult profileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", userName));
			// If it exists then return a generic error
			if (!profileXists.hasNext()) {
				System.out.print("Test1");
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Check if friend exists
			StatementResult frndProfileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", frndUserName));
			if (!frndProfileXists.hasNext()) {
				System.out.print("Test2");
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// check if a relationship between the two already exists
			StatementResult relExists = session.run(
					"RETURN EXISTS( (:profile {userName: $x})-[:follows]-(:profile {userName: $y}) )",
					parameters("x", userName, "y", frndUserName));
			if (relExists.hasNext()) {
				Record record = relExists.single();
				if (record.get(0).toString().equals("TRUE")) {
					session.run("MATCH (n)-[rel:follows]->(r) WHERE n.userName=$x AND r.userName=$y DELETE rel",
							parameters("x", userName, "y", frndUserName));
					return new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				}
			}
			return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			// Delete the relationship between them

		} catch (Exception e) {
			return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}

	@Override
	public DbQueryStatus getAllSongFriendsLike(String userName) {
		OkHttpClient client = new OkHttpClient();
		JSONObject frnd = new JSONObject();
		try (Session session = driver.session()) {
			// Check if the user exists
			StatementResult profileXists = session.run("MATCH (n:profile) WHERE n.userName = $x RETURN n",
					parameters("x", userName));
			if (!profileXists.hasNext()) {
				return new DbQueryStatus("404", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
			}
			// Get the users friends friend
			StatementResult friends = session.run(
					"MATCH (:profile {userName: $x})-[r:follows]-(a:profile) RETURN collect(a.userName)",
					parameters("x", userName));
			if (friends.hasNext()) {
				DbQueryStatus response = new DbQueryStatus("OK", DbQueryExecResult.QUERY_OK);
				Record ac = friends.next();
				List<Object> frndNames = ac.get(0).asList();
				// System.out.println(frndNames);
				for (int i = 0; i < frndNames.size(); i++) {
					ArrayList<String> songNames = new ArrayList<>();
					String name = frndNames.get(i).toString();
					StatementResult songs = session.run(
							"MATCH (:playlist {plName: $x})-[r:includes]-(a:song) RETURN collect(a.songId)",
							parameters("x", frndNames.get(i) + "-favorites"));
					if (songs.hasNext()) {
						// add each song to an array
						Record sg = songs.next();
						List<Object> songIdsList = sg.get(0).asList();
						for (int y = 0; y < songIdsList.size(); y++) {
							String songId = songIdsList.get(y).toString();
							try {
								String url = String.format("http://localhost:3001/getSongTitleById/%s", songId);
								Request request = new Request.Builder().url(url).get().build();
								Response res = client.newCall(request).execute();
									String resString = res.body().string();
									JSONObject obj = new JSONObject(resString);
									songNames.add(obj.getString("data"));
								} catch (Exception e) {
									System.out.println(e);
									return new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
								}
							}
							frnd.put(name, songNames);
						}
					}
					response.setData(frnd.toMap());
					return response;
				}
				return new DbQueryStatus("NOT_FOUND", DbQueryExecResult.QUERY_ERROR_NOT_FOUND);
		} catch (Exception e) {
			return new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
		}
	}
}
