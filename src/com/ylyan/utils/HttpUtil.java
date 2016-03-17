package com.ylyan.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.Header;
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
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpUtil {

	private static final String ENCODE = "utf-8";

	private static final int BUFF_SIZE = 1024;

	public static String geHttpResult(String url, String encoding) {
		String result = "";

		CloseableHttpClient httpClient = null;
		HttpGet httpGet = null;
		CloseableHttpResponse response = null;
		try {
			if (httpClient == null) {
				httpClient = getHttpClient();
			}

			httpGet = getMethod(url);
			response = httpClient.execute(httpGet);
			if (200 == response.getStatusLine().getStatusCode()) {
				result = getResult(response.getEntity(), encoding);
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			httpResponseClose(response);

			httpGetAbort(httpGet);

			httpClientClose(httpClient);
		}

		return result;
	}

	// 根据URL获得数据MAP
	public static Map<String, Object> getIPAndPort(String url) {
		Map<String, Object> map = new HashMap<String, Object>();
		CloseableHttpClient httpClient = null;
		HttpGet httpGet = null;
		CloseableHttpResponse response = null;
		try {
			if (httpClient == null) {
				httpClient = getHttpClient();
			}

			httpGet = getMethod(url);
			response = httpClient.execute(httpGet);

			int requestCode = response.getStatusLine().getStatusCode();
			if (200 == requestCode) {
				Header headerSize = response.getFirstHeader("Content-Length");
				if (headerSize != null) {
					map.put("headerSize", headerSize);
				}

				Header headerExpires = response.getFirstHeader("Expires");
				long expiresTime = 0L;
				if (headerExpires != null) {
					expiresTime = Date.parse(headerExpires.getValue());
					map.put("expiresTime", expiresTime);
				}

				// 根据反射得到IP和Port
				Field connHolder = response.getClass().getDeclaredField(
						"connHolder");
				connHolder.setAccessible(true);
				Object object = connHolder.get(response);

				Field managedConn = object.getClass().getDeclaredField(
						"managedConn");
				managedConn.setAccessible(true);
				Object conn = managedConn.get(object);

				if (conn.getClass().getSimpleName().equals("CPoolProxy")) {
					Method getConnection = conn.getClass().getDeclaredMethod(
							"getConnection", new Class[0]);
					getConnection.setAccessible(true);

					Method getPoolEntry = conn.getClass().getDeclaredMethod(
							"getPoolEntry", new Class[0]);
					getPoolEntry.setAccessible(true);
					Object poolEntry = getPoolEntry.invoke(conn, new Object[0]);

					Method getRoute = poolEntry.getClass().getSuperclass()
							.getDeclaredMethod("getRoute", new Class[0]);
					getRoute.setAccessible(true);
					HttpRoute route = (HttpRoute) getRoute.invoke(poolEntry,
							new Object[0]);
					HttpHost host = route.getTargetHost();

					map.put("ip", host.getHostName());
					map.put("port", host.getPort());
				}
			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} finally {
			httpResponseClose(response);

			httpGetAbort(httpGet);

			httpClientClose(httpClient);
		}
		return map;
	}

	public static CloseableHttpClient getHttpClient() {
		CloseableHttpClient httpClient = null;
		boolean isProxy = false;
		if (isProxy) {
			CredentialsProvider credsProvider = new BasicCredentialsProvider();
			credsProvider.setCredentials(new AuthScope("proxyIP", 8080),
					new UsernamePasswordCredentials("userName", "passWord"));

			httpClient = HttpClients.custom()
					.setDefaultCredentialsProvider(credsProvider).build();
		} else {
			httpClient = HttpClients.createDefault();
		}

		return httpClient;
	}

	public static HttpGet getMethod(String url) {
		HttpGet httpGet = new HttpGet(url);

		// 伪装google浏览器
		String chrome_User_Agent = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.80 Safari/537.36";
		httpGet.addHeader("User-Agent", chrome_User_Agent);
		// URL跳转来源
		httpGet.addHeader("Referer", url);

		// 代理
		boolean isProxy = false;
		if (isProxy) {
			HttpHost proxyHost = new HttpHost("proxyIP", 8080, "http");
			RequestConfig config = RequestConfig.custom().setProxy(proxyHost)
					.build();
			httpGet.setConfig(config);
		}

		return httpGet;
	}

	public static String getResult(HttpEntity entity) {
		String result = "";
		try {
			if (null != entity) {
				// 打印响应内容长度
				// System.out.println(entity.getContentLength());
				result = EntityUtils.toString(entity);
			}

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return result;
	}

	public static String getResult(HttpEntity entity, String encoding) {
		StringBuilder stb = new StringBuilder();
		try {
			if ((encoding == null) || (encoding.trim().isEmpty())) {
				encoding = ENCODE;
			}

			if (entity != null) {
				InputStream instream = entity.getContent();
				byte[] buff = new byte[BUFF_SIZE];
				int tag = -1;
				while ((tag = instream.read(buff)) > 0) {
					if (tag == BUFF_SIZE) {
						stb.append(new String(buff, encoding));
					} else {
						stb.append(new String(Arrays.copyOf(buff, tag),
								encoding));
					}
				}
				buff = null;
			}
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return stb.toString();
	}

	public static void httpClientClose(CloseableHttpClient httpClient) {
		try {
			if (null != httpClient) {
				httpClient.close();
			}
		} catch (IOException e) {
			httpClient = null;
			e.printStackTrace();
		}
	}

	public static void httpGetAbort(HttpGet httpGet) {
		if (null != httpGet) {
			httpGet.abort();
		}
	}

	public static void httpResponseClose(CloseableHttpResponse response) {
		try {
			if (null != response) {
				response.close();
			}
		} catch (IOException e) {
			response = null;
			e.printStackTrace();
		}
	}
}
