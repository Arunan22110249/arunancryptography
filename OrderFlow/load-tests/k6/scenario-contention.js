import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 100,
  iterations: 1000,
  thresholds: {
    http_req_failed: ['rate<0.1'],
  },
};

export default function () {
  const payload = JSON.stringify({
    item: { productId: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', quantity: 1 },
  });

  const headers = {
    'Content-Type': 'application/json',
    'Idempotency-Key': `order-${__VU}-${__ITER}`,
  };

  const res = http.post('http://localhost:8080/api/v1/orders', payload, { headers });
  check(res, {
    'status is 2xx or 409': (r) => r.status >= 200 && r.status < 500,
  });
  sleep(0.1);
}
