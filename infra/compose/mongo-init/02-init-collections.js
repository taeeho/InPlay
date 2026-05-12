// Create the 11 inplay collections + indexes (idempotent).
// Timeseries collections must be created explicitly — auto-creation does
// not pick up the timeseries options. Regular collections are created on
// first index write, so we just create indexes.
//
// Schema map matches plan §3.

const dbi = db.getSiblingDB("inplay");

function ensureTimeseries(name, metaField, granularity, expireAfterSeconds) {
  const existing = dbi.getCollectionInfos({ name })[0];
  if (existing) {
    print(`[init] timeseries '${name}' already exists`);
    return;
  }
  dbi.createCollection(name, {
    timeseries: {
      timeField: "event_ts",
      metaField,
      granularity,
    },
    expireAfterSeconds,
  });
  print(`[init] timeseries '${name}' created`);
}

// 8 months ≈ KBO 시즌 + 1개월 buffer.
const TS_TTL = 60 * 60 * 24 * 240;

ensureTimeseries("live_event", "meta", "seconds", TS_TTL);
ensureTimeseries("pitch_log", "meta", "seconds", TS_TTL);
ensureTimeseries("alert_event", "meta", "seconds", TS_TTL);

// Regular collections — indexes.
const indexPlan = [
  ["user", [
    [{ user_id: 1 }, { unique: true, name: "uniq_user_id" }],
    [{ api_key_hash: 1 }, { unique: true, name: "uniq_api_key_hash" }],
  ]],
  ["team", [
    [{ team_code: 1 }, { unique: true, name: "uniq_team_code" }],
  ]],
  ["player", [
    [{ player_id: 1 }, { unique: true, name: "uniq_player_id" }],
    [{ team_code: 1 }, { name: "by_team_code" }],
  ]],
  ["game", [
    [{ game_id: 1 }, { unique: true, name: "uniq_game_id" }],
    [{ date: 1, home_team: 1, away_team: 1 }, { name: "by_date_teams" }],
  ]],
  ["pre_game_brief", [
    [{ user_id: 1, game_id: 1 }, { unique: true, name: "uniq_user_game" }],
  ]],
  ["pitcher_stat_daily", [
    [{ player_id: 1, date: 1 }, { unique: true, name: "uniq_player_date" }],
  ]],
  ["model_snapshot", [
    [{ model_name: 1, version: 1 }, { unique: true, name: "uniq_model_version" }],
  ]],
  ["season_journal", [
    [{ user_id: 1, season: 1, game_id: 1 }, { unique: true, name: "uniq_user_season_game" }],
  ]],
];

for (const [coll, indexes] of indexPlan) {
  for (const [keys, opts] of indexes) {
    dbi.getCollection(coll).createIndex(keys, opts);
  }
  print(`[init] indexes ensured on '${coll}'`);
}

print("[init] inplay collections + indexes ready");
