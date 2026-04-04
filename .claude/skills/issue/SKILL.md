---
name: issue
description: Create a GitHub issue based on a description
argument-hint: <description>
model: haiku
---

Create a GitHub issue based on the following description: $ARGUMENTS

- If it describes unexpected or broken behavior, create it as a **bug** (add the `bug` label).
- If it describes a new feature or improvement, add the `enhancement` label.
- Write a clear, concise title.
- Write a structured body with relevant sections (e.g. Problem, Expected behavior, Actual behavior, Steps to reproduce, or just a description for features).
- Use `gh issue create` to submit it and return the issue URL.
