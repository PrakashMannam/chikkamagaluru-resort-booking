# Design Pack v2 - Patch

## Patch P1: GST Slab Lookup and Calculation Rule

The average nightly value used for the single slab lookup is:
`DiscountedPreTax / rooms / nights` (AFTER the promo discount).

**Reason:** Under GST rules, value of supply is the amount charged after discount.

GST is calculated as:
`GST = DiscountedPreTax x slab rate`

Where:
- Exactly **one** slab lookup is performed using the check-in date and the post-promo average nightly tariff (`DiscountedPreTax / rooms / nights`).
- Slab lookup is evaluated on a half-open interval:
  `tariff > min_tariff_inr AND (max_tariff_inr IS NULL OR tariff <= max_tariff_inr)`
  with `effective_from <= check-in date AND (effective_to IS NULL OR effective_to >= check-in date)`.
