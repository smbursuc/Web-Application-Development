import os
import json

from groq import Groq

client = Groq(
    api_key="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"
)

clusters = {
    "Cluster 0": ["knife", "toaster", "spoon"],
    "Cluster 1": ["lab coat", "stethoscope"],
    "Cluster 2": ["helmet", "football"]
}

chat_completion = client.chat.completions.create(
    messages=[
        {
            "role": "user",
            "content": "Reply with only the categories of objects these clusters belong to e.g \"Bathroom, \"Restaurant\": " + json.dumps(clusters)
        }
    ],
    model="llama-3.1-70b-versatile",
)

print(chat_completion.choices[0].message.content)