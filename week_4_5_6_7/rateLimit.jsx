import { useState, useEffect, useRef, useCallback } from "react";

const ALGORITHMS = {
  token: {
    name: "Token Bucket",
    color: "#FF6B35",
    accent: "#FFD166",
    description: "Tokens accumulate at a fixed rate up to a max capacity. Each request consumes one token. Allows bursting up to bucket capacity.",
    params: { capacity: 10, refillRate: 2, tokens: 10 },
    icon: "🪙",
  },
  leaky: {
    name: "Leaky Bucket",
    color: "#06D6A0",
    accent: "#00F5D4",
    description: "Requests enter a queue (bucket) and are processed at a fixed rate. Excess requests overflow and are dropped. Smooths traffic.",
    params: { capacity: 8, leakRate: 1, queue: 0 },
    icon: "🪣",
  },
  fixed: {
    name: "Fixed Window",
    color: "#118AB2",
    accent: "#73D2DE",
    description: "Counts requests within a fixed time window. Counter resets at window boundary. Simple but can allow double the rate at boundaries.",
    params: { limit: 5, windowSize: 5, count: 0, windowStart: 0 },
    icon: "🪟",
  },
  sliding: {
    name: "Sliding Window Log",
    color: "#9B5DE5",
    accent: "#F15BB5",
    description: "Tracks timestamps of recent requests within a rolling window. More accurate than fixed window — no boundary spike problem.",
    params: { limit: 5, windowSize: 5, log: [] },
    icon: "🔄",
  },
};

function TokenBucket({ state, onRequest, time }) {
  const tokens = Math.floor(state.tokens);
  const cap = state.capacity;
  const pct = (tokens / cap) * 100;

  return (
    <div className="algo-body">
      <div className="bucket-visual">
        <div className="bucket-container" style={{ "--color": ALGORITHMS.token.color, "--accent": ALGORITHMS.token.accent }}>
          <div className="bucket-label">Capacity: {cap}</div>
          <div className="bucket-outer">
            <div className="bucket-fill" style={{ height: `${pct}%`, background: `linear-gradient(180deg, ${ALGORITHMS.token.accent}, ${ALGORITHMS.token.color})` }} />
            <div className="bucket-tokens">
              {Array.from({ length: cap }).map((_, i) => (
                <div key={i} className={`token-dot ${i < tokens ? "active" : "empty"}`} style={{ background: i < tokens ? ALGORITHMS.token.accent : "rgba(255,255,255,0.1)" }} />
              ))}
            </div>
            <div className="bucket-count">{tokens}</div>
          </div>
          <div className="bucket-meta">
            <span>+{state.refillRate}/s refill</span>
          </div>
        </div>
      </div>
      <div className="stats-row">
        <div className="stat"><span className="stat-label">Available</span><span className="stat-val" style={{ color: ALGORITHMS.token.accent }}>{tokens}</span></div>
        <div className="stat"><span className="stat-label">Capacity</span><span className="stat-val">{cap}</span></div>
        <div className="stat"><span className="stat-label">Refill</span><span className="stat-val">{state.refillRate}/s</span></div>
      </div>
    </div>
  );
}

function LeakyBucket({ state, onRequest, time }) {
  const qLen = state.queue;
  const cap = state.capacity;
  const pct = (qLen / cap) * 100;

  return (
    <div className="algo-body">
      <div className="bucket-visual">
        <div className="bucket-container" style={{ "--color": ALGORITHMS.leaky.color, "--accent": ALGORITHMS.leaky.accent }}>
          <div className="bucket-label">Queue: {cap} max</div>
          <div className="bucket-outer leaky">
            <div className="bucket-fill" style={{ height: `${pct}%`, background: `linear-gradient(180deg, ${ALGORITHMS.leaky.accent}, ${ALGORITHMS.leaky.color})` }} />
            <div className="bucket-count">{qLen}</div>
            <div className="leak-drops">
              {[0, 1, 2].map(i => (
                <div key={i} className="drop" style={{ animationDelay: `${i * 0.4}s`, background: ALGORITHMS.leaky.accent }} />
              ))}
            </div>
          </div>
          <div className="bucket-meta">
            <span>-{state.leakRate}/s leak</span>
          </div>
        </div>
      </div>
      <div className="stats-row">
        <div className="stat"><span className="stat-label">Queued</span><span className="stat-val" style={{ color: ALGORITHMS.leaky.accent }}>{qLen}</span></div>
        <div className="stat"><span className="stat-label">Capacity</span><span className="stat-val">{cap}</span></div>
        <div className="stat"><span className="stat-label">Drain</span><span className="stat-val">{state.leakRate}/s</span></div>
      </div>
    </div>
  );
}

