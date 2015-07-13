package com.splunk;

import java.io.InputStream;
import java.net.HttpURLConnection;

/**
 * Created by cwang on 4/16/15.
 */
public interface Connecter {
	  public ResponseMessage send(HttpService service,String path, RequestMessage request) ;
}
