The Odds API (odds data)

**Base URL:** `https://api.the-odds-api.com/v4/`

**Docs:** https://the-odds-api.com/liveapi/guides/v4/

**Auth:** `apiKey` query param on every request.

**Free tier:** 500 credits/month. **Credits ≠ requests** — cost formula:

```
live_odds_cost      = number_of_markets × number_of_regions
historical_odds_cost = 10 × number_of_markets × number_of_regions
```

A call with `markets=h2h&regions=us` costs 1 credit. Adding `spreads,totals` or more regions multiplies the cost. `/sports` and `/events` are free; a call returning zero events isn't charged.

### Key endpoint for our use case

`GET /sports/baseball_mlb/odds`

```
https://api.the-odds-api.com/v4/sports/baseball_mlb/odds
  ?regions=us
  &markets=h2h
  &oddsFormat=american
  &apiKey=YOUR_API_KEY
```

| Param                                 | Notes                                                                     |
| ------------------------------------- | ------------------------------------------------------------------------- |
| `regions`                             | Start with `us` only (DraftKings, FanDuel, BetMGM, Caesars, Bovada, etc.) |
| `markets`                             | Start with `h2h` (moneyline) only — cheapest, and what Elo/arb need first |
| `oddsFormat`                          | `american` or `decimal`                                                   |
| `dateFormat`                          | `iso` (default) or `unix`                                                 |
| `eventIds`                            | Optional comma-separated filter                                           |
| `commenceTimeFrom` / `commenceTimeTo` | Optional ISO8601 time window filters                                      |

### Response notes

- `id` field is the join key between `/odds` and `/scores` for this API — but does **not** match MLB Stats API's `gamePk`. Need a small mapping layer (team names/times) to correlate the two.
- Every response includes quota headers: `x-requests-remaining`, `x-requests-used`, `x-requests-last`. Log these on every call.
- Rate limiting returns 429 — back off, don't retry immediately.
 `x-requests-used`, `x-requests-last`. Log these on every call.
- Rate limiting returns 429 — back off, don't retry immediately.
