package com.soloproductions.wade.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Startup component that validates PostgreSQL connectivity and attempts to start or
 * initialise a local cluster when the configured port is unreachable.
 *
 * <p>Controlled by {@code wade.datasource.postgres.auto-init} (default {@code true}).
 * When enabled, the service tries the following recovery steps in order:
 * <ol>
 *   <li>{@code pg_ctlcluster} (Debian/Ubuntu)</li>
 *   <li>{@code systemctl start postgresql}</li>
 *   <li>{@code pg_ctl start} using the directory at {@code PGDATA} or common default paths</li>
 *   <li>{@code initdb} followed by {@code pg_ctl start} when no cluster exists yet</li>
 * </ol>
 * All steps are logged at INFO/WARN level so that connectivity problems are always
 * visible in the application log instead of producing a clean-but-empty dataset list.
 */
@Component
public class PostgresStartupService
{
    private static final Logger LOG = LoggerFactory.getLogger(PostgresStartupService.class);

    /** JDBC datasource URL, used to extract host and port when no explicit port is set. */
    @Value("${spring.datasource.url:jdbc:postgresql://localhost:5432/postgres}")
    private String datasourceUrl;

    /**
     * Default/fallback PostgreSQL port.  Overrides the port embedded in the datasource URL
     * only when the URL contains no explicit port.
     */
    @Value("${wade.datasource.postgres.port:5432}")
    private int defaultPostgresPort;

    /**
     * When {@code true}, attempt to start or initialise a local PostgreSQL cluster if the
     * configured port is not reachable.
     */
    @Value("${wade.datasource.postgres.auto-init:true}")
    private boolean autoInit;

    /** Socket connection timeout used when probing the postgres port (milliseconds). */
    private static final int PROBE_TIMEOUT_MS = 1_000;

    /** Maximum seconds to wait for postgres to become ready after a start attempt. */
    private static final int START_WAIT_SECONDS = 30;

