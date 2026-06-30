# 67-quiz MCP Server

Allows Claude to create and manage quizzes on a 67-quiz instance.

## Setup

### 1. Build

```bash
cd mcp
npm install
npm run build
```

### 2. Add to Claude Desktop

Open `~/Library/Application Support/Claude/claude_desktop_config.json` and add:

```json
{
  "mcpServers": {
    "67-quiz": {
      "command": "node",
      "args": ["/absolute/path/to/67-quiz/mcp/dist/index.js"],
      "env": {
        "QUIZ_BASE_URL": "http://localhost:8080"
      }
    }
  }
}
```

Replace `/absolute/path/to/67-quiz` with the actual path on your machine.

Restart Claude Desktop — the tools will appear automatically.

### 3. Use

Tell Claude:
> "Login to 67-quiz with username X and password Y, then create a quiz about the solar system with 3 questions."

## Available tools

| Tool | Description |
|---|---|
| `login` | Log in with username + password |
| `list_quizzes` | List all quizzes |
| `create_quiz` | Create a new quiz |
| `add_question` | Add a question with options to a quiz |
| `get_quiz` | Get full quiz details including questions |
| `delete_quiz` | Delete a quiz you authored |

## Environment variables

| Variable | Default | Description |
|---|---|---|
| `QUIZ_BASE_URL` | `http://localhost:8080` | Base URL of the 67-quiz backend |
