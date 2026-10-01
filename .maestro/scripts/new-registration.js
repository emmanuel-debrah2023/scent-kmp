// Unique credentials for the register step of the auth flow, so every run creates a fresh
// account rather than colliding with the last one. Each run leaves one account behind in
// the local database.
var stamp = String(new Date().getTime());

output.regUsername = 'e2e_' + stamp;
output.regDisplayName = 'E2E ' + stamp;
output.regEmail = 'e2e+' + stamp + '@scent.dev';
output.regPassword = 'ScentE2e-Passw0rd';
