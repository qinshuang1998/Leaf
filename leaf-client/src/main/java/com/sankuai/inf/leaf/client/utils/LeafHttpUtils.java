package com.sankuai.inf.leaf.client.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * @author qinshuang1998
 * @date 2019/7/12
 */
public class LeafHttpUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeafHttpUtils.class);

    private LeafHttpUtils() {

    }

    public static String get(String url, Integer readTimeout, Integer connectTimeout) {
        HttpURLConnection conn = null;
        BufferedReader rd = null;
        StringBuilder sb = new StringBuilder();
        String line = null;
        String response = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(readTimeout);
            conn.setConnectTimeout(connectTimeout);
            conn.setUseCaches(false);
            conn.connect();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();
        } catch (Exception e) {
            LOGGER.warn("error get url:{}", url);
        } finally {
            try {
                if (rd != null) {
                    rd.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                LOGGER.info("error close conn");
            }
        }
        return response;
    }

    public static String post(String url, Integer readTimeout, Integer connectTimeout) {
        return post(url, null, readTimeout, connectTimeout);
    }

    public static String post(String url, Map<String, String> form, Integer readTimeout, Integer connectTimeout) {
        HttpURLConnection conn = null;
        OutputStreamWriter os = null;
        BufferedReader rd = null;
        StringBuilder param = new StringBuilder();
        StringBuilder sb = new StringBuilder();
        String line = null;
        String response = null;
        if (form != null) {
            for (Map.Entry<String, String> entry : form.entrySet()) {
                String key = entry.getKey();
                if (param.length() != 0) {
                    param.append("&");
                }
                param.append(key).append("=").append(entry.getValue());
            }
        }
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(readTimeout);
            conn.setConnectTimeout(connectTimeout);
            conn.setUseCaches(false);
            conn.connect();
            os = new OutputStreamWriter(conn.getOutputStream(), "UTF-8");
            os.write(param.toString());
            os.flush();
            rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while ((line = rd.readLine()) != null) {
                sb.append(line);
            }
            response = sb.toString();
        } catch (Exception e) {
            LOGGER.warn("error post url:{} {}", url, param);
        } finally {
            try {
                if (os != null) {
                    os.close();
                }
                if (rd != null) {
                    rd.close();
                }
                if (conn != null) {
                    conn.disconnect();
                }
            } catch (IOException e) {
                LOGGER.info("error close conn");
            }
        }
        return response;
    }
}
