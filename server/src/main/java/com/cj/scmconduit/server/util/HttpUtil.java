package com.cj.scmconduit.server.util;

import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;

import com.cj.scmconduit.core.p4.P4Credentials;

public class HttpUtil {

	public static P4Credentials readBasicCredentials(HttpServletRequest request)
			throws UnsupportedEncodingException {
		final P4Credentials credentials;
		final String authHeader = request.getHeader("Authorization");
		if(authHeader==null){
			credentials = null;
		} else {
			
			String s = new String(Base64.decodeBase64(authHeader.replaceAll(Pattern.quote("Basic:"), "").trim()), "UTF-8");
			String[] parts = s.split(Pattern.quote(":"));
			credentials = new P4Credentials(parts[0], parts[1]);
		}
		return credentials;
	}

}
