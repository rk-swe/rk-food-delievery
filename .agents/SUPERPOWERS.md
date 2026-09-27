# Superpowers skills

Repository-local skills from [obra/superpowers](https://github.com/obra/superpowers).

- Upstream commit: `8ca22dba9a94f28898bbce59f2537ff4d87c747d`
- Location: `.agents/skills/` (15 skills, including their supporting files)
- License: [MIT](SUPERPOWERS-LICENSE)

These skills are vendored into this repository for Codex project-level discovery.
Invoke a skill by name, for example `$using-superpowers` or `$systematic-debugging`.
This installs the skill collection only; it does not install global plugins or session hooks.

To update, use the skill-installer with `--repo obra/superpowers`, an explicit
`--ref`, each upstream `skills/<name>` path, and a temporary `--dest`. Review the
changes before replacing these directories, then update this commit reference
and the license.
