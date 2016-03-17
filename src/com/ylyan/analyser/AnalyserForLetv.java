package com.ylyan.analyser;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ylyan.enumer.EnumClarity;
import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class AnalyserForLetv extends Analyser {

	private String GET_SERVER_TIME_URL = "http://api.letv.com/time?tn=0.9946684762835503";

	// "http://api.letv.com/mms/out/video/playJson?id=(id)&platid=1&splatid=1&tkey=(tkey)&domain=http%3A%2F%2Fwww.letv.com";
	private String GET_API_URL = "http://api.letv.com/mms/out/video/playJsonH5?platid=3&splatid=345&tss=no&id=(id)&detect=1&accessyx=1&domain=.letv.com&tkey=(tkey)";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();
		try {
			String id = urlStr.substring(urlStr.lastIndexOf("/") + 1,
					urlStr.indexOf(".html"));
			int time = getTime();
			// int tkey = getTkey(time);
			int tkey = getHtml5Tkey(time);
			String apiUrl = GET_API_URL.replace("(tkey)", String.valueOf(tkey))
					.replace("(id)", id);
			String json = HttpUtil.geHttpResult(apiUrl, UTF_8);
			if ("".equals(json)) {
				System.out.println("链接失败");
				return null;
			}
			getVideos(json, videoAR);

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return videoAR;
	}

	private void getVideos(String str, VideoAnalyseResult videoAR) {
		try {
			JSONObject json = new JSONObject(str);
			JSONObject playurlJson = json.getJSONObject("playurl");
			int duration = playurlJson.getInt("duration");
			JSONArray domain = playurlJson.getJSONArray("domain");
			JSONObject dispatch = playurlJson.getJSONObject("dispatch");

			JSONArray types = dispatch.names();
			List<VideoEntry> videoEntryList = new ArrayList<VideoEntry>();
			VideoEntry videoEntry = null;
			// 格式循环
			for (int i = 0; i < types.length(); i++) {
				String type = types.getString(i);
				videoEntry = new VideoEntry();

				JSONArray jarr = (JSONArray) dispatch.get(type);
				String videoInfoUrl = domain.getString(0)
						+ jarr.getString(0)
						+ "&ctv=pc&m3v=1&termid=1&format=1&hwtype=un&ostype=Windows7&tag=letv&sign=letv&expect=3&pay=0&rateid="
						+ type;

				String json2 = HttpUtil.geHttpResult(videoInfoUrl, UTF_8);
				if ("".equals(json2)) {
					System.out.println("链接失败:" + videoInfoUrl);
					continue;
				}
				JSONObject jsonObj = new JSONObject(json2);
				if (!jsonObj.has("location")) {
					continue;
				}

				String realUrl = jsonObj.getString("location");

				videoEntry.setSegName(i + "");
				videoEntry.setDuration(duration);
				videoEntry.setServerUrl(realUrl);

				videoEntryList.add(videoEntry);
				videoAR.setVideosMap(getType(type), videoEntryList);

				System.out.println(realUrl);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private EnumClarity getType(String type) {
		if ("350".equals(type)) {
			return EnumClarity.LIUCHANG;
		}
		if ("1000".equals(type)) {
			return EnumClarity.GAOQING;
		}
		if ("720p".equals(type)) {
			return EnumClarity.CHAOQING;
		}
		if ("1080p".equals(type)) {
			return EnumClarity.LANGUANG;
		}
		if ("1300".equals(type)) {
			return EnumClarity.ONOTHREEOO;
		}

		return EnumClarity.LIUCHANG;
	}

	private int getTime() throws JSONException {
		String serverJson = HttpUtil.geHttpResult(GET_SERVER_TIME_URL, UTF_8);
		JSONObject timesonArray = new JSONObject(serverJson);
		return timesonArray.getInt("stime");
	}

	private int getHtml5Tkey(int e) {
		int t = 185025305;

		e = GenerateKeyRor(e, t % 17);

		int o = e ^ t;

		return o;
	}

	// private int getTkey(int time) {
	// int key = 773625421;
	// int value = GenerateKeyRor(time, key % 13);
	// value ^= key;
	// value = GenerateKeyRor(value, key % 17);
	// return value;
	// }

	private int GenerateKeyRor(int value, int key) {
		for (int i = 0; i < key; i++)
			value = (value >>> 1) + ((value & 1) << 31);
		return value;
	}

	public static void main(String[] args) {

		AnalyserForLetv analyer = new AnalyserForLetv();
		analyer.analyser("http://www.letv.com/ptv/vplay/24203354.html");

	}

}
