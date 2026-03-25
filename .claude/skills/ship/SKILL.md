---
name: ship
description: Commit all changes and push to the remote repository in one step
---

Commit all changes and push to the remote repository in one step.

Follow the standard commit protocol:
1. Run `git status` and `git diff` to review all changes.
2. Stage the relevant modified files (never `.env` or secrets).
3. Write a concise commit message focused on *why* the change was made, using Conventional Commits structure (feat/fix/docs/test/refactor/chore).
4. Commit using a HEREDOC to ensure correct formatting.
5. Push to the current remote branch (`git push`), adding `-u origin <branch>` if no upstream is set yet.
6. Confirm success by reporting the commit hash and push result.
