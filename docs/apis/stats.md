## MLB Stats API (game/event data)

**Base URL:** `https://statsapi.mlb.com/api/v1/`

**Auth:** None. No API key, token, or header required for any endpoint.

**Status:** Unofficial/undocumented, but stable and widely used (powers the `MLB-StatsAPI` PyPI package and many community tools). No published SLA or rate-limit policy — implement backoff/retry defensively.

**Note:** `statsapi.mlb.com` (browser) and `docs.statsapi.mlb.com` show an Okta login/register wall — that's just their internal documentation portal, unrelated to actual API access. The `/api/v1/` endpoints below are open.

### Key endpoints for our use case

| Endpoint                                                                   | Purpose                                                 |
| -------------------------------------------------------------------------- | ------------------------------------------------------- |
| `GET /schedule?sportId=1&date=YYYY-MM-DD`                                  | Games on a given date (returns `gamePk` per game)       |
| `GET /game/{gamePk}/feed/live`                                             | Full live game feed — plays, pitches, state, timestamps |
| `GET /game/{gamePk}/feed/live?timecode=YYYYMMDD_HHMMSS`                    | Game state at a specific point in time                  |
| `GET /teams?sportId=1`                                                     | All MLB teams                                           |
| `GET /people/{personId}`                                                   | Player profile (e.g. `660271` = Shohei Ohtani)          |
| `GET /standings?leagueId=103,104&season=YYYY&standingsTypes=regularSeason` | AL/NL standings                                         |

### Example calls

```
curl "https://statsapi.mlb.com/api/v1/schedule?sportId=1&date=2026-07-06"
curl "https://statsapi.mlb.com/api/v1/game/747175/feed/live"
curl "https://statsapi.mlb.com/api/v1/people/660271"
```

### Ingestion notes

- `gamePk` is the primary key to key our Kafka messages on.
- Live feed is pitch-by-pitch — poll `feed/live` on an interval per active `gamePk`, or diff against `timecode` to avoid re-processing unchanged state.
- No quota concerns — this is our free, unlimited leg of ingestion. active `gamePk`, or diff against `timecode` to avoid re-processing unchanged state.
- No quota concerns — this is our free, unlimited leg of ingestion.
