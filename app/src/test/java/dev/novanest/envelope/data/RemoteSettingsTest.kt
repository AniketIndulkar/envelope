package dev.novanest.envelope.data

import dev.novanest.envelope.privacy.PiiType
import dev.novanest.envelope.privacy.TestRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteSettingsTest {
    private val bundled = TestRules.json

    @Test fun blankRemoteUsesBundled() =
        assertTrue(RemoteSettings.patterns("", bundled).find("NI QQ123456C").isNotEmpty())

    @Test fun invalidRemoteUsesBundled() =
        assertTrue(RemoteSettings.patterns("{oops", bundled).find("NI QQ123456C").isNotEmpty())

    @Test fun validRemoteWins() {
        val remote = """[{"type":"REFERENCE","regex":"ZZ\\d{4}"}]"""
        val r = RemoteSettings.patterns(remote, bundled)
        assertEquals(PiiType.REFERENCE, r.find("code ZZ1234").single().type)
        assertTrue(r.find("NI QQ123456C").isEmpty())
    }

    @Test fun emptyRemoteListUsesBundled() =
        assertTrue(RemoteSettings.patterns("[]", bundled).find("NI QQ123456C").isNotEmpty())
}
