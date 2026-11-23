"""Generates app/src/main/assets/redaction_patterns.json (also the Remote Config value shape)."""
import json, pathlib
R = [
    ("NI_NUMBER", r"\b[A-Z]{2}\s?\d{2}\s?\d{2}\s?\d{2}\s?[A-D]\b", 0, None),
    ("ACCOUNT",   r"(?i)sort\s*code[^\d]{0,20}(\d{2}[-\s]?\d{2}[-\s]?\d{2})\b", 1, None),
    ("ACCOUNT",   r"(?i)account\s*(?:no\.?|number)[^\d]{0,20}(\d{8})\b", 1, None),
    ("ACCOUNT",   r"\bGB\d{2}\s?[A-Z]{4}(?:\s?\d{4}){3}\s?\d{2}\b", 0, None),
    ("ACCOUNT",   r"\b(?:\d[ -]?){12,18}\d\b", 0, "luhn"),
    ("POSTCODE",  r"\b[A-Z]{1,2}\d[A-Z\d]?\s?\d[A-Z]{2}\b", 0, None),
    ("PHONE",     r"(?:\+44\s?(?:\(0\)\s?)?|\b0)\d(?:[\s-]?\d){8,9}\b", 0, None),
    ("EMAIL",     r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", 0, None),
    ("REFERENCE", r"(?i)(?:\bUTR\b|unique taxpayer reference)[^\d]{0,20}(\d{5}\s?\d{5})\b", 1, None),
    ("REFERENCE", r"\b(\d{3}\s?\d{3}\s?\d{4})\b", 1, "nhs_mod11"),
    ("DOB",       r"(?i)(?:date of birth|\bDOB\b)[:\s]{0,5}(\d{1,2}[/.\- ][0-9A-Za-z]{1,9}[/.\- ]\d{2,4})", 1, None),
    ("REFERENCE", r"(?i)(?:your reference|customer (?:number|ref(?:erence)?)|council tax (?:account|reference)(?: number)?)[^\w]{0,5}([A-Z0-9]{6,14})\b", 1, None),
]
out = [dict(type=t, regex=rx, group=g, **({"validator": v} if v else {})) for t, rx, g, v in R]
p = pathlib.Path(__file__).resolve().parent.parent / "app/src/main/assets/redaction_patterns.json"
p.write_text(json.dumps(out, indent=2) + "\n")
print(f"wrote {len(out)} rules to {p}")
