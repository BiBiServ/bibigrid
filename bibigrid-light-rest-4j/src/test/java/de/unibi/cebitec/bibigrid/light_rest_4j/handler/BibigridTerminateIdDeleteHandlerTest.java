
package de.unibi.cebitec.bibigrid.light_rest_4j.handler;

import com.networknt.client.Http2Client;
import com.networknt.exception.ClientException;
import com.networknt.openapi.ResponseValidator;
import com.networknt.schema.SchemaValidatorsConfig;
import com.networknt.status.Status;
import com.networknt.utility.StringUtils;
import io.undertow.UndertowOptions;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientRequest;
import io.undertow.client.ClientResponse;
import io.undertow.util.HeaderValues;
import io.undertow.util.HttpString;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.IoUtils;
import org.xnio.OptionMap;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

public class BibigridTerminateIdDeleteHandlerTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();

    static final Logger logger = LoggerFactory.getLogger(BibigridTerminateIdDeleteHandlerTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final String JSON_MEDIA_TYPE = "application/json";
    /*
    Expected response body for requests that have a valid json/application body but dont match the openapi specs. This
    response is auto-generated by light-rest-4j
    */
    static final String BAD_REQUEST_BODY = "{\"statusCode\":500,\"code\":\"ERR10010\",\"message\":\"RUNTIME_EXCEPTION\",\"description\":\"Unexpected runtime exception\",\"severity\":\"ERROR\"}";

    /*
    Expected response body for requests that have a valid json/application body and match the openapi specs, but the
    CloudComputing-openrc.sh file wasnt sourced.
    */
    static final String NO_ENV_VARS = "{\"message\":\"Failed to connect openstack client: NullPointerException: null\"}";


    public void sendTestRequest(String requestBody, @Nullable String expectedResponseBody, int expectedStatusCode) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestUri = "/bibigrid/terminate/OvKlUdP";
        String httpMethod = "delete";
        try {
            ClientRequest request = new ClientRequest().setPath(requestUri).setMethod(Methods.DELETE);

            request.getRequestHeaders().put(Headers.CONTENT_TYPE, JSON_MEDIA_TYPE);
            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");

            //customized header parameters
            connection.sendRequest(request, client.createClientCallback(reference, latch, requestBody));

            latch.await();
        } catch (Exception e) {
            logger.error("Exception: ", e);
            throw new ClientException(e);
        } finally {
            IoUtils.safeClose(connection);
        }
        String body = reference.get().getAttachment(Http2Client.RESPONSE_BODY);
        Optional<HeaderValues> contentTypeName = Optional.ofNullable(reference.get().getResponseHeaders().get(Headers.CONTENT_TYPE));
        SchemaValidatorsConfig config = new SchemaValidatorsConfig();
        config.setMissingNodeAsError(true);
        ResponseValidator responseValidator = new ResponseValidator(config);
        int statusCode = reference.get().getResponseCode();
        Status status;
        if(contentTypeName.isPresent()) {
            status = responseValidator.validateResponseContent(body, requestUri, httpMethod, String.valueOf(statusCode), contentTypeName.get().getFirst());
        } else {
            status = responseValidator.validateResponseContent(body, requestUri, httpMethod, String.valueOf(statusCode), JSON_MEDIA_TYPE);
        }
        Assert.assertNull(status);
        Assert.assertEquals(statusCode, expectedStatusCode);
        if(expectedResponseBody != null){
            Assert.assertEquals(body, expectedResponseBody);
        }
    }


    @Test
    public void testBibigridTerminateIdDeleteHandlerTest() throws ClientException {
        try{
            // Valid Request but CloudComputingopenrc.sh file isnt sourced.
            sendTestRequest("{\"mode\":\"openstack\"}", NO_ENV_VARS, 400);
            System.out.println();

            // Request body with valid json syntax but doesnt match the openapi specs.
            sendTestRequest("{\"badBody\":\"badProvider\"}", BAD_REQUEST_BODY, 500);

            // Request with body that has no valid json syntax
            sendTestRequest("{\"\"}", null, 400);
            sendTestRequest("{\"loremipsum\":}", null, 400);
        } catch (ClientException c){
            System.out.println(c.getMessage());
        }
    }
}

