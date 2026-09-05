import http from 'k6/http';

export const options = {
    scenarios: {
        load: {
            executor: 'ramping-arrival-rate',

            startRate: 0,
            timeUnit: '1s',

            preAllocatedVUs: 200,
            maxVUs: 300,

            stages: [
                { target: 600, duration: '5m' }, // 0 > 1000 RPS (15m)
                { target: 1000, duration: '15m' }, // 1000 RPS 유지 (15m)
            ],
        },
    },
};

export default function () {
    http.get('http://nginx/health/health-check');
}

// export default function () {
//     const orderId = (__ITER % 1000) + 1;
//
//     const orderStatus = Math.random() < 0.5
//         ? 'ORDERED'
//         : 'PAID';
//
//     const payload = JSON.stringify({
//         orderId: orderId,
//         orderStatus: orderStatus
//     });
//
//     const params = {
//         headers: {
//             'Content-Type': 'application/json',
//         },
//     };
//
//     http.post(
//         'http://nginx/order/update',
//         payload,
//         params
//     );
// }