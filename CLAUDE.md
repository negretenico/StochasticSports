# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

StochasticSports is a real-time MLB sports analytics platform in the planning/early implementation phase. The goal is to ingest live game data and betting odds, correlate them, and apply analytics (Elo ratings, arbitrage detection, win probability modeling).

## Architecture

The intended data pipeline has three layers:

1. **Data Ingestion**
   - **Odds:** The Odds API (`https://api.the-odds-api.com/v4/`) — credit-based, paid; use `h2h` (moneyline) market as cheapest option
   - **Game Events:** MLB Stats API (`https://statsapi.mlb.com/api/v1/`) — free, unlimited, pitch-by-pitch live feed

2. **Data Correlation**
   - `The Odds API id` does NOT match `MLB Stats API gamePk` — joining requires mapping via team names + timestamps
   - Game events are keyed by `gamePk` in Kafka messages
   - Live feed polling uses `timecode` diffs (`?timecode=YYYYMMDD_HHMMSS`) to avoid reprocessing

3. **Stream Processing**
   - Kafka as message broker, keyed on `gamePk`
   - Orchestration layer planned but not yet implemented (see `docs/apis/orchestrate.md`)

## Key API Details

### The Odds API
- Auth: API key as query param
- Rate limit: 429 on breach; monitor `x-requests-remaining` / `x-requests-used` / `x-requests-last` response headers
- Credit cost: `number_of_markets × number_of_regions` per request
- Recommended endpoint: `GET /sports/baseball_mlb/odds?regions=us&markets=h2h`

### MLB Stats API
- No auth required; unofficial but stable; also available via the `MLB-StatsAPI` PyPI package
- Live game feed: `GET /game/{gamePk}/feed/live`
- Schedule for a date: `GET /schedule?sportId=1&date=YYYY-MM-DD`
- Standings: `GET /standings?leagueId=103,104&season=YYYY&standingsTypes=regularSeason`

## Research Context

Articles in `docs/articles/` inform the modeling approach:
- Comparing financial vs gambling markets (market efficiency assumptions)
- Defining "win" for probability modeling
- Sports arbitrage detection
- Win strength measurement for MLB (Elo-style ratings)

## Languages

This is a polyglot project:
- **Java / Spring Boot** — all services (listener/ingestion, Kafka producing, risk/execution layer). The listener service polls the MLB Stats API and produces to Kafka — this is Java, not Python.
- **Python** — analytics and modeling only (Elo ratings, arbitrage detection, win probability). No ingestion services in Python.

## Coding Conventions

Convention skills are in `.claude/skills/`. Always load the relevant skill before writing or reviewing code in that language.

- **Java:** `/java` — Optional (return types only, monadic consumption), streams over loops, records over classes, unchecked custom exceptions, minimal try scope
- **Spring Boot:** `/spring-boot` — constructor injection (max 2–3 deps), `@ConfigurationProperties` with records, `@ControllerAdvice` for REST errors, `DefaultErrorHandler` for Kafka, domain-based packaging, config-file profiles only
- **TDD:** `/tdd` — mandatory for all implementation work. Write the failing test first, then the implementation. Never write code without a failing test driving it.

## Build System

The Makefile is currently empty. As the project grows, add targets here for running ingestion services, tests, and linting.
