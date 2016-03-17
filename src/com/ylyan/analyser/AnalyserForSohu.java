package com.ylyan.analyser;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class AnalyserForSohu extends Analyser {

	private static String REG_VID = ".*?vid=\"(\\d+).*?";

	private static String GET_JSON_URL = "http://hot.vrs.sohu.com/vrs_flash.action?vid=";
	private static String GET_JSON_MYURL = "http://my.tv.sohu.com/play/videonew.do?vid=";

	private static String GET_PERLOCATION_URL = "http://allot/?prot=9&file=clipsURL[i]&new=su[i]";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();
		String resultHtml = HttpUtil.geHttpResult(urlStr, UTF_8);

		resultHtml = resultHtml.replaceAll("[\\t\\r\\n]", "");
		System.out.println(resultHtml);
		String vId = getParamsByReg(resultHtml, REG_VID);
		String jsonUrl = GET_JSON_URL + vId;
		if (urlStr.contains("my.tv.sohu.com")) {
			jsonUrl = GET_JSON_MYURL + vId;
		}

		String resultJson = HttpUtil.geHttpResult(jsonUrl, UTF_8);
		;
		getVideos(resultJson, videoAR);

		return videoAR;
	}

	private void getVideos(String str, VideoAnalyseResult videoAR) {
		try {
			JSONObject json = new JSONObject(str);
			String allot = json.getString("allot");
			JSONObject data = (JSONObject) json.get("data");
			JSONArray clipsURL = (JSONArray) data.get("clipsURL");
			JSONArray clipsDuration = (JSONArray) data.get("clipsDuration");
			JSONArray clipsBytes = (JSONArray) data.get("clipsBytes");
			JSONArray su = (JSONArray) data.get("su");

			List<VideoEntry> videoEntryList = new ArrayList<VideoEntry>();
			VideoEntry videoEntry = null;

			// 分段循环
			for (int i = 0; i < clipsURL.length(); i++) {
				String perClipUrl = clipsURL.getString(i);
				String perSu = su.getString(i);

				Double duration = clipsDuration.getDouble(i);
				int mediaSize = clipsBytes.getInt(i);

				String url = GET_PERLOCATION_URL.replace("allot", allot)
						.replace("clipsURL[i]", perClipUrl)
						.replace("su[i]", perSu);
				String details = HttpUtil.geHttpResult(url, UTF_8);
				JSONObject detailJson = new JSONObject(details);
				String realUrl = detailJson.getString("url");

				System.out.println(realUrl);

				videoEntry = new VideoEntry();
				videoEntry.setSegName(i + "");
				videoEntry.setServerUrl(realUrl);
				videoEntry.setDuration(duration.intValue());
				videoEntry.setMediaSize(mediaSize);

				videoEntryList.add(videoEntry);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String getParamsByReg(String str, String regex) {
		Matcher m = Pattern.compile(regex).matcher(str);
		if (m.find()) {
			// m.group(1) 获取正则表达式里的第一个()里匹配的数据
			// m.group() 获取整个正则表达式匹配的数据
			return m.group(1);
		}

		return null;
	}

	public static void main(String[] args) {
		AnalyserForSohu analyser = new AnalyserForSohu();
		analyser.analyser("http://tv.sohu.com/20151223/n432262620.shtml");
	}

}
