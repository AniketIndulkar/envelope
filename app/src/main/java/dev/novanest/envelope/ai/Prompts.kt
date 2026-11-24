package dev.novanest.envelope.ai

/**
 * Prompt templates. Placeholders: {text}, {language}, {today}.
 * Defaults ship in the app; Remote Config (prompt_redact / prompt_quickcheck / prompt_explain) can override.
 */
class Prompts(
    private val redactTemplate: String = REDACT,
    private val quickCheckTemplate: String = QUICK_CHECK,
    private val explainTemplate: String = EXPLAIN,
) {
    fun redact(chunk: String) = redactTemplate.replace("{text}", chunk)
    fun quickCheck(redacted: String, today: String) =
        quickCheckTemplate.replace("{today}", today).replace("{text}", redacted)
    fun explain(redacted: String, language: String, today: String) =
        explainTemplate.replace("{today}", today).replace("{language}", language).replace("{text}", redacted)

    companion object {
        val REDACT = """
            You find personal details in a UK letter so they can be hidden before the letter is shared.
            Return JSON only, in this exact shape: {"entities":[{"text":"...","type":"..."}]}
            type is one of:
            NAME - a person's name (not an organisation)
            ADDRESS - a person's home or postal address, including each address line
            REFERENCE - an account, customer, case, policy, tenancy or reference number about the person
            PHONE - a phone number
            EMAIL - an email address
            Rules: copy "text" exactly as it appears in the letter. Do not include organisation names, dates or money amounts. If there are none, return {"entities":[]}.

            Letter:
            {text}
        """.trimIndent()

        val QUICK_CHECK = """
            Classify this UK letter. Personal details have been replaced with tokens like [NAME_1]; keep them as they are.
            Today is {today}.
            Return JSON only, in this exact shape:
            {"sender":"organisation that sent it","category":"TAX|COUNCIL|HEALTH|BANK|HOUSING|BENEFITS|OTHER","urgency":"LOW|MEDIUM|HIGH","deadline":"yyyy-MM-dd or null","oneLine":"what the person needs to do, starting with a verb, at most 12 words"}
            category: TAX only for HMRC; COUNCIL for anything from a council, including council tax; HOUSING for landlords and letting agents.
            oneLine examples: "Pay £286.40 overdue council tax by 25 November", "Confirm your hospital appointment by 2 December". Never start with "This letter".
            urgency is HIGH if there is a deadline within 14 days, a penalty, or legal action; LOW if it is information only.

            Letter:
            {text}
        """.trimIndent()

        val EXPLAIN = """
            You help people in the UK understand official letters (HMRC, councils, the NHS, banks, landlords).
            Personal details have been replaced with tokens like [NAME_1], [ADDRESS_1], [NI_NUMBER_1]. Keep every token exactly as written, brackets included. Never guess what a token stands for.
            Today is {today}.
            Write for a reading age of about 11: short sentences, no jargon. Explain, do not give legal or financial advice.
            Write summary, whatItMeans, actions, consequences and redFlags in {language}. Write draftReply in English, from the person to the sender, signed with the person's name token and using their address token.
            draftReply must be complete and ready to send: no placeholders such as [insert ...] or [your message here]. If the person has a choice, write the reply most people in their position would need (for example confirming they will pay, asking for a payment plan, or asking to rearrange an appointment). If the letter looks like a scam, the draftReply must not be addressed to the sender; instead write a short note to the real organisation's official contact asking them to confirm whether the letter is genuine. For a likely scam, set deadline to null and give no action a \"by\" date: the scammer's deadline is a pressure tactic, not a real one.
            redFlags: list signs this letter may be a scam (pressure, unusual payment methods such as gift cards or crypto, sender details that don't match, requests for passwords or PINs). Use an empty list if there are none.
            helpLinks: 1 to 3 official UK sources (GOV.UK, NHS, Citizens Advice, Shelter, the sender's official site).
            Return JSON only, in this exact shape:
            {"summary":"2 sentences","whatItMeans":"short paragraph","actions":[{"step":"...","by":"yyyy-MM-dd or null"}],"deadline":"yyyy-MM-dd or null","consequences":"what happens if they do nothing","redFlags":[],"draftReply":"...","helpLinks":[{"label":"...","url":"https://..."}]}

            Letter:
            {text}
        """.trimIndent()
    }
}
