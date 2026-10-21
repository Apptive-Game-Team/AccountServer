# Writing Style

Follow this file when writing or editing chat replies, progress updates, code
reviews, plans, handoffs, issues, commit titles and bodies, pull requests,
release notes, source comments, and all frontend user-facing copy.
This governs wording; keep project-specific workflows and required templates.
Keep agent instruction files in English where the repository requires it.

## Language and Tone

- Write user-facing explanations in natural Korean unless the user requests
  another language. Keep an explicitly required publication language.
- In chat, use polite, conversational sentences. In documents, use one
  consistent register. Do not imitate a press release or a motivational speech.
- Start with the result or the answer. Explain the cause and practical effect
  when the reader needs them. Name the file, behavior, or value you mean.
- Preserve identifiers, paths, commands, API names, quoted errors, and technical
  terms. Use ordinary Korean when it is exact; do not invent translations or
  metaphors for terms the code already names.
- Keep sentences short enough to follow, but retain grammar and necessary
  context. Do not use caveman-style fragments or omit particles just to be brief.

## Remove Empty Language

- Omit stock praise and openings such as "좋은 질문입니다", "완벽하게",
  and "성공적으로" when they add no information.
- Replace claims such as "견고한 기반", "매끄러운 경험", "강력한 기능",
  or "획기적인 개선" with the behavior or evidence that supports the claim.
- Avoid rhetorical questions, slogans, personification, and unsolicited
  contrasts such as "단순히 X가 아니라 Y입니다".
- Do not repeat the same point in an opening, bullets, and a closing summary.
  Use headings and lists only when they make the content easier to scan.
- State uncertainty plainly. Separate observed results from assumptions, and
  report relevant skipped checks without boilerplate warnings.

## Titles

Use the current library's concrete-title rule for commits, issues, pull
requests, plans, and document headings: name the affected thing and the change.
Use numbers when they matter. Avoid insights or metaphors that cannot be checked
against the work. Keep required Conventional Commit types and scopes in English.
End Korean work-item titles with a noun such as `추가`, `수정`, `적용`, or
`이동`, rather than `~한다`. This title rule supersedes older imperative-title
examples; prose paragraphs should still use complete sentences.

| Avoid | Prefer |
| --- | --- |
| `feat: 더 똑똑해진 세션 관리` | `feat(auth): 만료된 세션 자동 삭제 추가` |
| `refactor: 구조를 데이터로` | `refactor(qa): loop 상한을 QaArchSpec 필드로 이동` |
| `견고한 오류 처리 기반을 마련했습니다.` | `응답이 429이면 최대 3회 재시도하도록 바꿨습니다.` |
| `이것은 단순한 수정이 아닌 안정성 향상입니다.` | `토큰이 없을 때 발생하던 500 응답을 401로 바꿨습니다.` |

## Reviews and Handoffs

- A review finding states the triggering condition, the resulting problem, and
  a concrete fix. Cite the relevant location; omit praise and generic advice.
- A handoff states what changed, what was verified, and what remains. Do not
  recount the full conversation or pad empty sections beyond template needs.
- Pass these writing rules to reviewer subagents along with the review packet.
  Keep machine-readable verdict tokens such as `PASS` and `NONPASS` unchanged.

## Source Comments

- Apply these rules to line and block comments, docstrings, KDoc, JSDoc/TSDoc,
  C# XML documentation, and SQL comments when adding or editing them.
- Write Korean explanations of the constraint, reason, or failure being
  prevented. Preserve identifiers, exact quoted errors, and required doc tags.
- Do not narrate obvious code, praise the implementation, or add decorative
  section banners. Keep comments useful to the next person changing the code.

## Commits and Pull Requests

- Apply these rules to both titles and bodies. Use Korean prose and retain
  Conventional Commit types, scopes, required template headings, and metadata.
- Name the concrete change and explain its reason when it is not obvious.
  Report validation actually performed; do not invent success or benefits.
- Preserve repository-specific issue links, branch rules, and review workflow.

## Frontend Copy

- Apply these rules to headings, navigation, buttons, labels, placeholders,
  tooltips, validation errors, toasts, dialogs, loading and empty states,
  accessibility labels, FAQ answers, page metadata, and localization strings.
- Keep each locale in its intended language. Do not replace English locale
  strings with Korean; apply the same plain, concrete style in each language.
- Use the product's visible terminology. A screen named `관리자` stays
  `관리자`, not `admin`. The identifier-preservation rule applies to code and
  quoted technical material, not to forcing internal names into UI copy.
- Name actions directly: `저장`, `다시 시도`, `삭제`. Explain an error in terms
  of what happened and what the user can do next. Do not expose internal
  implementation details unless the user needs them to act.
- Use short labels for controls and headings, and polite complete sentences
  when addressing a person. Follow existing `user-copy.md` rules for the
  sentence form of bullets, cards, steps, and FAQ answers.
- State only supported product behavior. Avoid slogans, unsupported promises,
  filler introductions, and descriptions that repeat the adjacent heading.
- Review changed copy in its page context for repetition, consistent naming,
  and fit. Preserve localization keys, interpolation tokens, markup, and
  accessibility meaning. Follow the project's UI validation requirements.

## Scope of Edits

Apply these rules to all new or edited text in the categories above. Do not
rewrite unrelated source files, published commit history, or existing product
copy merely to normalize tone unless the user requests a wording cleanup.
