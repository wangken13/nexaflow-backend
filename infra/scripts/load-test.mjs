import { performance } from 'node:perf_hooks'

const target = process.argv[2]
const total = Number(process.argv[3] || 200)
const concurrency = Number(process.argv[4] || 10)
const maxP95 = Number(process.argv[5] || 500)

if (!target || !Number.isInteger(total) || !Number.isInteger(concurrency) || total < 1 || concurrency < 1) {
  console.error('Usage: node load-test.mjs <url> [requests=200] [concurrency=10] [max-p95-ms=500]')
  process.exit(2)
}

const durations = []
let cursor = 0
let failures = 0

async function worker() {
  while (cursor < total) {
    cursor += 1
    const started = performance.now()
    try {
      const response = await fetch(target, { redirect: 'manual' })
      if (response.status < 200 || response.status >= 400) failures += 1
      await response.arrayBuffer()
    } catch {
      failures += 1
    } finally {
      durations.push(performance.now() - started)
    }
  }
}

const suiteStarted = performance.now()
await Promise.all(Array.from({ length: Math.min(concurrency, total) }, worker))
const elapsed = performance.now() - suiteStarted
durations.sort((a, b) => a - b)
const percentile = value => durations[Math.min(durations.length - 1, Math.ceil(durations.length * value) - 1)]
const result = {
  target,
  requests: total,
  concurrency,
  failures,
  successRate: Number((((total - failures) / total) * 100).toFixed(2)),
  requestsPerSecond: Number((total / (elapsed / 1000)).toFixed(2)),
  p50Ms: Number(percentile(0.5).toFixed(2)),
  p95Ms: Number(percentile(0.95).toFixed(2)),
  p99Ms: Number(percentile(0.99).toFixed(2))
}
console.log(JSON.stringify(result, null, 2))

if (result.successRate < 99 || result.p95Ms > maxP95) process.exit(1)
