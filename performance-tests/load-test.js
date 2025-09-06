import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics for trading-specific monitoring
export let orderPlacementRate = new Rate('order_placement_success');
export let orderProcessingTime = new Trend('order_processing_time');
export let orderFailures = new Counter('order_failures');
export let portfolioFetchTime = new Trend('portfolio_fetch_time');
export let riskCheckTime = new Trend('risk_check_time');

// Test configuration
export let options = {
  stages: [
    // Ramp-up phase
    { duration: '2m', target: 100 },   // Ramp up to 100 users over 2 minutes
    { duration: '5m', target: 500 },   // Stay at 500 users for 5 minutes
    { duration: '3m', target: 1000 },  // Ramp up to 1000 users over 3 minutes
    { duration: '10m', target: 1000 }, // Stay at 1000 users for 10 minutes (peak load)
    { duration: '5m', target: 2000 },  // Stress test: ramp up to 2000 users
    { duration: '5m', target: 2000 },  // Stay at 2000 users for 5 minutes
    { duration: '3m', target: 0 },     // Ramp down to 0 users
  ],
  thresholds: {
    // Trading Service SLA Requirements
    'http_req_duration': ['p(95)<200'], // 95% of requests must complete within 200ms
    'http_req_duration{name:order_placement}': ['p(99)<50'], // 99% of orders within 50ms
    'http_req_duration{name:portfolio_fetch}': ['p(95)<100'], // 95% of portfolio fetches within 100ms
    'http_req_failed': ['rate<0.01'], // Less than 1% failure rate
    'order_placement_success': ['rate>0.99'], // 99% order success rate
    'order_processing_time': ['p(95)<50'], // 95% of orders processed within 50ms
  },
};

// Base URL configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8083';
const API_VERSION = '/api/v2';

// Test data generation
function generateOrderRequest() {
  const symbols = ['RELIANCE', 'INFY', 'TCS', 'HDFC', 'ICICIBANK', 'SBIN', 'BAJFINANCE'];
  const sides = ['BUY', 'SELL'];
  const orderTypes = ['MARKET', 'LIMIT'];
  
  return {
    symbol: symbols[Math.floor(Math.random() * symbols.length)],
    exchange: 'NSE',
    orderType: orderTypes[Math.floor(Math.random() * orderTypes.length)],
    side: sides[Math.floor(Math.random() * sides.length)],
    quantity: Math.floor(Math.random() * 100) + 1,
    limitPrice: orderTypes[0] === 'LIMIT' ? 2000 + Math.random() * 500 : null,
    timeInForce: 'DAY'
  };
}

// Authentication setup
function authenticate() {
  // In production, this would use actual authentication
  // For testing, we'll simulate with a mock JWT token
  return 'Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMDAxIiwibmFtZSI6IlRlc3QgVXNlciIsInJvbGUiOiJUUkFERVIiLCJpYXQiOjE2MzQ1Njc4OTB9.test';
}

