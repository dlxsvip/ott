package com.ylyan.analyser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.ylyan.enumer.EnumClarity;
import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class AnalyserForApiFree extends Analyser {

	// 第三方接口
	private String APIFREE = "http://www.apifree.net/parse.jsp?url={videoUrl}&type=1";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();

		APIFREE = APIFREE.replace("{videoUrl}", urlStr);
		String videoInfoXml = HttpUtil.geHttpResult(APIFREE, UTF_8);
		// 解析xml文件，获取结果
		getResult(videoAR, videoInfoXml);
		return videoAR;
	}

	private VideoAnalyseResult getResult(VideoAnalyseResult videoAR,
			String videoInfoXml) {
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = dbf.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(
					videoInfoXml)));// 获取到xml 文件
			// 获取元素列
			NodeList list = doc.getElementsByTagName("s");
			List<VideoEntry> entrys = new ArrayList<VideoEntry>();
			VideoEntry entry = null;
			for (int i = 0; i < list.getLength(); i++) {
				Element element = (Element) list.item(i);
				String hd = element.getAttribute("hd");// 获取属性值
				String url = element.getFirstChild().getNodeValue();

				String tmpUrl = getParamsByReg("url=(.*)", url);
				tmpUrl = tmpUrl.replaceAll("%3A", ":").replaceAll("%2F", "/")
						.replaceAll("%3F", "?").replaceAll("%3D", "=")
						.replaceAll("%26", "&");
				// System.out.println(tmpUrl);

				String realVideoInfo = HttpUtil.geHttpResult(tmpUrl, UTF_8);
				JSONObject jsonObj = new JSONObject(realVideoInfo);
				String realUrl = jsonObj.getString("location");
				System.out.println(realUrl);
				entry = new VideoEntry();
				entry.setSegName("1");
				entry.setServerUrl(realUrl);

				entrys.add(entry);

				/*
				 * JSONArray nodelist = jsonObj.getJSONArray("nodelist"); for
				 * (int j = 0; j < nodelist.length(); j++) { JSONObject obj =
				 * nodelist.getJSONObject(j); String realUrl =
				 * obj.getString("location");
				 * 
				 * entry = new VideoEntry(); entry.setSegName("1");
				 * entry.setServerUrl(realUrl); entrys.add(entry);
				 * System.out.println(realUrl); }
				 */

				videoAR.setVideosMap(getType(hd), entrys);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return videoAR;

	}

	private EnumClarity getType(String type) {
		if ("流畅".equals(type)) {
			return EnumClarity.LIUCHANG;
		} else if ("标清".equals(type)) {
			return EnumClarity.LANGUANG;
		} else if ("高清".equals(type)) {
			return EnumClarity.GAOQING;
		} else {
			return EnumClarity.CHAOQING;
		}

	}

	private String getParamsByReg(String regex, String str) {
		Matcher m = Pattern.compile(regex).matcher(str);
		if (m.find()) {
			// m.group(1) 获取正则表达式里的第一个()里匹配的数据
			// m.group() 获取整个正则表达式匹配的数据
			return m.group(1);
		}

		return null;
	}

	public static void main(String[] args) {
		AnalyserForApiFree analyer = new AnalyserForApiFree();
		analyer.analyser("http://www.letv.com/ptv/vplay/2204052.html");
	}

}
