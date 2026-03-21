import * as React from "react";
import { Box, TextField, Button, FormControlLabel, Checkbox, Alert } from "@mui/material";
import API_BASE_URL from '../../config';

export default function HeatmapForm(props) {
  const { initial = {}, onSubmit, onCancel, mode } = props;
  const [object1, setObject1] = React.useState(initial.object1 || "");
  const [object2, setObject2] = React.useState(initial.object2 || "");
  const [similarity, setSimilarity] = React.useState(initial.similarity || "");
  const [guessMode, setGuessMode] = React.useState(false);
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
      .replace(/^Server error \d+: /, '')
      .replace(/^Groq chat completion failed: /, '')
      .replace(/^LLM prediction failed: /, '')
      .replace(/<EOL>$/, '')
      .trim();
  };

  const handleGuess = async () => {
    if (!object1 || !object2) {
        setError("Both objects are required for guessing.");
        return;
    }
    setLoading(true);
    setError("");
    setStatusMessage("Waiting for AI to guess...");
    try {
        const resp = await fetch(`${API_BASE_URL}/api/prediction/similarity/score`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ object1, object2 })
        });
        const remaining = resp.headers.get("X-RateLimit-Remaining");
        if (remaining !== null) setRateLimitRemaining(Number(remaining));
        if (resp.status === 429) {
            throw new Error(await resp.text());
        }
        if (!resp.ok) {
            throw new Error("Server error " + resp.status + ": " + (await resp.text()));
        }
        const dataStr = await resp.text();
        let json = {};
         try {
            json = JSON.parse(dataStr);
         } catch (e) {
             const match = dataStr.match(/\{[\s\S]*\}/);
             if (match) {
                 try { json = JSON.parse(match[0]); } catch(e2) {}
             }
         }
         
         if (json.similarity !== undefined) {
             setSimilarity(json.similarity);
         } else {
             // Fallback if LLM just returned a number string
             const num = parseFloat(dataStr.replace(/[^\d.]/g,''));
             if (!isNaN(num)) setSimilarity(num);
         }
         setGuessMode(false);
         setStatusMessage("Guess complete.");

    } catch (err) {
        setError(err.message);
        setStatusMessage("");
    } finally {
        setLoading(false);
    }
  };

  const submit = (e) => {
    e.preventDefault();
    if (error) setError("");
    
    // For delete operations we only send object1/object2 (no similarity)
    if (mode === "delete") {
      onSubmit({ object1, object2 });
    } else {
      onSubmit({ object1, object2, similarity });
    }
  };

  return (
    <Box component="form" onSubmit={submit} sx={{ display: "flex", flexDirection: "column", gap: 2, width: 360, bgcolor: "background.paper", p: 3, borderRadius: 2 }}>
      <h2>{mode === "delete" ? "Delete Heatmap Pair" : mode === "update" ? "Update Heatmap Pair" : "Add Heatmap Pair"}</h2>
      {error && <Alert severity="error">{formatError(error)}</Alert>}
      {!error && statusMessage && <Alert severity={statusMessage.startsWith("Waiting") ? "info" : "success"}>{statusMessage}</Alert>}
      {guessMode && rateLimitRemaining !== null && (
        <Alert severity={rateLimitRemaining === 0 ? "warning" : "info"}>
          {rateLimitRemaining === 0 ? "AI guess quota reached. Please wait for the limit to reset." : `AI guesses remaining: ${rateLimitRemaining}`}
        </Alert>
      )}
      <TextField label="Object 1" value={object1} onChange={(e) => setObject1(e.target.value)} required />
      <TextField label="Object 2" value={object2} onChange={(e) => setObject2(e.target.value)} required />
      
      {mode !== "delete" && (
        <>
            <FormControlLabel
                control={<Checkbox checked={guessMode} onChange={(e) => setGuessMode(e.target.checked)} />}
                label="Guess probability (AI)"
            />

            {!guessMode ? (
                <TextField 
                    label="Similarity" 
                    value={similarity} 
                    onChange={(e) => setSimilarity(e.target.value)} 
                    required 
                    fullWidth
                />
            ) : (
                <Button 
                    variant="contained" 
                    onClick={handleGuess} 
                    disabled={!object1 || !object2 || loading}
                    fullWidth
                    sx={{ py: 1.5 }}
                >
                    {loading ? "Guessing..." : "Run AI Prediction"}
                </Button>
            )}
        </>
      )}
      <Box sx={{ display: "flex", gap: 1 }}>
        <Button type="submit" variant="contained">Submit</Button>
        <Button variant="outlined" onClick={onCancel}>Cancel</Button>
      </Box>
    </Box>
  );
}
