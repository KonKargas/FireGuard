# Publishing FireGuard to GitHub

## Repository configuration

- Repository name: **FireGuard**
- Description: **Educational fire-detection simulation and interactive 3D building digital twin built with Java, Spring Boot, SQLite and Three.js.**
- Suggested topics: `java`, `spring-boot`, `thymeleaf`, `sqlite`, `threejs`, `digital-twin`, `simulation`, `3d-visualization`, `fire-detection`, `maven`
- Visibility: public, if you are comfortable sharing all source and assets.

The repository includes an MIT `LICENSE` file. **Review this license choice before publishing**; it permits reuse and modification by others.

## Upload from the prepared folder

Create an **empty** repository named `FireGuard` at https://github.com/new (do not ask GitHub to initialize it with a README, .gitignore or license). Then run:

```bash
git init
git branch -M main
git add .
git status
git commit -m "Initial release: FireGuard digital twin and simulation"
git remote add origin https://github.com/KonKargas/FireGuard.git
git push -u origin main
```

If the directory is already tracked by Git, inspect `git remote -v` and `git status` before adding a remote; do not create a second repository inside an existing one.

## Verify before publishing

- `git status --short` should show no `target/` or SQLite database files.
- Confirm no personal information, API keys or real sensor/incident data appear in any tracked file.
- `./mvnw clean test` should pass locally. CI runs tests automatically for new pushes and PRs.
- Add your own screenshots to `screenshots/`. The directory includes `.gitkeep` so it is tracked when empty.
- The application is educational only; it must not be marketed as real fire safety monitoring.

## Notes

- Maven wrapper scripts are included.
- Keep `data/.gitkeep` tracked but never commit `data/fireguard.db`.
- For local work, Java 21+ and a browser with WebGL support are recommended.
