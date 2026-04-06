# Convergent Iteration Methodology

**Version 1.0.0**

> **Note:**
> This methodology is designed for AI agents conducting systematic simplification of codebases. It enforces a strict design-before-implementation workflow and prioritizes deletion over addition.

---

## Abstract

A structured approach for conducting "convergence-focused" iterations on codebases. The goal is to reduce complexity, eliminate redundancy, and create smaller, more maintainable systems while preserving core functionality. All changes must be justified through first-principles thinking and documented before implementation.

---

## Core Principles

### 1. Deletion Over Addition
- Remove unnecessary code before adding new code
- Every addition must prove its current necessity
- "Future-proofing" is not a valid reason to keep code

### 2. Convergence Over Extension
- Merge duplicate concepts
- Consolidate scattered logic
- Reduce module count and file count
- Eliminate unnecessary abstraction layers

### 3. Current Over Future
- Serve present requirements only
- Remove "might need later" code
- Delete speculative features
- Strip unused configuration options

### 4. Explicit Over Implicit
- Make all behaviors visible
- Remove hidden state
- Eliminate magic behaviors
- Prefer clear, direct code

### 5. Minimal Over Flexible
- Prefer small, verifiable systems
- Remove unnecessary extensibility points
- Delete unused plugin/extension mechanisms
- Reduce configuration surface

---

## Execution Steps (Strict Order)

### Phase 1: Design (MANDATORY - No Code Changes Yet)

**Step 1.1: Analyze Current State**
- Map codebase structure
- Identify dependencies between modules
- Find duplicate/redundant code
- Locate over-engineered abstractions
- Discover unused code paths

**Step 1.2: Identify Problems**
Document issues found:
- Redundancy: Duplicate logic, repeated patterns
- Forks: Multiple ways to do the same thing
- Over-abstraction: Unnecessary layers, premature generalization
- Unused code: Dead paths, unused exports, deprecated features
- Configuration bloat: Options that serve no current purpose
- Documentation drift: Docs that don't match reality

**Step 1.3: Design Strategy**
Define the simplification approach:
- What to delete
- What to merge
- What to consolidate
- What to inline

**Step 1.4: Create Design Document**
Create file: `docs/turn_N.md`

Required content:
```markdown
# Turn N: Convergent Iteration Design

## Current Problems
- [List specific issues identified]

## Deletion Strategy
- [What will be removed and why]

## Consolidation Strategy
- [What will be merged and why]

## Minimal Viable System Definition
- [What must remain for system to work]

## Files/Modules Affected
- Delete: [list]
- Merge: [list]
- Modify: [list]

## Target Structure
- [Brief description of end state]
```

### Phase 2: Implementation

**Step 2.1: Execute Changes**
- Make changes strictly according to design document
- No changes allowed that aren't documented
- If new issues found, update design document first

**Step 2.2: Generate Diff**
```bash
git diff > docs/turn_N.patch
```

### Phase 3: Summary

**Step 3.1: Document Results**
Create summary including:
- What non-essential complexity was removed
- What structures were consolidated
- Why deletions were justified by first principles
- How error surface was reduced

---

## Decision Framework

### When in Doubt, Delete

Ask these questions before keeping any code:

1. **Is it used right now?** If no, delete.
2. **Does it serve a documented requirement?** If no, delete.
3. **Is there another way to achieve the same result?** If yes, keep the simpler one.
4. **Would the system break without it?** If no, delete.
5. **Is it the simplest solution?** If no, simplify or delete.

### What to Delete

- Unused functions, classes, modules
- Duplicate implementations
- Over-abstracted interfaces
- Speculative features
- Unused configuration options
- Decorative documentation
- Unused dependencies
- Dead code paths
- Unused exports
- Premature optimizations

### What to Keep

- Core business logic
- Essential data flow
- Required integrations
- Active user-facing features
- Necessary error handling
- Required security measures

---

## Anti-Patterns to Avoid

### During Design Phase
- ❌ Skipping analysis and jumping to conclusions
- ❌ Keeping code "just in case"
- ❌ Designing without understanding dependencies
- ❌ Not documenting rationale

### During Implementation Phase
- ❌ Making changes not in design document
- ❌ Adding new abstractions while simplifying
- ❌ Keeping "flexibility" for future use
- ❌ Not generating diff file

### During Summary Phase
- ❌ Vague descriptions of changes
- ❌ Not explaining why deletions were safe
- ❌ Not confirming minimal system still works

---

## Completion Checklist

- [ ] Design document created at `docs/turn_N.md`
- [ ] All required sections present in design document
- [ ] Code changes match design document exactly
- [ ] Diff file generated at `docs/turn_N.patch`
- [ ] Summary provided explaining:
  - [ ] What was removed and why
  - [ ] How error surface was reduced
  - [ ] Confirmation of design-to-implementation alignment

---

## Example Usage

```
Start convergent iteration turn 1 for this codebase.
Focus on reducing module count and eliminating unused code.
```

The AI agent will then:
1. Analyze the codebase
2. Create `docs/turn_1.md` with design
3. Execute changes according to design
4. Generate `docs/turn_1.patch`
5. Provide summary of changes
