#!/usr/bin/env python3
"""A tiny OpenAI-compatible server for testing UTubeSummary without a real API key.

Run:   python3 tools/mock_llm_server.py [port]        (default port 8080)
Phone: Settings > AI provider > Custom Provider, URL http://<computer-ip>:8080 , no key.

Put a magic word anywhere in the content you summarize (or in a question) to get that behaviour:
  MOCK_401      incorrect API key            MOCK_429      rate limit
  MOCK_404MODEL model not found              MOCK_CONTEXT  context length exceeded
  MOCK_500      server error                 MOCK_SLOW     answer after 20 s (cancel / replace tests)
  MOCK_TABLE    answer with a Markdown table MOCK_LONG     a very long answer (text to speech, scrolling)
Anything else gets a normal answer. The answer says which profile prompt, language and length
instruction the app sent, so profile / language / length cases can be checked by reading it.
Each request is also printed here (first 300 characters of the system prompt and of the content).
Only the Python standard library is used.
"""
import json
import sys
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

MODELS = ["mock-small", "mock-large"]
SLOW_SECONDS = 20
counter = 0

ERRORS = {
    "MOCK_401": (401, "invalid_api_key", "Incorrect API key provided: sk-mock. You can find your API key at https://platform.openai.com/account/api-keys."),
    "MOCK_429": (429, "rate_limit_exceeded", "Rate limit reached for requests. Please try again later."),
    "MOCK_404MODEL": (404, "model_not_found", "The model `mock-small` does not exist or you do not have access to it."),
    "MOCK_CONTEXT": (400, "context_length_exceeded", "This model's maximum context length is 8192 tokens. However, your messages resulted in 250000 tokens. Please reduce the length of the messages."),
    "MOCK_500": (500, "server_error", "The server had an error while processing your request."),
}


def text_of(message):
    content = message.get("content", "")
    if isinstance(content, list):
        return " ".join(part.get("text", "") for part in content if isinstance(part, dict))
    return content or ""


def first_words(text, n=300):
    return " ".join(text.split())[:n]


def build_answer(system, user, number):
    lower = system.lower()
    language = "not named"
    for marker in ("language", "respond in", "write in"):
        index = lower.find(marker)
        if index >= 0:
            language = first_words(system[max(0, index - 20):index + 80], 100)
            break
    length = "no length rule"
    for word in ("short", "medium", "long", "concise", "detailed"):
        if word in lower:
            length = f"mentions '{word}'"
            break
    lines = [
        f"## Mock answer #{number}",
        "",
        f"**Prompt starts with:** {first_words(system, 160) or '(no system prompt)'}",
        f"**Language rule:** {language}",
        f"**Length rule:** {length}",
        f"**Content starts with:** {first_words(user, 160)}",
        "",
        "- first key point",
        "- second key point",
        "- third key point",
    ]
    if "MOCK_TABLE" in user:
        lines += ["", "| Name | Value |", "|------|-------|", "| alpha | 1 |", "| beta | 2 |"]
    if "MOCK_LONG" in user:
        lines += ["", *[f"Paragraph {i}: " + "This is a long sentence for reading aloud and scrolling. " * 8 for i in range(1, 40)]]
    return "\n".join(lines)


def build_card(system, user, number):
    body = build_answer(system, user, number)
    return json.dumps({
        "title": f"Mock summary #{number}",
        "author": "Mock Author",
        "overview": "A one-sentence overview made by the mock server.",
        "keyPoints": ["First key point", "Second key point", "Third key point"],
        "bodySummary": body,
        "tags": ["mock", "test"],
        "detectedLanguage": "en",
        "errorReason": None,
    })


class Handler(BaseHTTPRequestHandler):
    def log_message(self, fmt, *args):
        pass

    def send_json(self, status, payload):
        data = json.dumps(payload).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def do_GET(self):
        if self.path.rstrip("/").endswith("/models"):
            self.send_json(200, {"object": "list", "data": [{"id": m, "object": "model", "owned_by": "mock"} for m in MODELS]})
        else:
            self.send_json(404, {"error": {"message": "not found: " + self.path}})

    def do_POST(self):
        global counter
        length = int(self.headers.get("Content-Length", "0") or 0)
        request = json.loads(self.rfile.read(length) or b"{}")
        if not self.path.rstrip("/").endswith("/chat/completions"):
            self.send_json(404, {"error": {"message": "not found: " + self.path}})
            return
        counter += 1
        number = counter
        messages = request.get("messages", [])
        system = "\n".join(text_of(m) for m in messages if m.get("role") in ("system", "developer"))
        user = "\n".join(text_of(m) for m in messages if m.get("role") == "user")
        print(f"#{number} model={request.get('model')} structured={'response_format' in request}")
        print(f"   system: {first_words(system)}")
        print(f"   user:   {first_words(user)}", flush=True)

        everything = system + "\n" + user
        for word, (status, code, message) in ERRORS.items():
            if word in everything:
                self.send_json(status, {"error": {"message": message, "type": "invalid_request_error", "code": code}})
                return
        if "MOCK_SLOW" in everything:
            time.sleep(SLOW_SECONDS)

        structured = "response_format" in request
        content = build_card(system, user, number) if structured else build_answer(system, user, number)
        self.send_json(200, {
            "id": f"mock-{number}",
            "object": "chat.completion",
            "created": int(time.time()),
            "model": request.get("model", MODELS[0]),
            "choices": [{"index": 0, "message": {"role": "assistant", "content": content}, "finish_reason": "stop"}],
            "usage": {"prompt_tokens": 100, "completion_tokens": 100, "total_tokens": 200},
        })


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8080
    print(f"Mock LLM server on http://0.0.0.0:{port}  (Ctrl+C to stop)")
    ThreadingHTTPServer(("0.0.0.0", port), Handler).serve_forever()
