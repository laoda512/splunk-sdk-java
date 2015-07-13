package com.splunk;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;


public class StringUtil {

	public static boolean isEmpty(String s){
		return s==null||"".equals(s)||"null".equals(s.toLowerCase());
	}
	
	 public static String streamToString(java.io.InputStream is) {
	        Reader r = null;
	        try {
	            r = new InputStreamReader(is, "UTF-8");
	        } catch (UnsupportedEncodingException e) {
	            throw new RuntimeException("Your JVM does not support UTF-8!");
	        }
	        StringBuffer sb = new StringBuffer();
	        char[] buffer = new char[8192];
	        int bytesRead=0;
	        try {
	            while ((bytesRead = r.read(buffer,0,buffer.length)) > 0) {
	                sb.append(buffer, 0 , bytesRead);     
	            }
	        } catch (IOException ioex) {
	            throw new RuntimeException(ioex.getMessage());
	        }
	        return sb.toString();
	    }

}
