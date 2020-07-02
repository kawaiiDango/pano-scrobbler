package com.arn.scrobble;

import android.util.Base64;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


 public class IdentifyProtocolV1 {

     private String encryptByHMACSHA1(byte[] data, byte[] key) {
 		try {
 			SecretKeySpec signingKey = new SecretKeySpec(key, "HmacSHA1");
 			Mac mac = Mac.getInstance("HmacSHA1");
 			mac.init(signingKey);
 			byte[] rawHmac = mac.doFinal(data);
 			return  Base64.encodeToString(rawHmac, Base64.DEFAULT);
 		} catch (Exception e) {
 			e.printStackTrace();
 		}
 		return "";
 	}
 
 	private String getUTCTimeSeconds() {  
 	    Calendar cal = Calendar.getInstance();   
 	    int zoneOffset = cal.get(Calendar.ZONE_OFFSET);   
 	    int dstOffset = cal.get(Calendar.DST_OFFSET);    
 	    cal.add(Calendar.MILLISECOND, -(zoneOffset + dstOffset));    
 	    return cal.getTimeInMillis()/1000 + "";
 	}  
 	
 	private String postHttp(String posturl, Map<String, Object> params, int timeOut) {
 		StringBuilder res = new StringBuilder();
 		String BOUNDARYSTR = "*****2015.03.30.acrcloud.rec.copyright." + System.currentTimeMillis() + "*****";
 		String BOUNDARY = "--" + BOUNDARYSTR + "\r\n";
 		String ENDBOUNDARY = "--" + BOUNDARYSTR + "--\r\n\r\n";
 		
 		String stringKeyHeader = BOUNDARY +
                 "Content-Disposition: form-data; name=\"%s\"" +
                 "\r\n\r\n%s\r\n";
 		String filePartHeader = BOUNDARY +
                  "Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n" +
                  "Content-Type: application/octet-stream\r\n\r\n";		
  
 		URL url = null;
 		HttpURLConnection conn = null;
 		OutputStream out = null;
 		BufferedReader reader = null;
 		ByteArrayOutputStream postBufferStream = new ByteArrayOutputStream();
 		try {
 			for (String key : params.keySet()) {
 				Object value = params.get(key);
 				if (value instanceof String || value instanceof Integer) {
 					postBufferStream.write(String.format(stringKeyHeader, key, value).getBytes());
 				} else if (value instanceof File){
                    postBufferStream.write(String.format(filePartHeader, key, key).getBytes());

 				    File file = (File) value;
                    byte[] buffer = new byte[1024];
                    if (!file.exists()) {
                        throw new IOException("File doesn't exist");
                    }
                    FileInputStream fin= null;
                    try {
                        fin = new FileInputStream(file);
                        int bytesRead = 0;
                        while(fin.available()>0) {
                            if (Thread.interrupted())
                                throw new InterruptedException();
                            bytesRead = fin.read(buffer, 0, buffer.length);
                            postBufferStream.write(buffer, 0, bytesRead);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fin != null)
                                fin.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    postBufferStream.write("\r\n".getBytes());
                }
 			}
 			postBufferStream.write(ENDBOUNDARY.getBytes());
 			byte[] postByteArray = postBufferStream.toByteArray();

 			url = new URL(posturl);
 			conn = (HttpURLConnection) url.openConnection();
 			conn.setConnectTimeout(timeOut);
 			conn.setReadTimeout(timeOut);
 			conn.setRequestMethod("POST");
 			conn.setFixedLengthStreamingMode(postByteArray.length);
 			conn.setDoOutput(true);
 			conn.setDoInput(true);
 			conn.setRequestProperty("Accept-Charset", "utf-8");
 			conn.setRequestProperty("Content-type", "multipart/form-data;boundary=" + BOUNDARYSTR);
 
 			conn.connect();
 			out = conn.getOutputStream();
 			out.write(postByteArray);
 			out.flush();
 			int response = conn.getResponseCode();
 			if (response == HttpURLConnection.HTTP_OK) {
 				reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
 				String tmpRes = "";
 				while ((tmpRes = reader.readLine()) != null) {
 					if (tmpRes.length() > 0)
 						res.append(tmpRes);
 				}
 			}
 		} catch (Exception e) {
 			e.printStackTrace();
 		} finally {
 			try {
 				if (postBufferStream != null) {
 					postBufferStream.close();
 					postBufferStream = null;
 				}
 				if (out != null) {
 					out.close();
 					out = null;
 				}
 				if (reader != null) {
 					reader.close();
 					reader = null;
 				}
 				if (conn != null) {
 					conn.disconnect();
 					conn = null;
 				}
 			} catch (IOException e) {
 				e.printStackTrace();
 			}
 		}
 		return res.toString();
 	}
 
     public String recognize(String host, String accessKey, String secretKey, File file, String queryType, int timeout)
     {
     	String method = "POST";
     	String httpURL = "/v1/identify";
     	String dataType = queryType;
     	String sigVersion = "1";
     	String timestamp = getUTCTimeSeconds();
 
     	String reqURL = "https://" + host + httpURL;
 
     	String sigStr = method + "\n" + httpURL + "\n" + accessKey + "\n" + dataType + "\n" + sigVersion + "\n" + timestamp;
     	String signature = encryptByHMACSHA1(sigStr.getBytes(), secretKey.getBytes());
 
         Map<String, Object> postParams = new HashMap<>();
         postParams.put("access_key", accessKey);
         if (file == null){
             postParams.put("sample_bytes", "0");
             postParams.put("sample", "");
         } else {
             postParams.put("sample_bytes", file.length() + "");
             postParams.put("sample", file);
         }
         postParams.put("timestamp", timestamp);
         postParams.put("signature", signature);
         postParams.put("data_type", queryType);
         postParams.put("signature_version", sigVersion);

         return postHttp(reqURL, postParams, timeout);
     }
 }
