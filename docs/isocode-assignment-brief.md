# IsoCode Labs Take-Home Assignment Brief

**Role**: Software Engineering Intern  
**Due Date**: 9 Nov 2026, 11:59 pm IST  
**Client**: Company A, Chikkamagaluru, Karnataka  
**Submission Requirement**: One ZIP file (up to 25 MB) containing:
1. Technical Design Document (`Technical-Design-Document.pdf` or Word)
2. `deployment.md`

---

## Client Problem Statement: Hill Resort's Direct-Booking Website with AI Enquiry Routing

### Client Background
Company A, Chikkamagaluru, Karnataka. Coffee-estate resort with 64 rooms and 12 villas (76 keys) and lawns for weddings and offsites. 140 staff, revenue ₹24–28 crore a year. Run by a General Manager (GM).

### Today
68% of room nights come through online travel agencies at 18–22% commission; only 12% are direct. The website is a brochure with a phone number. Event enquiries arrive by email, phone, social media and the GM's own WhatsApp. Rates live in Excel.

### The Problem
Assumptions: 76 keys × 365 × 62% occupancy = 17,199 room nights at ₹9,500. 68% via agencies = 11,695 nights = ₹11.11 crore, at 20% commission = ₹2.22 crore a year; moving 15% of that to direct saves ₹33.3 lakh. Of 90 event enquiries a month, 30% wait over a day for a reply, losing about 3 events a month at ₹2 lakh contribution: 36 × ₹2 lakh = ₹72 lakh. Total ₹1.05 crore a year.

### Users
Guests, the reservations and sales team, and the GM. The owner signs the deal.

### The Ask
We pay agencies a fifth of every room they sell, while many guests would book direct if our site let them. I want a fast, beautiful site where guests pick dates, see real prices and pay. Every enquiry, from any channel, should land in one place and reach the right person with a reply ready.

### Must Have
- Booking website: rooms with photos, availability, rate plans, prices with taxes, test-mode payment and confirmation.
- Admin for daily rates, website allotment, blackout dates and promo codes.
- One enquiry inbox for the web form, forwarded emails and WhatsApp (simulated in this build).
- AI router: classifies enquiries (room, wedding, offsite, complaint), extracts dates, guests and budget, assigns an owner and drafts a reply.
- Structured wedding and event enquiry form.
- Daily booking export for the reservation system.

### Core Build
Software Engineering candidates must ship this slice end to end, in about 6 to 8 screens:
- Room pages with availability and price
- Booking with test payment
- Rate and allotment admin
- Enquiry inbox with AI routing
- Roles: guest and reservations staff (+ GM)
- AI: enquiry classification and reply draft
- Hard part: allotment control so the site never overbooks.
- Mock payments, messages and outside systems, and seed from the sample data.
- The rest of MUST HAVE is optional depth.

### Out of Scope
- Replacing the reservation system or a channel manager.
- Loyalty programme.
- Restaurant bookings.

### AI or Automation
AI classifies, routes and drafts replies. When it is wrong:
- Low-confidence enquiries go to an unsorted lane the GM sees.
- Staff re-route in one click.
- Drafts are never sent automatically.
- Prices in drafts come from the rate table, not the AI.
- Complaints always alert the GM.

### Constraints
- The site sells only an admin-set allotment, so it cannot overbook agency stock.
- Passport details for foreign guests' Form C are taken at check-in, not online.
- Guest data is personal data under the DPDP Act 2023.
- Pages and photos must load fast on slow hill-station networks.
- Show GST at the rate for the tariff.

### Design
The client values good, minimal design: clean layouts, white space, a restrained palette and clear type, with nothing on screen that lacks a purpose. Design counts as much as features. The booking site sells the experience: large photos, a quiet layout and a booking flow of no more than three steps.

### Budget and Time
₹8 lakh, 7 weeks. Success: direct share from 12% to 20% of room nights in 12 months; median first reply under 30 minutes in working hours; zero website overbookings.

### Sample Data Table
| Enquiry ID | Message | Expected Category |
| :--- | :--- | :--- |
| **ENQ-4410** | Venue for my sister's wedding in Feb 2027, about 250 guests, 2 nights | Wedding |
| **ENQ-4411** | Is a villa free 14-16 Nov for 2 adults and a child? | Room booking |
| **ENQ-4412** | Team of 45 wants a January offsite with a conference hall | Corporate offsite |

---

## Submission Guidelines

### How to Submit
Put these two files in one `.zip` (up to 25 MB) and upload it:
1. **Technical Design Document (PDF or Word)**
   - The GitHub URL of your project, at the top. Also add the link to the live app if you deployed it.
   - The architecture and why you chose it.
   - The data model, the main flows and how the AI part works.
   - UI and design decisions: the key screens and why they look the way they do.
   - What you cut, and what you would do next.
2. **`deployment.md`**
   - How to run it locally, step by step.
   - How it is deployed, and the environment variables it needs (names only, never the values).
   - Demo login details for the reviewer.

### Rules
- Use only the information in the problem statement. Label every assumption in your TDD.
- Keep the commit history. We read it.
- Do not put secrets or real personal data in the repository.
- Keep the repository public until we have finished reviewing.
- You can resubmit until the deadline; we review your latest version.