function FixedWindow({ state, time }) {
  const elapsed = time - state.windowStart;
  const remaining = Math.max(0, state.windowSize - elapsed);
  const progressPct = (elapsed / state.windowSize) * 100;
  const usedPct = (state.count / state.limit) * 100;

  return (
    <div className="algo-body">
      <div className="window-visual">
        <div className="window-grid">
          {Array.from({ length: state.limit }).map((_, i) => (
            <div key={i} className={`window-slot ${i < state.count ? "used" : "free"}`}
              style={{ background: i < state.count ? ALGORITHMS.fixed.color : "rgba(255,255,255,0.07)", borderColor: ALGORITHMS.fixed.accent + "44" }}>
              {i < state.count ? "●" : "○"}
            </div>
          ))}
        </div>
        <div className="window-timer">
          <div className="timer-track">
            <div className="timer-fill" style={{ width: `${Math.min(progressPct, 100)}%`, background: `linear-gradient(90deg, ${ALGORITHMS.fixed.color}, ${ALGORITHMS.fixed.accent})` }} />
          </div>
          <div className="timer-labels">
            <span>Window resets in</span>
            <span style={{ color: ALGORITHMS.fixed.accent, fontWeight: 700 }}>{remaining.toFixed(1)}s</span>
          </div>
        </div>
      </div>
      <div className="stats-row">
        <div className="stat"><span className="stat-label">Used</span><span className="stat-val" style={{ color: ALGORITHMS.fixed.accent }}>{state.count}/{state.limit}</span></div>
        <div className="stat"><span className="stat-label">Window</span><span className="stat-val">{state.windowSize}s</span></div>
        <div className="stat"><span className="stat-label">Resets</span><span className="stat-val">{remaining.toFixed(1)}s</span></div>
      </div>
    </div>
  );
}

function SlidingWindow({ state, time }) {
  const validLog = state.log.filter(t => time - t < state.windowSize);
  const count = validLog.length;
  const usedPct = (count / state.limit) * 100;

  return (
    <div className="algo-body">
      <div className="sliding-visual">
        <div className="timeline">
          <div className="timeline-label">← {state.windowSize}s rolling window →</div>
          <div className="timeline-track">
            <div className="window-highlight" style={{ background: `${ALGORITHMS.sliding.color}22`, borderColor: ALGORITHMS.sliding.color }} />
            {validLog.map((t, i) => {
              const age = time - t;
              const pos = ((state.windowSize - age) / state.windowSize) * 100;
              return (
                <div key={i} className="timeline-dot" style={{ left: `${pos}%`, background: ALGORITHMS.sliding.accent, boxShadow: `0 0 8px ${ALGORITHMS.sliding.accent}` }} />
              );
            })}
          </div>
          <div className="timeline-ends"><span>-{state.windowSize}s</span><span>now</span></div>
        </div>
        <div className="usage-bar">
          <div className="usage-fill" style={{ width: `${usedPct}%`, background: `linear-gradient(90deg, ${ALGORITHMS.sliding.color}, ${ALGORITHMS.sliding.accent})` }} />
          <span>{count}/{state.limit} requests</span>
        </div>
      </div>
      <div className="stats-row">
        <div className="stat"><span className="stat-label">In Window</span><span className="stat-val" style={{ color: ALGORITHMS.sliding.accent }}>{count}</span></div>
        <div className="stat"><span className="stat-label">Limit</span><span className="stat-val">{state.limit}</span></div>
        <div className="stat"><span className="stat-label">Window</span><span className="stat-val">{state.windowSize}s</span></div>
      </div>
    </div>
  );
}

