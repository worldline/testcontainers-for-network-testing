package com.worldline.network.examples;

import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.worldline.network.examples.TestContainersHelper.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

//@Execution(ExecutionMode.CONCURRENT)
public class BaselineTests extends DockerTestBase
{
    @Test
    @DisplayName("ensure the containers are properly setup and can netcat from client to server")
    void testContainers() throws IOException, InterruptedException
    {
        // setup
        var testMessage = "send this message with rock n roll";
        var serverName = "test-server";
        var clientName = "test-client";

        try (
            var testNetwork = createKNettyTestNetwork().get();
            var simpleServer = createTestContainer("simpleserver", serverName, testNetwork).get();
            var simpleClient = createTestContainer("simpleclient", clientName, testNetwork).get()
        ) {
            // specialize containers as needed
            simpleClient.withCommand(serverName, "8000", testMessage);

            // act
            simpleServer.start();
            simpleClient.start();

            // verify
            awaitCompletion(simpleServer.getContainerId());

            var reader = new BufferedReader(readFileFromContainer(simpleServer.getContainerId(), "/opt/receiving"));
            assertEquals(testMessage, reader.readLine(), "reading the first line from /opt/receiving to compare to data send");
        }
    }


    @Test
    @DisplayName("ensure the containers can communicate via a proxy on localhost")
    void testContainersViaLocalHost() throws IOException, InterruptedException
    {
        // setup
        var testMessage = "send this message with rock n roll";
        var serverName = "test-server";
        var clientName = "test-client";

        try (
           var testNetwork = createKNettyTestNetwork().get();
           var proxy = new SimpleNettyForwardProxy();
           var simpleServer = createTestContainer("simpleserver", serverName, testNetwork).get();
           var simpleClient = createTestContainer("simpleclient", clientName, testNetwork).get()
        ) {
            // specialize containers as needed
            simpleClient.withCommand("host.testcontainers.internal", "10000", testMessage);
            simpleServer.withExposedPorts(8000);

            // act
            simpleServer.start();

            proxy.start(10001, "localhost", simpleServer.getMappedPort(8000)).sync();
            org.testcontainers.Testcontainers.exposeHostPorts(Map.of(10001, 10000));

            simpleClient.start();

            // verify
            awaitCompletion(simpleServer.getContainerId());

            var reader = new BufferedReader(readFileFromContainer(simpleServer.getContainerId(), "/opt/receiving"));
            assertEquals(testMessage, reader.readLine(), "reading the first line from /opt/receiving to compare to data send");
        }
    }


    @Test
    @DisplayName("ensure the containers correctly stream 100 MB of data from the server to the client")
    void testStreamingContainers() throws IOException, TimeoutException
    {
        // setup
        var serverName = "test-stream-server";
        var clientName = "test-stream-client";

        try (
                var testNetwork = createKNettyTestNetwork().get();
                var streamServer = createTestContainer("streamserver", serverName, testNetwork).get();
                var streamClient = createTestContainer("streamclient", clientName, testNetwork).get()
        ) {
            var logSniffer = new WaitingConsumer();

            // specialize containers as needed
            streamServer
                    .withCommand("100") // MB of random data
                    .waitingFor(Wait.forLogMessage("\\[TestContainers\\] ready for testing\\n", 1));

            streamClient
                    .withCommand(serverName, "8000")
                    .withLogConsumer(logSniffer);

            // act
            streamServer.start();
            long startTime = System.currentTimeMillis();
            {
                streamClient.start();

                // verify
                logSniffer.waitUntil(frame -> frame.getUtf8String().contains("[TestContainers] ready for verification"), 10, TimeUnit.SECONDS);
            }
            long endTime = System.currentTimeMillis();

            var reader1 = readFileFromContainer(streamServer.getContainerId(), "/opt/streaming/random");
            var reader2 = readFileFromContainer(streamClient.getContainerId(), "/opt/receiving/random");

            System.out.println("transfer time was " + (endTime - startTime));
            assertTrue(compareInputStreams(reader1, reader2));
        }
    }

