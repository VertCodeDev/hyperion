package dev.vertcode.hyperion;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Properties;

import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import io.papermc.paper.plugin.loader.library.impl.MavenLibraryResolver;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;

import dev.vertcode.hyperion.api.version.MinecraftVersion;

/**
 * Has the server download the libraries Hyperion uses but does not ship: Cloud for the commands,
 * and InvUI for the menus, in the release that fits the server's version.
 *
 * <p>Paper runs this before the plugin loads and caches the downloads in its {@code libraries}
 * folder, so only the first start needs the internet. The libraries live on the plugin's own
 * classpath, apart from any other plugin's copies. Adventure is left out: Paper has it.
 */
public final class HyperionLoader implements PluginLoader {

    private static final String VERSIONS = "/hyperion-libraries.properties";
    private static final String CENTRAL = "https://maven-central.storage-download.googleapis.com/maven2";
    private static final String XENONDEVS = "https://repo.xenondevs.xyz/releases/";
    private static final String INVUI = "xyz.xenondevs.invui";
    private static final MinecraftVersion V26 = new MinecraftVersion(26, 0, 0);
    private static final MinecraftVersion V26_2 = MinecraftVersion.parse("26.2");
    private static final MinecraftVersion V26_3 = MinecraftVersion.parse("26.3");

    @Override
    public void classloader(final PluginClasspathBuilder classpath) {
        Properties versions = versions();
        MavenLibraryResolver resolver = new MavenLibraryResolver();
        resolver.addRepository(new RemoteRepository.Builder("central", "default", CENTRAL).build());
        resolver.addRepository(new RemoteRepository.Builder("xenondevs", "default", XENONDEVS).build());

        String cloud = versions.getProperty("cloud");
        String cloudMinecraft = versions.getProperty("cloudMinecraft");
        resolver.addDependency(library("org.incendo:cloud-paper:" + cloudMinecraft));
        resolver.addDependency(library("org.incendo:cloud-annotations:" + cloud));
        resolver.addDependency(library("org.incendo:cloud-minecraft-extras:" + cloudMinecraft));

        for (String artifact : invui(server(), versions)) {
            resolver.addDependency(library(artifact));
        }

        classpath.addLibrary(resolver);
    }

    /**
     * Pick the InvUI artifacts for a server version. InvUI 1 needs its core and the one module
     * that reaches into that version's inventories, built for Paper's Mojang names; InvUI 2 is a
     * single artifact per 26.x line.
     *
     * @param server the server's version
     * @param versions the library versions from the build
     * @return the artifacts, as Maven coordinates
     */
    static List<String> invui(final MinecraftVersion server, final Properties versions) {
        if (server.atLeast(V26_3)) {
            return List.of(INVUI + ":invui:" + versions.getProperty("invui2"));
        }

        if (server.atLeast(V26_2)) {
            return List.of(INVUI + ":invui:" + versions.getProperty("invui2For262"));
        }

        if (server.atLeast(V26)) {
            return List.of(INVUI + ":invui:" + versions.getProperty("invui2For261"));
        }

        String invui1 = versions.getProperty("invui1");

        return List.of(
                INVUI + ":invui-core:" + invui1,
                INVUI + ":inventory-access-" + inventoryAccess(server) + ":jar:remapped-mojang:" + invui1
        );
    }

    /**
     * Name InvUI 1's inventory module for a 1.21.x version.
     *
     * @param server the server's version
     * @return the module's revision, such as {@code r26}
     */
    static String inventoryAccess(final MinecraftVersion server) {
        if (server.atLeast(MinecraftVersion.parse("1.21.11"))) {
            return "r26";
        }

        if (server.atLeast(MinecraftVersion.parse("1.21.9"))) {
            return "r25";
        }

        if (server.atLeast(MinecraftVersion.parse("1.21.6"))) {
            return "r24";
        }

        if (server.atLeast(MinecraftVersion.parse("1.21.5"))) {
            return "r23";
        }

        if (server.atLeast(MinecraftVersion.parse("1.21.4"))) {
            return "r22";
        }

        if (server.atLeast(MinecraftVersion.parse("1.21.2"))) {
            return "r21";
        }

        return "r20";
    }

    /**
     * Read the server's Minecraft version. A version this build cannot read, such as a
     * pre-release name, counts as the newest supported one.
     *
     * @return the version
     */
    private static MinecraftVersion server() {
        try {
            return MinecraftVersion.parse(ServerBuildInfo.buildInfo().minecraftVersionId());
        } catch (IllegalArgumentException exception) {
            return V26_3;
        }
    }

    /**
     * Describe a library to download, without Adventure.
     *
     * @param coordinates the Maven coordinates
     * @return the dependency
     */
    private static Dependency library(final String coordinates) {
        List<Exclusion> noAdventure = List.of(new Exclusion("net.kyori", "*", "*", "*"));

        return new Dependency(new DefaultArtifact(coordinates), null, null, noAdventure);
    }

    /**
     * Read the library versions the build wrote into the jar.
     *
     * @return the versions
     */
    private static Properties versions() {
        Properties versions = new Properties();
        try (InputStream stream = HyperionLoader.class.getResourceAsStream(VERSIONS)) {
            if (stream == null) {
                throw new IllegalStateException("The jar has no " + VERSIONS);
            }

            versions.load(stream);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }

        return versions;
    }
}
