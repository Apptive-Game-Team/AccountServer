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
Do not bump it in a pull request. The monorepo `deploy` skill bumps it once per
promotion: it commits `chore(release): AccountServer vX.Y.Z` to `main`, merges
`main` into `deploy`, then tags and releases `vX.Y.Z` on the merge commit. The
level comes from the Conventional Commit messages promoted in that release:
MAJOR for a `!` marker or a `BREAKING CHANGE` trailer, MINOR for `feat:`, PATCH
otherwise, so write accurate commit types.

Never add a second runtime version or use a `-SNAPSHOT` deployable version.
Spring Boot build info embeds this value.

## Code Review Rules

- Write all code review titles, descriptions, comments, and summaries in Korean.
- Preserve code identifiers, file paths, commands, error messages, and technical terms in their original form.
- For each finding, concisely explain the cause, impact, and suggested fix.

## Writing Style

Follow [`.agents/docs/writing-style.md`](.agents/docs/writing-style.md) for chat
replies, reviews, plans, handoffs, issues, commit titles and bodies, pull
requests, release notes, source comments, and all frontend copy (including
localization, errors, labels, and accessibility text).
Its wording and title rules take precedence over older style examples;
preserve project-specific technical constraints and workflows.
