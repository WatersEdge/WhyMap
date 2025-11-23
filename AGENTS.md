# Repository Guidelines

## Project Structure & Module Organization
- `src/main/java/dev/wefhy/whymap`: Kotlin mod logic (rendering, waypoints, web server, mixins).
- `src/main/resources`: Fabric descriptors, mappings, packaged web assets under `web/` and `three/`.
- `src/test/kotlin`: JUnit 5 tests (config, migrations, geometry utilities).
- `src-vue` and `src-threejs`: web clients built with Yarn/npm; outputs copied into resources via Gradle tasks.
- `scripts/`: release/build helper scripts; `gradle/`, `gradlew*`: wrapper utilities.

## Build, Test, and Development Commands
- `./gradlew build` (or `gradlew.bat`): full mod jar; builds Vue + threejs bundles, refreshes mapping manifests, and emits the release Discord message helper.
- `./gradlew runClient`: launch a client dev instance; ensures latest web assets are copied.
- `./gradlew test`: run the JUnit 5 suite.
- `./gradlew yarnBuild` / `./gradlew threeBuild`: build frontend bundles only (after `yarn install` in `src-vue` and `npm install` in `src-threejs`).
- `cd src-vue && yarn serve` or `./gradlew serve`: optional frontend dev server during UI changes.

## Coding Style & Naming Conventions
- Kotlin-first codebase; 4-space indentation; prefer expression functions and null-safe APIs over `!!`.
- Package by feature (tiles, hud, waypoints, migrations); keep new files within the matching package.
- Use descriptive lowerCamelCase for vars/functions; PascalCase for types; constants upper snake.
- Keep mixins minimal and documented; avoid side effects in initializers.

## Testing Guidelines
- Place tests alongside features in `src/test/kotlin/.../*Test.kt`; mirror package paths.
- Cover config defaults, mapping migrations, and geometry/math utilities when changing logic.
- Use JUnit 5 assertions; prefer deterministic fixtures (see `TestUtils.kt`); run `./gradlew test` before publishing.

## Commit & Pull Request Guidelines
- Upstream currently does not accept external contributions; keep private forks aligned with these rules and open PRs only if maintainer policy changes.
- Use concise, imperative commit messages (e.g., `fix: guard null chunk loads`, `build: refresh web assets`).
- PRs should describe scope, link related issues, list commands run, and include screenshots/GIFs for UI or map-rendering changes.
- Note any data migrations or config defaults that affect existing worlds and update changelog entries accordingly.

## Security & Configuration Tips
- Do not commit generated world data or secrets; `.gitignore` already covers run outputs, so keep it intact.
- Web assets are served locally; verify server exposure settings before sharing builds.
