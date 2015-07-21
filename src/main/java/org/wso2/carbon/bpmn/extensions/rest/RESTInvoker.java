/*
 * Copyright 2005-2014 WSO2, Inc. (http://wso2.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wso2.carbon.bpmn.extensions.rest;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

public class RESTInvoker {

    private static final Logger log = LoggerFactory.getLogger(RESTInvoker.class);

    private final CloseableHttpClient client;

    public RESTInvoker() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(200);
        cm.setMaxTotal(200);
        client = HttpClients.custom().setConnectionManager(cm).build();
    }

    public String invokeGET(URI uri) throws Exception {

        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        String output = "";
        try {
            httpGet = new HttpGet(uri);

            response = client.execute(httpGet);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            output = result.toString();
            EntityUtils.consume(response.getEntity());

        } finally {
            if (response != null) {
                response.close();
            }

            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
        return output;
    }

    public String invokePOST(URI uri, String payload) throws Exception {

        HttpPost httpPost = null;
        CloseableHttpResponse response = null;
        String output = "";
        try {
            httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(payload));
            response = client.execute(httpPost);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }
            output = result.toString();
            EntityUtils.consume(response.getEntity());

        } finally {
            if (response != null) {
                response.close();
            }

            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
        return output;
    }
}
