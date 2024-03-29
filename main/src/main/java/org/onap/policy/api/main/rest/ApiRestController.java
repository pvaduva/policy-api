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

package org.onap.policy.api.main.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.Authorization;
import io.swagger.annotations.BasicAuthDefinition;
import io.swagger.annotations.Extension;
import io.swagger.annotations.ExtensionProperty;
import io.swagger.annotations.Info;
import io.swagger.annotations.ResponseHeader;
import io.swagger.annotations.SecurityDefinition;
import io.swagger.annotations.SwaggerDefinition;
import java.util.List;
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
import org.apache.commons.lang3.tuple.Pair;
import org.onap.policy.api.main.rest.provider.HealthCheckProvider;
import org.onap.policy.api.main.rest.provider.PolicyProvider;
import org.onap.policy.api.main.rest.provider.PolicyTypeProvider;
import org.onap.policy.api.main.rest.provider.StatisticsProvider;
import org.onap.policy.common.endpoints.event.comm.Topic.CommInfrastructure;
import org.onap.policy.common.endpoints.report.HealthCheckReport;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil;
import org.onap.policy.common.endpoints.utils.NetLoggerUtil.EventType;
import org.onap.policy.common.utils.coder.Coder;
import org.onap.policy.common.utils.coder.CoderException;
import org.onap.policy.common.utils.coder.StandardCoder;
import org.onap.policy.models.base.PfModelException;
import org.onap.policy.models.base.PfModelRuntimeException;
import org.onap.policy.models.tosca.authorative.concepts.ToscaPolicy;
import org.onap.policy.models.tosca.authorative.concepts.ToscaServiceTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class to provide REST API services.
 *
 * @author Chenfei Gao (cgao@research.att.com)
 */
@Path("/policy/api/v1")
@Api(value = "Policy Design API")
@Produces({"application/json", "application/yaml"})
@Consumes({"application/json", "application/yaml"})
@SwaggerDefinition(info = @Info(
        description = "Policy Design API is publicly exposed for clients to Create/Read/Update/Delete"
                    + " policy types, policy type implementation and policies which can be recognized"
                    + " and executable by incorporated policy engines. It is an"
                    + " independent component running rest service that takes all policy design API calls"
                    + " from clients and then assign them to different API working functions. Besides"
                    + " that, API is also exposed for clients to retrieve healthcheck status of this API"
                    + " rest service and the statistics report including the counters of API invocation.",
        version = "1.0.0",
        title = "Policy Design",
        extensions = {
                @Extension(properties = {
                        @ExtensionProperty(name = "planned-retirement-date", value = "tbd"),
                        @ExtensionProperty(name = "component", value = "Policy Framework")
                })
        }),
        schemes = { SwaggerDefinition.Scheme.HTTP, SwaggerDefinition.Scheme.HTTPS },
        securityDefinition = @SecurityDefinition(basicAuthDefinitions = { @BasicAuthDefinition(key = "basicAuth") }))
