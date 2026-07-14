import { NextResponse } from 'next/server';
import nodemailer from 'nodemailer';

// Server-side only, so the SMTP credentials never reach the browser.
const { SMTP_SERVER, SMTP_PORT, SMTP_USERNAME, SMTP_TOKEN, CONTACT_TO, CONTACT_FROM } = process.env;

export async function POST(request: Request) {
  if (!SMTP_SERVER || !SMTP_PORT || !SMTP_USERNAME || !SMTP_TOKEN) {
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
    secure: port === 465, // 465 is implicit TLS; 587 upgrades via STARTTLS
    auth: { user: SMTP_USERNAME, pass: SMTP_TOKEN },
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

  return NextResponse.json({ ok: true });
}
