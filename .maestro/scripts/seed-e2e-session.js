// Gray-box setup for the start-authenticated subflows (ADR-0002).
// Makes sure the E2E account exists through the dev-only seed-user route, then logs in
// through the real endpoint for a JWT. Runs on the host, so the backend is localhost:8080;
// the emulator reaches the same server as 10.0.2.2.
// Override with `maestro test -e E2E_EMAIL=... -e E2E_PASSWORD=... -e E2E_API_URL=... <flow>`,
// and pick a start screen with `-e E2E_ROUTE=marketplace` or a calling flow's runFlow env.

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
  throw new Error('Dev routes are not mounted: the running server was started without -DSCENT_ENV=dev -DDEV_ROUTES=true. Stop it, then run ' + startBackend);
}
if (seeded.status !== 200 && seeded.status !== 201) {
  throw new Error('seed-user failed: HTTP ' + seeded.status + ' ' + seeded.body);
}

var login = postJson('/api/v1/auth/login', { email: email, password: password });
if (login.status !== 200) {
  throw new Error('Login for the E2E account failed: HTTP ' + login.status + ' ' + login.body);
}

// Optional start screen for the debug launch hook (see E2eLaunchArguments.kt for the accepted
// routes). Blank means a normal start on Home; an unknown route also starts on Home and is logged.
output.e2eRoute = typeof E2E_ROUTE !== 'undefined' ? E2E_ROUTE : '';
// "true" makes Add photos return a bundled image instead of opening the system picker.
output.e2eFakeImages = typeof E2E_FAKE_IMAGES !== 'undefined' ? E2E_FAKE_IMAGES : '';
output.e2eEmail = email;
output.e2ePassword = password;
output.e2eToken = json(login.body).token;
