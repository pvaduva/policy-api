/*-
 * ============LICENSE_START=======================================================
 * ONAP Policy API
 * ================================================================================
 * Copyright (C) 2018 Samsung Electronics Co., Ltd. All rights reserved.
 * Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
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

package org.onap.policy.api.main.parameters;

import java.io.File;
import java.io.IOException;
import org.onap.policy.common.utils.resources.TextFileUtils;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;

/**
 * Class to hold/create all parameters for test cases.
 *
 */
public class CommonTestData {

    public static final String API_GROUP_NAME = "ApiGroup";

    /**
     * Server port, as it appears within the config files.
     */
    private static final String REST_SERVER_PORT = "6969";

    private static final String REST_SERVER_PASSWORD = "zb!XztG34";
    private static final String REST_SERVER_USER = "healthcheck";
    private static final String REST_SERVER_HOST = "0.0.0.0";
    private static final boolean REST_SERVER_HTTPS = false;
    private static final boolean REST_SERVER_AAF = false;

    private static final String PROVIDER_GROUP_NAME = "PolicyProviderParameterGroup";
    private static final String PROVIDER_IMPL = "org.onap.policy.models.provider.impl.DatabasePolicyModelsProviderImpl";
    private static final String DATABASE_DRIVER = "org.h2.Driver";
    private static final String DATABASE_URL = "jdbc:h2:mem:testdb";
    private static final String DATABASE_USER = "policy";
    private static final String DATABASE_PASSWORD = "P01icY";
    private static final String PERSISTENCE_UNIT = "ToscaConceptTest";

    /**
     * Returns an instance of RestServerParameters for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @param port server port
     * @return the RestServerParameters object
     */
    public RestServerParameters getRestServerParameters(final boolean isEmpty, int port) {
        final RestServerParameters restServerParameters;
        if (!isEmpty) {
            restServerParameters = new RestServerParameters(REST_SERVER_HOST, port, REST_SERVER_USER,
                    REST_SERVER_PASSWORD, REST_SERVER_HTTPS, REST_SERVER_AAF);
        } else {
            restServerParameters = new RestServerParameters(null, 0, null, null, false, false);
        }
        return restServerParameters;
    }

    /**
     * Returns an instance of PolicyModelsProviderParameters for test cases.
     *
     * @param isEmpty boolean value to represent that object created should be empty or not
     * @return the PolicyModelsProviderParameters object
     */
    public PolicyModelsProviderParameters getDatabaseProviderParameters(final boolean isEmpty) {
        final PolicyModelsProviderParameters databaseProviderParameters;
        if (!isEmpty) {
            databaseProviderParameters = new PolicyModelsProviderParameters();
            databaseProviderParameters.setName(PROVIDER_GROUP_NAME);
            databaseProviderParameters.setImplementation(PROVIDER_IMPL);
            databaseProviderParameters.setDatabaseDriver(DATABASE_DRIVER);
            databaseProviderParameters.setDatabaseUrl(DATABASE_URL);
            databaseProviderParameters.setDatabaseUser(DATABASE_USER);
            databaseProviderParameters.setDatabasePassword(DATABASE_PASSWORD);
            databaseProviderParameters.setPersistenceUnit(PERSISTENCE_UNIT);
        } else {
            databaseProviderParameters = new PolicyModelsProviderParameters();
        }
        return databaseProviderParameters;
    }

    /**
     * Copies a source file to a target file, replacing occurrances of
     * {@link #REST_SERVER_PORT} with the given port number.
     *
     * @param source source file name
     * @param target target file name
     * @param port port to be substituted
     * @throws IOException if an error occurs
     */
    public void makeParameters(String source, String target, int port) throws IOException {
        String text = TextFileUtils.getTextFileAsString(source);
        text = text.replace(REST_SERVER_PORT, String.valueOf(port));

        File file = new File(target);
        file.deleteOnExit();
        TextFileUtils.putStringAsFile(text, file);
    }
}
