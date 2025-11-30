import http from 'k6/http';
import { check, sleep } from 'k6';

export let options = {
    stages: [
        { duration: '10s', target: 50 },
        { duration: '20s', target: 50 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        'http_req_duration': ['p(95)<500'],
        'http_req_failed': ['rate<0.01'],
    },
};

export default function () {
    console.log('About to make a request');
    const res = http.post('http://localhost:8080/api/v1/jobs/recommendations');

    check(res, {
        'Job launch successful': (r) => r.status === 200,
        'Response body is correct': (r) => r.body === 'Recommendation generation job started.',
    });

    if (res.status !== 200) {
        console.error(`❌ Job launch failed: ${res.status} ${res.body}`);
    } else {
        console.log(`✅ Job launch successful: ${res.body}`);
    }

    sleep(1);
}