// Main test scenarios
export default function() {
  const authToken = authenticate();
  
  const headers = {
    'Authorization': authToken,
    'Content-Type': 'application/json',
    'Accept': 'application/json'
  };

  group('Trading Service Load Test', function() {
    
    // Scenario 1: Order Placement (40% of traffic)
    if (Math.random() < 0.4) {
      group('Order Placement', function() {
        const orderRequest = generateOrderRequest();
        
        const orderStartTime = Date.now();
        const response = http.post(
          `${BASE_URL}${API_VERSION}/orders`,
          JSON.stringify(orderRequest),
          { 
            headers: headers,
            tags: { name: 'order_placement' }
          }
        );
        
        const orderEndTime = Date.now();
        const processingTime = orderEndTime - orderStartTime;
        
        // Validate order placement
        const orderSuccess = check(response, {
          'Order placement status is 201': (r) => r.status === 201,
          'Order response has orderId': (r) => JSON.parse(r.body).orderId !== undefined,
          'Order processing time < 50ms': () => processingTime < 50,
        });
        
        // Record metrics
        orderPlacementRate.add(orderSuccess);
        orderProcessingTime.add(processingTime);
        
        if (!orderSuccess) {
          orderFailures.add(1);
        }
        
        // Simulate user think time
        sleep(0.1 + Math.random() * 0.2); // 100-300ms think time
      });
    }
    
    // Scenario 2: Portfolio Fetch (30% of traffic)
    else if (Math.random() < 0.7) {
      group('Portfolio Management', function() {
        const portfolioStartTime = Date.now();
        const response = http.get(
          `${BASE_URL}${API_VERSION}/portfolio`,
          { 
            headers: headers,
            tags: { name: 'portfolio_fetch' }
          }
        );
        
        const portfolioEndTime = Date.now();
        const fetchTime = portfolioEndTime - portfolioStartTime;
        
        check(response, {
          'Portfolio fetch status is 200': (r) => r.status === 200,
          'Portfolio has totalValue': (r) => JSON.parse(r.body).totalValue !== undefined,
          'Portfolio fetch time < 100ms': () => fetchTime < 100,
        });
        
        portfolioFetchTime.add(fetchTime);
        
        sleep(0.2 + Math.random() * 0.3); // 200-500ms think time
      });
    }
    
    // Scenario 3: Order History (20% of traffic)
    else if (Math.random() < 0.9) {
      group('Order History', function() {
        const response = http.get(
          `${BASE_URL}${API_VERSION}/orders?page=0&size=20`,
          { 
            headers: headers,
            tags: { name: 'order_history' }
          }
        );
        
        check(response, {
          'Order history status is 200': (r) => r.status === 200,
          'Order history has orders array': (r) => Array.isArray(JSON.parse(r.body).orders),
        });
        
        sleep(0.3 + Math.random() * 0.5); // 300-800ms think time
      });
    }
    
    // Scenario 4: Market Data (10% of traffic)
    else {
      group('Market Data', function() {
        const symbols = ['RELIANCE', 'INFY', 'TCS'];
        const symbol = symbols[Math.floor(Math.random() * symbols.length)];
        
        const response = http.get(
          `${BASE_URL}${API_VERSION}/market/quote/${symbol}`,
          { 
            headers: headers,
            tags: { name: 'market_data' }
          }
        );
        
        check(response, {
          'Market data status is 200': (r) => r.status === 200,
          'Market data has lastPrice': (r) => JSON.parse(r.body).lastPrice !== undefined,
        });
        
        sleep(0.05 + Math.random() * 0.1); // 50-150ms think time (faster for market data)
      });
    }
  });
  
  // Health check every 10 iterations
  if (__ITER % 10 === 0) {
    group('Health Check', function() {
      const response = http.get(
        `${BASE_URL}/actuator/health`,
        { tags: { name: 'health_check' } }
      );
      
      check(response, {
        'Health check status is 200': (r) => r.status === 200,
        'Service is UP': (r) => JSON.parse(r.body).status === 'UP',
      });
    });
  }
}

// Stress test scenarios
export function stressTest() {
  const authToken = authenticate();
  
  const headers = {
    'Authorization': authToken,
    'Content-Type': 'application/json',
  };

  // High-frequency order placement
  for (let i = 0; i < 10; i++) {
    const orderRequest = generateOrderRequest();
    
    const response = http.post(
      `${BASE_URL}${API_VERSION}/orders`,
      JSON.stringify(orderRequest),
      { headers: headers }
    );
    
    check(response, {
      'Stress test order placement success': (r) => r.status === 201,
    });
    
    sleep(0.01); // 10ms between orders (very aggressive)
  }
}

// Spike test for sudden load increases
export function spikeTest() {
  // Options for spike test
  options.stages = [
    { duration: '30s', target: 10 },   // Low load
    { duration: '10s', target: 2000 }, // Sudden spike to 2000 users
    { duration: '1m', target: 2000 },  // Hold spike load
    { duration: '30s', target: 10 },   // Drop back to low load
  ];
}

// Volume test for sustained high load
export function volumeTest() {
  options.stages = [
    { duration: '5m', target: 1500 },  // Ramp up to 1500 users
    { duration: '30m', target: 1500 }, // Hold for 30 minutes
    { duration: '5m', target: 0 },     // Ramp down
  ];
}

// Custom setup for each VU (Virtual User)
export function setup() {
  console.log('Starting TradeMaster Trading Service Performance Test');
  console.log(`Target URL: ${BASE_URL}`);
  console.log('Test Scenarios:');
  console.log('  - Order Placement (40%)');
  console.log('  - Portfolio Management (30%)');
  console.log('  - Order History (20%)');
  console.log('  - Market Data (10%)');
  console.log('SLA Targets:');
  console.log('  - 95% requests < 200ms');
  console.log('  - 99% order placement < 50ms');
  console.log('  - <1% error rate');
  
  return { startTime: Date.now() };
}

// Cleanup after test
export function teardown(data) {
  const testDuration = (Date.now() - data.startTime) / 1000;
  console.log(`Performance test completed in ${testDuration}s`);
}