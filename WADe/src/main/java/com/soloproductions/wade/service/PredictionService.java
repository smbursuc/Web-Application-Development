package com.soloproductions.wade.service;

import com.soloproductions.wade.dto.PredictionRequest;
import com.soloproductions.wade.util.GroqClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.URI;
import java.time.Instant;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for model-assisted predictions, OCR extraction, and similarity estimation.
 *
 * Supports OpenAI-compatible APIs and local Ollama provider execution.
 */
@Service
public class PredictionService
{

    /** Logger. */
    private static final Logger LOG = LoggerFactory.getLogger(PredictionService.class);

    /** Base URL for the configured LLM provider API. */
    @Value("${wade.llm.base-url:}")
    private String baseUrl;

    /** API key used for authenticated providers. */
    @Value("${wade.llm.api-key:}")
    private String apiKey;

    /** Text-generation model identifier. */
    @Value("${wade.llm.model:mistral:7b}")
    private String model;

    /** OCR/vision model identifier. */
    @Value("${wade.llm.ocr-model:llava:7b}")
    private String ocrModel;

    /** Provider name (for example: ollama, openai). */
    @Value("${wade.llm.provider:ollama}")
    private String provider;

    /** Flag controlling automatic local Ollama startup. */
    @Value("${wade.llm.ollama-auto-start:true}")
    private boolean ollamaAutoStart;
    
    /** Command used to start local Ollama service. */
    @Value("${wade.llm.ollama-start-cmd:ollama serve}")
    private String ollamaStartCmd;

    /** HTTP client for provider API calls. */
    private final RestTemplate restTemplate;

    /** Process handle for app-started Ollama process. */
    private Process ollamaProcess;

    /** Background executor for Ollama log streaming. */
    private ExecutorService logExecutor;

    /** Indicates whether Ollama was started by this service instance. */
    private volatile boolean startedOllamaByUs = false;

    /** Groq API client, initialised only when {@code wade.llm.provider=groq}. */
    private GroqClient groqClient;

    /**
     * Creates the prediction service and initializes its HTTP client.
     */
    public PredictionService()
    {
        this.restTemplate = new RestTemplate();
    }

