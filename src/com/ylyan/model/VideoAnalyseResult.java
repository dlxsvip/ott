package com.ylyan.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ylyan.enumer.EnumClarity;

public class VideoAnalyseResult {

	private String url;

	private Map<EnumClarity, List<VideoEntry>> videosMap = new HashMap<EnumClarity, List<VideoEntry>>();

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Map<EnumClarity, List<VideoEntry>> getVideosMap() {
		return videosMap;
	}

	public void setVideosMap(EnumClarity type, List<VideoEntry> videos) {
		if (!videosMap.containsKey(type)) {
			videosMap.put(type, videos);
		} else {
			videosMap.get(type).addAll(videos);
		}

	}

}
