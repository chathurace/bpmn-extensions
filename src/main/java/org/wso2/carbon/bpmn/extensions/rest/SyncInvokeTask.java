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

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;
import org.activiti.engine.impl.el.JuelExpression;
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
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Invokes a REST service asynchronously and assigns the return value to the given variable.
 *
 * Example:
 *  <serviceTask id="checkInventory" name="Check inventory" activiti:class="com.woods.servicetasks.rest.SyncInvokeTask">
        <documentation>Sends all ordered items to inventory service and gets the availability status of each item.</documentation>
        <extensionElements>
            <activiti:field name="serviceURL" stringValue="http://localhost:8090/woods/stock" />
            <activiti:field name="method" stringValue="GET" />
            <activiti:field name="vout" stringValue="woodsItems" />
        </extensionElements>
    </serviceTask>
 *
 */
public class SyncInvokeTask implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(SyncInvokeTask.class);

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";

    private final CloseableHttpClient client;

    private JuelExpression serviceURL;
    private FixedValue method;
    private JuelExpression input;
    private FixedValue vout;

    public SyncInvokeTask() {

        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setDefaultMaxPerRoute(200);
        cm.setMaxTotal(200);
        client = HttpClients.custom().setConnectionManager(cm).build();
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Executing SyncInvoke on " + serviceURL.getValue(execution).toString());
        }
        if (POST_METHOD.equals(method.getValue(execution).toString())) {
            executePost(execution);
        } else {
            executeGet(execution);
        }

    }

    private void executeGet(DelegateExecution execution) {
        HttpGet httpGet = null;
        CloseableHttpResponse response = null;
        try {
            httpGet = new HttpGet(serviceURL.getValue(execution).toString());

            response = client.execute(httpGet);
            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            String outVarName = vout.getValue(execution).toString();
            String output = result.toString();
            execution.setVariable(outVarName, output);

            EntityUtils.consume(response.getEntity());

        } catch (Exception e) {
            log.error("Failed to execute GET " + serviceURL.getValue(execution).toString(), e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("Failed to close HTTP response after invoking: GET " + serviceURL.getValue(execution).toString(), e);
            }

            if (httpGet != null) {
                httpGet.releaseConnection();
            }
        }
    }

    private void executePost(DelegateExecution execution) {

        HttpPost httpPost = null;
        CloseableHttpResponse response = null;

        try {
            httpPost = new HttpPost(serviceURL.getValue(execution).toString());
            String inputContent = input.getValue(execution).toString();
            httpPost.setEntity(new StringEntity(inputContent));
            response = client.execute(httpPost);

            BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
            StringBuffer result = new StringBuffer();
            String line = "";
            while ((line = rd.readLine()) != null) {
                result.append(line);
            }

            String outVarName = vout.getValue(execution).toString();
            String output = result.toString();
            execution.setVariable(outVarName, output);

            EntityUtils.consume(response.getEntity());

        } catch (Exception e) {
            log.error("Failed to execute POST " + serviceURL.getValue(execution).toString(), e);
        } finally {
            try {
                if (response != null) {
                    response.close();
                }
            } catch (IOException e) {
                log.error("Failed to close HTTP response after invoking: POST " + serviceURL.getValue(execution).toString(), e);
            }

            if (httpPost != null) {
                httpPost.releaseConnection();
            }
        }
    }

    public void setServiceURL(JuelExpression serviceURL) {
        this.serviceURL = serviceURL;
    }

    public void setInput(JuelExpression input) {
        this.input = input;
    }

    public void setVout(FixedValue vout) {
        this.vout = vout;
    }

    public void setMethod(FixedValue method) { this.method = method; }
}
