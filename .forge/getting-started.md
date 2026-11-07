# Getting started with forge

forge setup prints this guide and keeps a copy of it in
.forge/getting-started.md, rewriting the copy whenever the guide changes, so
edits to the copy don't last.

1. Get setup's files onto the default branch: merge the pull request
   forge setup opened, if it opened one.

2. Write a functional spec. In a Claude Code session in this repo, run
   /grill-with-docs, then /forge-pipeline:to-functional-spec, which writes
   docs/specs/<nnn>-<name>/functional-spec.md.

3. Write its technical plan. In a fresh session, run /grill-with-docs, then
   /forge-pipeline:to-technical-plan docs/specs/<nnn>-<name>/. Once the
   folder holds both the spec and the plan, the spec's user stories go into
   this repo's backlog.

4. Merge the functional spec and the technical plan to the default branch.
   Every story's branch starts from there, so a session sees only what's
   merged.

5. Run forge work. It claims the next user story, or resumes the one in
   progress, puts the checkout on that story's branch, and starts a session
   with the story in its prompt. It refuses a tree with uncommitted changes,
   and while a story is in review it only names that story's pull request.

6. The session opens the story's pull request through forge gate, and the
   story moves to in review. You review and merge it: merging moves the story
   to done, and closing the pull request unmerged moves it back to in
   progress. Then run forge work again for the next story.

Checking diagrams:

  forge diagrams checks every Mermaid diagram in the repo's Markdown: that
  it renders as GitHub renders it, and that a functional spec's or technical
  plan's diagrams follow the pipeline's rules. If setup didn't declare it,
  add this line to the checks in .forge/checks.json, so forge check and
  forge gate run it:

    { "name": "diagrams", "run": "forge diagrams" }

Other commands:

  forge backlog list      the backlog, grouped by epic
  forge backlog show      the story for the current branch
  forge backlog unclaim   put the story in progress back to todo
  forge check             run the project checks on their own
  forge scan              run the scanners on their own
  forge conventions       print the conventions every session receives
