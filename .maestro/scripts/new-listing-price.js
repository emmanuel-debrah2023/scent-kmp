// Unique price for the listing the create-listing flow publishes, so the final Listings-tab
// assertion can only be satisfied by the listing this run just published. clearState does not
// reset the server, so the E2E account keeps every listing earlier runs created. A whole-pound
// price of three digits (200 to 989) keeps the row text predictable.
output.listingPrice = String(200 + (new Date().getTime() % 790));
