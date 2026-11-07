-- V7__correct_seeded_enquiry_drafts.sql
-- Idempotent correction for legacy seeded demo enquiry drafts with ungrounded claims.
-- Only applies if:
--   1. Enquiry code matches ENQ-DEMO-ROOM1 or ENQ-DEMO-WED1
--   2. Draft status is strictly DRAFT_PENDING_REVIEW (preserves staff-edited or sent drafts)
--   3. Draft body matches known legacy unsafe text

UPDATE enquiry_drafts d
JOIN enquiries e ON d.enquiry_id = e.id
SET d.draft_body = 'Dear Dr. Swaminathan,\n\nThank you for reaching out to us regarding Private Pool Villa availability for your requested dates (Nov 20–22, 2026). Please note that general enquiries do not hold room allotment until a reservation is formally created.\n\nOur reservations team will confirm current rate plans and availability with you shortly.\n\nWarm regards,\nAnanya Hegde\nReservations Team'
WHERE e.enquiry_code = 'ENQ-DEMO-ROOM1'
  AND d.status = 'DRAFT_PENDING_REVIEW'
  AND d.draft_body LIKE '%We have held a Private Pool Villa%';

UPDATE enquiry_drafts d
JOIN enquiries e ON d.enquiry_id = e.id
SET d.draft_body = 'Dear Sunil & Radhika,\n\nCongratulations on your upcoming wedding celebration! Our coffee estate offers scenic banquet lawn spaces and bespoke catering for events. Regarding accommodation for your requested dates in December 2026, room availability and packages require personalized coordinator review.\n\nOur wedding coordinator Karthik Urs will connect with you shortly with available dates and customized packages.\n\nWarm regards,\nEstate Weddings Team'
WHERE e.enquiry_code = 'ENQ-DEMO-WED1'
  AND d.status = 'DRAFT_PENDING_REVIEW'
  AND d.draft_body LIKE '%30 boutique rooms for 120 guests%';
