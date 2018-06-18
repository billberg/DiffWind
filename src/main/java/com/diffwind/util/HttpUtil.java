package com.diffwind.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Consts;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

public class HttpUtil {

	private static Logger logger = Logger.getLogger(HttpUtil.class);

	private static int CONN_TIMEOUT = 5000;
	private static int SOCKET_TIMEOUT = 5000;

	private String requestUrl = null;
	// private Map<String, String> requestParams = null;
	List<NameValuePair> requestParams = new ArrayList<NameValuePair>();

	/**
	 * 
	 * @return
	 */
	protected String doPost() {
		String ret = null;
		try {
			HttpClient client = HttpClients.createDefault();
			// HttpGet httpGet = new HttpGet(requestUrl);

			HttpPost httpPost = new HttpPost(requestUrl);
			httpPost.setEntity(new UrlEncodedFormEntity(requestParams, Consts.UTF_8));

			logger.info("Request: " + requestUrl);
			// add request header
			// request.addHeader("User-Agent", USER_AGENT);
			HttpResponse response = client.execute(httpPost);

			logger.info("Response Code: " + response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			// String ret = response.asString();
			ret = result.toString();
			logger.info("Response Body: " + ret);

			// response = HttpClient.httpPostRequest(BASE_URL + requestUrl,
			// URLEncoder.encode(jsonParams.toString(),"UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;

	}

	public String doGet() {
		String host = "vip.stock.finance.sina.com.cn";
		String ret = null;
		try {
			HttpClient httpClient = HttpClients.createDefault();

			// requestUrl += "?" + EntityUtils.toString(new
			// UrlEncodedFormEntity(requestParams),Consts.UTF_8);
			HttpGet httpGet = new HttpGet(requestUrl);

			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT)
					.setConnectTimeout(CONN_TIMEOUT).build();// 设置请求和传输超时时间
			httpGet.setConfig(requestConfig);

			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
			httpGet.addHeader("Host", host);
			httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Connection", "keep-alive");
			// httpGet.addHeader("Cookie", "s=1adh11bwxe;
			// xq_a_token=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// Hm_lvt_1db88642e346389874251b5a1eded6e3=1450057065;
			// Hm_lpvt_1db88642e346389874251b5a1eded6e3=1450265829;
			// __utma=1.1137084750.1450057065.1450242574.1450265829.10;
			// __utmc=1;
			// __utmz=1.1450057065.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none);
			// xqat=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// xq_r_token=1e6effcb2447f456d684a00e2503cfcc481c9dcd;
			// xq_is_login=1; u=1776991309;
			// xq_token_expire=Fri%20Jan%2008%202016%2009%3A37%3A59%20GMT%2B0800%20(CST);
			// snbim_minify=true; bid=d9601d32cacaec39ec8731be9526b977_ii5aliuu;
			// webp=0");

			logger.info("Request: " + requestUrl);
			// add request header
			// request.addHeader("User-Agent", USER_AGENT);
			HttpResponse response = httpClient.execute(httpGet);

			logger.info("Response Code: " + response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), "GBK"));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			// String ret = response.asString();
			ret = result.toString();
			logger.info("Response Body: " + ret);

			// response = HttpClient.httpPostRequest(BASE_URL + requestUrl,
			// URLEncoder.encode(jsonParams.toString(),"UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;

	}

