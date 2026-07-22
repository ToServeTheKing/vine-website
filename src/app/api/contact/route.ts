import { NextResponse } from 'next/server';
import nodemailer from 'nodemailer';

// Server-side only, so the SMTP credentials never reach the browser.
const { SMTP_SERVER, SMTP_PORT, SMTP_USERNAME, SMTP_TOKEN, CONTACT_TO, CONTACT_FROM } = process.env;

// Optional automation hub (n8n). When set, we also POST the enquiry here so it can
// send the customer auto-reply and (later) create a CRM lead / task. Best-effort:
// the direct email above is the reliable path, so a slow or down hub never blocks
// — or fails — the contact form.
const CONTACT_HUB_URL = process.env.CONTACT_HUB_URL;

function notifyHub(payload: { name: string; email: string; message: string }) {
  if (!CONTACT_HUB_URL) return;
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 4000);
  fetch(CONTACT_HUB_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
    signal: controller.signal,
  })
    .catch((err) => console.error('contact: hub notify failed (non-fatal)', err))
    .finally(() => clearTimeout(timeout));
}

export async function POST(request: Request) {
  if (!SMTP_SERVER || !SMTP_PORT) {
    console.error('contact: SMTP env vars missing');
    return NextResponse.json({ error: 'Email is not configured.' }, { status: 500 });
  }

  let body: { name?: string; email?: string; message?: string };
  try {
    body = await request.json();
  } catch {
    return NextResponse.json({ error: 'Invalid request.' }, { status: 400 });
  }

  const name = body.name?.trim();
  const email = body.email?.trim();
  const message = body.message?.trim();

  // Trust boundary: validate before handing anything to the mailer.
  if (!name || !email || !message) {
    return NextResponse.json({ error: 'Please fill in every field.' }, { status: 400 });
  }
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return NextResponse.json({ error: 'That email address does not look right.' }, { status: 400 });
  }
  if (message.length > 5000) {
    return NextResponse.json({ error: 'That message is too long.' }, { status: 400 });
  }

  const port = Number(SMTP_PORT);
  const transporter = nodemailer.createTransport({
    host: SMTP_SERVER,
    port,
    secure: port === 465, // 465 is implicit TLS; 587/other upgrade via STARTTLS
    auth: SMTP_USERNAME && SMTP_TOKEN ? { user: SMTP_USERNAME, pass: SMTP_TOKEN } : undefined,
    // local Proton Bridge / SMTP relay presents a self-signed cert (CN=127.0.0.1) — trust it
    tls: { rejectUnauthorized: false },
  });

  try {
    await transporter.sendMail({
      from: CONTACT_FROM || SMTP_USERNAME,
      to: CONTACT_TO || 'morissa@itsthevine.com',
      replyTo: `${name} <${email}>`, // so hitting reply answers the customer
      subject: `Website enquiry from ${name}`,
      text: `${message}\n\nFrom: ${name} <${email}>`,
    });
  } catch (err) {
    console.error('contact: send failed', err);
    return NextResponse.json({ error: 'Could not send the message.' }, { status: 502 });
  }

  // Email delivered — fan out to the automation hub (fire-and-forget).
  notifyHub({ name, email, message });

  return NextResponse.json({ ok: true });
}
