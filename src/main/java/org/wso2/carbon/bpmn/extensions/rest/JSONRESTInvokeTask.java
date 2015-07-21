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

import com.jayway.jsonpath.JsonPath;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.activiti.engine.impl.el.FixedValue;
import org.activiti.engine.impl.el.JuelExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

/**
 * Invokes a REST service asynchronously and assigns the return value to the given variable.
 *
 * Example:
 *  <serviceTask id="checkInventory" name="Check inventory" activiti:class="com.woods.servicetasks.rest.SyncInvokeTask">
        <documentation>Sends all ordered items to inventory service and gets the availability status of each item.</documentation>
        <extensionElements>
            <activiti:field name="serviceURL" stringValue="http://localhost:8090/woods/stock/${service1}" />
            <activiti:field name="method" stringValue="GET | POST" />
            <activiti:field name="input" stringValue="{}" /> JSON with expressions
            <activiti:field name="outputMappings" stringValue="var1:output.name,var2:address" />
        </extensionElements>
    </serviceTask>
 *
 */
public class JSONRESTInvokeTask implements JavaDelegate {

    private static final Logger log = LoggerFactory.getLogger(JSONRESTInvokeTask.class);

    private static final String GET_METHOD = "GET";
    private static final String POST_METHOD = "POST";

    private RESTInvoker restInvoker;

    private JuelExpression serviceURL;
    private FixedValue method;
    private JuelExpression input;
    private FixedValue vout;
    private FixedValue outputMappings;

    public JSONRESTInvokeTask() {
        restInvoker = new RESTInvoker();
    }

    @Override
    public void execute(DelegateExecution execution) {
        if (log.isDebugEnabled()) {
            log.debug("Executing RESTInvokeTask on " + serviceURL.getValue(execution).toString());
        }
        if (POST_METHOD.equals(method.getValue(execution).toString())) {
            executePost(execution);
        } else {
            executeGet(execution);
        }

    }

    private void executeGet(DelegateExecution execution) {
        try {

            String output = restInvoker.invokeGET(new URI(serviceURL.getValue(execution).toString()));
            String outVarName = vout.getValue(execution).toString();
            String outMappings = outputMappings.getValue(execution).toString();

            outMappings = outMappings.trim();
            String[] mappings = outMappings.split(",");
            for (String mapping : mappings) {
                String[] mappingParts = mapping.split(":");
                String varName = mappingParts[0];
                String jsonExpression = mappingParts[1];
                String value = JsonPath.read(output, jsonExpression);
                execution.setVariable(varName, value);
            }

        } catch (Exception e) {
            log.error("Failed to execute GET " + serviceURL.getValue(execution).toString(), e);
        }
    }

    private void executePost(DelegateExecution execution) {

        try {
            String inputContent = input.getValue(execution).toString();
            String outMappings = outputMappings.getValue(execution).toString();
            String output = restInvoker.invokePOST(new URI(serviceURL.getValue(execution).toString()), inputContent);

            outMappings = outMappings.trim();
            String[] mappings = outMappings.split(",");
            for (String mapping : mappings) {
                String[] mappingParts = mapping.split(":");
                String varName = mappingParts[0];
                String jsonExpression = mappingParts[1];
                String value = JsonPath.read(output, jsonExpression);
                execution.setVariable(varName, value);
            }

        } catch (Exception e) {
            log.error("Failed to execute POST " + serviceURL.getValue(execution).toString(), e);
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

    public FixedValue getOutputMappings() {
        return outputMappings;
    }

    public void setOutputMappings(FixedValue outputMappings) {
        this.outputMappings = outputMappings;
    }
}
