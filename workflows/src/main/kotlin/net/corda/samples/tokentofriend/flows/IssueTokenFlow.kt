package net.corda.samples.tokentofriend.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.of
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.AnonymousParty
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.utilities.ProgressTracker
import net.corda.samples.tokentofriend.states.CustomTokenState
import java.util.*

// *********
// * Flows *
// *********
@StartableByRPC
class IssueTokenFlow(val uuid: String, val amount: Long, val recipient: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        /* Get a reference of own identity */
        val issuer = ourIdentity

        /* Fetch the house state from the vault using the vault query */
        val inputCriteria = QueryCriteria.LinearStateQueryCriteria(uuid = listOf(UUID.fromString(uuid)),status = Vault.StateStatus.UNCONSUMED)
        val customTokenState = serviceHub.vaultService.queryBy(CustomTokenState::class.java, criteria = inputCriteria).states.single().state.data

        val account = accountService.accountInfo(recipient)[0].state.data
        val holder = retrieveAccountKey(account)

        val issuedToken = amount of customTokenState.toPointer(customTokenState.javaClass) issuedBy issuer heldBy holder

        subFlow(IssueTokens(listOf(issuedToken)))

        return "Issued $amount of tokens to $recipient"
    }

    @Suspendable
    private fun retrieveAccountKey(accountInfo: AccountInfo): AnonymousParty {
        val accountKeys = accountService.accountKeys(accountInfo.identifier.id)
        return if (accountKeys.isEmpty()) {
            subFlow(RequestKeyForAccount(accountInfo))
        } else {
            AnonymousParty(accountKeys.first())
        }
    }
}