    @Test
    @DisplayName("ensure the containers correctly stream 100 MB in 20seconds (+50% -10%) if we reduce the bandwidth to 40 Mbit/s with ToxiProxy")
    void testStreamingContainersWithToxiProxy() throws IOException, InterruptedException, TimeoutException
    {
        // setup
        var serverName = "test-stream-server";
        var clientName = "test-stream-client";

        try (
                var testNetwork = createKNettyTestNetwork().get();
                var streamServer = createTestContainer("streamserver", serverName, testNetwork).get();
                var streamClient = createTestContainer("streamclient", clientName, testNetwork).get();
                var toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
        ) {
            var logSniffer = new WaitingConsumer();

            // specialize containers as needed
            streamServer
                    .withCommand("100") // MB of random data
                    .waitingFor(Wait.forLogMessage("\\[TestContainers\\] ready for testing\\n", 1));

            streamClient
                    .withCommand("toxiproxy", "8666")
                    .withLogConsumer(logSniffer);

            // setup the toxic proxy
            toxiproxy
                    .withNetwork(testNetwork)
                    .withNetworkAliases("toxiproxy")
                    .start();

            var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
            var proxy = toxiproxyClient.createProxy("lowBandwidth", "0.0.0.0:8666", serverName + ":8000");
            proxy.toxics()
                    .bandwidth("40mbit", ToxicDirection.DOWNSTREAM, toKiloBytePerSec(40));

            // act
            streamServer.start();

            long startTime = System.currentTimeMillis();
            {
                streamClient.start();

                // verify
                logSniffer.waitUntil(frame -> frame.getUtf8String().contains("[TestContainers] ready for verification"), 60, TimeUnit.SECONDS);
            }
            long endTime = System.currentTimeMillis();

            var reader1 = readFileFromContainer(streamServer.getContainerId(), "/opt/streaming/random");
            var reader2 = readFileFromContainer(streamClient.getContainerId(), "/opt/receiving/random");

            assertTrue(compareInputStreams(reader1, reader2));

            System.out.println("transfer time was " + (endTime - startTime));
            assertTrue(endTime - startTime >= (20000 * 0.9));
            assertTrue(endTime - startTime <= (20000 * 1.5));
        }
    }

    @Test
    @DisplayName("ensure the full setup works, streaming from the server via toxiproxy via the proxy-app to the client (5 Mbit/s, 10 MB => 16 seconds (+50% -10%))")
    void testStreamingFullExample() throws IOException, InterruptedException, TimeoutException
    {
        // setup
        var serverName = "test-stream-server";
        var clientName = "test-stream-client";

        try (
                var testNetwork = createKNettyTestNetwork().get();
                var proxyUnderTest = new SimpleNettyForwardProxy();
                var streamServer = createTestContainer("streamserver", serverName, testNetwork).get();
                var streamClient = createTestContainer("streamclient", clientName, testNetwork).get();
                var toxiproxy = new ToxiproxyContainer("ghcr.io/shopify/toxiproxy:2.5.0")
        ) {

            // | container:8000 | <- | container:8666 | <- Exposed <-     Host:11011        <- Tunneled:900 <- | container |
            // |      Server    | <- |    ToxiProxy   |       <-       ProxyUnderTest       <-      <-         |   Client  |
            int serverPort = 8000;
            int toxiPort = 8666;
            int exposedPort = -1; // will be set at runtime
            int hostPort = 11011;
            int tunneledPort = 900;

            // setup
            var logSniffer = new WaitingConsumer();

            // server
            {
                streamServer
                        .withCommand("10") // MB of random data
                        .waitingFor(Wait.forLogMessage("\\[TestContainers\\] ready for testing\\n", 1));
                streamServer.start();
            }
            // toxiproxy
            {
                toxiproxy
                        .withNetwork(testNetwork)
                        .withNetworkAliases("toxiproxy")
                        .start();

                var toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
                var proxy = toxiproxyClient.createProxy("lowBandwidth", "0.0.0.0:" + toxiPort, serverName + ":" + serverPort);
                proxy.toxics().bandwidth("5mbit", ToxicDirection.DOWNSTREAM, toKiloBytePerSec(5));
            }
            // proxy under test
            {
                exposedPort = toxiproxy.getMappedPort(toxiPort);
                proxyUnderTest.start(hostPort, "localhost", exposedPort).sync();
            }
            // client
            {
                org.testcontainers.Testcontainers.exposeHostPorts(Map.of(hostPort, tunneledPort));
                streamClient
                        .withCommand("host.testcontainers.internal", Integer.toString(tunneledPort))
                        .withLogConsumer(logSniffer);
            }

            // act
            long startTime = System.currentTimeMillis();
            {
                streamClient.start();

                // verify
                logSniffer.waitUntil(frame -> frame.getUtf8String().contains("[TestContainers] ready for verification"), 60, TimeUnit.SECONDS);
            }
            long endTime = System.currentTimeMillis();

            var reader1 = readFileFromContainer(streamServer.getContainerId(), "/opt/streaming/random");
            var reader2 = readFileFromContainer(streamClient.getContainerId(), "/opt/receiving/random");

            assertTrue(compareInputStreams(reader1, reader2));

            System.out.println("transfer time was " + (endTime - startTime));
            assertTrue(endTime - startTime >= (16000 * 0.9));
            assertTrue(endTime - startTime <= (16000 * 1.5));
        }
    }

}