    /**
     * Initializes local provider runtime (Ollama) when configured for auto-start.
     *
     * @throws  Exception
     *          when initialization fails in a non-recoverable way
     */
    @PostConstruct
    public void init() throws Exception
    {
        if ("groq".equalsIgnoreCase(provider))
        {
            LOG.info("Provider is Groq — initialising GroqClient (model={}, ocrModel={})", model, ocrModel);
            groqClient = new GroqClient(apiKey, model, baseUrl);
            return;
        }

        if (!ollamaAutoStart)
        {
            LOG.info("Ollama auto-start is disabled by configuration (wade.llm.ollama-auto-start=false). Skipping start.");
            return;
        }

        // parse host:port from baseUrl (expect http://host:port)
        String host = "localhost";
        int port = 11434;
        try
        {
            URI uri = new URI(baseUrl);
            if (uri.getHost() != null) 
            {
                host = uri.getHost();
            }
            if (uri.getPort() != -1) 
            {
                port = uri.getPort();
            }
        }
        catch (Exception e)
        {
            LOG.warn("Invalid baseUrl for Ollama, falling back to localhost:11434: {}", baseUrl);
        }

        boolean alreadyRunning = isPortOpen(host, port, 500);
        if (alreadyRunning)
        {
            LOG.info("Ollama is already running at {}:{}. Stopping existing instance to capture logs...", host, port);
            killGenericOllamaProcesses();
            
            // Wait for port to close (max 10s)
            Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
            while (Instant.now().isBefore(deadline))
            {
                if (!isPortOpen(host, port, 200))
                {
                    alreadyRunning = false;
                    break;
                }
                try 
                { 
                    Thread.sleep(500); 
                } 
                catch (Exception ignored) 
                {

                }
            }
            
            if (alreadyRunning)
            {
                LOG.warn("Could not stop existing Ollama instance (port still open). Logs will not be available.");
                // We proceed, but we won't be able to attach logs since we didn't start it.
                // However, user specifically asked to fix the logger. If we fail here, we effectively fail the request.
                // But we can't do magic. We'll just have to return and let it run silent.
                LOG.info("Using existing Ollama instance (silent mode).");
                return;
            }
        }


        // We check if the configured command is an absolute path to an executable (bypass scan),
        // OR if the default 'ollama' is found on PATH.
        String startCmd = this.ollamaStartCmd;
        boolean explicitExeFound = false;
        try 
        {
            String exePart = startCmd.split("\\s+")[0];
            File exeFile = new File(exePart);
            if (exeFile.isAbsolute() && exeFile.exists() && exeFile.canExecute()) 
            {
                explicitExeFound = true;
                LOG.info("Using configured explicit Ollama executable: {}", exeFile.getAbsolutePath());
            }
        } 
        catch (Exception ignored) 
        {

        }

        if (!explicitExeFound && !isOllamaInstalled())
        {
            String msg = "Ollama CLI not found on PATH. Skipping Ollama auto-start.";
            LOG.warn(msg);
            return;
        }

        LOG.info("Starting ollama with command: {}", startCmd);

        // We use the environment of the current process to ensure DLLs and PATH are correct.
        ProcessBuilder pb;
        String os = System.getProperty("os.name").toLowerCase();
        
        // Split the command manually to avoid cmd.exe shell orphaning if possible
        String[] cmdArray = startCmd.split("\\s+");
        pb = new ProcessBuilder(cmdArray);
        pb.environment().putAll(System.getenv());
        
        // AMD GPU Support (RX 6700 XT / RDNA2)
        // ROCm can be picky on Windows. Enabling Vulkan is often more stable for consumer AMD cards.
        pb.environment().put("OLLAMA_VULKAN", "1");
        
        // Debug logging for Ollama to help diagnose GPU loading issues
        pb.environment().put("OLLAMA_DEBUG", "1");
        
        // If an absolute path to the EXE is provided, set the working directory to that folder.
        // This is crucial for Windows processes to find their secondary DLLs/runners.
        try
        {
            File exeFile = new File(cmdArray[0]);
            if (exeFile.isAbsolute() && exeFile.getParentFile() != null) 
            {
                pb.directory(exeFile.getParentFile());
                LOG.info("Set Ollama working directory to: {}", exeFile.getParentFile().getAbsolutePath());
            }
        } 
        catch (Exception ignored) 
        {

        }
        
        pb.redirectErrorStream(true);
        try
        {
            try
            {
                ollamaProcess = pb.start();
            }
            catch (IOException ioe)
            {
                LOG.warn("Direct process start failed ({}). Attempting shell fallback.", ioe.getMessage());
                if (os.contains("win"))
                {
                    pb = new ProcessBuilder("cmd.exe", "/c", startCmd);
                }
                else
                {
                    pb = new ProcessBuilder("sh", "-c", startCmd);
                }
                pb.environment().putAll(System.getenv());
                pb.redirectErrorStream(true);
                ollamaProcess = pb.start();
            }
            startedOllamaByUs = true;
            LOG.info("Ollama started by us with PID: {}", ollamaProcess.pid());
            logExecutor = Executors.newSingleThreadExecutor(r ->
            {
                Thread t = new Thread(r, "ollama-log-reader");
                t.setDaemon(true);
                return t;
            });

            // stream logs
            logExecutor.submit(() ->
            {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(ollamaProcess.getInputStream())))
                {
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        LOG.info("[ollama] {}", line);
                    }
                }
                catch (Exception ex)
                {
                    LOG.debug("Error reading ollama output", ex);
                }
            });

            // wait for port
            Instant deadline = Instant.now().plus(Duration.ofSeconds(20));
            boolean ok = false;
            while (Instant.now().isBefore(deadline))
            {
                if (isPortOpen(host, port, 200))
                {
                    ok = true;
                    break;
                }
                if (!ollamaProcess.isAlive()) 
                {
                    break;
                }
                Thread.sleep(300);
            }
            if (!ok)
            {
                String out = "(no output)";
                try
                {
                    out = readProcessTail(ollamaProcess, 10);
                }
                catch (Exception ignored)
                {

                }
                String msg = String.format("Failed to start Ollama process or port %d not open. Recent output: %s", port, out);
                LOG.error(msg);
            }
            LOG.info("Ollama started and listening on {}:{}", host, port);

            // Register a hard shutdown hook to ensure Ollama dies even if Spring's PreDestroy is skipped or fails
            // This is meant to fix a hanging Ollama process
            Runtime.getRuntime().addShutdownHook(new Thread(() -> 
            {
                try 
                {
                    this.shutdown();
                } 
                catch (Exception e) 
                {
                    System.err.println("Error in Ollama shutdown hook: " + e.getMessage());
                }
            }));
        }
        catch (Exception e)
        {
            LOG.error("Failed to start Ollama process; continuing without Ollama", e);
            // Do not fail application startup because external process couldn't be spawned.
            startedOllamaByUs = false;
            if (ollamaProcess != null && ollamaProcess.isAlive())
            {
                try 
                { 
                    ollamaProcess.destroyForcibly(); 
                } 
                catch (Exception ignored) 
                {

                }
            }
            return;
        }
    }

    /**
     * Stops local provider processes and background log readers on shutdown.
     *
     * @throws  Exception
     *          when shutdown operations fail
     */
    @PreDestroy
    public void shutdown() throws Exception
    {
        if (startedOllamaByUs && ollamaProcess != null && ollamaProcess.isAlive())
        {
            LOG.info("Stopping Ollama process started by application (PID: {})", ollamaProcess.pid());
            
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) 
            {
                // On Windows, destroying the process won't kill the child ollama.exe processes (the runner).
                // Use taskkill /F /T to kill the entire process tree.
                try 
                {
                    ProcessBuilder killer = new ProcessBuilder("taskkill", "/F", "/T", "/PID", String.valueOf(ollamaProcess.pid()));
                    killer.start().waitFor();
                    LOG.info("Ollama process tree killed via taskkill.");
                } 
                catch (Exception e) 
                {
                    LOG.warn("Failed to kill Ollama process tree via taskkill: {}. Falling back to destroy().", e.getMessage());
                    ollamaProcess.destroyForcibly();
                }
            } 
            else 
            {
                ollamaProcess.destroy();
                try 
                {
                    ollamaProcess.waitFor(5, TimeUnit.SECONDS);
                } 
                catch (InterruptedException ignored) 
                {

                }
                if (ollamaProcess.isAlive()) 
                {
                    ollamaProcess.destroyForcibly();
                }
            }
        }
        if (logExecutor != null)
        {
            logExecutor.shutdownNow();
        }
    }

    /**
     * Checks whether a TCP port is reachable.
     *
     * @param   host
     *          target host
     * @param   port
     *          target port
     * @param   timeoutMillis
     *          socket connection timeout in milliseconds
     *
     * @return  {@code true} when the endpoint is reachable
     */
    private boolean isPortOpen(String host, int port, int timeoutMillis)
    {
        try (Socket s = new Socket())
        {
            s.connect(new java.net.InetSocketAddress(host, port), timeoutMillis);
            return true;
        }
        catch (Exception e)
        {
            return false;
        }
    }

    /**
     * Detects whether Ollama executable is available in the runtime environment.
     *
     * @return  {@code true} when Ollama can be executed
     */
    private boolean isOllamaInstalled()
    {
        // Prefer locating the executable on the PATH (fast, reliable)
        try
        {
            String path = System.getenv("PATH");
            LOG.debug("Runtime PATH: {}", path);
            if (path != null && !path.isEmpty())
            {
                String[] parts = path.split(java.io.File.pathSeparator);
                for (String part : parts)
                {
                    try
                    {
                        if (part == null || part.isEmpty())
                        {
                            LOG.debug("Skipping empty PATH entry");
                            continue;
                        }
                        File f1 = new File(part, "ollama");
                        File f2 = new File(part, "ollama.exe");
                        LOG.debug("Checking PATH entry: {} -> ollama: {} (exists={} exec={}), ollama.exe: {} (exists={} exec={})",
                                  part,
                                  f1.getAbsolutePath(), f1.exists(), f1.canExecute(),
                                  f2.getAbsolutePath(), f2.exists(), f2.canExecute());

                        if (f1.exists() && f1.canExecute())
                        {
                            LOG.info("Found ollama executable on PATH: {}", f1.getAbsolutePath());
                            return true;
                        }
                        if (f2.exists() && f2.canExecute())
                        {
                            LOG.info("Found ollama executable on PATH: {}", f2.getAbsolutePath());
                            return true;
                        }
                    }
                    catch (Exception inner)
                    {
                        LOG.debug("Error checking PATH entry {}", part, inner);
                    }
                }
            }
        }
        catch (Exception ex)
        {
            LOG.debug("Error while scanning PATH for ollama", ex);
        }

        // Fallback: run `ollama --version` and look for a client/version string in output.
        try
        {
            ProcessBuilder pb = new ProcessBuilder("ollama", "--version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder out = new StringBuilder();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())))
            {
                String line;
                while ((line = br.readLine()) != null)
                {
                    out.append(line).append('\n');
                }
            }
            boolean exited = p.waitFor(3, TimeUnit.SECONDS);
            String combined = out.toString().toLowerCase();
            if (combined.contains("client version") || combined.contains("ollama"))
            {
                return true;
            }
            if (exited)
            {
                return p.exitValue() == 0;
            }
            else
            {
                p.destroyForcibly();
                return false;
            }
        }
        catch (IOException ioe)
        {
            LOG.debug("ollama --version failed, attempting where.exe fallback", ioe);
            try
            {
                ProcessBuilder wherePb = new ProcessBuilder("where.exe", "ollama");
                wherePb.redirectErrorStream(true);
                Process whereP = wherePb.start();
                StringBuilder whereOut = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(whereP.getInputStream())))
                {
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        whereOut.append(line).append('\n');
                    }
                }
                boolean wExited = whereP.waitFor(2, TimeUnit.SECONDS);

                if (!wExited) 
                { 
                    whereP.destroyForcibly(); 
                    return false; 
                
                }
                String whereStr = whereOut.toString().trim();
                LOG.debug("where.exe output: {}", whereStr);
                if (whereStr.isEmpty()) 
                {
                    return false;
                }

                // where.exe may print informational messages like "INFO: Could not find files for the given pattern(s)."
                String lower = whereStr.toLowerCase();
                if (lower.contains("could not find files") || lower.startsWith("info:"))
                {
                    LOG.debug("where.exe reported no matches");
                    return false;
                }
                String firstPath = whereStr.split("\\r?\\n")[0].trim();
                if (firstPath.isEmpty()) 
                {
                    return false;
                }
                // Ensure it looks like an absolute path before attempting to exec it
                if (!(firstPath.contains(java.io.File.separator) || firstPath.contains(":")))
                {
                    LOG.debug("where.exe returned non-path output: {}", firstPath);
                    return false;
                }
                File resolved = new File(firstPath);
                if (!resolved.exists())
                {
                    LOG.debug("where.exe returned path does not exist: {}", firstPath);
                    return false;
                }
                LOG.info("Resolved ollama path via where.exe: {}", firstPath);

                ProcessBuilder pb2 = new ProcessBuilder(firstPath, "--version");
                pb2.redirectErrorStream(true);
                Process p2 = pb2.start();
                StringBuilder out2 = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(p2.getInputStream())))
                {
                    String line;
                    while ((line = br.readLine()) != null)
                    {
                        out2.append(line).append('\n');
                    }
                }
                boolean exited2 = p2.waitFor(3, TimeUnit.SECONDS);
                String combined2 = out2.toString().toLowerCase();
                if (combined2.contains("client version") || combined2.contains("ollama"))
                {
                    return true;
                }
                if (exited2)
                {
                    return p2.exitValue() == 0;
                }
                else
                {
                    p2.destroyForcibly();
                    return false;
                }
            }
            catch (Exception e2)
            {
                LOG.debug("where.exe fallback failed", e2);
                return false;
            }
        }
        catch (Exception e)
        {
            LOG.debug("Error invoking ollama --version", e);
            return false;
        }
    }

    /**
     * Terminates running Ollama-related processes detected on the host.
     */
    private void killGenericOllamaProcesses()
    {
        try
        {
            ProcessHandle.allProcesses()
                    .filter(p -> p.info().command().map(c -> c.toLowerCase().contains("ollama")).orElse(false))
                    .forEach(p -> {
                        try
                        {
                            LOG.info("Killing detected Ollama process PID: {}", p.pid());
                            p.destroyForcibly();
                        }
                        catch (Exception e)
                        {
                            LOG.warn("Failed to kill PID {}", p.pid(), e);
                        }
                    });

            if (System.getProperty("os.name").toLowerCase().contains("win"))
            {
                new ProcessBuilder("taskkill", "/F", "/IM", "ollama.exe").start().waitFor();
                new ProcessBuilder("taskkill", "/F", "/IM", "ollama_llama_server.exe").start().waitFor();
            }
        }
        catch (Exception e)
        {
            LOG.warn("Error trying to kill existing Ollama processes", e);
        }
    }

    /**
     * Counts currently running processes whose command includes "ollama".
     *
     * @return  number of detected Ollama-related processes
     */
    private int countOllamaProcesses()
    {
        try
        {
            return (int) ProcessHandle.allProcesses()
                    .map(ProcessHandle::info)
                    .map(info -> info.command().orElse(""))
                    .filter(cmd -> cmd.toLowerCase().contains("ollama"))
                    .count();
        }
        catch (Exception e)
        {
            LOG.debug("Error counting processes", e);
            return 0;
        }
    }

    /**
     * Reads the tail of a process output stream.
     *
     * @param   p
     *          process whose output should be inspected
     * @param   lines
     *          number of trailing lines to return
     *
     * @return  output tail string
     *
     * @throws  Exception
     *          when stream access fails
     */
    private String readProcessTail(Process p, int lines) throws Exception
    {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream())))
        {
            List<String> all = br.lines().collect(Collectors.toList());
            int start = Math.max(0, all.size() - lines);
            return String.join("\n", all.subList(start, all.size()));
        }
        catch (Exception e)
        {
            return "(unavailable)";
        }
    }

    /**
     * Predicts best cluster and confidence for an object candidate set.
     *
     * @param   request
     *          prediction request containing object and candidate clusters
     *
     * @return  model response JSON string
     */
    public String guessClusterWithConfidence(PredictionRequest request)
    {
        String candidates = String.join(", ", request.getCandidates());
        String prompt = String.format(
                "You are a strict data classifier.\n" +
                "Task: Associate the object '%s' with exactly one cluster from this list: [%s].\n" +
                "Return valid JSON only using this schema: {\"cluster\": \"<exact candidate>\", \"confidence\": <number between 0.0 and 1.0>}.\n" +
                "Rules: cluster must exactly match one candidate string. No extra keys. No explanation.",
                request.getObject1(), candidates
        );
        return callLlm(prompt);
    }

    /**
     * Builds Ollama-native endpoint URL from configured base URL.
     *
     * @param   endpoint
     *          endpoint path fragment
     *
     * @return  normalized Ollama endpoint URL
     */
    private String getOllamaNativeUrl(String endpoint) {
        String base = baseUrl;
        if (base.endsWith("/v1")) {
            base = base.substring(0, base.length() - 3);
        } else if (base.endsWith("/v1/")) {
            base = base.substring(0, base.length() - 4);
        }
        return base.endsWith("/") ? base + endpoint : base + "/" + endpoint;
    }

    /**
     * Sends a text prompt to the configured LLM provider.
     *
     * @param   prompt
     *          prompt text to submit
     *
     * @return  provider response text
     */
    private String callLlm(String prompt)
    {
        if (groqClient != null)
        {
            return groqClient.chatCompletion(prompt);
        }

        try
        {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty())
            {
                headers.setBearerAuth(apiKey);
            }

            ResponseEntity<Map> response;

            if ("ollama".equalsIgnoreCase(provider))
            {
                // Ollama local API: POST { model, prompt, temperature, max_tokens } to /api/generate
                String url = getOllamaNativeUrl("api/generate");
                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("prompt", prompt);
                body.put("temperature", 0.0);
                body.put("max_tokens", 32);
                body.put("stream", false);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                response = restTemplate.postForEntity(url, entity, Map.class);
                return extractTextFromResponse(response.getBody());
            }
            else
            {
                // Default: OpenAI-compatible chat completions
                String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", Collections.singletonList(message));
                body.put("temperature", 0.0);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                response = restTemplate.postForEntity(url, entity, Map.class);

                if (response.getBody() != null && response.getBody().containsKey("choices"))
                {
                    List choices = (List) response.getBody().get("choices");
                    if (!choices.isEmpty())
                    {
                        Map choice = (Map) choices.get(0);
                        Object msgObj = choice.get("message");
                        if (msgObj instanceof Map)
                        {
                            Map msg = (Map) msgObj;
                            Object content = msg.get("content");
                            if (content instanceof String) return ((String) content).trim();
                        }
                        else if (choice.containsKey("text"))
                        {
                            Object text = choice.get("text");
                            if (text instanceof String) return ((String) text).trim();
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOG.error("LLM prediction failed", e);
            return "Error: " + e.getMessage();
        }
        return "Unknown";
    }

    /**
     * Extracts the best textual answer from provider-specific response shapes.
     *
     * @param   body
     *          provider response body map
     *
     * @return  extracted text response or fallback string
     */
    private String extractTextFromResponse(Map body)
    {
        if (body == null) return "";

        // Common Ollama /api/generate format
        if (body.containsKey("response")) {
            Object resp = body.get("response");
            if (resp instanceof String) return ((String) resp).trim();
        }

        // Try common OpenAI chat response shapes
        try
        {
            if (body.containsKey("choices"))
            {
                Object choicesObj = body.get("choices");
                if (choicesObj instanceof List && !((List) choicesObj).isEmpty())
                {
                    Object firstChoice = ((List) choicesObj).get(0);
                    if (firstChoice instanceof Map) {
                        Map choiceMap = (Map) firstChoice;
                        if (choiceMap.containsKey("message")) {
                            Object msgObj = choiceMap.get("message");
                            if (msgObj instanceof Map) {
                                Object content = ((Map)msgObj).get("content");
                                if (content instanceof String) return ((String) content).trim();
                            }
                        }
                    }
                }
            }

            if (body.containsKey("output"))
            {
                Object out = body.get("output");
                if (out instanceof List && !((List) out).isEmpty())
                {
                    Object first = ((List) out).get(0);
                    if (first instanceof Map)
                    {
                        Map m = (Map) first;
                        if (m.containsKey("content"))
                        {
                            Object content = m.get("content");
                            if (content instanceof String) return ((String) content).trim();
                            if (content instanceof List && !((List) content).isEmpty())
                            {
                                Object c0 = ((List) content).get(0);
                                if (c0 instanceof Map && ((Map) c0).containsKey("text"))
                                {
                                    Object text = ((Map) c0).get("text");
                                    if (text instanceof String) return ((String) text).trim();
                                }
                            }
                        }
                        if (m.containsKey("text"))
                        {
                            Object text = m.get("text");
                            if (text instanceof String) return ((String) text).trim();
                        }
                    }
                }
            }

            if (body.containsKey("results"))
            {
                Object results = body.get("results");
                if (results instanceof List && !((List) results).isEmpty())
                {
                    Object first = ((List) results).get(0);
                    if (first instanceof Map)
                    {
                        Map m = (Map) first;
                        if (m.containsKey("content"))
                        {
                            Object content = m.get("content");
                            if (content instanceof List && !((List) content).isEmpty())
                            {
                                Object c0 = ((List) content).get(0);
                                if (c0 instanceof Map && ((Map) c0).containsKey("text"))
                                {
                                    Object text = ((Map) c0).get("text");
                                    if (text instanceof String) return ((String) text).trim();
                                }
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            LOG.debug("Failed to extract text from LLM body", e);
        }

        // Fallback: toString the whole body
        return body.toString();
    }
    
    /**
     * Downloads image bytes from a URL.
     *
     * @param   imageUrl
     *          source image URL
     *
     * @return  downloaded image bytes
     */
    private byte[] downloadImage(String imageUrl) {
        try {
            java.net.URL url = new java.net.URL(imageUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            int status = conn.getResponseCode();
            if (status != 200) {
                throw new RuntimeException("HTTP error " + status + " while downloading image from " + imageUrl);
            }

            try (java.io.InputStream is = conn.getInputStream()) {
                byte[] bytes = is.readAllBytes();
                LOG.info("Downloaded image from {}: {} bytes", imageUrl, bytes.length);
                return bytes;
            }
        } catch (Exception e) {
            LOG.error("Failed to download image: {}", imageUrl, e);
            throw new RuntimeException("Could not download image: " + e.getMessage());
        }
    }

    /**
     * Converts image data to JPEG and resizes oversized images.
     *
     * @param   imageBytes
     *          source image bytes
     *
     * @return  standardized JPEG bytes (or original bytes when conversion fails)
     */
    private byte[] convertToJpeg(byte[] imageBytes) {
        try {
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(bais);
            if (image == null) return imageBytes;

            // Resize if too large to prevent Ollama memory crashes
            int maxDim = 1024;
            if (image.getWidth() > maxDim || image.getHeight() > maxDim) {
                double scale = (double) maxDim / Math.max(image.getWidth(), image.getHeight());
                int newW = (int) (image.getWidth() * scale);
                int newH = (int) (image.getHeight() * scale);
                java.awt.Image tmp = image.getScaledInstance(newW, newH, java.awt.Image.SCALE_SMOOTH);
                java.awt.image.BufferedImage resized = new java.awt.image.BufferedImage(newW, newH, java.awt.image.BufferedImage.TYPE_INT_RGB);
                java.awt.Graphics2D g2 = resized.createGraphics();
                g2.drawImage(tmp, 0, 0, null);
                g2.dispose();
                image = resized;
            }

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            java.awt.image.BufferedImage rgbImage = new java.awt.image.BufferedImage(
                    image.getWidth(), image.getHeight(), java.awt.image.BufferedImage.TYPE_INT_RGB);
            java.awt.Graphics2D g = rgbImage.createGraphics();
            g.drawImage(image, 0, 0, java.awt.Color.WHITE, null);
            g.dispose();

            javax.imageio.ImageIO.write(rgbImage, "jpg", baos);
            byte[] jpegBytes = baos.toByteArray();
            LOG.info("Final image for AI: {}x{}, {} bytes", image.getWidth(), image.getHeight(), jpegBytes.length);
            return jpegBytes;
        } catch (Exception e) {
            LOG.warn("Image processing failed: {}", e.getMessage());
            return imageBytes;
        }
    }

    /**
     * Performs OCR/object extraction prediction for an image URL.
     *
     * @param   imageUrl
     *          image URL to analyze
     *
     * @return  model response JSON string
     */
    public String guessNodeData(String imageUrl) {
        // 1. Download image and convert to Base64
        byte[] imageBytes = downloadImage(imageUrl);
        // Standardize to JPEG to ensure model compatibility
        byte[] standardizedBytes = convertToJpeg(imageBytes);
        String base64Image = java.util.Base64.getEncoder().encodeToString(standardizedBytes);

        // 2. Construct Prompt for OCR Model
        // Avoid using literal examples like "TEXT" as small models might copy them.
        String userPrompt = "<image>\n" +
                "Carefully analyze this image:\n" +
                "1. Extract all readable text.\n" +
                "2. Identify the primary subject or object.\n" +
                "3. Estimate your confidence (0.0 to 1.0).\n" +
                "Respond with valid JSON only. Structure: {\"extracted_text\": string, \"object\": string, \"probability\": number}";

        if (groqClient != null)
        {
            return groqClient.visionCompletion(userPrompt, base64Image, ocrModel);
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.setBearerAuth(apiKey);
            }

            ResponseEntity<Map> response;
            if ("ollama".equalsIgnoreCase(provider)) {
                // Use Ollama /api/generate with "images" field
                String url = getOllamaNativeUrl("api/generate");
                Map<String, Object> body = new HashMap<>();
                body.put("model", ocrModel);
                body.put("prompt", userPrompt);
                body.put("stream", false);
                body.put("format", "json");
                body.put("temperature", 0.0);
                body.put("num_predict", 256);
                // Ollama expects images as a list of Base64 strings
                body.put("images", Collections.singletonList(base64Image));

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                response = restTemplate.postForEntity(url, entity, Map.class);
                return extractTextFromResponse(response.getBody());
            } else {
                // Use OpenAI Vision format (chat completions)
                String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
                
                Map<String, Object> textContent = new HashMap<>();
                textContent.put("type", "text");
                textContent.put("text", userPrompt);

                Map<String, Object> imageContent = new HashMap<>();
                imageContent.put("type", "image_url");
                Map<String, String> imageUrlMap = new HashMap<>();
                imageUrlMap.put("url", "data:image/jpeg;base64," + base64Image); // Assuming JPEG for now, or just send URL if model supports
                imageContent.put("image_url", imageUrlMap);

                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", java.util.Arrays.asList(textContent, imageContent));

                Map<String, Object> body = new HashMap<>();
                body.put("model", ocrModel); // Assumes ocrModel is set to something OpenAI compatible (e.g., gpt-4-vision-preview)
                body.put("messages", Collections.singletonList(message));
                body.put("max_tokens", 300);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                response = restTemplate.postForEntity(url, entity, Map.class);
                return extractTextFromResponse(response.getBody());
            }

        } catch (Exception e) {
            LOG.error("Failed to perform OCR prediction", e);
            throw new RuntimeException("AI Service Error: " + e.getMessage());
        }
    }
    
    /**
     * Predicts semantic similarity probability between two objects.
     *
     * @param   obj1
     *          first object label
     * @param   obj2
     *          second object label
     *
     * @return  provider response JSON string containing similarity score
     */
    public String guessSimilarityProbability(String obj1, String obj2) {
        String prompt = String.format(
                "Determine the semantic similarity probability between \"%s\" and \"%s\" as a float number between 0.0 and 1.0. " +
                "Respond with a JSON object: {\"similarity\": <number>}. Only return the JSON.",
                obj1, obj2
        );
        
        if (groqClient != null)
        {
            return groqClient.chatCompletion(prompt);
        }

        // Use text model for this
         try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            if (apiKey != null && !apiKey.isEmpty()) {
                headers.setBearerAuth(apiKey);
            }
            
            ResponseEntity<Map> response;
             if ("ollama".equalsIgnoreCase(provider)) {
                 String url = getOllamaNativeUrl("api/generate");
                 Map<String, Object> body = new HashMap<>();
                 body.put("model", model);
                 body.put("prompt", prompt);
                 body.put("stream", false);
                 body.put("format", "json");

                 HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                 response = restTemplate.postForEntity(url, entity, Map.class);
                 return extractTextFromResponse(response.getBody());
             } else {
                 // OpenAI Chat
                  String url = baseUrl.endsWith("/") ? baseUrl + "chat/completions" : baseUrl + "/chat/completions";
                Map<String, Object> message = new HashMap<>();
                message.put("role", "user");
                message.put("content", prompt);

                Map<String, Object> body = new HashMap<>();
                body.put("model", model);
                body.put("messages", Collections.singletonList(message));
                
                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                 response = restTemplate.postForEntity(url, entity, Map.class);
                 return extractTextFromResponse(response.getBody());
             }

        } catch (Exception e) {
            LOG.error("Similarity prediction failed", e);
            throw new RuntimeException("AI Service Error: " + e.getMessage());
        }
    }
}
