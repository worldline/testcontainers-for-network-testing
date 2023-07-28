package com.worldline.network.examples;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.WaitContainerResultCallback;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import com.github.dockerjava.transport.DockerHttpClient;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DockerTestBase
{
    // the npipe string is the Windows version, to support linux we need a different connector
    //
    // Windows: npipe:////./pipe/docker_engine
    // Linux: unix:///var/run/docker.sock
    public static final String DOCKER_CONNECT_STRING = "npipe:////./pipe/docker_engine";

    protected DockerClient dockerClient;

    @BeforeAll
    void setupDocker()
    {
        DefaultDockerClientConfig config = DefaultDockerClientConfig
                .createDefaultConfigBuilder()
                .withDockerHost(DOCKER_CONNECT_STRING)
                .build();

        DockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(config.getDockerHost())
                .sslConfig(config.getSSLConfig())
                .build();

        dockerClient = DockerClientImpl.getInstance(config, httpClient);
    }

    /**
     * wait for the container to complete, timeout at 5 seconds
     *
     * @param containerId of the containers that should complete
     * @throws InterruptedException
     */
    protected void awaitCompletion(final String containerId) throws InterruptedException
    {
        WaitContainerResultCallback resultCallback = new WaitContainerResultCallback();
        dockerClient.waitContainerCmd(containerId).exec(resultCallback);

        resultCallback.awaitCompletion(5000, TimeUnit.MILLISECONDS);
    }

    /**
     * read file data from a running or stopped container
     *
     * @param containerId of the target container
     * @param resource    a file within the container that should be copied and read
     * @return Buffered reader that allows to read from the single file that has been copied
     * @throws IOException
     */
    protected InputStreamReader readFileFromContainer(final String containerId, final String resource) throws IOException
    {
        var stream = dockerClient.copyArchiveFromContainerCmd(containerId, resource).exec();

        TarArchiveInputStream tarStream = new TarArchiveInputStream(stream);
        tarStream.getNextTarEntry();

        return new InputStreamReader(tarStream);
    }
}