	/*
	 * public String doGet(String host, String cookie) { String ret = null; try
	 * { HttpClient httpClient = HttpClients.createDefault();
	 * 
	 * HttpGet httpGet = new HttpGet(requestUrl);
	 * 
	 * //RequestConfig requestConfig =
	 * RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout
	 * (CONN_TIMEOUT).build();//设置请求和传输超时时间
	 * 
	 * RequestConfig requestConfig =
	 * RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout
	 * (CONN_TIMEOUT).build();//设置请求和传输超时时间 httpGet.setConfig(requestConfig);
	 * 
	 * httpGet.addHeader("Host", host); httpGet.addHeader("User-Agent",
	 * "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0"
	 * ); httpGet.addHeader("Accept",
	 * "text/html,application/xhtml+xml,application/xml;q=0.9;q=0.8");
	 * httpGet.addHeader("Accept-Language","zh-CN,zh;q=0.8,en-US;q=0.5,en;q=0.3"
	 * ); httpGet.addHeader("Accept-Encoding","gzip, deflate, br");
	 * httpGet.addHeader("Connection", "keep-alive");
	 * 
	 * //httpGet.addHeader("Cookie",
	 * "s=1adh11bwxe; xq_a_token=53e209bbcfd63cc4f4497d6d26df42da7977ff5a; Hm_lvt_1db88642e346389874251b5a1eded6e3=1450057065; Hm_lpvt_1db88642e346389874251b5a1eded6e3=1450265829; __utma=1.1137084750.1450057065.1450242574.1450265829.10; __utmc=1; __utmz=1.1450057065.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none); xqat=53e209bbcfd63cc4f4497d6d26df42da7977ff5a; xq_r_token=1e6effcb2447f456d684a00e2503cfcc481c9dcd; xq_is_login=1; u=1776991309; xq_token_expire=Fri%20Jan%2008%202016%2009%3A37%3A59%20GMT%2B0800%20(CST); snbim_minify=true; bid=d9601d32cacaec39ec8731be9526b977_ii5aliuu; webp=0"
	 * );
	 * 
	 * StringBuffer cookieStr = new StringBuffer(); Map<String,String> cookieMap
	 * = new HashMap<String,String>(); for (Header cookie : cookies) { String[]
	 * keyvalue = cookie.getValue().split("; *"); for (String kv : keyvalue) {
	 * String[] k2v = kv.split("="); if (k2v.length == 2 &&
	 * !cookieMap.containsKey(k2v[0])) { cookieMap.put(k2v[0], k2v[1]);
	 * cookieStr.append(kv).append("; "); } }
	 * //cookieStr.append(cookie.getValue()); }
	 * httpGet.addHeader("Cookie",cookie);
	 * 
	 * logger.info("Request: " + requestUrl); // add request header //
	 * request.addHeader("User-Agent", USER_AGENT); HttpResponse response =
	 * httpClient.execute(httpGet);
	 * 
	 * logger.info("Response Code: " +
	 * response.getStatusLine().getStatusCode());
	 * 
	 * BufferedReader rd = new BufferedReader(new InputStreamReader(
	 * response.getEntity().getContent(),Consts.UTF_8)); StringBuffer result =
	 * new StringBuffer(); String line = ""; while ((line = rd.readLine()) !=
	 * null) { result.append(line); }
	 * 
	 * // String ret = response.asString(); ret = result.toString();
	 * //logger.info("Response Body: " + ret);
	 * 
	 * // response = HttpClient.httpPostRequest(BASE_URL + requestUrl, //
	 * URLEncoder.encode(jsonParams.toString(),"UTF-8")); } catch (Exception e)
	 * { // TODO Auto-generated catch block e.printStackTrace(); }
	 * 
	 * return ret;
	 * 
	 * }
	 */

	public static String doGet(String requestUrl, Map<String, String> headers, int timeout) throws IOException {
		String ret = null;

		HttpClient httpClient = HttpClients.createDefault();

		HttpGet httpGet = new HttpGet(requestUrl);

		// RequestConfig requestConfig =
		// RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONN_TIMEOUT).build();//设置请求和传输超时时间

		RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(timeout).setConnectTimeout(timeout)
				.build();// 设置请求和传输超时时间
		httpGet.setConfig(requestConfig);

		if (headers != null) {
			for (Entry<String, String> head : headers.entrySet()) {
				httpGet.addHeader(head.getKey(), head.getValue());
			}
		}

		logger.info("Request: " + requestUrl);
		// add request header
		// request.addHeader("User-Agent", USER_AGENT);
		HttpResponse response = httpClient.execute(httpGet);

		logger.info("Response Code: " + response.getStatusLine().getStatusCode());
		if (200 != response.getStatusLine().getStatusCode() ) {
			throw new RuntimeException("http请求失败，响应码: " + response.getStatusLine().getStatusCode());
		}

		BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
		StringBuffer result = new StringBuffer();
		String line = "";
		while ((line = rd.readLine()) != null) {
			result.append(line);
		}

		// String ret = response.asString();
		ret = result.toString();
		// logger.info("Response Body: " + ret);

		// response = HttpClient.httpPostRequest(BASE_URL + requestUrl,
		// URLEncoder.encode(jsonParams.toString(),"UTF-8"));

		return ret;

	}

