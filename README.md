<div align="center">

<img src="src/main/resources/static/images/Logo_big.png" alt="FireGuard logo" width="360" />

# FireGuard

### Fire Detection Simulation & 3D Building Digital Twin

**Java 21 · Spring Boot 4.1.2 · Thymeleaf · SQLite · Three.js**

[Features](#features) · [Screenshots](#screenshots) · [Installation](#getting-started) · [Architecture](#architecture) · [Tests](#testing)

</div>

---

## Overview

**FireGuard** is an educational web application for managing a virtual building infrastructure and simulating fire-detection conditions. It combines spatial modeling, virtual temperature and luminosity sensors, simulated measurements, fire alerts, historical reports, and interactive 2D/3D floor visualization.

Originally conceived as a 2019 programming project, FireGuard has been rebuilt as a Java/Spring Boot application with a database-backed management interface and a browser-based digital twin.

> [!IMPORTANT]
> **Simulation only.** FireGuard is **not** a certified fire-detection, fire-protection, or emergency-notification system. Do not use it to monitor real buildings or make safety decisions.

## Features

| Module | Capabilities |
| --- | --- |
| **Dashboard** | Infrastructure overview, sensor and alert statistics |
| **Buildings** | Building records, types, descriptions and address details |
| **Floors** | Building-specific floors, numeric levels and elevations |
| **Locations** | Rooms and other spaces such as corridors, bathrooms and offices; individually defined 3D coordinates and dimensions |
| **2D Floor Planner** | Floor visualization, interactive space positioning and resizing with overlap validation |
| **Location Form Preview** | Read-only placement preview, displaying neighboring spaces and potential overlap |
| **Sensors** | Virtual temperature and luminosity sensors, placement within locations, refresh count and reliability |
| **Simulation** | Generate new virtual measurements per location or building |
| **Fire Alerts** | Active and resolved simulated fire events, linked to affected locations |
| **3D Visualizer** | Architectural cutaway floors, rooms, virtual sensors, alert highlights, camera controls and floor isolation |
| **Reports** | Measurement statistics, recent history, charts and CSV exports |

### Simulation model

- **Temperature:** simulated values in the 0–100 °C range.
- **Luminosity:** simulated values in the 0–200 range.
- **Sensor reliability:** `exp(-0.001 × refreshCount)`.
- **Simulated fire condition:** temperature **> 50 °C**, luminosity **> 140**, and combined sensor reliability **> 0.9**.
- An alert is a **simulated event**, not a real-world fire alarm.

## Technology stack

| Layer | Technology |
| --- | --- |
| Language | Java 21 |
| Application framework | Spring Boot 4.1.2, Spring MVC |
| Server-rendered UI | Thymeleaf, HTML, CSS, JavaScript |
| Persistence | Spring Data JPA, Hibernate, SQLite JDBC |
| 3D visualization | Three.js, browser-based rendering |
| Build | Maven and Maven Wrapper |
| Tests | JUnit 5, Spring Boot Test, Mockito |

The 3D visualizer currently loads Three.js from a CDN, so the browser needs an internet connection to load the library.

## Screenshots

Add screenshots to the [`screenshots/`](screenshots/) directory. Suggested filenames and views:

| File | Screen |
| --- | --- |
| `01-dashboard.png` | Dashboard overview |
| `02-buildings.png` | Buildings management |
| `03-floor-planner.png` | 2D layout planner |
| `04-location-preview.png` | Location placement preview |
| `05-sensors.png` | Sensors management |
| `06-simulation.png` | Simulation results |
| `07-3d-visualizer.png` | 3D architectural cutaway |
| `08-fire-alerts.png` | Fire alert history |
| `09-reports.png` | Analytics and reports |

<!-- After adding screenshots, uncomment the line below.
![FireGuard 3D Visualizer](screenshots/07-3d-visualizer.png)
-->

## Getting started

### Prerequisites

- JDK **21 or newer** (the project targets Java 21).
- An internet connection for initial Maven dependencies and the Three.js CDN.
- Git (for cloning).

### Clone

```bash
git clone https://github.com/KonKargas/FireGuard.git
cd FireGuard
```

### Run

**Windows:**

```powershell
.\mvnw.cmd spring-boot:run
```

**Linux / macOS:**

```bash
./mvnw spring-boot:run
```

If Maven is already installed, `mvn spring-boot:run` also works.

Open **http://localhost:8080/dashboard** in your browser. The application creates its local SQLite database at `data/fireguard.db` when started with a writable `data/` directory. Runtime database files are excluded from Git.

### Typical workflow

1. Create a **Building**, then add one or more **Floors**.
2. Add **Locations** (e.g., rooms, corridors or offices), defining coordinates and dimensions.
3. Use the **2D Floor Planner** to organize spaces and check overlap.
4. Configure virtual **Sensors**, or create them automatically during location setup.
5. Run a **Simulation** for one location or an entire building.
6. Inspect **Fire Alerts**, navigate the **3D Visualizer**, and view **Reports**.

> [!NOTE]
> `spring.jpa.hibernate.ddl-auto=update` is suitable for the current local development workflow, but is not a substitute for controlled database migrations in production.

## Architecture

```text
src/
├── main/
│   ├── java/com/kkargas/fireguard/
│   │   ├── controller/        # MVC pages and REST endpoint
│   │   ├── service/           # business logic and simulation
│   │   ├── repository/        # JPA repositories
│   │   ├── entity/            # persisted data model
│   │   ├── dto/               # model / response records
│   │   ├── enums/
│   │   └── util/
│   └── resources/
│       ├── templates/         # Thymeleaf views and fragments
│       ├── static/            # styles, scripts, logos, favicon
│       └── application.properties
└── test/java/com/kkargas/fireguard/
```

**Data hierarchy:** `Building → Floor → Location → Sensor → Measurement`; simulated fire alerts belong to a location. Spatial coordinates and room dimensions drive both the 2D planner and the 3D digital twin.

### Key routes

`/dashboard` · `/buildings` · `/floors` · `/locations` · `/sensors` · `/simulation` · `/visualizer` · `/alerts` · `/reports`

## Testing

```bash
# Windows
.\mvnw.cmd clean test

# macOS / Linux
./mvnw clean test
```

**Latest locally verified result (November 13, 2026):** **33 tests passed**, 0 failures, 0 errors, 0 skipped. Automated GitHub Actions test execution is also configured for pushes and pull requests.

## Development notes

- The current application is a single-user local simulation environment; do not expose it to the public internet without adding authentication, authorization and appropriate security controls.
- UI assets are available under `src/main/resources/static/`.
- Local SQLite runtime data is not part of this repository.
- The simulated fire thresholds and random measurements are educational abstractions and should not be interpreted as validated physical models.

## License

Distributed under the **MIT License**. See [`LICENSE`](LICENSE).

## Author

**Konstantinos Kargas** — Full-Stack Software Engineer  
**Website:** https://kkargas.com  
**GitHub:** https://github.com/KonKargas

---

<div align="center">

*FireGuard — Exploring spatial monitoring, simulation and interactive building visualization.*

</div>
