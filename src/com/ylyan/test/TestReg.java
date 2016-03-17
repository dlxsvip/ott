package com.ylyan.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestReg {

	private static String HTML_SCRIPT = "<script[^>]*?>[\\s\\S]*?</script>";
	private static String HTML_DIV = "<div[^>]*>.*?</div>*?";
	private static String HTML_SPAN = "<span[^>]*>.*?</span>*?";
	
	public static void main(String[] args) {
		
		
		String reg = "<script.*>.*</script>";
		String reg2 = "tvId:\\s*\\d+";
		String str = ""
				+ "<script type=\"text/javascript\" src=\"http://static.iqiyi.com/js/lib/sea1.2.js\"></script>"
				+ "<link href=/>"
				+ "<script type=\"text/javascript\">"
				+ "Q.PageInfo.playPageInfo = {"
				+ "albumId: 415065600,"
				+ "tvId: 415065600,"
				+ "cid: 1,"
				+ "userId: 0,"
				+ "sourceId:0,"
				+ "tvYear:201509,"
				+ "vType:'video'"
				+ "};"
				+ "</script>";
		String script = getParamsByReg(str,reg);
		System.out.println(script);
		System.out.println(getParamsByReg(script,reg2));
	}

	private static String getParamsByReg(String str, String regex) {
		Matcher m = Pattern.compile(regex).matcher(str);
		if (m.find()) {
			return m.group();
		}

		return null;
	}
}
