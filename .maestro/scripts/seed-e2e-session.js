// Gray-box setup for the start-authenticated subflows (ADR-0002).
// Makes sure the E2E account exists through the dev-only seed-user route, then logs in
// through the real endpoint for a JWT. Runs on the host, so the backend is localhost:8080;
// the emulator reaches the same server as 10.0.2.2.
// Override with `maestro test -e E2E_EMAIL=... -e E2E_PASSWORD=... -e E2E_API_URL=... <flow>`.

var apiUrl = typeof E2E_API_URL !== 'undefined' ? E2E_API_URL : 'http://localhost:8080';
var email = typeof E2E_EMAIL !== 'undefined' ? E2E_EMAIL : 'e2e@scent.dev';
var password = typeof E2E_PASSWORD !== 'undefined' ? E2E_PASSWORD : 'ScentE2e-Passw0rd';
var startBackend = './scripts/e2e-up.sh';

function postJson(path, body) {
  try {
    return http.post(apiUrl + path, {
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    });
  } catch (e) {
    throw new Error('Could not reach ' + apiUrl + '. Start the backend with ' + startBackend + ' (' + e + ')');
  }
}

var seeded = postJson('/api/v1/dev/seed-user', {
  email: email,
  username: 'scent_e2e',
  password: password,
  displayName: 'Scent E2E',
});
if (seeded.status === 404) {
  throw new Error('Dev routes are not mounted: the running server was started without -DSTREAM_PROVIDER=fake. Stop it, then run ' + startBackend);
}
if (seeded.status !== 200 && seeded.status !== 201) {
  throw new Error('seed-user failed: HTTP ' + seeded.status + ' ' + seeded.body);
}

var login = postJson('/api/v1/auth/login', { email: email, password: password });
if (login.status !== 200) {
  throw new Error('Login for the E2E account failed: HTTP ' + login.status + ' ' + login.body);
}

output.e2eEmail = email;
output.e2ePassword = password;
output.e2eToken = json(login.body).token;
