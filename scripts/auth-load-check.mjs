const baseUrl = process.env.AUTH_PERF_BASE_URL || 'http://localhost:18080'
const username = process.env.AUTH_PERF_USERNAME
const password = process.env.AUTH_PERF_PASSWORD
const total = Number(process.env.AUTH_PERF_REQUESTS || 20)
const concurrency = Number(process.env.AUTH_PERF_CONCURRENCY || 4)

if (!username || !password) {
  throw new Error('Set AUTH_PERF_USERNAME and AUTH_PERF_PASSWORD before running this check.')
}
if (!Number.isInteger(total) || !Number.isInteger(concurrency) || total < 1 || concurrency < 1 || concurrency > total) {
  throw new Error('AUTH_PERF_REQUESTS and AUTH_PERF_CONCURRENCY must be positive integers, and concurrency cannot exceed requests.')
}

const durations = []
const failures = []
let cursor = 0

async function worker() {
  while (true) {
    const current = cursor++
    if (current >= total) return
    const startedAt = performance.now()
    try {
      const response = await fetch(`${baseUrl}/api/auth/login`, {
        method: 'POST',
        headers: { 'content-type': 'application/json' },
        body: JSON.stringify({ username, password })
      })
      const body = await response.json().catch(() => null)
      if (!response.ok || !body?.success) throw new Error(`request ${current + 1} returned ${response.status}`)
      durations.push(performance.now() - startedAt)
    } catch (error) {
      failures.push(error instanceof Error ? error.message : `request ${current + 1} failed`)
    }
  }
}

await Promise.all(Array.from({ length: concurrency }, worker))
durations.sort((left, right) => left - right)
const percentile = (ratio) => durations[Math.max(0, Math.ceil(durations.length * ratio) - 1)] || 0
const summary = {
  target: `${baseUrl}/api/auth/login`,
  requests: total,
  concurrency,
  success: durations.length,
  failed: failures.length,
  p50Ms: Number(percentile(0.5).toFixed(1)),
  p95Ms: Number(percentile(0.95).toFixed(1)),
  maxMs: Number(percentile(1).toFixed(1))
}

console.log(JSON.stringify(summary, null, 2))
if (failures.length) {
  console.error(failures.slice(0, 3).join('\n'))
  process.exitCode = 1
}
