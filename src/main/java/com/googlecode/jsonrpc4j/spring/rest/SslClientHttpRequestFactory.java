/*
 * The MIT License
 *
 * Copyright (c) 2014 jsonrpc4j
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.googlecode.jsonrpc4j.spring.rest;

import java.io.IOException;
import java.net.HttpURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

public class SslClientHttpRequestFactory
     extends SimpleClientHttpRequestFactory
{

    private SSLContext        sslContext;
    private HostnameVerifier  hostNameVerifier;    

    @Override
    protected void prepareConnection(HttpURLConnection connection, String httpMethod)
        throws IOException  {

        if (connection instanceof HttpsURLConnection) {
            final HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
            if (hostNameVerifier!=null)
                httpsConnection.setHostnameVerifier(hostNameVerifier);
            if (sslContext!=null)
                httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }
        
        super.prepareConnection(connection, httpMethod);
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public void setHostNameVerifier(HostnameVerifier hostNameVerifier) {
        this.hostNameVerifier = hostNameVerifier;
    }
    
}
