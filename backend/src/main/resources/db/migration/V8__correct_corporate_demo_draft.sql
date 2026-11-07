-- V8__correct_corporate_demo_draft.sql
-- Idempotent correction for legacy seeded corporate demo enquiry draft with ungrounded facility claims.
-- Only applies if:
--   1. Enquiry code matches ENQ-DEMO-CORP1
--   2. Draft status is strictly DRAFT_PENDING_REVIEW (preserves staff-edited or sent drafts)
--   3. Draft body matches known legacy unsupported text

UPDATE enquiry_drafts d
JOIN enquiries e ON d.enquiry_id = e.id
SET d.draft_body = 'Dear Ms. Sen,\n\nThank you for considering our estate for your annual leadership offsite. Regarding meeting space, accommodation, and group activities for your team, our corporate specialist will confirm available facilities, room allotments, and event options with you directly.\n\nOur corporate specialist Meera Nair will contact you today with comprehensive proposal options.\n\nWarm regards,\nCorporate Events Team'
WHERE e.enquiry_code = 'ENQ-DEMO-CORP1'
  AND d.status = 'DRAFT_PENDING_REVIEW'
  AND (
    d.draft_body LIKE '%AV projection%'
    OR d.draft_body LIKE '%panoramic plantation views%'
    OR d.draft_body LIKE '%guided cupping sessions%'
  );
