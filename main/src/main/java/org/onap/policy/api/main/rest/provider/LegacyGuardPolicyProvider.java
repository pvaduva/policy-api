/*-
 * ============LICENSE_START=======================================================
 * ONAP Policy API
 * ================================================================================
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

package org.onap.policy.api.main.rest.provider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.onap.policy.api.main.parameters.ApiParameterGroup;
import org.onap.policy.common.parameters.ParameterService;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.pdp.concepts.PdpGroup;
import org.onap.policy.models.pdp.concepts.PdpGroupFilter;
import org.onap.policy.models.provider.PolicyModelsProvider;
import org.onap.policy.models.provider.PolicyModelsProviderFactory;
import org.onap.policy.models.provider.PolicyModelsProviderParameters;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicyIdentifier;
import org.onap.policy.models.tosca.legacy.concepts.LegacyGuardPolicyInput;
import org.onap.policy.models.tosca.legacy.concepts.LegacyGuardPolicyOutput;

/**
 * Class to provide all kinds of legacy guard policy operations.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
public class LegacyGuardPolicyProvider implements AutoCloseable {

    private PolicyModelsProvider modelsProvider;

    /**
     * Default constructor.
     */
    public LegacyGuardPolicyProvider() throws PfModelException {

        ApiParameterGroup parameterGroup = ParameterService.get("ApiGroup");
        PolicyModelsProviderParameters providerParameters = parameterGroup.getDatabaseProviderParameters();
        modelsProvider = new PolicyModelsProviderFactory().createPolicyModelsProvider(providerParameters);
    }

    /**
     * Retrieves a list of guard policies matching specified ID and version.
     *
     * @param policyId the ID of policy
     * @param policyVersion the version of policy
     *
     * @return the map of LegacyGuardPolicyOutput objects
     */
    public Map<String, LegacyGuardPolicyOutput> fetchGuardPolicy(String policyId, String policyVersion)
            throws PfModelException {

        if (policyVersion != null) {
            validateLegacyGuardPolicyVersion(policyVersion);
        }
        return modelsProvider.getGuardPolicy(policyId, policyVersion);
    }

    /**
     * Creates a new guard policy.
     *
     * @param body the entity body of policy
     *
     * @return the map of LegacyGuardPolicyOutput objectst
     */
    public Map<String, LegacyGuardPolicyOutput> createGuardPolicy(LegacyGuardPolicyInput body)
            throws PfModelException {

        return modelsProvider.createGuardPolicy(body);
    }

    /**
     * Deletes the guard policies matching specified ID and version.
     *
     * @param policyId the ID of policy
     * @param policyVersion the version of policy
     *
     * @return the map of LegacyGuardPolicyOutput objects
     */
    public Map<String, LegacyGuardPolicyOutput> deleteGuardPolicy(String policyId, String policyVersion)
            throws PfModelException {

        validateLegacyGuardPolicyVersion(policyVersion);
        validateDeleteEligibility(policyId, policyVersion);

        return modelsProvider.deleteGuardPolicy(policyId, policyVersion);
    }

    /**
     * Validates whether specified policy can be deleted based on the rule that deployed policy cannot be deleted.
     *
     * @param policyId the ID of policy
     * @param policyVersion the version of policy
     *
     * @throws PfModelException the PfModel parsing exception
     */
    private void validateDeleteEligibility(String policyId, String policyVersion) throws PfModelException {

        List<ToscaPolicyIdentifier> policies = new ArrayList<>();
        policies.add(new ToscaPolicyIdentifier(policyId, policyVersion));
        PdpGroupFilter pdpGroupFilter = PdpGroupFilter.builder().policyList(policies).build();

        List<PdpGroup> pdpGroups = modelsProvider.getFilteredPdpGroups(pdpGroupFilter);

        if (!pdpGroups.isEmpty()) {
            throw new PfModelException(Response.Status.CONFLICT,
                    constructDeleteRuleViolationMessage(policyId, policyVersion, pdpGroups));
        }
    }

    /**
     * Validates whether the legacy guard policy version is an integer.
     *
     * @param policyVersion the version of policy
     *
     * @throws PfModelException the PfModel parsing exception
     */
    private void validateLegacyGuardPolicyVersion(String policyVersion) throws PfModelException {

        try {
            Integer.valueOf(policyVersion);
        } catch (NumberFormatException exc) {
            throw new PfModelException(Response.Status.BAD_REQUEST,
                    "legacy policy version is not an integer", exc);
        }
    }

    /**
     * Constructs returned message for policy delete rule violation.
     *
     * @param policyId the ID of policy
     * @param policyVersion the version of policy
     * @param pdpGroups the list of pdp groups
     *
     * @return the constructed message
     */
    private String constructDeleteRuleViolationMessage(
            String policyId, String policyVersion, List<PdpGroup> pdpGroups) {

        List<String> pdpGroupNameVersionList = new ArrayList<>();
        for (PdpGroup pdpGroup : pdpGroups) {
            pdpGroupNameVersionList.add(pdpGroup.getName() + ":" + pdpGroup.getVersion());
        }
        String deployedPdpGroups = String.join(",", pdpGroupNameVersionList);
        return "policy with ID " + policyId + ":" + policyVersion
                + " cannot be deleted as it is deployed in pdp groups " + deployedPdpGroups;
    }

    /**
     * Closes the connection to database.
     *
     * @throws PfModelException the PfModel parsing exception
     */
    @Override
    public void close() throws PfModelException {

        modelsProvider.close();
    }
}