public class ApiRestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiRestController.class);

    private final Coder coder = new StandardCoder();

    /**
     * Retrieves the healthcheck status of the API component.
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/healthcheck")
    @ApiOperation(value = "Perform a system healthcheck",
            notes = "Returns healthy status of the Policy API component",
            response = HealthCheckReport.class,
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
            tags = { "HealthCheck", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getHealthCheck(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        updateApiStatisticsCounter(Target.OTHER, Result.SUCCESS, HttpMethod.GET);
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
            .entity(new HealthCheckProvider().performHealthCheck()).build();
    }

    /**
     * Retrieves the statistics report of the API component.
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/statistics")
    @ApiOperation(value = "Retrieve current statistics",
            notes = "Returns current statistics including the counters of API invocation",
            response = StatisticsReport.class,
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
            tags = { "Statistics", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getStatistics(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        updateApiStatisticsCounter(Target.OTHER, Result.SUCCESS, HttpMethod.GET);
        return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
            .entity(new StatisticsProvider().fetchCurrentStatistics()).build();
    }

    /**
     * Retrieves all available policy types.
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes")
    @ApiOperation(value = "Retrieve existing policy types",
            notes = "Returns a list of existing policy types stored in Policy Framework",
            response = ToscaServiceTemplate.class,
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
            tags = { "PolicyType", },
            extensions = {
                    @Extension(name = "interface info", properties = {
                            @ExtensionProperty(name = "api-version", value = "1.0.0"),
                            @ExtensionProperty(name = "last-mod-release", value = "Dublin")
                    })
            })
    @ApiResponses(value = {
            @ApiResponse(code = 401, message = "Authentication Error"),
            @ApiResponse(code = 403, message = "Authorization Error"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response getAllPolicyTypes(
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.fetchPolicyTypes(null, null);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes", pfme);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves all versions of a particular policy type.
     *
     * @param policyTypeId the ID of specified policy type
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}")
    @ApiOperation(value = "Retrieve all available versions of a policy type",
            notes = "Returns a list of all available versions for the specified policy type",
            response = ToscaServiceTemplate.class,
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
            tags = { "PolicyType", },
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
    public Response getAllVersionsOfPolicyType(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.fetchPolicyTypes(policyTypeId, null);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}", policyTypeId, pfme);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves specified version of a particular policy type.
     *
     * @param policyTypeId the ID of specified policy type
     * @param versionId the version of specified policy type
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{versionId}")
    @ApiOperation(value = "Retrieve one particular version of a policy type",
            notes = "Returns a particular version for the specified policy type",
            response = ToscaServiceTemplate.class,
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
            tags = { "PolicyType", },
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
    public Response getSpecificVersionOfPolicyType(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("versionId") @ApiParam(value = "Version of policy type", required = true) String versionId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.fetchPolicyTypes(policyTypeId, versionId);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/{}", policyTypeId, versionId, pfme);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves latest version of a particular policy type.
     *
     * @param policyTypeId the ID of specified policy type
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/latest")
    @ApiOperation(value = "Retrieve latest version of a policy type",
            notes = "Returns latest version for the specified policy type",
            response = ToscaServiceTemplate.class,
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
            tags = { "PolicyType", },
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
    public Response getLatestVersionOfPolicyType(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.fetchLatestPolicyTypes(policyTypeId);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/latest", policyTypeId, pfme);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Creates a new policy type.
     *
     * @param body the body of policy type following TOSCA definition
     *
     * @return the Response object containing the results of the API operation
     */
    @POST
    @Path("/policytypes")
    @ApiOperation(value = "Create a new policy type",
            notes = "Client should provide TOSCA body of the new policy type",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "PolicyType", },
            response = ToscaServiceTemplate.class,
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
    public Response createPolicyType(
            @ApiParam(value = "Entity body of policy type", required = true) ToscaServiceTemplate body,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        if (NetLoggerUtil.getNetworkLogger().isInfoEnabled()) {
            NetLoggerUtil.log(EventType.IN, CommInfrastructure.REST, "/policytypes", toJson(body));
        }

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.createPolicyType(body);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.SUCCESS, HttpMethod.POST);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("POST /policytypes", pfme);
            updateApiStatisticsCounter(Target.POLICY_TYPE, Result.FAILURE, HttpMethod.POST);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Deletes specified version of a particular policy type.
     *
     * @param policyTypeId the ID of specified policy type
     * @param versionId the version of specified policy type
     *
     * @return the Response object containing the results of the API operation
     */
    @DELETE
    @Path("/policytypes/{policyTypeId}/versions/{versionId}")
    @ApiOperation(value = "Delete one version of a policy type",
            notes = "Rule 1: pre-defined policy types cannot be deleted;"
                  + "Rule 2: policy types that are in use (parameterized by a TOSCA policy) cannot be deleted."
                  + "The parameterizing TOSCA policies must be deleted first;",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "PolicyType", },
            response = ToscaServiceTemplate.class,
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
    public Response deleteSpecificVersionOfPolicyType(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("versionId") @ApiParam(value = "Version of policy type", required = true) String versionId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyTypeProvider policyTypeProvider = new PolicyTypeProvider()) {
            ToscaServiceTemplate serviceTemplate = policyTypeProvider.deletePolicyType(policyTypeId, versionId);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("DELETE /policytypes/{}/versions/{}", policyTypeId, versionId, pfme);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves all versions of a particular policy.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies")
    @ApiOperation(value = "Retrieve all versions of a policy created for a particular policy type version",
            notes = "Returns a list of all versions of specified policy created for the specified policy type version",
            response = ToscaServiceTemplate.class,
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
            tags = { "Policy", },
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
    public Response getAllPolicies(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate =
                    policyProvider.fetchPolicies(policyTypeId, policyTypeVersion, null, null);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/{}/policies", policyTypeId, policyTypeVersion, pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves all versions of a particular policy.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param policyId the ID of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies/{policyId}")
    @ApiOperation(value = "Retrieve all version details of a policy created for a particular policy type version",
            notes = "Returns a list of all version details of the specified policy",
            response = ToscaServiceTemplate.class,
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
            tags = { "Policy", },
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
    public Response getAllVersionsOfPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate = policyProvider
                    .fetchPolicies(policyTypeId, policyTypeVersion, policyId, null);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("/policytypes/{}/versions/{}/policies/{}", policyTypeId, policyTypeVersion, policyId,
                    pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves the specified version of a particular policy.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Retrieve one version of a policy created for a particular policy type version",
            notes = "Returns a particular version of specified policy created for the specified policy type version",
            response = ToscaServiceTemplate.class,
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
            tags = { "Policy", },
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
    public Response getSpecificVersionOfPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate = policyProvider
                    .fetchPolicies(policyTypeId, policyTypeVersion, policyId, policyVersion);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/{}/policies/{}/versions/{}", policyTypeId,
                    policyTypeVersion, policyId, policyVersion, pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves the latest version of a particular policy.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param policyId the ID of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies/{policyId}/versions/latest")
    @ApiOperation(value = "Retrieve the latest version of a particular policy",
            notes = "Returns the latest version of specified policy",
            response = ToscaServiceTemplate.class,
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
            tags = { "Policy", },
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
    public Response getLatestVersionOfPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate =
                    policyProvider.fetchLatestPolicies(policyTypeId, policyTypeVersion, policyId);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/{}/policies/{}/versions/latest", policyTypeId,
                    policyTypeVersion, policyId, pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Retrieves deployed versions of a particular policy in pdp groups.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param policyId the ID of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @GET
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies/{policyId}/versions/deployed")
    @ApiOperation(value = "Retrieve deployed versions of a particular policy in pdp groups",
            notes = "Returns deployed versions of specified policy in pdp groups",
            response = ToscaPolicy.class, responseContainer = "List",
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
            tags = { "Policy", },
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
    public Response getDeployedVersionsOfPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            Map<Pair<String, String>, List<ToscaPolicy>> deployedPolicies = policyProvider
                    .fetchDeployedPolicies(policyTypeId, policyTypeVersion, policyId);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(deployedPolicies).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("GET /policytypes/{}/versions/{}/policies/{}/versions/deployed", policyTypeId,
                    policyTypeVersion, policyId, pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.GET);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Creates a new policy for a particular policy type and version.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param body the body of policy following TOSCA definition
     *
     * @return the Response object containing the results of the API operation
     */
    @POST
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies")
    @ApiOperation(value = "Create a new policy for a policy type version",
            notes = "Client should provide TOSCA body of the new policy",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Policy", },
            response = ToscaServiceTemplate.class,
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
            @ApiResponse(code = 404, message = "Resource Not Found"),
            @ApiResponse(code = 500, message = "Internal Server Error")
        })
    public Response createPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "ID of policy type", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId,
            @ApiParam(value = "Entity body of policy", required = true) ToscaServiceTemplate body) {

        if (NetLoggerUtil.getNetworkLogger().isInfoEnabled()) {
            NetLoggerUtil.log(EventType.IN, CommInfrastructure.REST,
                            "/policytypes/" + policyTypeId + "/versions/" + policyTypeVersion + "/policies",
                            toJson(body));
        }

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate = policyProvider
                    .createPolicy(policyTypeId, policyTypeVersion, body);
            updateApiStatisticsCounter(Target.POLICY, Result.SUCCESS, HttpMethod.POST);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("POST /policytypes/{}/versions/{}/policies", policyTypeId, policyTypeVersion, pfme);
            updateApiStatisticsCounter(Target.POLICY, Result.FAILURE, HttpMethod.POST);
            return addLoggingHeaders(addVersionControlHeaders(
                    Response.status(pfme.getErrorResponse().getResponseCode())), requestId)
                    .entity(pfme.getErrorResponse()).build();
        }
    }

    /**
     * Deletes the specified version of a particular policy.
     *
     * @param policyTypeId the ID of specified policy type
     * @param policyTypeVersion the version of specified policy type
     * @param policyId the ID of specified policy
     * @param policyVersion the version of specified policy
     *
     * @return the Response object containing the results of the API operation
     */
    @DELETE
    @Path("/policytypes/{policyTypeId}/versions/{policyTypeVersion}/policies/{policyId}/versions/{policyVersion}")
    @ApiOperation(value = "Delete a particular version of a policy",
            notes = "Rule: the version that has been deployed in PDP group(s) cannot be deleted",
            authorizations = @Authorization(value = "basicAuth"),
            tags = { "Policy", },
            response = ToscaServiceTemplate.class,
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
    public Response deleteSpecificVersionOfPolicy(
            @PathParam("policyTypeId") @ApiParam(value = "PolicyType ID", required = true) String policyTypeId,
            @PathParam("policyTypeVersion")
                @ApiParam(value = "Version of policy type", required = true) String policyTypeVersion,
            @PathParam("policyId") @ApiParam(value = "ID of policy", required = true) String policyId,
            @PathParam("policyVersion") @ApiParam(value = "Version of policy", required = true) String policyVersion,
            @HeaderParam("X-ONAP-RequestID") @ApiParam("RequestID for http transaction") UUID requestId) {

        try (PolicyProvider policyProvider = new PolicyProvider()) {
            ToscaServiceTemplate serviceTemplate = policyProvider
                    .deletePolicy(policyTypeId, policyTypeVersion, policyId, policyVersion);
            return addLoggingHeaders(addVersionControlHeaders(Response.status(Response.Status.OK)), requestId)
                    .entity(serviceTemplate).build();
        } catch (PfModelException | PfModelRuntimeException pfme) {
            LOGGER.error("DELETE /policytypes/{}/versions/{}/policies/{}/versions/{}", policyTypeId,
                    policyTypeVersion, policyId, policyVersion, pfme);
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

    private enum Target {
        POLICY, POLICY_TYPE, OTHER
    }

    private enum Result {
        SUCCESS, FAILURE
    }

    private enum HttpMethod {
        POST, GET
    }

    private void updateApiStatisticsCounter(Target target, Result result, HttpMethod http) {

        ApiStatisticsManager.updateTotalApiCallCount();
        if (target == Target.POLICY) {
            if (result == Result.SUCCESS) {
                if (http == HttpMethod.GET) {
                    ApiStatisticsManager.updateApiCallSuccessCount();
                    ApiStatisticsManager.updateTotalPolicyGetCount();
                    ApiStatisticsManager.updatePolicyGetSuccessCount();
                } else if (http == HttpMethod.POST) {
                    ApiStatisticsManager.updateApiCallSuccessCount();
                    ApiStatisticsManager.updateTotalPolicyPostCount();
                    ApiStatisticsManager.updatePolicyPostSuccessCount();
                }
            } else {
                if (http == HttpMethod.GET) {
                    ApiStatisticsManager.updateApiCallFailureCount();
                    ApiStatisticsManager.updateTotalPolicyGetCount();
                    ApiStatisticsManager.updatePolicyGetFailureCount();
                } else {
                    ApiStatisticsManager.updateApiCallFailureCount();
                    ApiStatisticsManager.updateTotalPolicyPostCount();
                    ApiStatisticsManager.updatePolicyPostFailureCount();
                }
            }
        } else if (target == Target.POLICY_TYPE) {
            if (result == Result.SUCCESS) {
                if (http == HttpMethod.GET) {
                    ApiStatisticsManager.updateApiCallSuccessCount();
                    ApiStatisticsManager.updateTotalPolicyTypeGetCount();
                    ApiStatisticsManager.updatePolicyTypeGetSuccessCount();
                } else if (http == HttpMethod.POST) {
                    ApiStatisticsManager.updateApiCallSuccessCount();
                    ApiStatisticsManager.updatePolicyTypePostSuccessCount();
                    ApiStatisticsManager.updatePolicyTypePostSuccessCount();
                }
            } else {
                if (http == HttpMethod.GET) {
                    ApiStatisticsManager.updateApiCallFailureCount();
                    ApiStatisticsManager.updateTotalPolicyTypeGetCount();
                    ApiStatisticsManager.updatePolicyTypeGetFailureCount();
                } else {
                    ApiStatisticsManager.updateApiCallFailureCount();
                    ApiStatisticsManager.updateTotalPolicyTypePostCount();
                    ApiStatisticsManager.updatePolicyTypePostFailureCount();
                }
            }
        } else {
            ApiStatisticsManager.updateApiCallSuccessCount();
        }
    }
}