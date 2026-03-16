import * as React from "react";
import { Box, TextField, Button, FormControlLabel, Checkbox } from "@mui/material";

export default function HeatmapForm(props) {
  const { initial = {}, onSubmit, onCancel, mode } = props;
  const [object1, setObject1] = React.useState(initial.object1 || "");
  const [object2, setObject2] = React.useState(initial.object2 || "");
  const [similarity, setSimilarity] = React.useState(initial.similarity || "");
  const [guessMode, setGuessMode] = React.useState(false);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState("");

  const handleGuess = async () => {
    if (!object1 || !object2) {
        setError("Both objects are required for guessing.");
        return;
    }
    setLoading(true);
    setError("");
    try {
        const resp = await fetch("http://localhost:8081/api/prediction/similarity/score", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ object1, object2 })
        });
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

    } catch (err) {
        setError(err.message);
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
    <Box component="form" onSubmit={submit} sx={{ display: "flex", flexDirection: "column", gap: 2, width: 360 }}>
      {error && <div style={{color:'red'}}>Error: {error}</div>}
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
