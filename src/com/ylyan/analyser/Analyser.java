package com.ylyan.analyser;

import java.util.Map;

import com.ylyan.model.VideoEntry;
import com.ylyan.utils.HttpUtil;

public class Analyser {

	// http 正则
	private static final String VALIDATION_URL_PATTERN = "((http|https?)?://)?(([a-zA-Z0-9\\._-]+\\.[a-zA-Z]{2,6})|([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}))(:[0-9]{1,4})*(/[a-zA-Z0-9\\&%_\\./-~-]*)?";

	protected static final String UTF_8 = "utf-8";
	protected static final String GBK = "GBK";
	protected static final String GBK2312 = "GBK2312";

	protected VideoEntry getVideo(String url) {
		VideoEntry videoEntry = new VideoEntry();

		Map<String, Object> map = HttpUtil.getIPAndPort(url);

		Object ip = map.get("ip");
		Object port = map.get("port");
		videoEntry.setIP(null == ip ? "" : (String) ip);
		videoEntry.setPort(null == port ? 0 : (int) port);

		return videoEntry;
	}

}
