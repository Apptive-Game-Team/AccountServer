# Agent Workflow

Create or identify a GitHub issue before starting tracked work. Use a dedicated branch named `<issue-label>/<issue-number>`, such as `documentation/34`, and open a pull request after verification.

Every issue and pull request must set an assignee and a label. Do not leave either blank.

- Assignee: `--assignee @me`.
- Label: use the same value as the branch prefix. Check available labels with `gh label list`; do not invent a new label when none fit.
- Do not attach a project.
- When GitHub CLI authentication appears invalid inside a sandbox but the user says their session is valid, request escalated execution and retry `gh` with the user's session credentials before asking them to re-authenticate.

```bash
gh issue create --title "..." --body "..." --assignee @me --label documentation
gh pr create --base <base> --title "..." --body "..." --assignee @me --label documentation
```

Confirm the metadata after creation:

```bash
gh issue view <issue-number> --json assignees,labels
gh pr view <pr-number> --json assignees,labels
```

## Versioning

`version` in `build.gradle` is the account server's single version source.
Update it in every runtime-behavior change: PATCH for backward-compatible fixes
and internal changes, MINOR for backward-compatible features, and MAJOR for
breaking API or protocol changes. Do not bump for documentation, tests, or
agent-instruction-only changes. Never add a second runtime version or use a
`-SNAPSHOT` deployable version. Spring Boot build info embeds this value.
