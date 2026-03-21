import * as React from "react";
import { Box, TextField, Button, FormControl, InputLabel, Select, MenuItem, FormControlLabel, Checkbox, Alert } from "@mui/material";
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
  
  // For update by ID
  const [id, setId] = React.useState(initial.id || "");
  
  // For create/add/delete by name+parent
  const [nodeName, setNodeName] = React.useState(initial.nodeName || initial.name || "");
  const [clusterName, setClusterName] = React.useState(initial.clusterName || initial.parent || "");
  const [probability, setProbability] = React.useState(initial.probability || initial.Probability || "");
  const [uri, setUri] = React.useState(initial.uri || initial.URI || "");
  const [isLeaf, setIsLeaf] = React.useState(initial.isLeaf !== undefined ? initial.isLeaf : true);
  
  // New State variables
  const [guessMode, setGuessMode] = React.useState(false);
  const [isRootChild, setIsRootChild] = React.useState(mode !== "update"); // Default true for new, or manual toggle
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");
  const [statusMessage, setStatusMessage] = React.useState("");
  const [rateLimitRemaining, setRateLimitRemaining] = React.useState(null);

  React.useEffect(() => {
    fetch("http://localhost:8081/api/prediction/rate-limit/status")
      .then(r => r.json())
      .then(data => setRateLimitRemaining(data.remaining))
      .catch(() => {});
  }, []);

  const handleGuess = async () => {
    if (!uri) {
      setError("Image URL is required for AI guessing.");
      return;
    }
    if (candidates.length === 0) {
      setError("AI guess cannot assign a parent cluster because no clusters exist yet. Please add at least one cluster node first, then try again.");
      return;
    }
    setLoading(true);
    setError("");
    setStatusMessage("Waiting for AI to guess...");
    try {
      const resp = await fetch(`${API_BASE_URL}/api/prediction/node/guess`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ uri })
      });
      // Read rate-limit remaining from header (available after the first LLM call)
      const remaining = resp.headers.get("X-RateLimit-Remaining");
      if (remaining !== null) setRateLimitRemaining(Number(remaining));
      if (resp.status === 429) {
        throw new Error(await resp.text());
      }
      if (!resp.ok) {
        throw new Error("Server error " + resp.status + ": " + (await resp.text()));
      }
      const dataStr = await resp.text();
      // Try to parse the textual JSON response from the LLM
      let json = {};
      try {
        json = JSON.parse(dataStr);
      } catch (e) {
         // Sometimes LLM returns text with markdown JSON blocks
         const match = dataStr.match(/\{[\s\S]*\}/);
         if (match) {
             try { json = JSON.parse(match[0]); } catch(e2) {}
         }
      }
      
      if (json.object) {
        setNodeName(json.object);
        
        // Secondary guess: Predict cluster (parent)
        console.log("Predicting parent cluster. Candidates:", candidates);
        if (candidates && candidates.length > 0) {
            try {
                const clusterResp = await fetch(`${API_BASE_URL}/api/prediction/cluster/select-with-confidence`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ object1: json.object, candidates })
                });
                const clusterRemaining = clusterResp.headers.get("X-RateLimit-Remaining");
                if (clusterRemaining !== null) setRateLimitRemaining(Number(clusterRemaining));
                if (clusterResp.ok) {
                    const predictionRaw = await clusterResp.text();
                    console.log("Predicted Cluster Raw Response:", predictionRaw);

                    let clusterJson = null;
                    try {
                      clusterJson = JSON.parse(predictionRaw);
                    } catch (parseErr) {
                      const match = predictionRaw.match(/\{[\s\S]*\}/);
                      if (match) {
                        try {
                          clusterJson = JSON.parse(match[0]);
                        } catch (_) {
                        }
                      }
                    }

                    const predictedCluster = typeof clusterJson?.cluster === "string"
                      ? clusterJson.cluster
                      : predictionRaw;
                    const clusterConfidence = Number(clusterJson?.confidence);

                    if (!Number.isNaN(clusterConfidence)) {
                      const clampedConfidence = Math.max(0, Math.min(1, clusterConfidence));
                      setProbability(clampedConfidence.toFixed(4));
                    }

                    const cleanCluster = predictedCluster.replace(/['"\[\]]+/g, '').trim();
                    console.log("Cleaned Cluster Name:", cleanCluster);
                    
                    // Normalize text for fuzzy search
                    const textToSearch = cleanCluster.toLowerCase();

                    // Robust matching: Check if response 'includes' candidate, prioritizing exact match, then longest match
                    let matched = candidates.find(c => c.toLowerCase() === textToSearch);
                    
                    if (!matched) {
                        // If no exact match, try to find a candidate that appears in the response text
                        // Sort candidates by length descending to match specific over general
                        const sortedCandidates = [...candidates].sort((a, b) => b.length - a.length);
                        matched = sortedCandidates.find(c => textToSearch.includes(c.toLowerCase()));
                        
                        if (!matched) {
                           console.log("Fuzzy match failed. Text to search:", textToSearch);
                           console.log("Candidates keys:", sortedCandidates.map(c => c.toLowerCase()));
                        }
                    }

                    if (matched) {
                         console.log("Match found in candidates:", matched);
                         setClusterName(matched);
                         setIsRootChild(false);
                    } else if (cleanCluster.toLowerCase() === "root") {
                         setIsRootChild(true);
                         setClusterName("Root");
                    } else {
                        console.warn("No match found for predicted cluster in candidates:", cleanCluster);
                    }
                }
                else {
                  const failureText = await clusterResp.text();
                  setError("Cluster prediction failed: " + failureText);
                }
            } catch (ce) {
                console.error("Failed to predict cluster:", ce);
                setError("Cluster prediction failed: " + ce.message);
            }
        } else {
            console.warn("No candidates available for cluster prediction.");
        }
      }
      if (json.probability) setProbability(json.probability);
      // Turn off guess mode to show the fields again
      setGuessMode(false);
      setStatusMessage("Guess complete.");
      
      // If LLM returned a 'extracted_text' useful debug
      if (json.extracted_text) console.log("OCR Text:", json.extracted_text);

    } catch (err) {
      setError(err.message);
      setStatusMessage("");
    } finally {
      setLoading(false);
    }
  };

  const submit = (e) => {
    e.preventDefault();
    setError("");
    
    // Build payload based on mode
    let payload = {};
    
    if (mode === "update" && id) {
      // Update by ID
      payload = {
        id: Number(id),
      };
      if (isLeaf && probability !== "") {
        payload.probability = Number(probability);
      }
    } else if (mode === "delete") {
      // Delete by name + parent
      payload = {
        name: nodeName,
        parent: clusterName,
      };
    } else {
      // Add/create
      payload = {
        name: nodeName, 
        parent: isRootChild ? "Root" : clusterName,
      };
      if (isLeaf) {
        if (probability !== "") payload.probability = Number(probability);
        if (uri) payload.uri = uri;
      }
    }
    
    onSubmit(payload);
  };

  return (
    <Box component="form" onSubmit={submit} sx={{ display: "flex", flexDirection: "column", gap: 2, width: 480, bgcolor: "background.paper", p: 3, borderRadius: 2 }}>
      <h2>{mode === "delete" ? "Delete Cluster Node" : mode === "update" ? "Update Cluster Node" : "Add Cluster Node"}</h2>
      
      {error && <Alert severity="error" sx={{ mb: 1 }}>{formatError(error)}</Alert>}
      {!error && statusMessage && <Alert severity={statusMessage.startsWith("Waiting") ? "info" : "success"} sx={{ mb: 1 }}>{statusMessage}</Alert>}
      {guessMode && rateLimitRemaining !== null && (
        <Alert severity={rateLimitRemaining === 0 ? "warning" : "info"} sx={{ mb: 1 }}>
          {rateLimitRemaining === 0 ? "AI guess quota reached. Please wait for the limit to reset." : `AI guesses remaining: ${rateLimitRemaining}`}
        </Alert>
      )}

      {mode === "update" && (
        <TextField 
          label="Node ID (optional - leave empty to use name+parent)" 
          value={id} 
          onChange={(e) => setId(e.target.value)} 
          type="number"
        />
      )}
      
      {(!id || mode !== "update") && (
        <>
            {/* If guessing is active, hide Node Name */}
           {!guessMode && (
              <TextField 
                label="Node Name" 
                value={nodeName} 
                onChange={(e) => setNodeName(e.target.value)} 
                required 
              />
           )}
          
          {/* Toggle for Root child (typically for clusters only, not leaves) */}
          {mode !== "delete" && !isLeaf && (
            <FormControlLabel
                control={<Checkbox checked={isRootChild} onChange={(e) => {
                    setIsRootChild(e.target.checked);
                    if (e.target.checked) setClusterName("Root");
                    else setClusterName("");
                }} />}
                label="Is Root Child"
            />
          )}

          {!isRootChild && !guessMode && (
            <TextField 
                label="Cluster Name (Parent)" 
                value={clusterName} 
                onChange={(e) => setClusterName(e.target.value)} 
                required 
                disabled={isRootChild}
                error={!clusterName && !isRootChild}
                helperText={!clusterName && !isRootChild ? "Parent cluster name is required" : ""}
            />
          )}
        </>
      )}
      
      {mode !== "delete" && (
        <>
          <FormControlLabel
            control={<Checkbox checked={isLeaf} onChange={(e) => {
                const checked = e.target.checked;
                setIsLeaf(checked);
                if (!checked) {
                    setGuessMode(false);
                } else {
                    // Leaf nodes cannot be root children
                    setIsRootChild(false);
                    if (clusterName === "Root") setClusterName("");
                }
            }} />}
            label="Is leaf node"
          />

          {isLeaf && (
             <FormControlLabel
                control={<Checkbox checked={guessMode} onChange={(e) => setGuessMode(e.target.checked)} />}
                label="Guess cluster and probability (AI)"
             />
          )}

          {/* Remove the fields completely when the checkbox is checked */}
          {isLeaf && (
            <>
              {/* If NOT in guess mode, show Probability */}
              {!guessMode && (
                  <TextField 
                    label="Probability" 
                    value={probability} 
                    onChange={(e) => setProbability(e.target.value)} 
                    type="number" 
                    inputProps={{ step: "0.01", min: "0", max: "1" }}
                  />
              )}

              {mode !== "update" && (
                <Box sx={{display: 'flex', gap: 1}}>
                    <TextField 
                      label="URI (Image URL)" 
                      value={uri} 
                      onChange={(e) => setUri(e.target.value)} 
                      fullWidth
                      required={guessMode}
                    />
                    {guessMode && (
                        <Button variant="contained" onClick={handleGuess} disabled={loading}>
                            {loading ? "Thinking..." : "Guess"}
                        </Button>
                    )}
                </Box>
              )}
            </>
          )}
        </>
      )}

      <Box sx={{ display: "flex", gap: 1 }}>

        <Button type="submit" variant="contained">
          {mode === "delete" ? "Delete" : mode === "update" ? "Update" : "Add"}
        </Button>
        <Button variant="outlined" onClick={onCancel}>Cancel</Button>
      </Box>
    </Box>
  );
}
