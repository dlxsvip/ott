package com.ylyan.analyser;

import com.ylyan.model.VideoAnalyseResult;
import com.ylyan.utils.HttpUtil;

public class AnalyserForFlvcd extends Analyser {

	// 硕鼠接口
	private String API_FLVCD = "http://www.flvcd.com/parse.php?format=&kw={videoUrl}";

	public VideoAnalyseResult analyser(String urlStr) {
		VideoAnalyseResult videoAR = new VideoAnalyseResult();

		API_FLVCD = API_FLVCD.replace("{videoUrl}", urlStr);
		String htmlInfo = HttpUtil.geHttpResult(API_FLVCD, UTF_8);
		// 解析xml文件，获取结果
		getResult(videoAR, htmlInfo);
		return videoAR;
	}

	private VideoAnalyseResult getResult(VideoAnalyseResult videoAR,
			String htmlInfo) {

		System.out.println(htmlInfo);

		return videoAR;
	}

	public static void main(String[] args) {
		AnalyserForFlvcd af = new AnalyserForFlvcd();
		af.analyser("http://www.letv.com/ptv/vplay/2204052.html");
	}

}
