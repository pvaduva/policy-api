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

package org.onap.policy.api.main.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.ResponseHeader;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.onap.policy.api.main.rest.provider.LegacyGuardPolicyProvider;
import org.onap.policy.api.main.rest.provider.LegacyOperationalPolicyProvider;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil.EventType;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.legacy.concepts.LegacyGuardPolicyInput;
import org.onap.policy.models.tosca.legacy.concepts.LegacyGuardPolicyOutput;
import org.onap.policy.models.tosca.legacy.concepts.LegacyOperationalPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide legacy REST API services.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Path("/policy/api/v1")
@Api(value = "Legacy Policy Design API")
@Produces("application/json")
@Consumes("application/json")
public class LegacyApiRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(LegacyApiRestController.class);

    private final Coder coder = new StandardCoder();

    /**
     * Retrieves the latest version of a particular guard policy.
     *
     * @param policyId the ID of specified guard policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{policyId}/versions/latest")
    @ApiOperation(value = "Retrieve the latest version of a particular guard policy",
            notes = "Returns the latest version of the specified guard policy",
            response = LegacyGuardPolicyOutput.class, responseContainer = "Map",
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Guard Policy", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getLatestVersionOfGuardPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyGuardPolicyProvider guardPolicyProvider = new LegacyGuardPolicyProvider()) {
            Map<String, LegacyGuardPolicyOutput> policies = guardPolicyProvider.fetchGuardPolicy(policyId, null);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policies).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{}"
                + "/versions/latest", policyId, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves the specified version of a particular guard policy.
     *
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Retrieve one version of a particular guard policy",
            notes = "Returns a particular version of a specified guard policy",
            response = LegacyGuardPolicyOutput.class, responseContainer = "Map",
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Guard Policy", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getSpecificVersionOfGuardPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyGuardPolicyProvider guardPolicyProvider = new LegacyGuardPolicyProvider()) {
            Map<String, LegacyGuardPolicyOutput> policies = guardPolicyProvider
                    .fetchGuardPolicy(policyId, policyVersion);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policies).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{}/versions/{}",
                    policyId, policyVersion, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Creates a new guard policy.
     *
     * @param body the body of policy
     *
     * @return the Response object containing the results of the API operation
     */
    @POST
    @Path("/policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies")
    @ApiOperation(value = "Create a new guard policy",
            notes = "Client should provide entity body of the new guard policy",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Guard Policy", },
            response = LegacyGuardPolicyOutput.class, responseContainer = "Map",
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid Body"),
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response createGuardPolicy(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId,
            @ApiParam(value = "Entity body of policy", required = true) LegacyGuardPolicyInput body) {

        if (NetLoggerUtil.getNetworkLogger().isInfoEnabled()) {
            NetLoggerUtil.log(EventType.IN, CommInfrastructure.REST,
                            "/policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies", toJson(body));
        }

        try (LegacyGuardPolicyProvider guardPolicyProvider = new LegacyGuardPolicyProvider()) {
            Map<String, LegacyGuardPolicyOutput> policy = guardPolicyProvider.createGuardPolicy(body);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policy).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("POST /policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies", pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Deletes the specified version of a particular guard policy.
     *
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @DELETE
    @Path("/policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Delete a particular version of a guard policy",
            notes = "Rule: the version that has been deployed in PDP group(s) cannot be deleted",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Guard Policy", },
            response = LegacyGuardPolicyOutput.class, responseContainer = "Map",
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 409, message = "Delete Conflict, Rule Violation"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response deleteSpecificVersionOfGuardPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyGuardPolicyProvider guardPolicyProvider = new LegacyGuardPolicyProvider()) {
            Map<String, LegacyGuardPolicyOutput> policies = guardPolicyProvider
                    .deleteGuardPolicy(policyId, policyVersion);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policies).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("DELETE /policytypes/onap.policies.controlloop.Guard/versions/1.0.0/policies/{}/versions/{}",
                    policyId, policyVersion, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves the latest version of a particular operational policy.
     *
     * @param policyId the ID of specified operational policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/onap.policies.controlloop.Operational/versions/1.0.0/policies/{policyId}/versions/latest")
    @ApiOperation(value = "Retrieve the latest version of a particular operational policy",
            notes = "Returns the latest version of the specified operational policy",
            response = LegacyOperationalPolicy.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Operational Policy", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getLatestVersionOfOperationalPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyOperationalPolicyProvider operationalPolicyProvider = new LegacyOperationalPolicyProvider()) {
            LegacyOperationalPolicy policy = operationalPolicyProvider.fetchOperationalPolicy(policyId, null);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policy).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/onap.policies.controlloop.Operational/versions/1.0.0/policies/{}"
                + "/versions/latest", policyId, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves the specified version of a particular operational policy.
     *
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/onap.policies.controlloop.Operational/versions/1.0.0/"
         + "policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Retrieve one version of a particular operational policy",
            notes = "Returns a particular version of a specified operational policy",
            response = LegacyOperationalPolicy.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Operational Policy", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getSpecificVersionOfOperationalPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyOperationalPolicyProvider operationalPolicyProvider = new LegacyOperationalPolicyProvider()) {
            LegacyOperationalPolicy policy = operationalPolicyProvider.fetchOperationalPolicy(policyId, policyVersion);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policy).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/onap.policies.controlloop.Operational/versions/1.0.0/"
                + "policies/{}/versions/{}", policyId, policyVersion, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Creates a new operational policy.
     *
     * @param body the body of policy
     *
     * @return the Response object containing the results of the API operation
     */
    @POST
    @Path("/policytypes/onap.policies.controlloop.Operational/versions/1.0.0/policies")
    @ApiOperation(value = "Create a new operational policy",
            notes = "Client should provide entity body of the new operational policy",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Operational Policy", },
            response = LegacyOperationalPolicy.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid Body"),
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response createOperationalPolicy(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId,
            @ApiParam(value = "Entity body of policy", required = true) LegacyOperationalPolicy body) {

        if (NetLoggerUtil.getNetworkLogger().isInfoEnabled()) {
            NetLoggerUtil.log(EventType.IN, CommInfrastructure.REST,
                            "/policytypes/onap.policies.controlloop.Operational/versions/1.0.0/policies", toJson(body));
        }

        try (LegacyOperationalPolicyProvider operationalPolicyProvider = new LegacyOperationalPolicyProvider()) {
            LegacyOperationalPolicy policy = operationalPolicyProvider.createOperationalPolicy(body);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policy).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("POST /policytypes/onap.policies.controlloop.Operational/versions/1.0.0/policies", pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Deletes the specified version of a particular operational policy.
     *
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @DELETE
    @Path("/policytypes/onap.policies.controlloop.Operational/versions/1.0.0/"
         + "policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Delete a particular version of a specified operational policy",
            notes = "Rule: the version that has been deployed in PDP group(s) cannot be deleted",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Legacy Operational Policy", },
            response = LegacyOperationalPolicy.class,
            responseHeaders = {
                    @ResponseHeader(name = "X-MinorVersion",
                                    description = "Used to request or communicate a MINOR version back from the client"
                                                + " to the server, and from the server back to the client",
                                    response = String.class),
                    @ResponseHeader(name = "X-PatchVersion",
                                    description = "Used only to communicate a PATCH version in a response for"
                                                + " troubleshooting purposes only, and will not be provided by"
                                                + " the client on request",
                                    response = String.class),
                    @ResponseHeader(name = "X-LatestVersion",
                                    description = "Used only to communicate an API's latest version",
                                    response = String.class),
                    @ResponseHeader(name = "X-ONAP-RequestID",
                                    description = "Used to track REST transactions for logging purpose",
                                    response = UUID.class)
            },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 409, message = "Delete Conflict, Rule Violation"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response deleteSpecificVersionOfOperationalPolicy(
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (LegacyOperationalPolicyProvider operationalPolicyProvider = new LegacyOperationalPolicyProvider()) {
            LegacyOperationalPolicy policy = operationalPolicyProvider
                    .deleteOperationalPolicy(policyId, policyVersion);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(policy).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("DELETE /policytypes/onap.policies.controlloop.Operational/versions/1.0.0/"
                + "policies/{}/versions/{}", policyId, policyVersion, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    private ResponseBuilder addVersionControlHeaders(ResponseBuilder rb) {
        return rb.header("X-MinorVersion", "0").header("X-PatchVersion", "0").header("X-LatestVersion", "1.0.0");
    }

    private ResponseBuilder addLoggingHeaders(ResponseBuilder rb, UUID requestId) {
        if (requestId == null) {
            // Generate a random uuid if client does not embed requestId in rest request
            return rb.header("X-ONAP-RequestID", UUID.randomUUID());
        }
        return rb.header("X-ONAP-RequestID", requestId);
    }

    /**
     * Converts an object to a JSON string.
     *
     * @param object object to convert
     * @return a JSON string representing the object
     */
    private String toJson(Object object) {
        if (object == null) {
            return null;
        }

        try {
            return coder.encode(object);

        } catch (CoderException e) {
            LOGGER.warn("cannot convert {} to JSON", object.getClass().getName(), e);
            return null;
        }
    }
}