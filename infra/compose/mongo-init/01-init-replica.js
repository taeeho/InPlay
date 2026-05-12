// Idempotent replica set initiation for the single-node rs0.
// Re-running is safe: rs.status() succeeds once initiated, so we skip.

try {
  const s = rs.status();
  print(`[init] rs0 already initiated (state=${s.myState})`);
} catch (e) {
  print(`[init] rs.status() failed (${e.codeName || e.code}), initiating rs0`);
  rs.initiate({
    _id: "rs0",
    members: [{ _id: 0, host: "mongo:27017" }],
  });
  print("[init] rs0 initiated");
}

// Wait until PRIMARY before downstream scripts run.
let attempts = 0;
while (attempts < 30) {
  try {
    const s = rs.status();
    if (s.myState === 1) {
      print("[init] rs0 PRIMARY");
      break;
    }
  } catch (e) {}
  sleep(1000);
  attempts += 1;
}
if (attempts === 30) {
  throw new Error("[init] rs0 did not reach PRIMARY within 30s");
}
