package com.worldline.network.examples;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.function.Supplier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

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

    /**
     * @return true if the two readers represent an identical file on the byte level
     */
    public static boolean compareInputStreams(InputStreamReader stream1, InputStreamReader stream2) throws IOException
    {
        return IOUtils.contentEquals(stream1, stream2);
    }

    /**
     * @return converts for toxiproxy bandwidth limits, uses 1024 as multiplier
     */
    public static int toKiloBytePerSec(int megaBitPerSec)
    {
        return megaBitPerSec * 1024 / 8;
    }

    private static ImageFromDockerfile createImage(final String resourceRelativePath)
    {
        return new ImageFromDockerfile()
                    .withFileFromClasspath(".", resourceRelativePath);
    }
}
