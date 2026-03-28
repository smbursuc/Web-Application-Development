import * as React from "react";
import { Box, TextField, Button, FormControlLabel, Radio, RadioGroup, Alert, Typography } from "@mui/material";
import API_BASE_URL from '../../config';

export default function ClusterForm(props) {
  const { initial = {}, onSubmit, onCancel, mode = "add", candidates = [] } = props;

  // Formats raw server/Groq error messages into something human-readable
  const formatError = (msg) => {
    if (!msg) return msg;
    try {
      const jsonMatch = msg.match(/\{[\s\S]*\}/);
      if (jsonMatch) {
        const parsed = JSON.parse(jsonMatch[0].replace(/<EOL>/g, '').trim());
        if (parsed.error?.message) return parsed.error.message;
      }
    } catch (_) {}
    return msg
      .replace(/^Server error \d+: Error processing image: /, '')
      .replace(/^Error processing image: /, '')
      .replace(/^Groq vision completion failed: /, '')
      .replace(/^Groq chat completion failed: /, '')
      .replace(/^LLM prediction failed: /, '')
      .replace(/<EOL>$/, '')
      .trim();
  };

  // ── Top-level option selection (only for "add" mode) ──
  // "new-cluster" | "add-to-existing" | "use-ai" | ""
  const [option, setOption] = React.useState("");

  // AI sub-option: "fit" | "auto-create"
  const [aiMode, setAiMode] = React.useState("fit");

  // Common fields
  const [nodeName, setNodeName]       = React.useState(initial.nodeName || initial.name || "");
  const [clusterName, setClusterName] = React.useState(initial.clusterName || initial.parent || "");
  const [probability, setProbability] = React.useState(initial.probability || initial.Probability || "");
  const [uri, setUri]                 = React.useState(initial.uri || initial.URI || "");

  // Update-by-ID field (update mode only)
  const [id, setId] = React.useState(initial.id || "");

  // AI state
  const [loading, setLoading]                   = React.useState(false);
  const [error, setError]                       = React.useState("");
  const [statusMessage, setStatusMessage]       = React.useState("");
  const [rateLimitRemaining, setRateLimitRemaining] = React.useState(null);
  const [localFile, setLocalFile]               = React.useState(null);
  const [clusterTag, setClusterTag]             = React.useState("");
  const [aiResolved, setAiResolved]             = React.useState(false); // true after guess completes

  React.useEffect(() => {
    fetch(`${API_BASE_URL}/api/prediction/rate-limit/status`)
      .then(r => r.json())
      .then(data => setRateLimitRemaining(data.remaining))
      .catch(() => {});
  }, []);

  const resetForm = () => {
    setOption("");
    setAiMode("fit");
    setNodeName("");
    setClusterName("");
    setProbability("");
    setUri("");
    setError("");
    setStatusMessage("");
    setAiResolved(false);
    setClusterTag("");
    setLocalFile(null);
  };

  const handleOptionChange = (e) => {
    const val = e.target.value;
    if (option === val) {
      // untick = reset
      resetForm();
    } else {
      setOption(val);
      setError("");
      setStatusMessage("");
      setAiResolved(false);
    }
  };

  const handleLocalFileChange = (e) => {
    const file = e.target.files[0];
    if (!file) return;
    setLocalFile(file);
    const reader = new FileReader();
    reader.onload = (ev) => {
      const img = new Image();
      img.onload = () => {
        const MAX_PX = 800;
        const scale = Math.min(1, MAX_PX / Math.max(img.width, img.height));
        const canvas = document.createElement('canvas');
        canvas.width = Math.round(img.width * scale);
        canvas.height = Math.round(img.height * scale);
        canvas.getContext('2d').drawImage(img, 0, 0, canvas.width, canvas.height);
        setUri(canvas.toDataURL('image/jpeg', 0.7));
      };
      img.src = ev.target.result;
    };
    reader.readAsDataURL(file);
  };

  const handleGuess = async () => {
    if (!uri) { setError("Image URL is required for AI guessing."); return; }
    if (aiMode === "fit" && candidates.length === 0) {
      setError("No clusters exist yet. Use 'Guess probability and automatically create cluster' instead.");
      return;
    }
    setLoading(true);
    setError("");
    setStatusMessage("Waiting for AI to guess…");
    try {
      const resp = await fetch(`${API_BASE_URL}/api/prediction/node/guess`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ uri }),
      });
      const remaining = resp.headers.get("X-RateLimit-Remaining");
      if (remaining !== null) setRateLimitRemaining(Number(remaining));
      if (resp.status === 429) throw new Error(await resp.text());
      if (!resp.ok) throw new Error("Server error " + resp.status + ": " + await resp.text());

      const dataStr = await resp.text();
      let json = {};
      try { json = JSON.parse(dataStr); } catch (_) {
        const m = dataStr.match(/\{[\s\S]*\}/);
        if (m) try { json = JSON.parse(m[0]); } catch (_2) {}
      }

      if (json.object) setNodeName(json.object);
      if (json.cluster_tag) setClusterTag(json.cluster_tag);

      // Predict cluster assignment (for both modes if there are candidates)
      if (candidates.length > 0) {
        const clusterResp = await fetch(`${API_BASE_URL}/api/prediction/cluster/select-with-confidence`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ object1: json.object || "", candidates }),
        });
        const clusterRemaining = clusterResp.headers.get("X-RateLimit-Remaining");
        if (clusterRemaining !== null) setRateLimitRemaining(Number(clusterRemaining));

        if (clusterResp.ok) {
          const predRaw = await clusterResp.text();
          let clusterJson = null;
          try { clusterJson = JSON.parse(predRaw); } catch (_) {
            const m = predRaw.match(/\{[\s\S]*\}/); if (m) try { clusterJson = JSON.parse(m[0]); } catch (_2) {}
          }
          const predictedCluster = typeof clusterJson?.cluster === "string" ? clusterJson.cluster : predRaw;
          const conf = Number(clusterJson?.confidence);
          if (!Number.isNaN(conf)) setProbability(Math.max(0, Math.min(1, conf)).toFixed(4));

          const clean = predictedCluster.replace(/['"\[\]]+/g, '').trim();
          const textLow = clean.toLowerCase();
          let matched = candidates.find(c => c.toLowerCase() === textLow);
          if (!matched) {
            const sorted = [...candidates].sort((a, b) => b.length - a.length);
            matched = sorted.find(c => textLow.includes(c.toLowerCase()));
          }

          if (matched) {
            setClusterName(matched);
          } else if (clean.toLowerCase() === "root") {
            setClusterName("Root");
          } else {
            // No match in candidates
            if (aiMode === "auto-create") {
              // Use AI-suggested cluster_tag as the new cluster name
              const newCluster = json.cluster_tag || clean;
              setClusterName(newCluster);
              setStatusMessage(`AI suggests a new cluster: "${newCluster}". Press Add to create it along with this object.`);
            } else {
              // fit mode: fall back to cluster_tag but warn
              if (json.cluster_tag) setClusterName(json.cluster_tag);
              setStatusMessage(`No matching cluster found. Suggested: "${json.cluster_tag || clean}". Edit before adding.`);
            }
          }
        }
      } else if (aiMode === "auto-create") {
        // No candidates at all — use cluster_tag as new cluster name
        const newCluster = json.cluster_tag || (json.object || "New Cluster");
        setClusterName(newCluster);
        setStatusMessage(`AI suggests a new cluster: "${newCluster}". Press Add to create it along with this object.`);
      }

      if (json.probability && !probability) setProbability(json.probability);
      setAiResolved(true);
      if (!statusMessage || statusMessage.startsWith("Waiting")) setStatusMessage("Guess complete.");
    } catch (err) {
      setError(err.message);
      setStatusMessage("");
    } finally {
      setLoading(false);
    }
  };

  const submit = async (e) => {
    e.preventDefault();
    setError("");

    // ── UPDATE mode ──
    if (mode === "update") {
      const payload = id ? { id: Number(id), probability: probability !== "" ? Number(probability) : undefined }
                         : { name: nodeName, parent: clusterName, probability: probability !== "" ? Number(probability) : undefined };
      onSubmit(payload);
      return;
    }

    // ── DELETE mode ──
    if (mode === "delete") {
      onSubmit({ name: nodeName, parent: clusterName });
      return;
    }

    // ── ADD mode ──
    if (option === "new-cluster") {
      onSubmit({ name: clusterName, parent: "Root" });
      return;
    }

    if (option === "add-to-existing") {
      let storedUri = uri;
      if (uri.startsWith("data:")) {
        setLoading(true);
        setStatusMessage("Uploading image…");
        try {
          const blob = await fetch(uri).then(r => r.blob());
          const fd = new FormData();
          fd.append("image", blob, localFile?.name || "upload.jpg");
          const up = await fetch(`${API_BASE_URL}/api/upload-image`, { method: "POST", body: fd });
          if (!up.ok) throw new Error(`Upload failed: ${up.status}`);
          storedUri = await up.text();
        } catch (err) { setError(`Image upload failed: ${err.message}`); setLoading(false); setStatusMessage(""); return; }
        setLoading(false);
        setStatusMessage("");
      }
      const payload = { name: nodeName, parent: clusterName };
      if (probability !== "") payload.probability = Number(probability);
      if (storedUri) payload.uri = storedUri;
      onSubmit(payload);
      return;
    }

    if (option === "use-ai") {
      if (!aiResolved) { setError("Please run the AI guess first."); return; }

      let storedUri = uri;
      if (uri.startsWith("data:")) {
        setLoading(true); setStatusMessage("Uploading image…");
        try {
          const blob = await fetch(uri).then(r => r.blob());
          const fd = new FormData();
          fd.append("image", blob, localFile?.name || "upload.jpg");
          const up = await fetch(`${API_BASE_URL}/api/upload-image`, { method: "POST", body: fd });
          if (!up.ok) throw new Error(`Upload failed: ${up.status}`);
          storedUri = await up.text();
        } catch (err) { setError(`Image upload failed: ${err.message}`); setLoading(false); setStatusMessage(""); return; }
        setLoading(false); setStatusMessage("");
      }

      const isNewCluster = aiMode === "auto-create" && !candidates.includes(clusterName);

      if (isNewCluster) {
        // Send hierarchical payload: cluster + leaf in one create request
        onSubmit({
          name: clusterName,
          parent: "Root",
          children: [{
            name: nodeName,
            Probability: probability !== "" ? Number(probability) : undefined,
            URI: storedUri || undefined,
          }],
          clusterTag,
        });
      } else {
        const payload = { name: nodeName, parent: clusterName };
        if (probability !== "") payload.probability = Number(probability);
        if (storedUri) payload.uri = storedUri;
        if (clusterTag) payload.clusterTag = clusterTag;
        onSubmit(payload);
      }
    }
  };

  const isAddMode = mode === "add";

  return (
    <Box
      component="form"
      onSubmit={submit}
      sx={{
        display: "flex", flexDirection: "column", gap: 2,
        width: { xs: '92vw', sm: 480 }, maxHeight: '85vh', overflowY: 'auto',
        bgcolor: "background.paper", p: 3, borderRadius: 2,
      }}
    >
      <Typography variant="h6">
        {mode === "delete" ? "Delete Cluster Node" : mode === "update" ? "Update Cluster Node" : "Add Cluster Node"}
      </Typography>

      {error && <Alert severity="error">{formatError(error)}</Alert>}
      {!error && statusMessage && (
        <Alert severity={statusMessage.startsWith("Waiting") ? "info" : "success"}>{statusMessage}</Alert>
      )}

      {/* ── UPDATE mode ── */}
      {mode === "update" && (
        <>
          <TextField label="Node ID (leave empty to use name + parent)" value={id}
            onChange={e => setId(e.target.value)} type="number" />
          {!id && (
            <>
              <TextField label="Object Name" value={nodeName} onChange={e => setNodeName(e.target.value)} required />
              <TextField label="Cluster Name (parent)" value={clusterName} onChange={e => setClusterName(e.target.value)} />
            </>
          )}
          <TextField label="Probability" value={probability} onChange={e => setProbability(e.target.value)}
            type="number" inputProps={{ step: "0.01", min: "0", max: "1" }} />
        </>
      )}

      {/* ── DELETE mode ── */}
      {mode === "delete" && (
        <>
          <TextField label="Object Name" value={nodeName} onChange={e => setNodeName(e.target.value)} required />
          <TextField label="Cluster Name (parent)" value={clusterName} onChange={e => setClusterName(e.target.value)} required />
        </>
      )}

      {/* ── ADD mode ── */}
      {isAddMode && (
        <>
          {/* Option selector — acts as a mutually exclusive radio group */}
          <RadioGroup value={option} onChange={handleOptionChange}>
            <FormControlLabel value="new-cluster" control={<Radio />} label="Add a new cluster" />
            <FormControlLabel value="add-to-existing" control={<Radio />} label="Add object to existing cluster" />
            <FormControlLabel value="use-ai" control={<Radio />} label="Add object using AI" />
          </RadioGroup>

          {/* ── New cluster ── */}
          {option === "new-cluster" && (
            <TextField label="Cluster name" value={clusterName} onChange={e => setClusterName(e.target.value)} required />
          )}

          {/* ── Add to existing ── */}
          {option === "add-to-existing" && (
            <>
              <TextField label="Object name" value={nodeName} onChange={e => setNodeName(e.target.value)} required />
              <TextField label="Cluster name (parent)" value={clusterName} onChange={e => setClusterName(e.target.value)} required
                error={!clusterName} helperText={!clusterName ? "Parent cluster name is required" : ""} />
              <TextField label="Probability" value={probability} onChange={e => setProbability(e.target.value)}
                type="number" inputProps={{ step: "0.01", min: "0", max: "1" }} />
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <TextField label="URI (Image URL)" value={localFile ? localFile.name : uri}
                  onChange={e => { setUri(e.target.value); setLocalFile(null); }} fullWidth
                  disabled={!!localFile} />
              </Box>
              <Button variant="outlined" component="label" size="small">
                {localFile ? `File: ${localFile.name}` : "Upload local image"}
                <input type="file" accept="image/*" hidden onChange={handleLocalFileChange} />
              </Button>
            </>
          )}

          {/* ── AI mode ── */}
          {option === "use-ai" && (
            <>
              {rateLimitRemaining !== null && (
                <Alert severity={rateLimitRemaining === 0 ? "warning" : "info"}>
                  {rateLimitRemaining === 0
                    ? "AI guess quota reached. Please wait for the limit to reset."
                    : `AI guesses remaining: ${rateLimitRemaining}`}
                </Alert>
              )}
              <RadioGroup value={aiMode} onChange={e => { setAiMode(e.target.value); setAiResolved(false); setStatusMessage(""); }}>
                <FormControlLabel value="fit" control={<Radio size="small" />}
                  label="Guess probability and fit into existing clusters" />
                <FormControlLabel value="auto-create" control={<Radio size="small" />}
                  label="Guess probability and automatically create cluster if necessary" />
              </RadioGroup>
              {aiMode === "auto-create" && (
                <Typography variant="body2" color="text.secondary">
                  If the AI does not agree with any existing cluster, it will
                  suggest a new one. Pressing Add will create the new cluster
                  and add this object to it in one step.
                </Typography>
              )}

              {/* Image input */}
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
                <TextField label="Image URL (or pick a file below)"
                  value={localFile ? localFile.name : uri}
                  onChange={e => { setUri(e.target.value); setLocalFile(null); }}
                  fullWidth required disabled={!!localFile} />
                <Button variant="contained" onClick={handleGuess} disabled={loading}>
                  {loading ? "Thinking…" : "Guess"}
                </Button>
              </Box>
              <Button variant="outlined" component="label" size="small">
                {localFile ? `File: ${localFile.name}` : "Upload local image"}
                <input type="file" accept="image/*" hidden onChange={handleLocalFileChange} />
              </Button>

              {/* Show resolved fields after guess */}
              {aiResolved && (
                <>
                  <TextField label="Object name" value={nodeName} onChange={e => setNodeName(e.target.value)} />
                  <TextField label="Cluster name" value={clusterName} onChange={e => setClusterName(e.target.value)} />
                  <TextField label="Probability" value={probability} onChange={e => setProbability(e.target.value)}
                    type="number" inputProps={{ step: "0.0001", min: "0", max: "1" }} />
                </>
              )}
            </>
          )}
        </>
      )}

      <Box sx={{ display: "flex", gap: 1, position: 'sticky', bottom: 0, bgcolor: 'background.paper', pt: 1 }}>
        <Button type="submit" variant="contained">
          {mode === "delete" ? "Delete" : mode === "update" ? "Update" : "Add"}
        </Button>
        <Button variant="outlined" onClick={onCancel}>Cancel</Button>
      </Box>
    </Box>
  );
}
