import Groq from "groq-sdk";

const groq = new Groq({
  apiKey: "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",
});

export async function queryGrok(prompt) {
  const chatCompletion = await getGroqChatCompletion(prompt);
  // Print the completion returned by the LLM.
  return chatCompletion.choices[0]?.message?.content || "";
}

export async function getGroqChatCompletion(prompt) {
  return groq.chat.completions.create({
    messages: [
      {
        role: "user",
        content: prompt,
      },
    ],
    model: "llama-3.3-70b-versatile",
  });
}
