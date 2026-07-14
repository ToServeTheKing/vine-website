// Sends one test email through the configured SMTP server, then exits.
//   node --env-file=.env scripts/test-email.mjs [recipient]
// Verifies the connection first, so auth/TLS problems are reported distinctly
// from delivery problems.
import nodemailer from 'nodemailer';

const { SMTP_SERVER, SMTP_PORT, SMTP_USERNAME, SMTP_TOKEN, CONTACT_FROM } = process.env;
const to = process.argv[2] || process.env.CONTACT_TO;

const missing = ['SMTP_SERVER', 'SMTP_PORT', 'SMTP_USERNAME', 'SMTP_TOKEN'].filter((k) => !process.env[k]);
if (missing.length) {
  console.error(`Missing env vars: ${missing.join(', ')}`);
  console.error('Fill in .env (see .env.example), then re-run.');
  process.exit(1);
}
if (!to) {
  console.error('No recipient. Pass one as an argument or set CONTACT_TO.');
  process.exit(1);
}

const port = Number(SMTP_PORT);
const transporter = nodemailer.createTransport({
  host: SMTP_SERVER,
  port,
  secure: port === 465,
  auth: { user: SMTP_USERNAME, pass: SMTP_TOKEN },
});

console.log(`Connecting to ${SMTP_SERVER}:${port} (secure=${port === 465}) as ${SMTP_USERNAME}...`);
await transporter.verify();
console.log('SMTP connection and auth OK.');

const info = await transporter.sendMail({
  from: CONTACT_FROM || SMTP_USERNAME,
  to,
  subject: 'The Vine — contact form test',
  text: 'This is a test from the itsthevine.com contact form wiring. If you are reading this, SMTP works.',
});

console.log(`Sent to ${to}`);
console.log(`messageId: ${info.messageId}`);
if (info.rejected?.length) console.log(`rejected: ${info.rejected.join(', ')}`);
