package com.ylyan.analyser;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

public class AnalyserForlqiyi extends Analyser {

	private static String IQIYI_MD5PARAM = ")(*&^flash@#$%a";

	private static String TVID_REG = "tvId:\\s*(\\d*)";

	private static String VIDEOID_REG = "data-player-videoid=\"([\\w\\d]*)\"";

	private static String GET_JSON_URL = "http://cache.video.qiyi.com/vp/tvid/videoid/";

	private static String GET_SERVER_TIME_URL = "http://data.video.qiyi.com/t?tn=0.32949503720738";

	private static String TOGETREALVIDEOADR = "http://data.video.qiyi.com/key/videos/perVideoUrl";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult video = new VideoAnalyseResult();
		String resultHtml = HttpUtil.geHttpResult(urlStr, UTF_8);
		;
		// 替换掉HTML页面里的空格,制表符,回车, 换行
		// resultHtml = resultHtml.replaceAll("\\s*|\t|\r|\n","");
		// System.out.println(resultHtml);
		if ("".equals(resultHtml)) {
			System.out.println("链接失败:" + urlStr);
			return null;
		}
		String tvId = getParamsByReg(resultHtml, TVID_REG);
		String videoId = getParamsByReg(resultHtml, VIDEOID_REG);
		String jsonUrl = GET_JSON_URL.replace("tvid", tvId).replace("videoid",
				videoId);

		String resultJson = HttpUtil.geHttpResult(jsonUrl, UTF_8);
		;
		if ("".equals(resultJson)) {
			System.out.println("链接失败:" + jsonUrl);
			return null;
		}
		getVideos(resultJson, video);

		return video;
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

	private void getVideos(String str, VideoAnalyseResult videoAR) {
		try {
			JSONObject data = new JSONObject(str);

			JSONArray infoObj = (JSONArray) data.get("tkl");
			JSONArray vs = (JSONArray) infoObj.getJSONObject(0).get("vs");
			List<VideoEntry> videoEntryList = null;
			VideoEntry videoEntry = null;
			// 格式循环
			for (int i = 0; i < vs.length(); i++) {
				String type = "BID" + vs.getJSONObject(i).getString("bid");
				videoEntryList = new ArrayList<VideoEntry>();

				JSONArray perVs = (JSONArray) vs.getJSONObject(i).get("fs");

				// 片段循环
				for (int j = 0; j < perVs.length(); j++) {
					if (videoEntry == null) {
						videoEntry = new VideoEntry();
					}

					String perUrl = (String) perVs.getJSONObject(j).get("l");
					if (!perUrl.contains("f4v")) {
						continue;
					}

					Long time = getTime();
					String timeFloor = String.valueOf(Math.floor(time
							.longValue() / 600L));
					String vname = perUrl.substring(
							perUrl.lastIndexOf("/") + 1, perUrl.indexOf("."));

					String strToMd5 = timeFloor.substring(0,
							timeFloor.length() - 2) + IQIYI_MD5PARAM + vname;
					String key = encodeByMD5(strToMd5);
					perUrl = TOGETREALVIDEOADR.replace("key", key)
							.replace("perVideoUrl",
									perUrl.substring(1, perUrl.length()));

					String resultJson = HttpUtil.geHttpResult(perUrl, UTF_8);
					;
					if ("".equals(resultJson)) {
						System.out.println("链接失败:" + perUrl);
						continue;
					}

					JSONObject obj = new JSONObject(resultJson);
					String realUrl = obj.getString("l");
					// int perMediaSize = obj.getInt("b");
					// int perDuration = obj.getInt("d") / 1000;

					// videoEntry.setMediaSize(perMediaSize);
					videoEntry.setSegName(String.valueOf(j));
					// videoEntry.setDuration(perDuration);
					videoEntryList.add(videoEntry);

					System.out.println(realUrl);
				}

				videoAR.setVideosMap(getType(type), videoEntryList);
			}

		} catch (JSONException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	private Long getTime() throws JSONException {
		String serverJson = HttpUtil.geHttpResult(GET_SERVER_TIME_URL, UTF_8);
		JSONObject timesonArray = new JSONObject(serverJson);
		return timesonArray.getLong("t");
	}

	private String encodeByMD5(String encodeStr)
			throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("MD5");
		BigInteger bigInt = new BigInteger(1, digest.digest(encodeStr
				.getBytes()));
		return bigInt.toString(16);
	}

	private EnumClarity getType(String type) {
		if (EnumClarity.BID1.toString().equals(type)) {
			return EnumClarity.BID1;
		} else if (EnumClarity.BID2.toString().equals(type)) {
			return EnumClarity.BID2;
		} else if (EnumClarity.BID3.toString().equals(type)) {
			return EnumClarity.BID3;
		} else if (EnumClarity.BID4.toString().equals(type)) {
			return EnumClarity.BID4;
		} else if (EnumClarity.BID96.toString().equals(type)) {
			return EnumClarity.BID96;
		} else if (EnumClarity.BID5.toString().equals(type)) {
			return EnumClarity.BID5;
		} else {
			return EnumClarity.BID1;
		}
	}

	public static void main(String[] args) {
		AnalyserForlqiyi analyer = new AnalyserForlqiyi();
		analyer.analyser("http://www.iqiyi.com/v_19rrkqya24.html");
	}

}
