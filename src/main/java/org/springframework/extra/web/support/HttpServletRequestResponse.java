package org.springframework.extra.web.support;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class HttpServletRequestResponse {
    private final HttpServletRequest request;
    private final HttpServletResponse response;

    public HttpServletRequestResponse(final HttpServletRequest request,
	    final HttpServletResponse response) {
	this.request = request;
	this.response = response;
    }

    public HttpServletRequest getRequest() {
	return request;
    }

    public HttpServletResponse getResponse() {
	return response;
    }
}
