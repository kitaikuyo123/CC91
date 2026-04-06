# Convergent Iteration Skill

A structured methodology for conducting "convergence-focused" iterations on codebases.

## Purpose

This skill guides AI agents through a disciplined process of simplifying codebases by:
- Removing unnecessary complexity, redundancy, and over-engineering
- Preserving only the minimal viable system
- Reducing error surface and maintenance burden
- Enforcing design-before-implementation workflow

## Core Principles

1. **Deletion over Addition** - Remove first, add only when proven necessary
2. **Convergence over Extension** - Merge, consolidate, simplify
3. **Current over Future** - Serve present needs, not hypothetical future ones
4. **Explicit over Implicit** - Make behaviors visible and predictable
5. **Minimal over Flexible** - Prefer small, verifiable systems

## Workflow

### Phase 1: Design (Mandatory)

1. Analyze current codebase structure
2. Identify problems: redundancy, forks, over-abstraction, unused code
3. Design simplification strategy
4. Create design document: `docs/turn_N.md`
5. Document must include:
   - Current problems identified
   - Deletion/consolidation strategy
   - Minimal viable system definition
   - Files/modules to be removed or merged
   - Target structure after iteration

### Phase 2: Implementation

1. Execute changes strictly according to design document
2. No changes allowed that aren't documented
3. Generate diff and save to `docs/turn_N.patch`

### Phase 3: Summary

1. Document what was removed and why
2. Explain how error surface was reduced
3. Confirm design-to-implementation alignment

## Usage

```
Start convergent iteration turn N for this codebase.
Focus on [specific area/concern].
```

## Files

- `AGENTS.md` - Complete methodology for AI agents
- `rules/` - Individual rules for specific aspects
