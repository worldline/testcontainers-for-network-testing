package com.worldline.network.examples;

import java.util.function.Supplier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;

public class TestContainersHelper
{
    /**
     * creates a testcontainer from a docker file and attaches it to the supplied network in bridge mode with the
     * container name as network alias.
     * @param dockerFilePathRelativeToResources path of the dockerfile relative to the resource folder
     * @param containerName how to name/alias our test container
     * @param kNettyTestNetwork the network to attach to in bridge mode
     * @return a supplier for a test container, ready to be wrapped in a try catch block
     */
    public static Supplier<GenericContainer<?>> createTestContainer(final String dockerFilePathRelativeToResources,
                    final String containerName, final Network kNettyTestNetwork)
    {
        return () -> new GenericContainer<>(createImage(dockerFilePathRelativeToResources))
                        //.withCreateContainerCmdModifier(cmd -> cmd.withName(containerName))
                        .withNetwork(kNettyTestNetwork)
                        .withNetworkAliases(containerName)
                        .withNetworkMode("bridge");
    }

    /**
     * @return creates a new default network with a random name
     */
    public static Supplier<Network> createKNettyTestNetwork()
    {
        return () -> Network.newNetwork();
    }

    private static ImageFromDockerfile createImage(final String resourceRelativePath)
    {
        return new ImageFromDockerfile()
                    .withFileFromClasspath(".", resourceRelativePath);
    }
}
