/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.policy.api.main.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.junit.Test;
import org.onap.policy.api.main.PolicyApiException;
import org.onap.policy.api.main.parameters.CommonTestData;
import org.onap.policy.api.main.parameters.RestServerParameters;
import org.onap.policy.api.main.startstop.Main;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.logging.flexlogger.FlexLogger;
import org.onap.policy.common.logging.flexlogger.Logger;

/**
 * Class to perform unit test of HealthCheckMonitor.
 *
 */
public class TestApiRestServer {

    private static final Logger LOGGER = FlexLogger.getLogger(TestApiRestServer.class);
    private static final String NOT_ALIVE = "not alive";
    private static final String ALIVE = "alive";
    private static final String SELF = "self";
    private static final String NAME = "Policy API";

    @Test
    public void testHealthCheckSuccess() throws PolicyApiException, InterruptedException {
        final String reportString = "Report [name=Policy API, url=self, healthy=true, code=200, message=alive]";
        final Main main = startApiService();
        final HealthCheckReport report = performHealthCheck();
        validateReport(NAME, SELF, true, 200, ALIVE, reportString, report);
        stopApiService(main);
    }

    @Test
    public void testHealthCheckFailure() throws InterruptedException {
        final String reportString = "Report [name=Policy API, url=self, healthy=false, code=500, message=not alive]";
        final RestServerParameters restServerParams = new CommonTestData().getRestServerParameters(false);
        restServerParams.setName(CommonTestData.API_GROUP_NAME);
        final ApiRestServer restServer = new ApiRestServer(restServerParams);
        restServer.start();
        final HealthCheckReport report = performHealthCheck();
        validateReport(NAME, SELF, false, 500, NOT_ALIVE, reportString, report);
        assertTrue(restServer.isAlive());
        assertTrue(restServer.toString().startsWith("ApiRestServer [servers="));
        restServer.shutdown();
    }

    private Main startApiService() {
        final String[] apiConfigParameters = { "-c", "parameters/ApiConfigParameters.json" };
        return new Main(apiConfigParameters);
    }

    private void stopApiService(final Main main) throws PolicyApiException {
        main.shutdown();
    }

    private HealthCheckReport performHealthCheck() throws InterruptedException {
        HealthCheckReport response = null;
        final ClientConfig clientConfig = new ClientConfig();

        final HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic("healthcheck", "zb!XztG34");
        clientConfig.register(feature);

        final Client client = ClientBuilder.newClient(clientConfig);
        final WebTarget webTarget = client.target("http://localhost:6969/healthcheck");

        final Invocation.Builder invocationBuilder = webTarget.request(MediaType.APPLICATION_JSON);
        while (response == null) {
            try {
                response = invocationBuilder.get(HealthCheckReport.class);
            } catch (final Exception exp) {
                LOGGER.info("the server is not started yet. We will retry again");
            }
        }
        return response;
    }

    private void validateReport(final String name, final String url, final boolean healthy, final int code,
            final String message, final String reportString, final HealthCheckReport report) {
        assertEquals(name, report.getName());
        assertEquals(url, report.getUrl());
        assertEquals(healthy, report.isHealthy());
        assertEquals(code, report.getCode());
        assertEquals(message, report.getMessage());
        assertEquals(reportString, report.toString());
    }
}
