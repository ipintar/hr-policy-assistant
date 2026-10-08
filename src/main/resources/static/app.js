const form = document.querySelector("#question-form");
const input = document.querySelector("#question");
const submitButton = document.querySelector("#submit-question");
const messages = document.querySelector("#messages");
const examples = document.querySelector("#examples");
const status = document.querySelector("#status");
const newConversationButton = document.querySelector("#new-conversation");

let conversationId = sessionStorage.getItem("policyConversationId");

function appendMessage(role, text, citations = []) {
    const article = document.createElement("article");
    article.className = `message ${role}-message`;

    const label = document.createElement("div");
    label.className = "message-label";
    label.textContent = role === "user" ? "You" : "Policy Assistant";

    const content = document.createElement("p");
    content.textContent = text;

    article.append(label, content);

    if (citations.length > 0) {
        const sources = document.createElement("div");
        sources.className = "sources";

        const sourcesLabel = document.createElement("span");
        sourcesLabel.textContent = "Sources";
        sources.append(sourcesLabel);

        const uniqueSources = new Map();
        citations.forEach((citation) => {
            const key = `${citation.policyId}:${citation.chunkIndex}`;
            uniqueSources.set(key, citation);
        });

        uniqueSources.forEach((citation) => {
            const source = document.createElement("span");
            source.className = "source-chip";
            source.textContent = citation.title;
            source.title = citation.source;
            sources.append(source);
        });

        article.append(sources);
    }

    messages.append(article);
    article.scrollIntoView({ behavior: "smooth", block: "end" });
}

function setLoading(loading) {
    input.disabled = loading;
    submitButton.disabled = loading;
    submitButton.textContent = loading ? "Thinking…" : "Ask";
    status.textContent = loading ? "Searching the policy knowledge base…" : "";
}

async function askQuestion(question) {
    appendMessage("user", question);
    examples.hidden = true;
    setLoading(true);

    try {
        const response = await fetch("/api/policies/ask", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({
                conversationId,
                question
            })
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.message || "The question could not be answered.");
        }

        conversationId = payload.conversationId;
        sessionStorage.setItem("policyConversationId", conversationId);
        appendMessage("assistant", payload.answer, payload.citations);
    } catch (error) {
        appendMessage(
            "assistant",
            error instanceof Error
                ? error.message
                : "The question could not be answered. Please try again."
        );
    } finally {
        setLoading(false);
        input.focus();
    }
}

form.addEventListener("submit", async (event) => {
    event.preventDefault();
    const question = input.value.trim();
    if (!question) {
        return;
    }

    input.value = "";
    await askQuestion(question);
});

input.addEventListener("keydown", (event) => {
    if (event.key === "Enter" && !event.shiftKey) {
        event.preventDefault();
        form.requestSubmit();
    }
});

examples.addEventListener("click", (event) => {
    const button = event.target.closest("[data-question]");
    if (button) {
        input.value = button.dataset.question;
        form.requestSubmit();
    }
});

newConversationButton.addEventListener("click", async () => {
    const previousConversationId = conversationId;
    conversationId = null;
    sessionStorage.removeItem("policyConversationId");

    messages.replaceChildren();
    appendMessage(
        "assistant",
        "A new conversation has started. What would you like to know about company policies?"
    );
    examples.hidden = false;
    input.focus();

    if (previousConversationId) {
        try {
            await fetch(
                `/api/policies/conversations/${encodeURIComponent(previousConversationId)}`,
                { method: "DELETE" }
            );
        } catch {
            // The local conversation is already reset; server cleanup is best-effort.
        }
    }
});
