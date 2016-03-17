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

public class AnalyserFor56 extends Analyser {

	private static final String GET56ID_URL = "http://vxml.56.com/json/%s/?src=site2";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();

		String resultHtml = HttpUtil.geHttpResult(urlStr, UTF_8);
		// System.out.println(resultHtml);
		// 替换掉HTML页面里的空格,制表符,回车, 换行
		resultHtml = resultHtml.replaceAll("\\s*|\t|\r|\n", "");
		if ("".equals(resultHtml))
			return videoAR;

		String urlId = getId(resultHtml);
		if ("".equals(urlId))
			return videoAR;

		String xmlUrl = String.format(GET56ID_URL, new Object[] { urlId });
		String xmlJson = HttpUtil.geHttpResult(xmlUrl, UTF_8);
		getVideos(xmlJson, videoAR);

		return videoAR;
	}

	private void getVideos(String str, VideoAnalyseResult videoAR) {
		try {
			JSONObject json = new JSONObject(str);

			JSONObject info = (JSONObject) json.get("info");
			JSONArray rfiles = (JSONArray) info.get("rfiles");

			List<VideoEntry> videoEntryList = new ArrayList<VideoEntry>();
			VideoEntry videoEntry = null;

			for (int i = 0; i < rfiles.length(); i++) {
				JSONObject video = (JSONObject) rfiles.get(i);
				String type = video.getString("type");

				String realUrl = video.getString("url");

				videoEntry = getVideo(realUrl);
				videoEntry.setSegName(i + "");
				videoEntry.setServerUrl(realUrl);
				videoEntry.setDuration(video.getInt("totaltime") / 1000);
				videoEntry.setMediaSize(video.getInt("filesize"));

				videoEntryList.add(videoEntry);
				videoAR.setVideosMap(getType(type), videoEntryList);

				System.out.println(realUrl);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	private String getId(String sourceCode) {
		String urlId = "";
		Matcher mId = Pattern.compile("(?s)\"id\":(\\d+)").matcher(sourceCode);
		if (mId.find())
			urlId = mId.group(1);
		if ("".equals(urlId)) {
			Matcher mFlvid = Pattern.compile("\flvid\":\\d+").matcher(
					sourceCode);
			if (mFlvid.find())
				urlId = mId.group(1);
			else
				try {
					String[] code = sourceCode.split("videoId:");
					String id = code[1].split(",")[0];
					urlId = id.substring(1, id.length() - 1);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		return urlId;
	}

	private EnumClarity getType(String type) {
		if (EnumClarity.CLEAR.toString().equals(type.toUpperCase())) {
			return EnumClarity.CLEAR;
		} else if (EnumClarity.NORMAl.toString().equals(type.toUpperCase())) {
			return EnumClarity.NORMAl;
		} else if (EnumClarity.SUPER.toString().equals(type.toUpperCase())) {
			return EnumClarity.SUPER;
		} else {
			return EnumClarity.CLEAR;
		}
	}

	public static void main(String[] args) {
		AnalyserFor56 analyser = new AnalyserFor56();
		analyser.analyser("http://www.56.com/u58/v_MTI4MTc2Nzk5.html");
	}

}
