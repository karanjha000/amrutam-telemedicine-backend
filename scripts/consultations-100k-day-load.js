import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://host.docker.internal:8080';
const TARGET_PER_DAY = Number(__ENV.TARGET_PER_DAY || 100000);
const TARGET_RPS = TARGET_PER_DAY / 86400;
const SCENARIO_RATE = Math.max(1, Math.round(Math.max(TARGET_RPS, 1) * 2));
const TEST_DURATION = __ENV.TEST_DURATION || '1m';

const loginSuccess = new Rate('login_success');
const bookingSuccess = new Rate('consultation_booking_success');
const bookingLatency = new Trend('booking_latency_ms');

const KNOWN_DOCTOR_ID = 'd0ff4642-d772-48ea-9804-e8955ab9e061';

function randomSuffix(prefix) {
  return `${prefix}-${Math.random().toString(36).slice(2, 10)}-${Date.now().toString(36)}`;
}

export const options = {
  scenarios: {
    consultation_target: {
      executor: 'constant-arrival-rate',
      rate: SCENARIO_RATE,
      timeUnit: '1s',
      duration: TEST_DURATION,
      preAllocatedVUs: 12,
      maxVUs: 50,
      gracefulStop: '10s'
    }
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    login_success: ['rate>0.99'],
    consultation_booking_success: ['rate>0.95'],
    http_req_duration: ['p(95)<800']
  }
};

export function setup() {
  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: 'admin', password: 'admin123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const adminToken = loginRes.json('data.token');

  check(loginRes, {
    'admin login ok': (r) => r.status === 200 && !!adminToken
  });

  const doctorId = KNOWN_DOCTOR_ID;
  return { token: adminToken, doctorId };
}

export default function (data) {
  const adminToken = data.token;
  const doctorId = data.doctorId || KNOWN_DOCTOR_ID;
  const slotStart = new Date(Date.now() + 1000 + __VU * 5000 + Math.random() * 1000).toISOString();
  const slotEnd = new Date(new Date(slotStart).getTime() + 3600000).toISOString();

  const slotRes = http.post(
    `${BASE_URL}/api/v1/doctors/${doctorId}/availability`,
    JSON.stringify({ startAt: slotStart, endAt: slotEnd }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${adminToken}`
      }
    }
  );

  if (slotRes.status !== 200 && slotRes.status !== 201) {
    console.log(`slot create failed: ${slotRes.status} ${slotRes.body}`);
    return;
  }

  const slotId = slotRes.json('data.id');
  if (!slotId) {
    console.log(`slot id missing: ${slotRes.body}`);
    return;
  }

  const patientUsername = randomSuffix('ptload');
  const patientEmail = `${patientUsername}@example.com`;

  const registerRes = http.post(
    `${BASE_URL}/api/v1/auth/register`,
    JSON.stringify({
      username: patientUsername,
      password: 'StrongPass123!',
      email: patientEmail,
      displayName: 'Load Test Patient',
      phone: '+91-9000000000'
    }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (registerRes.status !== 200 && registerRes.status !== 201 && registerRes.status !== 409) {
    console.log(`register failed: ${registerRes.status} ${registerRes.body}`);
    return;
  }

  const loginRes = http.post(
    `${BASE_URL}/api/v1/auth/login`,
    JSON.stringify({ username: patientUsername, password: 'StrongPass123!' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginOk = check(loginRes, {
    'patient login ok': (r) => r.status === 200 && !!r.json('data.token')
  });
  loginSuccess.add(loginOk ? 1 : 0);

  if (!loginOk) {
    return;
  }

  const token = loginRes.json('data.token');
  const bookingStart = Date.now();
  const bookingRes = http.post(
    `${BASE_URL}/api/v1/consultations/booking`,
    JSON.stringify({
      doctorId: doctorId,
      slotId: slotId,
      paymentMethod: 'card',
      paymentToken: 'tok_visa_123'
    }),
    {
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token}`,
        'X-Idempotency-Key': `${__VU}-${__ITER}-${Date.now()}`
      }
    }
  );

  bookingLatency.add(Date.now() - bookingStart);
  const bookingOk = check(bookingRes, {
    'booking status ok': (r) => r.status === 200 || r.status === 201
  });
  bookingSuccess.add(bookingOk ? 1 : 0);

  if (!bookingOk) {
    console.log(`booking failed: ${bookingRes.status} ${bookingRes.body}`);
  }

  sleep(0.2);
}
