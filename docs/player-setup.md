# Player Setup Screen

## Overview

The landing screen lets users configure a game before it starts. It currently handles two setup steps:

1. **Choose the number of players** (3, 4, or 5)
2. **Enter each player's name**

## How it works

### Player count selection

Three `FilterChip` buttons (labeled 3, 4, 5) let the user pick the number of players. The selected chip is highlighted. The default is **3 players**.

### Player name inputs

Below the chips, one `OutlinedTextField` is rendered per player (e.g. "Player 1", "Player 2", …). The number of fields updates instantly when the chip selection changes:

- Switching from 3 → 5 adds two new empty fields.
- Switching from 5 → 3 removes the last two fields (and any names typed in them).
- Names already typed in the remaining fields are **preserved** when the count changes.
