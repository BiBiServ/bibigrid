
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
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

@Ignore
public class BibigridListGetHandlerTest {
    @ClassRule
    public static TestServer server = TestServer.getInstance();
    static final Logger logger = LoggerFactory.getLogger(BibigridListGetHandlerTest.class);
    static final boolean enableHttp2 = server.getServerConfig().isEnableHttp2();
    static final boolean enableHttps = server.getServerConfig().isEnableHttps();
    static final int httpPort = server.getServerConfig().getHttpPort();
    static final int httpsPort = server.getServerConfig().getHttpsPort();
    static final String url = enableHttp2 || enableHttps ? "https://localhost:" + httpsPort : "http://localhost:" + httpPort;
    static final String JSON_MEDIA_TYPE = "application/json";
    static final String NO_REQUEST_BODY = "{\"statusCode\":400,\"code\":\"ERR11014\",\"message\":\"VALIDATOR_REQUEST_BODY_MISSING\",\"description\":\"Method get on path /bibigrid/list requires a request body. None found.\",\"severity\":\"ERROR\"}";
    static final String BAD_REQUEST_BODY = "{\"statusCode\":500,\"code\":\"ERR10010\",\"message\":\"RUNTIME_EXCEPTION\",\"description\":\"Unexpected runtime exception\",\"severity\":\"ERROR\"}";
    static final String NO_ENV_VARS = "{\"error\":\"Failed to connect openstack client: NullPointerException: null\"}";

    public void sendTestRequest(String requestBody, String expectedResponseBody, int expectedStatusCode) throws ClientException {
        final Http2Client client = Http2Client.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        final ClientConnection connection;
        try {
            connection = client.connect(new URI(url), Http2Client.WORKER, Http2Client.SSL, Http2Client.BUFFER_POOL, enableHttp2 ? OptionMap.create(UndertowOptions.ENABLE_HTTP2, true): OptionMap.EMPTY).get();
        } catch (Exception e) {
            throw new ClientException(e);
        }
        final AtomicReference<ClientResponse> reference = new AtomicReference<>();
        String requestUri = "/bibigrid/list";
        String httpMethod = "get";
        try {
            ClientRequest request = new ClientRequest().setPath(requestUri).setMethod(Methods.GET);

            request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
            request.getRequestHeaders().put(Headers.CONTENT_TYPE, "application/json");
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
        Assert.assertEquals(statusCode,expectedStatusCode);
        Assert.assertEquals(body, expectedResponseBody);
    }

    @Test
    public void testBibigridListGetHandlerTest() {
        try{
            // Test rejection of invalid requests
            sendTestRequest("{\"mode\":\"openstack\"}", NO_ENV_VARS, 400);
            sendTestRequest("{\"badBody\":\"badProvider\"}", BAD_REQUEST_BODY, 500);
        } catch (ClientException c){
            System.out.println(c.getMessage());
        }
    }
}

