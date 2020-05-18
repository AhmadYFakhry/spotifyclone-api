package com.csc301.profilemicroservice;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import okhttp3.OkHttpClient;


@RestController
@RequestMapping("/")
public class ProfileController {
	public static final String KEY_USER_NAME = "name";
	public static final String KEY_USER_FULLNAME = "fullName";
	public static final String KEY_USER_PASSWORD = "password";

	@Autowired
	private final ProfileDriverImpl profileDriver;

	@Autowired
	private final PlaylistDriverImpl playlistDriver;

	OkHttpClient client = new OkHttpClient();

	public ProfileController(ProfileDriverImpl profileDriver, PlaylistDriverImpl playlistDriver) {
		this.profileDriver = profileDriver;
		this.playlistDriver = playlistDriver;
	}

	@RequestMapping(value = "/profile", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addProfile(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		// String line = null;
		// StringBuffer jb = new StringBuffer();
		try {
			String fullName = request.getParameter("fullName");
			String userName = request.getParameter("userName");
			String password = request.getParameter("password");
			if (fullName == null || userName == null || password == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = profileDriver.createUserProfile(userName, fullName, password);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
		}
		return response;
	}

	@RequestMapping(value = "/followFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> followFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (userName == null || friendUserName == null) {
				throw new Error("Test");
			} else {
				if(userName.equals(friendUserName)) {
					DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
					response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
					return response;
				}
				else {
					DbQueryStatus test = profileDriver.followFriend(userName, friendUserName);
					response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
				}
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
		}
		return response;
	}

	@RequestMapping(value = "/getAllFriendFavouriteSongTitles/{userName}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getAllFriendFavouriteSongTitles(@PathVariable("userName") String userName,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (userName == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus res = profileDriver.getAllSongFriendsLike(userName);
				response = Utils.setResponseStatus(response, res.getdbQueryExecResult(), res.getData());
			}
		} catch (Exception e) {
			DbQueryStatus res = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, res.getdbQueryExecResult(), res.getData());
		}
		return response;
	}

	@RequestMapping(value = "/unfollowFriend/{userName}/{friendUserName}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unfollowFriend(@PathVariable("userName") String userName,
			@PathVariable("friendUserName") String friendUserName, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (userName == null || friendUserName == null) {
				throw new Error("Test");
			} else {
				if(userName.equals(friendUserName)) {
					DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
					response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
					return response;
				}
				DbQueryStatus test = profileDriver.unfollowFriend(userName, friendUserName);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
		}
		return response;
	}

	@RequestMapping(value = "/likeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> likeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {
		
		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if(userName == null || songId == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = playlistDriver.likeSong(userName, songId);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
		}
		return response;
	}

	@RequestMapping(value = "/unlikeSong/{userName}/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> unlikeSong(@PathVariable("userName") String userName,
			@PathVariable("songId") String songId, HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if(userName == null || songId == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = playlistDriver.unlikeSong(userName, songId);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
		}
		return response;
	}

	@RequestMapping(value = "/deleteAllSongsFromDb/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> deleteAllSongsFromDb(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if(songId == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = playlistDriver.deleteSongFromDb(songId);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus test = new DbQueryStatus("ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			return response;
		}
		return response;
	}
}