	@Deprecated
	public String doGet2() {
		String ret = null;
		try {
			HttpClient client = HttpClients.createDefault();
			// requestUrl += "?" + EntityUtils.toString(new
			// UrlEncodedFormEntity(requestParams),Consts.UTF_8);

			HttpGet httpGet = new HttpGet("http://xueqiu.com/");

			RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT)
					.setConnectTimeout(CONN_TIMEOUT).build();// 设置请求和传输超时时间
			httpGet.setConfig(requestConfig);

			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
			httpGet.addHeader("Host", "xueqiu.com");
			httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Connection", "keep-alive");
			// httpGet.addHeader("Cookie", "s=1adh11bwxe;
			// xq_a_token=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// Hm_lvt_1db88642e346389874251b5a1eded6e3=1450057065;
			// Hm_lpvt_1db88642e346389874251b5a1eded6e3=1450265829;
			// __utma=1.1137084750.1450057065.1450242574.1450265829.10;
			// __utmc=1;
			// __utmz=1.1450057065.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none);
			// xqat=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// xq_r_token=1e6effcb2447f456d684a00e2503cfcc481c9dcd;
			// xq_is_login=1; u=1776991309;
			// xq_token_expire=Fri%20Jan%2008%202016%2009%3A37%3A59%20GMT%2B0800%20(CST);
			// snbim_minify=true; bid=d9601d32cacaec39ec8731be9526b977_ii5aliuu;
			// webp=0");

			// logger.info("Request: " + requestUrl);
			// add request header
			// request.addHeader("User-Agent", USER_AGENT);
			HttpResponse response = client.execute(httpGet);
			Header[] cookies = response.getHeaders("Set-Cookie");

			httpGet = new HttpGet(requestUrl);

			// RequestConfig requestConfig =
			// RequestConfig.custom().setSocketTimeout(SOCKET_TIMEOUT).setConnectTimeout(CONN_TIMEOUT).build();//设置请求和传输超时时间
			httpGet.setConfig(requestConfig);

			httpGet.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:39.0) Gecko/20100101 Firefox/39.0");
			httpGet.addHeader("Host", "xueqiu.com");
			httpGet.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			httpGet.addHeader("Connection", "keep-alive");
			// httpGet.addHeader("Cookie", "s=1adh11bwxe;
			// xq_a_token=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// Hm_lvt_1db88642e346389874251b5a1eded6e3=1450057065;
			// Hm_lpvt_1db88642e346389874251b5a1eded6e3=1450265829;
			// __utma=1.1137084750.1450057065.1450242574.1450265829.10;
			// __utmc=1;
			// __utmz=1.1450057065.1.1.utmcsr=(direct)|utmccn=(direct)|utmcmd=(none);
			// xqat=53e209bbcfd63cc4f4497d6d26df42da7977ff5a;
			// xq_r_token=1e6effcb2447f456d684a00e2503cfcc481c9dcd;
			// xq_is_login=1; u=1776991309;
			// xq_token_expire=Fri%20Jan%2008%202016%2009%3A37%3A59%20GMT%2B0800%20(CST);
			// snbim_minify=true; bid=d9601d32cacaec39ec8731be9526b977_ii5aliuu;
			// webp=0");
			StringBuffer cookieStr = new StringBuffer();
			for (Header cookie : cookies) {
				cookieStr.append(cookie.getValue());
			}
			httpGet.addHeader("Cookie", cookieStr.toString());

			logger.info("Request: " + requestUrl);
			// add request header
			// request.addHeader("User-Agent", USER_AGENT);
			response = client.execute(httpGet);

			logger.info("Response Code: " + response.getStatusLine().getStatusCode());

			BufferedReader rd = new BufferedReader(
					new InputStreamReader(response.getEntity().getContent(), Consts.UTF_8));
			StringBuffer result = new StringBuffer();
			String line = "";
			while ((line = rd.readLine()) != null) {
				result.append(line);
			}

			// String ret = response.asString();
			ret = result.toString();
			// logger.info("Response Body: " + ret);

			// response = HttpClient.httpPostRequest(BASE_URL + requestUrl,
			// URLEncoder.encode(jsonParams.toString(),"UTF-8"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return ret;

	}

	protected void addParam(String param, String value) {
		requestParams.add(new BasicNameValuePair(param, value));
	}

	protected void addParams(String[] params, String[] values) {
		for (int i = 0; i < params.length; i++)
			addParam(params[i], values[i]);
	}

	protected void clearParams() {
		requestParams.clear();
	}

	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}

	public static void main(String[] args) {

		String requestUrl = "http://xueqiu.com/";

		// String requestUrl = "http://hq.sinajs.cn/list=sh601003,sh601001";
		HttpUtil httpUtil = new HttpUtil();
		httpUtil.setRequestUrl(requestUrl);
		httpUtil.doGet2();
	}

}
