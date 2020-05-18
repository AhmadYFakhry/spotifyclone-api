package com.csc301.songmicroservice;

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
public class SongController {

	@Autowired
	private final SongDal songDal;

	private OkHttpClient client = new OkHttpClient();

	
	public SongController(SongDal songDal) {
		this.songDal = songDal;
	}

	
	@RequestMapping(value = "/getSongById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		if(songId == null) {
			DbQueryStatus err = new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
		}
		else {
			DbQueryStatus dbQueryStatus = songDal.findSongById(songId);
			response = Utils.setResponseStatus(response, dbQueryStatus.getdbQueryExecResult(), dbQueryStatus.getData());
		}
		return response;
	}

	
	@RequestMapping(value = "/getSongTitleById/{songId}", method = RequestMethod.GET)
	public @ResponseBody Map<String, Object> getSongTitleById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (songId == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = songDal.getSongTitleById(songId);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			DbQueryStatus err = new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
		}
		return response;

	}

	
	@RequestMapping(value = "/deleteSongById/{songId}", method = RequestMethod.DELETE)
	public @ResponseBody Map<String, Object> deleteSongById(@PathVariable("songId") String songId,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		try {
			if (songId == null) {
				throw new Error("Test");
			} else {
				DbQueryStatus test = songDal.deleteSongById(songId);
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			System.out.println(e);
			DbQueryStatus err = new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
		}
		return response;
	}

	
	@RequestMapping(value = "/addSong", method = RequestMethod.POST)
	public @ResponseBody Map<String, Object> addSong(@RequestParam Map<String, String> params,
			HttpServletRequest request) {

		Map<String, Object> response = new HashMap<String, Object>();
		String songName = request.getParameter("songName");
		String songArtistFullName = request.getParameter("songArtistFullName");
		String songAlbum = request.getParameter("songAlbum");
		try {
			if(songName != null && songArtistFullName != null && songAlbum != null) {
				DbQueryStatus test = songDal.addSong(new Song(songName, songArtistFullName, songAlbum));
				response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
			}
		} catch (Exception e) {
			System.out.println(e);
			DbQueryStatus err = new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
			response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
		}		
		return response;
	}

	
	@RequestMapping(value = "/updateSongFavouritesCount/{songId}", method = RequestMethod.PUT)
	public @ResponseBody Map<String, Object> updateFavouritesCount(@PathVariable("songId") String songId,
			@RequestParam("shouldDecrement") String shouldDecrement, HttpServletRequest request) {
			
			Map<String, Object> response = new HashMap<String, Object>();

			// System.out.print(shouldDecrement);
			try {
				if (songId == null) {
					DbQueryStatus err = new DbQueryStatus("PARAM_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
					response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
				} else {
					//check if provided 
					if (!shouldDecrement.equalsIgnoreCase("true") && !shouldDecrement.equalsIgnoreCase("false")) {
						DbQueryStatus err = new DbQueryStatus("PARAM_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
						response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
					}
					else {
						DbQueryStatus test = songDal.updateSongFavouritesCount(songId, Boolean.parseBoolean(shouldDecrement));
						response = Utils.setResponseStatus(response, test.getdbQueryExecResult(), test.getData());
					}
					//converts shouldDecrement to boolean as required by docstring
				}
			} catch (Exception e) {
				DbQueryStatus err = new DbQueryStatus("INTERNAL_ERROR", DbQueryExecResult.QUERY_ERROR_GENERIC);
				response = Utils.setResponseStatus(response, err.getdbQueryExecResult(), err.getData());
			}
			
	
			return response;

	}
}