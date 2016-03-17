package com.ylyan.test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

public class TestHttpClient {

	public static void main(String[] args) {
		String url = "http://v.youku.com/v_show/id_XODU1NTYxOTg4.html";
		CloseableHttpClient httpclient = getHttpClient(false);
		HttpGet httpGet = httpGet(url, false);

		String str = getResult(httpclient, httpGet, "UTF-8");
		System.out.println(str);

		try {
			httpGet.abort();
			httpclient.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static HttpGet httpGet(String url, boolean isProxy) {

		// 创建httpget.
		HttpGet httpGet = new HttpGet(url);
		// 伪装 火狐
		httpGet.addHeader(
				"User-Agent",
				"Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.1; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; Media Center PC 6.0; .NET4.0C; .NET4.0E)");

		if (isProxy) {
			HttpHost proxyHost = new HttpHost("12.14.0.2", 8080, "http");
			RequestConfig config = RequestConfig.custom().setProxy(proxyHost)
					.build();
			httpGet.setConfig(config);
		}

		return httpGet;
	}

	public static CloseableHttpClient getHttpClient(boolean isProxy) {

		CloseableHttpClient httpclient = null;

		if (isProxy) {
			CredentialsProvider cp = new BasicCredentialsProvider();
			cp.setCredentials(
					new AuthScope("12.14.0.2", 8080),
					new UsernamePasswordCredentials("ywx234799", "U2520@huawei"));

			httpclient = HttpClients.custom().setDefaultCredentialsProvider(cp)
					.build();
		} else {
			httpclient = HttpClients.createDefault();
		}

		return httpclient;
	}

	public static String getResult(CloseableHttpClient httpclient,
			HttpGet httpGet, String encoding) {
		String str = "";
		try {
			// 执行get请求.
			CloseableHttpResponse response = httpclient.execute(httpGet);
			try {
				// 获取响应实体
				HttpEntity entity = response.getEntity();
				// 打印响应状态
				System.out.println(response.getStatusLine());
				if (200 == response.getStatusLine().getStatusCode()) {
					if (entity != null) {
						// 打印响应内容 方式1
						// str = EntityUtils.toString(entity);

						// 打印响应内容 方式2
						str = getResponse(entity, encoding);
					}
				}

			} finally {
				response.close();
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return str;
	}

	public static String getResponse(HttpEntity entity, String encoding) {
		StringBuilder sb = new StringBuilder();

		InputStream inputStream = null;

		try {
			inputStream = entity.getContent();
			int tag = -1;
			byte[] buff = new byte[1024];
			while ((tag = inputStream.read(buff)) > 0) {
				if (tag == 1024) {
					sb.append(new String(buff, encoding));
				} else {
					sb.append(new String(Arrays.copyOf(buff, tag), encoding));
				}
			}
			buff = null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}

}
