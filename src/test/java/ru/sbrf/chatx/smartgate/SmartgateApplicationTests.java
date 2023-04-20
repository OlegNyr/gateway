package ru.sbrf.chatx.smartgate;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Request;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.restassured.RestAssured;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;

import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Slf4j
@WireMockTest(httpPort = 9080)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class SmartgateApplicationTests {


    private static WireMock wireMock1;
    private static WireMock wireMock2;

    @Autowired
    private LoadBalancerClientFactory clientFactory;

    @BeforeAll
    static void beforeAll() {

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8080;

        WireMockServer wireMockServer = new WireMockServer(8081);
        wireMockServer.addMockServiceRequestListener(SmartgateApplicationTests::requestReceived);
        wireMockServer.start();
        WireMockServer wireMockServer2 = new WireMockServer(8082);
        wireMockServer2.addMockServiceRequestListener(SmartgateApplicationTests::requestReceived);
        wireMockServer2.start();
        wireMock1 = new WireMock(wireMockServer);
        wireMock2 = new WireMock(wireMockServer2);

    }

    @Test
    void contextLoads() throws InterruptedException {
        WireMock.configureFor(wireMock1);
        stubFor(post(urlMatching("/mypath/message/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "text/plain")
                        .withBody("Hello world!")
                )
        );
        stubFor(get("/health").willReturn(aResponse()
                .withStatus(200)
                .withFixedDelay(5000)
        ));
//        WireMock.configureFor(wireMock2);
//        stubFor(get("/health").willReturn(aResponse()
//                .withStatus(200)
//        ));

        TimeUnit.SECONDS.sleep(10);


            RestAssured
                    .given().log().all()
                    .with()
                    .body("gggg")
                    .post("/message/text")

                    .then().log().body().statusCode(200);

    //        RestAssured
    //                .given().log().all()
    //                .with()
    //                .body("gggg")
    //                .post("/message/text")
    //
    //                .then().log().body().statusCode(200);


    }

    protected static void requestReceived(Request inRequest,
                                          com.github.tomakehurst.wiremock.http.Response inResponse) {
        log.info("WireMock request at URL: {}", inRequest.getAbsoluteUrl());
        log.info("WireMock request headers: \n{}", inRequest.getHeaders());
        log.info("WireMock response body: \n{}", inResponse.getBodyAsString());
        log.info("WireMock response headers: \n{}", inResponse.getHeaders());
    }
}
