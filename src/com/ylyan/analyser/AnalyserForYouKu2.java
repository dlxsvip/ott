package com.ylyan.analyser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ylyan.enumer.EnumClarity;
import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class AnalyserForYouKu2 extends Analyser {

	private static String VID_REG = "^http://v.youku.com/v_show/id_([0-9a-zA-Z=]+)([_a-z0-9]+)?\\.html";

	// private static String API =
	// "https://openapi.youku.com/v2/videos/files.json?client_id=e57bc82b1a9dcd2f&client_secret=a361608273b857415ee91a8285a16b4a&type=play&video_id=";
	protected static String API = "https://openapi.youku.com/v2/videos/show.json?client_id=e57bc82b1a9dcd2f&video_id=";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();

		try {
			String id = getId(urlStr, VID_REG);
			String apiUrl = API + id;

			String json = HttpUtil.geHttpResult(apiUrl, UTF_8);
			if ("".equals(json)) {
				System.out.println("链接失败");
				return null;
			}
			JSONObject data = new JSONObject(json);
			JSONObject files = data.getJSONObject("files");

			JSONObject stream = null;

			// 只爬MP4格式
			// if(files.has("mp4")){
			// stream = (JSONObject) files.get("mp4");
			// }
			// getVideos(stream, videoEntryList);

			JSONArray types = files.names();
			List<VideoEntry> videoEntryList = null;
			// 格式循环
			for (int i = 0; i < types.length(); i++) {
				videoEntryList = new ArrayList<VideoEntry>();
				String type = types.getString(i);
				System.out.println("-----" + type + "------");
				stream = (JSONObject) files.get(type);
				getVideos(stream, videoEntryList);

				videoAR.setVideosMap(getType(type), videoEntryList);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}

		return videoAR;
	}

	private String getId(String sourceCode, String regStr) {
		Matcher matcher = Pattern.compile(regStr).matcher(sourceCode);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}

	protected void getVideos(JSONObject stream, List<VideoEntry> videoEntryList)
			throws JSONException {
		JSONArray segs = stream.getJSONArray("segs");

		JSONObject voide = null;
		VideoEntry videoEntry = null;
		for (int i = 0; i < segs.length(); i++) {
			voide = segs.getJSONObject(i);

			String perVideoUrl = voide.getString("url");

			if (videoEntry == null) {
				videoEntry = new VideoEntry();
			}

			videoEntry.setSegName(voide.getString("no"));
			videoEntry.setMediaSize(voide.getInt("size"));
			videoEntry.setDuration(voide.getInt("duration"));
			videoEntry.setServerUrl(voide.getString("url"));

			videoEntryList.add(videoEntry);

			System.out.println(perVideoUrl);
		}

	}

	private EnumClarity getType(String type) {
		if (EnumClarity.MP4.toString().equals(type)) {
			return EnumClarity.MP4;
		} else if (EnumClarity.FLV.toString().equals(type)) {
			return EnumClarity.FLV;
		} else if (EnumClarity.HD1.toString().equals(type)) {
			return EnumClarity.HD1;
		} else if (EnumClarity.HD2.toString().equals(type)) {
			return EnumClarity.HD2;
		} else if (EnumClarity.HD3.toString().equals(type)) {
			return EnumClarity.HD3;
		} else if ("3GPHD".equals(type)) {
			return EnumClarity._3GPHD;
		} else {
			return EnumClarity.FLV;
		}
	}

	public static void main(String[] args) {
		AnalyserForYouKu2 analyer = new AnalyserForYouKu2();
		analyer.analyser("http://v.youku.com/v_show/id_XODU1NTYxOTg4.html");
	}

}