export default function App() {
  const [time, setTime] = useState(0);
  const [states, setStates] = useState({
    token: { capacity: 10, refillRate: 2, tokens: 10 },
    leaky: { capacity: 8, leakRate: 1, queue: 0 },
    fixed: { limit: 5, windowSize: 5, count: 0, windowStart: 0 },
    sliding: { limit: 5, windowSize: 5, log: [] },
  });
  const [results, setResults] = useState({ token: null, leaky: null, fixed: null, sliding: null });
  const [activeTab, setActiveTab] = useState("all");
  const timerRef = useRef(null);

  useEffect(() => {
    timerRef.current = setInterval(() => {
      setTime(t => {
        const newTime = t + 0.1;
        setStates(prev => {
          const s = { ...prev };
          // Token refill
          s.token = { ...s.token, tokens: Math.min(s.token.capacity, s.token.tokens + s.token.refillRate * 0.1) };
          // Leaky drain
          s.leaky = { ...s.leaky, queue: Math.max(0, s.leaky.queue - s.leaky.leakRate * 0.1) };
          // Fixed window reset
          if (newTime - s.fixed.windowStart >= s.fixed.windowSize) {
            s.fixed = { ...s.fixed, count: 0, windowStart: newTime };
          }
          // Sliding window prune
          const newLog = s.sliding.log.filter(t => newTime - t < s.sliding.windowSize);
          s.sliding = { ...s.sliding, log: newLog };
          return s;
        });
        return newTime;
      });
    }, 100);
    return () => clearInterval(timerRef.current);
  }, []);

  const sendRequest = useCallback((algo) => {
    setStates(prev => {
      const s = { ...prev };
      let allowed = false;
      if (algo === "token") {
        if (s.token.tokens >= 1) { s.token = { ...s.token, tokens: s.token.tokens - 1 }; allowed = true; }
      } else if (algo === "leaky") {
        if (s.leaky.queue < s.leaky.capacity) { s.leaky = { ...s.leaky, queue: s.leaky.queue + 1 }; allowed = true; }
      } else if (algo === "fixed") {
        if (s.fixed.count < s.fixed.limit) { s.fixed = { ...s.fixed, count: s.fixed.count + 1 }; allowed = true; }
      } else if (algo === "sliding") {
        const validLog = s.sliding.log.filter(t => time - t < s.sliding.windowSize);
        if (validLog.length < s.sliding.limit) { s.sliding = { ...s.sliding, log: [...validLog, time] }; allowed = true; }
      }
      setResults(r => ({ ...r, [algo]: { allowed, ts: Date.now() } }));
      return s;
    });
  }, [time]);

  const sendAll = () => {
    ["token", "leaky", "fixed", "sliding"].forEach(a => sendRequest(a));
  };

  const algoKeys = activeTab === "all" ? ["token", "leaky", "fixed", "sliding"] : [activeTab];

  return (
    <div className="app">
      <style>{`
        @import url('https://fonts.googleapis.com/css2?family=Space+Mono:wght@400;700&family=DM+Sans:wght@400;500;600;700&display=swap');

        *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }

        .app {
          min-height: 100vh;
          background: #080B14;
          color: #E8EAF0;
          font-family: 'DM Sans', sans-serif;
          padding: 24px 16px 48px;
          background-image: radial-gradient(ellipse at 20% 0%, #1a0a2e 0%, transparent 50%),
                            radial-gradient(ellipse at 80% 10%, #0a1a2e 0%, transparent 50%);
        }

        .header {
          text-align: center;
          margin-bottom: 32px;
        }

        .header-badge {
          display: inline-block;
          font-family: 'Space Mono', monospace;
          font-size: 11px;
          letter-spacing: 3px;
          text-transform: uppercase;
          color: #73D2DE;
          background: rgba(115,210,222,0.1);
          border: 1px solid rgba(115,210,222,0.3);
          padding: 4px 14px;
          border-radius: 20px;
          margin-bottom: 14px;
        }

        .header h1 {
          font-family: 'Space Mono', monospace;
          font-size: clamp(22px, 5vw, 36px);
          font-weight: 700;
          background: linear-gradient(135deg, #fff 30%, #73D2DE);
          -webkit-background-clip: text;
          -webkit-text-fill-color: transparent;
          margin-bottom: 8px;
        }

        .header p {
          font-size: 14px;
          color: #7a8099;
          max-width: 480px;
          margin: 0 auto;
        }

        .tabs {
          display: flex;
          justify-content: center;
          gap: 8px;
          margin-bottom: 24px;
          flex-wrap: wrap;
        }

        .tab-btn {
          font-family: 'DM Sans', sans-serif;
          font-size: 13px;
          font-weight: 600;
          padding: 8px 18px;
          border-radius: 8px;
          border: 1px solid rgba(255,255,255,0.1);
          background: rgba(255,255,255,0.04);
          color: #7a8099;
          cursor: pointer;
          transition: all 0.2s;
        }

        .tab-btn.active, .tab-btn:hover {
          background: rgba(255,255,255,0.1);
          color: #fff;
          border-color: rgba(255,255,255,0.2);
        }

        .send-all-btn {
          display: block;
          margin: 0 auto 28px;
          font-family: 'Space Mono', monospace;
          font-size: 13px;
          font-weight: 700;
          letter-spacing: 1px;
          padding: 12px 32px;
          background: linear-gradient(135deg, #FF6B35, #9B5DE5);
          border: none;
          border-radius: 10px;
          color: #fff;
          cursor: pointer;
          transition: all 0.2s;
          box-shadow: 0 4px 24px rgba(155,93,229,0.3);
        }

        .send-all-btn:hover { transform: translateY(-2px); box-shadow: 0 8px 32px rgba(155,93,229,0.5); }
        .send-all-btn:active { transform: translateY(0); }

        .grid {
          display: grid;
          grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
          gap: 20px;
          max-width: 1200px;
          margin: 0 auto;
        }

        .card {
          background: rgba(255,255,255,0.04);
          border: 1px solid rgba(255,255,255,0.08);
          border-radius: 16px;
          overflow: hidden;
          transition: transform 0.2s, box-shadow 0.2s;
        }

        .card:hover { transform: translateY(-3px); box-shadow: 0 12px 40px rgba(0,0,0,0.4); }

        .card-header {
          padding: 16px 20px 14px;
          border-bottom: 1px solid rgba(255,255,255,0.06);
          display: flex;
          align-items: center;
          gap: 10px;
        }

        .card-icon { font-size: 22px; }

        .card-title {
          font-family: 'Space Mono', monospace;
          font-size: 14px;
          font-weight: 700;
        }

        .card-desc {
          padding: 14px 20px;
          font-size: 12px;
          line-height: 1.6;
          color: #7a8099;
          border-bottom: 1px solid rgba(255,255,255,0.06);
        }

        .algo-body { padding: 16px 20px; }

        /* Bucket visuals */
        .bucket-visual { display: flex; justify-content: center; margin-bottom: 16px; }

        .bucket-container { display: flex; flex-direction: column; align-items: center; gap: 8px; }

        .bucket-label { font-size: 11px; color: #7a8099; font-family: 'Space Mono', monospace; }

        .bucket-outer {
          width: 80px; height: 120px;
          border: 2px solid rgba(255,255,255,0.15);
          border-radius: 4px 4px 12px 12px;
          position: relative;
          overflow: hidden;
          background: rgba(255,255,255,0.03);
          display: flex; align-items: flex-end;
        }

        .bucket-fill {
          position: absolute; bottom: 0; left: 0; right: 0;
          transition: height 0.3s ease;
        }

        .bucket-tokens {
          position: absolute; inset: 0;
          display: grid; grid-template-columns: repeat(5, 1fr);
          gap: 4px; padding: 8px; align-content: end;
        }

        .token-dot {
          width: 10px; height: 10px; border-radius: 50%;
          transition: background 0.3s;
        }

        .bucket-count {
          position: absolute; inset: 0;
          display: flex; align-items: center; justify-content: center;
          font-family: 'Space Mono', monospace;
          font-size: 28px; font-weight: 700; color: rgba(255,255,255,0.9);
          text-shadow: 0 2px 10px rgba(0,0,0,0.8);
        }

        .bucket-meta { font-size: 11px; color: #7a8099; font-family: 'Space Mono', monospace; }

        /* Leaky drops */
        .bucket-outer.leaky { border-radius: 4px 4px 2px 2px; }

        .leak-drops {
          position: absolute; bottom: -30px; left: 50%; transform: translateX(-50%);
          display: flex; flex-direction: column; gap: 6px;
        }

        .drop {
          width: 6px; height: 10px; border-radius: 50% 50% 50% 50% / 60% 60% 40% 40%;
          animation: drip 1.2s ease-in infinite;
          opacity: 0;
        }

        @keyframes drip {
          0% { opacity: 0; transform: translateY(0); }
          30% { opacity: 1; }
          100% { opacity: 0; transform: translateY(30px); }
        }

        /* Window visual */
        .window-visual { margin-bottom: 16px; }

        .window-grid {
          display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px;
          margin-bottom: 14px;
        }

        .window-slot {
          aspect-ratio: 1; border-radius: 8px; border: 1px solid;
          display: flex; align-items: center; justify-content: center;
          font-size: 18px; transition: all 0.3s;
        }

        .window-slot.used { transform: scale(1.05); }

        .window-timer { }
        .timer-track {
          height: 6px; background: rgba(255,255,255,0.08); border-radius: 3px; overflow: hidden;
          margin-bottom: 8px;
        }
        .timer-fill { height: 100%; border-radius: 3px; transition: width 0.1s linear; }
        .timer-labels { display: flex; justify-content: space-between; font-size: 12px; color: #7a8099; }

        /* Sliding window */
        .sliding-visual { margin-bottom: 16px; }
        .timeline { margin-bottom: 14px; }
        .timeline-label { font-size: 11px; color: #7a8099; margin-bottom: 8px; text-align: center; font-family: 'Space Mono', monospace; }
        .timeline-track {
          height: 36px; background: rgba(255,255,255,0.04);
          border-radius: 8px; position: relative; overflow: hidden;
          border: 1px solid rgba(255,255,255,0.08);
        }
        .window-highlight { position: absolute; inset: 2px; border-radius: 6px; border: 1px solid; }
        .timeline-dot {
          position: absolute; top: 50%; transform: translate(-50%, -50%);
          width: 12px; height: 12px; border-radius: 50%;
          transition: left 0.1s linear;
        }
        .timeline-ends { display: flex; justify-content: space-between; font-size: 11px; color: #555; margin-top: 4px; font-family: 'Space Mono', monospace; }

        .usage-bar {
          height: 28px; background: rgba(255,255,255,0.05); border-radius: 6px;
          position: relative; overflow: hidden; display: flex; align-items: center;
        }
        .usage-fill { position: absolute; left: 0; top: 0; bottom: 0; border-radius: 6px; transition: width 0.3s; }
        .usage-bar span { position: relative; z-index: 1; padding: 0 12px; font-size: 12px; font-family: 'Space Mono', monospace; font-weight: 700; }

        /* Stats row */
        .stats-row {
          display: flex; gap: 8px; margin-bottom: 16px;
        }
        .stat {
          flex: 1; background: rgba(255,255,255,0.04); border-radius: 8px;
          padding: 8px 10px; display: flex; flex-direction: column; gap: 3px;
        }
        .stat-label { font-size: 10px; color: #555; text-transform: uppercase; letter-spacing: 1px; font-family: 'Space Mono', monospace; }
        .stat-val { font-size: 16px; font-weight: 700; font-family: 'Space Mono', monospace; color: #fff; }

        /* Request button */
        .req-btn {
          width: 100%; padding: 12px;
          font-family: 'Space Mono', monospace;
          font-size: 13px; font-weight: 700;
          border: none; border-radius: 10px; cursor: pointer;
          transition: all 0.2s; letter-spacing: 0.5px;
          position: relative; overflow: hidden;
        }

        .req-btn:hover { transform: translateY(-1px); }
        .req-btn:active { transform: translateY(0); }

        /* Result flash */
        .result-flash {
          text-align: center; font-family: 'Space Mono', monospace;
          font-size: 13px; font-weight: 700; padding: 8px;
          border-radius: 8px; margin-top: 10px;
          transition: all 0.3s;
        }

        .result-flash.allowed {
          background: rgba(6,214,160,0.15); color: #06D6A0;
          border: 1px solid rgba(6,214,160,0.3);
        }

        .result-flash.denied {
          background: rgba(255,71,71,0.15); color: #FF4747;
          border: 1px solid rgba(255,71,71,0.3);
        }

        /* Time display */
        .time-display {
          text-align: center;
          font-family: 'Space Mono', monospace;
          font-size: 12px;
          color: #555;
          margin-bottom: 20px;
        }
      `}</style>

      <div className="header">
        <div className="header-badge">System Design</div>
        <h1>Rate Limiting Algorithms</h1>
        <p>Visualize and compare four classical rate limiting strategies. Send requests to see how each algorithm responds.</p>
      </div>

      <div className="time-display">⏱ t = {time.toFixed(1)}s</div>

      <div className="tabs">
        {[["all", "All Algorithms"], ["token", "🪙 Token"], ["leaky", "🪣 Leaky"], ["fixed", "🪟 Fixed"], ["sliding", "🔄 Sliding"]].map(([k, label]) => (
          <button key={k} className={`tab-btn ${activeTab === k ? "active" : ""}`} onClick={() => setActiveTab(k)}>{label}</button>
        ))}
      </div>

      {activeTab === "all" && (
        <button className="send-all-btn" onClick={sendAll}>⚡ SEND REQUEST TO ALL</button>
      )}

      <div className="grid">
        {algoKeys.map(algo => {
          const meta = ALGORITHMS[algo];
          const res = results[algo];
          const s = states[algo];

          return (
            <div className="card" key={algo} style={{ borderColor: meta.color + "33", boxShadow: `0 0 30px ${meta.color}11` }}>
              <div className="card-header" style={{ background: `${meta.color}15` }}>
                <span className="card-icon">{meta.icon}</span>
                <span className="card-title" style={{ color: meta.accent }}>{meta.name}</span>
              </div>
              <div className="card-desc">{meta.description}</div>

              {algo === "token" && <TokenBucket state={s} time={time} />}
              {algo === "leaky" && <LeakyBucket state={s} time={time} />}
              {algo === "fixed" && <FixedWindow state={s} time={time} />}
              {algo === "sliding" && <SlidingWindow state={s} time={time} />}

              <div style={{ padding: "0 20px 20px" }}>
                <button
                  className="req-btn"
                  style={{ background: `linear-gradient(135deg, ${meta.color}, ${meta.accent})`, color: "#fff" }}
                  onClick={() => sendRequest(algo)}
                >
                  → SEND REQUEST
                </button>

                {res && (
                  <div className={`result-flash ${res.allowed ? "allowed" : "denied"}`} key={res.ts}>
                    {res.allowed ? "✅ ALLOWED" : "❌ RATE LIMITED"}
                  </div>
                )}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}