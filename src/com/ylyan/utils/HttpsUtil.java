package com.ylyan.utils;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.Consts;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.AuthSchemes;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

public class HttpsUtil {

	//
	private static SSLConnectionSocketFactory socketFactory;

	public static void main(String[] args) {

		sendXML("https://www.baidu.com", false);

	}

	public static void sendXML(String url, boolean isPost) {
		String str = null;
		if (isPost) {
			str = doHttpsPost(url, null, null, null);
		} else {
			str = doHttpsGet(url, null, null);
		}
		System.out.println(str);
	}

	// 获取跳过证书验证的httpClient
	public static CloseableHttpClient getNoSSLHttpClient() {
		enableSSL();
		RequestConfig defaultRequestConfig = RequestConfig
				.custom()
				.setCookieSpec(CookieSpecs.STANDARD_STRICT)
				//
				.setExpectContinueEnabled(true)
				//
				.setTargetPreferredAuthSchemes(
						Arrays.asList(AuthSchemes.NTLM, AuthSchemes.DIGEST))//
				.setProxyPreferredAuthSchemes(Arrays.asList(AuthSchemes.BASIC))//
				.build();

		// 创建可用Scheme
		Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder
				.<ConnectionSocketFactory> create()
				.register("http", PlainConnectionSocketFactory.INSTANCE)//
				.register("https", socketFactory)//
				.build();

		// 创建ConnectionManager，添加Connection配置信息
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(
				socketFactoryRegistry);

		return HttpClients.custom() //
				.setConnectionManager(connectionManager) //
				.setDefaultRequestConfig(defaultRequestConfig)//
				.build();

	}

	// 执行get请求
	public static String doHttpsGet(String url, String cookie, String refer) {
		String str = "";

		CloseableHttpClient httpClient = null;
		HttpGet get = null;
		CloseableHttpResponse response = null;

		try {
			httpClient = getNoSSLHttpClient();

			get = new HttpGet(url);

			if (cookie != null) {
				get.setHeader("Cookie", cookie);
			}

			if (refer != null) {
				get.setHeader("Referer", refer);
			}

			response = httpClient.execute(get);

			HttpEntity entity = response.getEntity();
			if (null != entity) {
				str = EntityUtils.toString(entity);

			}

		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			httpResponseClose(response);
			httpGetAbort(get);
			httpClientClose(httpClient);
		}

		return str;
	}

	// 执行post提交
	public static String doHttpsPost(String url, List<NameValuePair> values,
			String cookie, String refer) {
		String str = "";

		CloseableHttpClient httpClient = null;
		HttpPost post = null;
		CloseableHttpResponse response = null;
		try {

			httpClient = getNoSSLHttpClient();

			post = new HttpPost(url);

			if (cookie != null) {
				post.setHeader("Cookie", cookie);
			}

			if (refer != null) {
				post.setHeader("Referer", refer);
			}

			UrlEncodedFormEntity entity = new UrlEncodedFormEntity(values,
					Consts.UTF_8);
			post.setEntity(entity);

			response = httpClient.execute(post);

			HttpEntity resultEntity = response.getEntity();
			if (null != resultEntity) {
				str = EntityUtils.toString(resultEntity);
			}
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			httpResponseClose(response);
			httpPostAbort(post);
			httpClientClose(httpClient);
		}

		return str;
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

	public static void httpPostAbort(HttpPost httpPost) {
		if (null != httpPost) {
			httpPost.abort();
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

	// 重写验证方法，取消检测ssl
	private static TrustManager manager = new X509TrustManager() {

		@Override
		public X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkServerTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {

		}

		@Override
		public void checkClientTrusted(X509Certificate[] arg0, String arg1)
				throws CertificateException {

		}
	};

	// 调用ssl
	private static void enableSSL() {
		try {
			SSLContext context = SSLContext.getInstance("TLS");
			context.init(null, new TrustManager[] { manager }, null);
			socketFactory = new SSLConnectionSocketFactory(context,
					NoopHostnameVerifier.INSTANCE);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (KeyManagementException e) {
			e.printStackTrace();
		}

	}
}
