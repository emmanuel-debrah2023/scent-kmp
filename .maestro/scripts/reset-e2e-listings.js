// Clears the E2E account's listings and photos on the server (ADR-0002).
// clearState resets the app, not the backend, so flows that publish listings call this
// to stop the marketplace feed and the profile Listings tab filling up across runs.
// Uses the dev-only reset-listings route, which refuses accounts other than the E2E ones.
// Override with `maestro test -e E2E_EMAIL=... -e E2E_API_URL=... <flow>`.

var apiUrl = typeof E2E_API_URL !== 'undefined' ? E2E_API_URL : 'http://localhost:8080';
var email = typeof E2E_EMAIL !== 'undefined' ? E2E_EMAIL : 'e2e@scent.dev';
var startBackend = './scripts/e2e-up.sh';

var response;
try {
  response = http.post(apiUrl + '/api/v1/dev/reset-listings', {
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: email }),
  });
} catch (e) {
  throw new Error('Could not reach ' + apiUrl + '. Start the backend with ' + startBackend + ' (' + e + ')');
}

if (response.status === 404 && !response.body) {
  throw new Error('reset-listings is not mounted: the running server predates this route or has no dev routes. Restart it: ./scripts/e2e-up.sh --down && ./scripts/e2e-up.sh');
}

if (response.status === 404) {
  // The account does not exist yet: onFlowStart runs before start-authenticated seeds it.
  output.e2eListingsRemoved = 0;
} else if (response.status !== 200) {
  throw new Error('reset-listings failed: HTTP ' + response.status + ' ' + response.body);
} else {
  output.e2eListingsRemoved = json(response.body).removed;
}
