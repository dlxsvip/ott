package com.ylyan.analyser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpHost;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ylyan.enumer.EnumClarity;
import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class AnalyserForYouKu extends Analyser {

	private static String html = "^http://v.youku.com/v_show/id_([0-9a-zA-Z=]+)([_a-z0-9]+)?\\.html";

	private static String API = "http://play.youku.com/play/get.json?vid=%s&ct=12";

	// 拼接地址
	private static String TOGETP1P2HB2 = "http://k.youku.com/player/getFlvPath/sid/%s_0%s/st/%s/fileid/$P1?K=$P2&ctype=12&ev=1&ts=$P3&oip=$P4&token=$P5&ep=$P6";

	private static final String KEY_P1 = "$P1";

	private static final String KEY_P2 = "$P2";

	private static final String KEY_P3 = "$P3";

	private static final String KEY_P4 = "$P4";

	private static final String KEY_P5 = "$P5";

	private static final String KEY_P6 = "$P6";

	private static final String TYPE_MP4 = "mp4";

	private static final String TYPE_FLV = "flv";

	private static final String TYPE_3GP = "3gp";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();

		try {
			String id = getId(urlStr, html);
			String apiUrl = String.format(API, id);
			String json = getYouKuUrlJson(apiUrl);
			if ("".equals(json)) {
				System.out.println("链接失败");
				return null;
			}
			getYouKuVideos(json, videoAR);

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

	private String getYouKuUrlJson(String url) {
		String json = "";

		CloseableHttpClient httpClient = null;
		HttpGet httpGet = null;
		CloseableHttpResponse response = null;
		try {
			if (httpClient == null) {
				httpClient = HttpUtil.getHttpClient();
			}

			httpGet = HttpUtil.getMethod(url);
			httpGet.addHeader("Referer", url);

			String cookie = "__ysuid=" + getPvid(6);
			httpGet.addHeader("Cookie", cookie);

			response = httpClient.execute(httpGet);
			if (200 == response.getStatusLine().getStatusCode()) {
				json = HttpUtil.getResult(response.getEntity());
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			HttpUtil.httpResponseClose(response);

			HttpUtil.httpGetAbort(httpGet);

			HttpUtil.httpClientClose(httpClient);
		}

		return json;
	}

	private void getYouKuVideos(String json,
			VideoAnalyseResult videoAnalyseResult) throws JSONException {

		JSONObject jsonObj = new JSONObject(json);
		JSONObject data = jsonObj.getJSONObject("data");

		String oip = data.getJSONObject("security").get("ip").toString();
		String ep = data.getJSONObject("security").get("encrypt_string")
				.toString();

		JSONArray streams = data.getJSONArray("stream");
		JSONObject stream = null;
		String type = TYPE_MP4;
		int length = streams.length();

		for (int i = 0; i < length; i++) {
			stream = streams.getJSONObject(i);
			type = stream.getString("stream_type");
			if ((type.toLowerCase().contains(TYPE_MP4))
					|| (type.toLowerCase().contains(TYPE_3GP))) {
				break;
			}

		}

		JSONArray segs = stream.getJSONArray("segs");
		String fileId = stream.getString("stream_fileid");

		String result = getSize(changeSize("b4eto0b4"), decodeEP(ep));
		if (result.indexOf("_") == -1) {
			return;
		}
		String sid = result.split("_")[0];
		String token = result.split("_")[1];

		List<VideoEntry> videoEntryList = new ArrayList<VideoEntry>();
		for (int i = 0; i < segs.length(); i++) {
			JSONObject seg = segs.getJSONObject(i);
			String p2 = seg.get("key").toString();
			String ts = seg.get("total_milliseconds_video").toString();
			String no = i + "";
			if (!"-1".equals(p2)) {
				String newType = type.toLowerCase().contains(TYPE_FLV) ? TYPE_FLV
						: TYPE_MP4;

				String encodeEP = setSize(getSize(changeSize("boa4poz1"), sid
						+ "_" + fileId + "_" + token));

				String videoUrl = "";
				try {
					videoUrl = String
							.format(TOGETP1P2HB2,
									new Object[] { sid, no, newType })
							.replace(KEY_P1, fileId)
							.replace(KEY_P2, p2)
							.replace(KEY_P3, ts)
							.replace(KEY_P4, oip)
							.replace(KEY_P5, token)
							.replace(KEY_P6,
									URLEncoder.encode(encodeEP, "UTF-8"));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}

				System.out.println("假地址：" + videoUrl);
				String perVideoUrl = getPerVideoUrl(videoUrl);
				System.out.println("真地址：" + perVideoUrl);

				VideoEntry videoEntry = null;
				// videoEntry = getVideoEntry(perVideoUrl, params,false);
				videoEntry = new VideoEntry();
				int mediaSize = seg.getInt("size");

				int duration = (seg.getInt("total_milliseconds_video") + 500) / 1000;

				String segName = String.valueOf(i + 1);

				videoEntry.setSegName(segName);
				videoEntry.setMediaSize(mediaSize);
				videoEntry.setDuration(duration);
				videoEntryList.add(videoEntry);

			}
		}

		videoAnalyseResult.setVideosMap(getType(type.toUpperCase()),
				videoEntryList);
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

	private String getPvid(int len) {
		String[] randchar = { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
				"a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l",
				"m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x",
				"y", "z", "A", "B", "C", "D", "E", "F", "G", "H", "I", "J",
				"K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V",
				"W", "X", "Y", "Z" };
		int i = 0;
		String r = "";
		long seconds = System.currentTimeMillis();
		for (i = 0; i < len; i++) {
			int index = (int) (Math.random() * 2147483647.0D
					* Math.pow(10.0D, 6.0D) % randchar.length);
			r = r + randchar[index];
		}
		return seconds + r;
	}

	private String decodeEP(String a) {
		String e = "";
		if ((a == null) || (a.trim().equals(""))) {
			return e;
		}
		int[] h = { -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
				-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
				-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1,
				63, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1,
				-1, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15,
				16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1, -1,
				26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
				42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };
		int i = a.length();
		int f = 0;
		int c = 0;
		int b = 0;
		for (e = ""; f < i;) {
			do {
				c = h[(a.charAt(f++) & 0xFF)];
			} while ((f < i) && (-1 == c));
			if (-1 == c) {
				break;
			}
			do {
				b = h[(a.charAt(f++) & 0xFF)];
			} while ((f < i) && (-1 == b));
			if (-1 == b) {
				break;
			}
			e = e + String.valueOf((char) (c << 2 | (b & 0x30) >> 4));
			do {
				c = a.charAt(f++) & 0xFF;
				if (61 == c) {
					return e;
				}
				c = h[c];
			} while ((f < i) && (-1 == c));
			if (-1 == c) {
				break;
			}
			e = e + String.valueOf((char) ((b & 0xF) << 4 | (c & 0x3C) >> 2));
			do {
				b = a.charAt(f++) & 0xFF;
				if (61 == b) {
					return e;
				}
				b = h[b];
			} while ((f < i) && (-1 == b));
			if (-1 == b) {
				break;
			}
			e = e + String.valueOf((char) ((c & 0x3) << 6 | b));
		}
		return e;
	}

	private String changeSize(String key) {
		char[] a = key.toCharArray();
		int[] c = { 19, 1, 4, 7, 30, 14, 28, 8, 24, 17, 6, 35, 34, 16, 9, 10,
				13, 22, 32, 29, 31, 21, 18, 3, 2, 23, 25, 27, 11, 20, 5, 15,
				12, 0, 33, 26 };
		Object[] b = new Object[a.length];
		for (int f = 0; f < a.length; f++) {
			int i = ('a' <= a[f]) && ('z' >= a[f]) ? a[f] - 'a' : Integer
					.valueOf(String.valueOf(a[f])).intValue() - 0 + 26;
			for (int e = 0; e < 36; e++) {
				if (c[e] == i) {
					i = e;
					break;
				}
			}
			b[f] = (25 < i ? Integer.valueOf(i - 26) : String
					.valueOf((char) (i + 97)));
		}
		return Arrays.toString(b).replaceAll(",", "").replaceAll("\\[", "")
				.replaceAll("\\]", "").replaceAll(" ", "");
	}

	private String getSize(String a, String c) {
		int[] b = new int[256];
		int f = 0;
		int i = 0;
		String e = "";
		for (int h = 0; h < 256; h++) {
			b[h] = h;
		}
		for (int h = 0; h < 256; h++) {
			f = (f + b[h] + a.charAt(h % a.length())) % 256;
			i = b[h];
			b[h] = b[f];
			b[f] = i;
		}
		f = 0;
		int h = 0;
		for (int q = 0; q < c.length(); q++) {
			h = (h + 1) % 256;
			f = (f + b[h]) % 256;
			i = b[h];
			b[h] = b[f];
			b[f] = i;
			e = e
					+ String.valueOf((char) (c.charAt(q) ^ b[((b[h] + b[f]) % 256)]));
		}
		return e;
	}

	private String setSize(String a) {
		String c = "";
		if ((a == null) || (a.trim().equals(""))) {
			return c;
		}

		int f = a.length();
		int b = 0;
		for (c = ""; b < f;) {
			int e = a.charAt(b++) & 0xFF;
			if (b == f) {
				c = c
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
								.charAt(e >> 2);
				c = c
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
								.charAt((e & 0x3) << 4);
				c = c + "==";
				break;
			}
			int g = a.charAt(b++);
			if (b == f) {
				c = c
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
								.charAt(e >> 2);
				c = c
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
								.charAt((e & 0x3) << 4 | (g & 0xF0) >> 4);
				c = c
						+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
								.charAt((g & 0xF) << 2);
				c = c + "=";
				break;
			}
			int h = a.charAt(b++);
			c = c
					+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
							.charAt(e >> 2);
			c = c
					+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
							.charAt((e & 0x3) << 4 | (g & 0xF0) >> 4);
			c = c
					+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
							.charAt((g & 0xF) << 2 | (h & 0xC0) >> 6);
			c = c
					+ "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
							.charAt(h & 0x3F);
		}
		return c;
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

	private String getPerVideoUrl(String url) {
		String result = "";

		CloseableHttpClient httpClient = null;
		HttpGet httpGet = null;
		CloseableHttpResponse response = null;
		try {
			if (httpClient == null) {
				httpClient = HttpUtil.getHttpClient();
			}

			HttpClientContext context = HttpClientContext.create();

			httpGet = HttpUtil.getMethod(url);

			response = httpClient.execute(httpGet, context);
			System.out.println("获取真地址的返回码："
					+ response.getStatusLine().getStatusCode());

			HttpHost target = context.getTargetHost();
			List<URI> redirects = context.getRedirectLocations();
			URI uri = URIUtils.resolve(httpGet.getURI(), target, redirects);

			String location = uri.toASCIIString();

			result = "".equals(location) ? url : location;
		} catch (ClientProtocolException e) {
		} catch (IOException e) {
		} catch (URISyntaxException e) {
		} finally {
			HttpUtil.httpResponseClose(response);
			HttpUtil.httpGetAbort(httpGet);
			HttpUtil.httpClientClose(httpClient);
		}

		return result;
	}

	public static void main(String[] args) {
		AnalyserForYouKu analyer = new AnalyserForYouKu();
		analyer.analyser("http://v.youku.com/v_show/id_XODU1NTYxOTg4.html");
	}

}