    /**
     * Invoked on application startup. Checks postgres connectivity and attempts recovery
     * when {@code auto-init} is enabled and the port is unreachable.
     */
    @PostConstruct
    public void checkAndStart()
    {
        String host = extractHost(datasourceUrl);
        int port = extractPort(datasourceUrl);

        LOG.info("=== PostgreSQL connectivity check: host={}, port={} ===", host, port);

        if (isPortOpen(host, port))
        {
            LOG.info("PostgreSQL is reachable at {}:{}. No action needed.", host, port);
            return;
        }

        LOG.warn("PostgreSQL is NOT reachable at {}:{}. SQL datasets will be unavailable until " +
                 "a connection is established.", host, port);

        if (!autoInit)
        {
            LOG.warn("wade.datasource.postgres.auto-init=false — skipping automatic startup attempt. " +
                     "Please start PostgreSQL manually on port {}.", port);
            return;
        }

        LOG.info("wade.datasource.postgres.auto-init=true — attempting to start PostgreSQL...");
        boolean started = attemptStart(host, port);

        if (started)
        {
            LOG.info("PostgreSQL is now reachable at {}:{} — SQL datasets should persist correctly.", host, port);
        }
        else
        {
            LOG.error("Could not start PostgreSQL on {}:{}. " +
                      "SQL datasets will NOT be persisted in this session. " +
                      "Possible causes: PostgreSQL is not installed, the data directory does not exist, " +
                      "or an incompatible version is present. " +
                      "Run 'pg_lsclusters' (Debian/Ubuntu) or 'pg_ctl status' to diagnose.", host, port);
        }
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Extracts the hostname from a JDBC PostgreSQL URL.
     * Falls back to {@code localhost} when parsing fails.
     *
     * @param url JDBC URL
     * @return extracted hostname
     */
    private String extractHost(String url)
    {
        try
        {
            // jdbc:postgresql://host:port/database
            String withoutScheme = url.replace("jdbc:postgresql://", "");
            String hostPort = withoutScheme.split("/")[0];
            return hostPort.contains(":") ? hostPort.split(":")[0] : hostPort;
        }
        catch (Exception e)
        {
            LOG.debug("Could not parse host from datasource URL '{}', defaulting to localhost", url);
            return "localhost";
        }
    }

    /**
     * Extracts the port number from a JDBC PostgreSQL URL.
     * Falls back to {@link #defaultPostgresPort} when the URL contains no port.
     *
     * @param url JDBC URL
     * @return extracted port number
     */
    private int extractPort(String url)
    {
        try
        {
            String withoutScheme = url.replace("jdbc:postgresql://", "");
            String hostPort = withoutScheme.split("/")[0];
            if (hostPort.contains(":"))
            {
                return Integer.parseInt(hostPort.split(":")[1]);
            }
        }
        catch (Exception e)
        {
            LOG.debug("Could not parse port from datasource URL '{}', using default {}", url, defaultPostgresPort);
        }
        return defaultPostgresPort;
    }

    /**
     * Tests whether a TCP port is accepting connections.
     *
     * @param host target host
     * @param port target port
     * @return {@code true} when the port is reachable
     */
    private boolean isPortOpen(String host, int port)
    {
        try (Socket s = new Socket())
        {
            s.connect(new InetSocketAddress(host, port), PROBE_TIMEOUT_MS);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Tries all supported startup strategies in order.
     * Returns {@code true} when PostgreSQL is reachable after any successful attempt.
     *
     * @param host postgres host
     * @param port postgres port
     * @return {@code true} when postgres became reachable
     */
    private boolean attemptStart(String host, int port)
    {
        // Strategy 1 — pg_ctlcluster (Debian/Ubuntu)
        if (tryPgCtlCluster(port))
        {
            return waitForPort(host, port);
        }

        // Strategy 2 — systemctl start postgresql
        if (trySystemctl())
        {
            return waitForPort(host, port);
        }

        // Strategy 3 — pg_ctl start (generic, uses PGDATA env or common paths)
        if (tryPgCtl())
        {
            return waitForPort(host, port);
        }

        // Strategy 4 — initdb + pg_ctl start (fresh cluster)
        if (tryInitDbAndStart(port))
        {
            return waitForPort(host, port);
        }

        return false;
    }

    /**
     * Attempts to start all postgres clusters via {@code pg_ctlcluster}.
     * This is the preferred method on Debian / Ubuntu systems.
     *
     * @param port target port to verify after starting
     * @return {@code true} when the command was found and executed without an error exit code
     */
    private boolean tryPgCtlCluster(int port)
    {
        if (!isCommandAvailable("pg_lsclusters"))
        {
            LOG.debug("pg_lsclusters not found — skipping pg_ctlcluster strategy.");
            return false;
        }

        LOG.info("Trying pg_ctlcluster strategy (Debian/Ubuntu)...");

        // Discover clusters via pg_lsclusters
        List<String[]> clusters = discoverPgClusters();
        if (clusters.isEmpty())
        {
            LOG.warn("pg_lsclusters found no clusters. Falling back to next strategy.");
            return false;
        }

        boolean anyStarted = false;
        for (String[] cluster : clusters)
        {
            String version = cluster[0];
            String name = cluster[1];
            LOG.info("Starting cluster: version={} name={}", version, name);
            String output = runCommand(List.of("pg_ctlcluster", version, name, "start"), false);
            LOG.info("pg_ctlcluster output: {}", output.isBlank() ? "(no output)" : output);
            anyStarted = true;
        }
        return anyStarted;
    }

    /**
     * Lists installed PostgreSQL clusters using {@code pg_lsclusters}.
     *
     * @return list of (version, name) pairs; empty when none found or command unavailable
     */
    private List<String[]> discoverPgClusters()
    {
        List<String[]> result = new ArrayList<>();
        try
        {
            String output = runCommand(List.of("pg_lsclusters", "--no-header"), false);
            for (String line : output.split("\\r?\\n"))
            {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                String[] parts = trimmed.split("\\s+");
                if (parts.length >= 2)
                {
                    result.add(new String[]{ parts[0], parts[1] });
                }
            }
        }
        catch (Exception e)
        {
            LOG.debug("Error listing pg clusters", e);
        }
        return result;
    }

    /**
     * Attempts to start PostgreSQL via {@code systemctl start postgresql}.
     *
     * @return {@code true} when systemctl was found and the command executed
     */
    private boolean trySystemctl()
    {
        if (!isCommandAvailable("systemctl"))
        {
            LOG.debug("systemctl not available — skipping.");
            return false;
        }

        LOG.info("Trying systemctl start postgresql...");
        String output = runCommand(List.of("systemctl", "start", "postgresql"), false);
        LOG.info("systemctl output: {}", output.isBlank() ? "(no output)" : output);
        return true;
    }

    /**
     * Attempts to start PostgreSQL using {@code pg_ctl start} with a detected or default data
     * directory (PGDATA environment variable or common paths such as
     * {@code /var/lib/postgresql/data}).
     *
     * @return {@code true} when a data directory was found and the command attempted
     */
    private boolean tryPgCtl()
    {
        if (!isCommandAvailable("pg_ctl"))
        {
            LOG.debug("pg_ctl not on PATH — skipping.");
            return false;
        }

        String dataDir = resolveDataDir();
        if (dataDir == null)
        {
            LOG.warn("pg_ctl found but no data directory could be determined. Skipping pg_ctl strategy.");
            return false;
        }

        LOG.info("Trying pg_ctl start -D {}...", dataDir);
        String output = runCommand(List.of("pg_ctl", "start", "-D", dataDir, "-l", dataDir + "/logfile"), false);
        LOG.info("pg_ctl output: {}", output.isBlank() ? "(no output)" : output);
        return true;
    }

    /**
     * Attempts to initialise a new PostgreSQL cluster via {@code initdb} and then start it with
     * {@code pg_ctl}.  Only executed when no data directory exists.
     *
     * @param port port number to embed in the started cluster
     * @return {@code true} when initdb succeeded and pg_ctl was invoked
     */
    private boolean tryInitDbAndStart(int port)
    {
        if (!isCommandAvailable("initdb"))
        {
            LOG.debug("initdb not found — cannot initialise a new cluster.");
            return false;
        }

        String dataDir = System.getenv("PGDATA");
        if (dataDir == null || dataDir.isBlank())
        {
            dataDir = System.getProperty("user.home") + "/pgdata";
        }

        File dataDirFile = new File(dataDir);
        if (dataDirFile.exists() && new File(dataDir, "PG_VERSION").exists())
        {
            LOG.info("Data directory {} already exists — skipping initdb.", dataDir);
            // Try pg_ctl start directly
            return tryPgCtl();
        }

        LOG.info("Attempting to initialise a new PostgreSQL cluster at {}...", dataDir);
        String initOutput = runCommand(List.of("initdb", "-D", dataDir, "--auth=trust", "--no-instructions"), false);
        LOG.info("initdb output: {}", initOutput.isBlank() ? "(no output)" : initOutput);

        if (!new File(dataDir, "PG_VERSION").exists())
        {
            LOG.error("initdb did not produce a valid cluster at {}. Cannot start PostgreSQL.", dataDir);
            return false;
        }

        LOG.info("initdb succeeded. Starting cluster on port {}...", port);
        String startOutput = runCommand(
                List.of("pg_ctl", "start", "-D", dataDir, "-o", "-p " + port, "-l", dataDir + "/logfile"),
                false);
        LOG.info("pg_ctl start output: {}", startOutput.isBlank() ? "(no output)" : startOutput);
        return true;
    }

    /**
     * Resolves the PostgreSQL data directory from environment or well-known locations.
     *
     * @return absolute path to the data directory, or {@code null} when not found
     */
    private String resolveDataDir()
    {
        // 1. PGDATA env
        String pgdata = System.getenv("PGDATA");
        if (pgdata != null && !pgdata.isBlank() && new File(pgdata, "PG_VERSION").exists())
        {
            return pgdata;
        }

        // 2. Common Linux paths
        List<String> candidates = List.of(
                "/var/lib/postgresql/data",
                "/var/lib/pgsql/data",
                "/usr/local/var/postgres"
        );
        for (String candidate : candidates)
        {
            if (new File(candidate, "PG_VERSION").exists())
            {
                return candidate;
            }
        }

        // 3. Versioned Debian/Ubuntu paths like /var/lib/postgresql/14/main
        File pgBase = new File("/var/lib/postgresql");
        if (pgBase.exists() && pgBase.isDirectory())
        {
            File[] versions = pgBase.listFiles(File::isDirectory);
            if (versions != null)
            {
                for (File version : versions)
                {
                    File main = new File(version, "main");
                    if (new File(main, "PG_VERSION").exists())
                    {
                        return main.getAbsolutePath();
                    }
                }
            }
        }

        return null;
    }

    /**
     * Waits up to {@link #START_WAIT_SECONDS} seconds for the postgres port to open.
     *
     * @param host target host
     * @param port target port
     * @return {@code true} when the port became reachable within the timeout
     */
    private boolean waitForPort(String host, int port)
    {
        LOG.info("Waiting up to {}s for PostgreSQL to become ready on {}:{}...", START_WAIT_SECONDS, host, port);
        Instant deadline = Instant.now().plus(Duration.ofSeconds(START_WAIT_SECONDS));
        while (Instant.now().isBefore(deadline))
        {
            if (isPortOpen(host, port))
            {
                return true;
            }
            try { Thread.sleep(500); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
        }
        LOG.warn("Timed out waiting for PostgreSQL on {}:{} after {}s.", host, port, START_WAIT_SECONDS);
        return false;
    }

    /**
     * Checks whether a command is available on the system PATH.
     *
     * @param command command name (e.g. {@code pg_ctl})
     * @return {@code true} when the command was found
     */
    private boolean isCommandAvailable(String command)
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder("which", command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())))
            {
                output = br.lines().collect(Collectors.joining("\n")).trim();
            }
            p.waitFor(2, TimeUnit.SECONDS);
            return !output.isEmpty();
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Runs an external command and returns its combined stdout/stderr output.
     *
     * @param command    command and arguments
     * @param failFast   when {@code true}, throws on non-zero exit code
     * @return captured output string
     */
    private String runCommand(List<String> command, boolean failFast)
    {
        try
        {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output;
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())))
            {
                output = br.lines().collect(Collectors.joining("\n")).trim();
            }
            boolean finished = p.waitFor(60, TimeUnit.SECONDS);
            if (!finished)
            {
                LOG.warn("Command {} timed out after 60s", command);
                p.destroyForcibly();
            }
            else if (failFast && p.exitValue() != 0)
            {
                throw new RuntimeException("Command " + command + " exited with code " + p.exitValue() + ": " + output);
            }
            return output;
        }
        catch (Exception e)
        {
            LOG.warn("Error running command {}: {}", command, e.getMessage());
            return "";
        }
    }
}
